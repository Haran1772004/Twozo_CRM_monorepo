package com.tlc.crm.messenger.campaign.manager.impl;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.language.Language;
import com.tlc.crm.common.models.ViewType;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.core.campaign.fields.CampaignFields;
import com.tlc.crm.messenger.campaign.manager.WATemplateFilterPreferenceManager;
import com.tlc.crm.messenger.campaign.model.WACampaignTemplateStatus;
import com.tlc.crm.messenger.campaign.model.WATemplateFilterPreference;
import com.tlc.crm.messenger.sql.resource.OMKCPWATEMPLATEFILTERPREFERENCE;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.common.whatsapp.model.template.WATemplate;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * <p>
 * Provides the {@link WATemplateFilterPreference} related DB actions.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public final class WATemplateFilterPreferenceManagerImpl implements WATemplateFilterPreferenceManager {

    private WATemplateFilterPreferenceManagerImpl() {
    }

    private static final class InstanceHolder {
        private static final WATemplateFilterPreferenceManagerImpl INSTANCE = new WATemplateFilterPreferenceManagerImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link WATemplateFilterPreferenceManagerImpl}.
     * </p>
     *
     * @return {@link WATemplateFilterPreferenceManagerImpl}.
     */
    public static WATemplateFilterPreferenceManagerImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    private Table getTable() {
        return Table.get(OMKCPWATEMPLATEFILTERPREFERENCE.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the org id
     * @param userId Represents the user id
     */
    @Override
    public WATemplateFilterPreference get(final Long orgId, final Long userId) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMKCPWATEMPLATEFILTERPREFERENCE.ORG_ID), orgId))
                .and(Criteria.eq(table.getColumn(OMKCPWATEMPLATEFILTERPREFERENCE.USER_ID), userId)));
        final Row row = orgDataStore(orgId).get(selectQuery).getRow(table);

        return Objects.nonNull(row) ? convertRowToPreference(orgId, row) : null;
    }

    /**
     * {@inheritDoc}
     *
     * @param preference Represents the {@link WATemplateFilterPreference}
     */
    @Override
    public void create(final WATemplateFilterPreference preference) {
        final DataContainer targetContainer = DataContainer.create();

        loadPreferenceIntoContainer(true, preference, null, targetContainer);
        orgDataStore(preference.orgId()).commitChanges(targetContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param preference Represents the {@link WATemplateFilterPreference}
     */
    @Override
    public void update(final WATemplateFilterPreference preference) {
        final Long orgId = preference.orgId();
        final Table table = getTable();
        final DataContainer sourceContainer = orgDataStore(orgId).get(table,
                new WhereClause(Criteria.eq(table.getColumn(OMKCPWATEMPLATEFILTERPREFERENCE.USER_ID), preference.getUserId()))
                        .and(Criteria.eq(table.getColumn(OMKCPWATEMPLATEFILTERPREFERENCE.ORG_ID), orgId)));
        final DataContainer targetContainer = DataContainer.create();

        loadPreferenceIntoContainer(false, preference, sourceContainer, targetContainer);
        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Loads the preference into the container
     * </p>
     *
     * @param isCreateEvent   Represents the create event
     * @param preference      Represents the {@link WATemplateFilterPreference}
     * @param sourceContainer Represents the source container
     * @param targetContainer Represents the target container
     */
    private void loadPreferenceIntoContainer(final boolean isCreateEvent, final WATemplateFilterPreference preference,
                                             final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Long orgId = preference.orgId();
        final Table table = getTable();
        final Collection<WATemplate.Category> categories = preference.getCategories();
        final Collection<WACampaignTemplateStatus> statuses = preference.getStatuses();
        final Collection<Language> languages = preference.getLanguages();
        final Row row = isCreateEvent ? new Row(table) : sourceContainer.getOrInitRow(table,
                new WhereClause(Criteria.eq(table.getColumn(OMKCPWATEMPLATEFILTERPREFERENCE.USER_ID), preference.getUserId()))
                        .and(Criteria.eq(table.getColumn(OMKCPWATEMPLATEFILTERPREFERENCE.ORG_ID), orgId)));

        if (row.isNewRow()) {
            preference.setId(row.getPKValue());
            row.setOrgId(orgId);
            row.set(OMKCPWATEMPLATEFILTERPREFERENCE.USER_ID, preference.getUserId());
        }
        row.set(OMKCPWATEMPLATEFILTERPREFERENCE.BUSINESS_PHONE_NUMBER_ID, preference.getBusinessPhoneNumberId());
        row.set(OMKCPWATEMPLATEFILTERPREFERENCE.VIEW, preference.getViewType().getId());

        final JsonObject filterConfig = Json.object();

        if (Objects.nonNull(categories) && !categories.isEmpty()) {
            filterConfig.put(CampaignFields.CATEGORY, categories.stream().map(WATemplate.Category::getId).toList());
        }

        if (Objects.nonNull(statuses) && !statuses.isEmpty()) {
            filterConfig.put(CampaignFields.STATUS, statuses.stream().map(WACampaignTemplateStatus::getId).toList());
        }

        if (Objects.nonNull(languages) && !languages.isEmpty()) {
            filterConfig.put(WAFields.LANGUAGE, languages.stream().map(Language::getId).toList());
        }
        row.set(OMKCPWATEMPLATEFILTERPREFERENCE.FILTER_CONFIG,
                Json.isEmpty(filterConfig) ? null : filterConfig.toString());
        row.set(OMKCPWATEMPLATEFILTERPREFERENCE.UPDATED_AT, new DateTime(Instant.now().toEpochMilli()));

        targetContainer.updateRow(row);
    }

    /**
     * <p>
     * Converts the row to {@link WATemplateFilterPreference}
     * </p>
     *
     * @param orgId Represents the ord id
     * @param row   Represents the {@link Row}
     * @return the {@link WATemplateFilterPreference}
     */
    private WATemplateFilterPreference convertRowToPreference(final Long orgId, final Row row) {
        final WATemplateFilterPreference preference = new WATemplateFilterPreference();

        preference.setId(row.getPKValue());
        preference.setOrgId(orgId);
        preference.setUserId(row.get(OMKCPWATEMPLATEFILTERPREFERENCE.USER_ID));
        preference.setBusinessPhoneNumberId(row.get(OMKCPWATEMPLATEFILTERPREFERENCE.BUSINESS_PHONE_NUMBER_ID));
        preference.setViewType(ViewType.get(row.get(OMKCPWATEMPLATEFILTERPREFERENCE.VIEW)));

        final String filterConfig = row.get(OMKCPWATEMPLATEFILTERPREFERENCE.FILTER_CONFIG);

        if (Objects.nonNull(filterConfig)) {
            final JsonObject criteriaJson = Json.object(filterConfig);

            if (criteriaJson.containsKey(CampaignFields.CATEGORY)) {
                final Collection<WATemplate.Category> categories = new HashSet<>();

                criteriaJson.getJsonArray(CampaignFields.CATEGORY).forEach(categoryId -> {
                    categories.add(WATemplate.Category.getById(NumberUtils.parseNumber(categoryId).shortValue()));
                });

                preference.setCategories(categories);
            }

            if (criteriaJson.containsKey(CampaignFields.STATUS)) {
                final Collection<WACampaignTemplateStatus> statuses = new HashSet<>();

                criteriaJson.getJsonArray(CampaignFields.STATUS).forEach(statusId -> {
                    statuses.add(WACampaignTemplateStatus.get(NumberUtils.parseNumber(statusId).shortValue()));
                });

                preference.setStatuses(statuses);
            }

            if (criteriaJson.containsKey(WAFields.LANGUAGE)) {
                final Collection<Language> languages = new HashSet<>();

                criteriaJson.getJsonArray(WAFields.LANGUAGE).forEach(languageId -> {
                    languages.add(Language.getById(NumberUtils.parseNumber(languageId).shortValue()));
                });

                preference.setLanguages(languages);
            }
        }

        return preference;
    }
}