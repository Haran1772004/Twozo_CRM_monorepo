package com.tlc.crm.core.field.fieldAudit.resolver;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.sql.resource.OMCRMTERRITORY;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;

import com.tlc.crm.entity.features.api.tag.manager.TagManager;
import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.crm.entity.fieldaudit.FieldAuditHandler;
import com.tlc.crm.entity.fieldaudit.manager.FieldTrackingConfigManager;
import com.tlc.crm.entity.fieldaudit.models.FieldTrackingConfig;
import com.tlc.crm.entity.manager.field.type.AddressManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.tlc.crm.core.api.summaryInfo.model.SummaryInfo;

/**
 * <p>
 * Abstract base class for field edit history resolvers. Contains common logic shared across
 * Contact, Company, and Deal field history resolution.
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
abstract class AbstractFieldAuditResolver {

    protected final FieldAuditHandler fieldAuditHandler;
    protected final FieldTrackingConfigManager fieldTrackingConfigManager;
    protected final FieldService fieldService;
    protected final UserService userService;
    protected final OptionService optionService;
    protected final TagManager tagManager;
    protected final AddressManager addressManager;

    protected AbstractFieldAuditResolver() {
        this.fieldAuditHandler = FieldAuditHandler.getInstance();
        this.fieldTrackingConfigManager = FieldTrackingConfigManager.getInstance();
        this.fieldService = FieldService.getSecureInstance();
        this.userService = UserService.getInstance();
        this.optionService = OptionService.getInstance();
        this.tagManager = TagManager.getInstance();
        this.addressManager = AddressManager.getInstance();
    }

    /**
     * Gets the org data store for the given org id.
     */
    protected OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * Resolves the territory name for the given territory id.
     */
    protected String resolveTerritoryName(final Long orgId, final Long territoryId) {
        if (Objects.isNull(territoryId)) {
            return null;
        }
        final Table territoryTable = Table.get(OMCRMTERRITORY.TABLE);
        final Row row = orgDataStore(orgId).get(territoryTable, territoryId, List.of(territoryTable.getColumn(OMCRMTERRITORY.NAME)));

        return Objects.isNull(row) ? null : row.get(OMCRMTERRITORY.NAME);
    }

    /**
     * Resolves the option name for the given option object, fetching from the database if necessary.
     */
    protected String resolveOptionName(final Long orgId, final Option option) {
        if (Objects.isNull(option)) {
            return null;
        }

        if (Objects.nonNull(option.getOption())) {
            return option.getOption();
        }

        if (Objects.isNull(option.id()) || Objects.isNull(option.getFieldId())) {
            return null;
        }
        return optionService.get(orgId, option.getFieldId(), option.id()).getOption();
    }

    /**
     * Resolves a single custom field value from its raw object representation.
     */
    protected String resolveCustomFieldValue(final Long orgId, final FieldInfo field, final Object rawValue, final StoreType storeType) {
        if (StoreType.isModelStore(storeType)) {

            return switch (storeType) {
                case DROPDOWN -> {
                    if (rawValue instanceof final Option option && Objects.isNull(option.getOption()) && Objects.nonNull(option.id())) {
                        yield optionService.get(orgId, field.id(), option.id()).getOption();
                    }
                    yield null;
                }
                case OWNER -> {
                    if (rawValue instanceof final CrmUser user && Objects.isNull(user.getFullName()) && Objects.nonNull(user.id())) {
                        yield userService.getUser(orgId, user.id()).getFullName();
                    }
                    yield null;
                }
                case COMPANY -> {
                    if (rawValue instanceof final Long companyId) {
                        yield CompanyManager.getInstance().getName(orgId, companyId);
                    }
                    yield null;
                }
                case CONTACT -> {
                    if (rawValue instanceof final Long contactId) {
                        yield ContactManager.getInstance().getName(orgId, contactId);
                    }
                    yield null;
                }
                default -> null;
            };
        } else if (StoreType.isMultiValueStore(storeType)) {

            if (rawValue instanceof final Collection<?> collection && !collection.isEmpty()) {
                final List<String> resolved = new ArrayList<>();

                for (final Object model : collection) {

                    if (model instanceof final Option option) {

                        if (Objects.isNull(option.getOption()) && Objects.nonNull(option.id())) {
                            resolved.add(optionService.get(orgId, field.id(), option.id()).getOption());
                        } else if (Objects.nonNull(option.getOption())) {
                            resolved.add(option.getOption());
                        }
                    }
                }
                return resolved.isEmpty() ? null : String.join(",", resolved);
            }
        }
        return null;
    }

    /**
     * Resolves tag names from the summary info, fetching names from the database for tags that only have ids.
     */
    protected String resolveTagNames(final Long orgId, final SummaryInfo summaryInfo) {
        if (Objects.isNull(summaryInfo)) {
            return null;
        }
        final List<Long> tagIds = new ArrayList<>();
        final List<Tag> tags = summaryInfo.getTags().stream().sorted(Comparator.comparing(Tag::id, Comparator.nullsLast(Comparator.naturalOrder()))).toList();

        for (final Tag tag : tags) {

            if (Objects.nonNull(tag.getId()) && Objects.isNull(tag.getName())) {
                tagIds.add(tag.getId());
            }
        }
        final Map<Long, String> fetchedTagNames = tagIds.isEmpty() ? Map.of() : tagManager.getNamesByIds(orgId, tagIds);
        final List<String> tagNames = new ArrayList<>();

        for (final Tag tag : tags) {
            final Long id = tag.getId();
            final String name = tag.getName();

            if (Objects.nonNull(name)) {
                tagNames.add(name);
            } else if (Objects.nonNull(id) && fetchedTagNames.containsKey(id)) {
                tagNames.add(fetchedTagNames.get(id));
            }
        }
        return tagNames.isEmpty() ? null : String.join(",", tagNames);
    }

    /**
     * Utility method to put a key-value pair into a map if the value is non-null.
     */
    protected static void putIfNonNull(final Map<String, String> map, final String key, final Object value) {
        if (Objects.nonNull(value)) {
            map.putIfAbsent(key, String.valueOf(value));
        }
    }

    /**
     * Utility method to add a field to the list of tracked complex fields if it's enabled for tracking in the config.
     */
    protected static void addFieldIfTrackEnabled(final FieldTrackingConfig config, final String field, final List<String> trackedComplexFields) {
        if (config.isFieldTracked(field)) {
            trackedComplexFields.add(field);
        }
    }

    /**
     * Helper method to conditionally put old row value if the field is tracked.
     */
    protected static void putIfTrackEnabled(final FieldTrackingConfig config, final Row oldRow, final Map<String, String> oldValues, final String fieldName, final String fieldColumn) {
        if (config.isFieldTracked(fieldName)) {
            putIfNonNull(oldValues, fieldName, oldRow.get(fieldColumn));
        }
    }
}