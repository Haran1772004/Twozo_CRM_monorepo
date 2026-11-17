package com.tlc.crm.core.field.fieldAudit.resolver;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.sql.resource.OMCRMTERRITORY;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.manager.PipelineManager;
import com.tlc.crm.core.api.deal.manager.PipelineStageManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.Participant;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.api.summaryInfo.model.SummaryInfo;
import com.tlc.crm.core.features.api.tag.SourceTagManager;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.sql.resource.MCOCOMPANY;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MDOCOMPANYFIELDS;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.MDODROPDOWNFIELDS;
import com.tlc.crm.core.sql.resource.MDOPARTICIPANT;
import com.tlc.crm.core.sql.resource.MDOPIPELINE;
import com.tlc.crm.core.sql.resource.MDOPIPELINESTAGE;
import com.tlc.crm.core.sql.resource.MTDEALTAG;

import com.tlc.crm.entity.fieldaudit.FieldAuditHandler;
import com.tlc.crm.entity.fieldaudit.models.FieldTrackingConfig;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.forecastcategory.model.ForecastCategory;
import com.tlc.crm.entity.manager.field.type.MonetaryManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.sql.resource.OMQFFORECASTCATEGORY;
import com.tlc.crm.entity.sql.resource.OMTTAG;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.l10n.temporal.model.DateTime;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Handles field edit history value loading, resolution, and comparison for {@link Deal} entities.
 * Extracts field-history-specific logic out of {@link DealManager} for better separation of concerns.
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class DealFieldAuditResolver extends AbstractFieldAuditResolver {

    private final ContactManager contactManager;
    private final CompanyManager companyManager;
    private final PipelineManager pipelineManager;
    private final PipelineStageManager pipelineStageManager;
    private final MonetaryManager monetaryManager;

    private DealFieldAuditResolver() {
        this.pipelineManager = PipelineManager.getInstance();
        this.pipelineStageManager = PipelineStageManager.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.companyManager = CompanyManager.getInstance();
        this.monetaryManager = MonetaryManager.getInstance();
    }

    /**
     * <p>
     * Gets the singleton instance of {@link DealFieldAuditResolver}.
     * </p>
     *
     * @return The singleton instance of {@link DealFieldAuditResolver}
     */
    public static DealFieldAuditResolver getInstance() {
        return Instance.DEAL_FIELD_HISTORY_RESOLVER;
    }

    /**
     * <p>
     * Holder class for lazy-loaded singleton instance of {@link DealFieldAuditResolver}.
     * </p>
     */
    private static class Instance {
        private static final DealFieldAuditResolver DEAL_FIELD_HISTORY_RESOLVER = new DealFieldAuditResolver();
    }

    /**
     * Gets the deal and company mapping table
     *
     * @return {@link Table}
     */
    public Table getDealCompanyTable() {
        return Table.get(MDOCOMPANYFIELDS.TABLE);
    }

    /**
     * <p>
     * Loads field edit history entries for the given deal update into the target container.
     * </p>
     */
    public void loadFieldAudit(final Deal deal, final Row oldRow, final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Long orgId = deal.orgId();
        final FieldTrackingConfig config = fieldTrackingConfigManager.getOrDefault(orgId, (short) Source.DEAL.getId());

        if (!config.isTrackingEnabled()) {
            return;
        }
        final Collection<FieldInfo> allFields = fieldService.getFieldsBySource(orgId, Deal.ENTITY_TYPE);
        final Map<String, String> oldDealValues = new HashMap<>();
        final Map<String, String> newDealValues = buildNewDealSystemFieldValues(deal, config);
        final Map<String, String> oldCustomValues = FieldAuditHandler.extractFieldValues(deal.getExistingValueHolder(), allFields, config);
        final Map<String, String> newCustomValues = FieldAuditHandler.extractFieldValues(deal.getValueHolder(), allFields, config);

        if (Objects.nonNull(oldRow)) {
            loadOldSystemFieldValues(config, oldRow, oldDealValues, sourceContainer);
        }
        oldCustomValues.forEach(oldDealValues::putIfAbsent);
        FieldAuditHandler.resolveNewCustomFieldValues(config, deal.getValueHolder(), allFields, newCustomValues,
                (field, rawValue) -> resolveCustomFieldValue(orgId, field, rawValue, field.storeType()));
        newCustomValues.forEach(newDealValues::putIfAbsent);

        fieldAuditHandler.loadFieldAuditForTrackableFields(orgId, Source.DEAL, deal.id(),
                deal.getUpdatedBy().id(), deal.getFieldAuditInfo(), allFields, oldDealValues, newDealValues, targetContainer);
    }

    /**
     * <p>
     * Loads old system field values from the existing deal row for field edit history comparison.
     * </p>
     */
    private void loadOldSystemFieldValues(final FieldTrackingConfig config, final Row oldDealRow, final Map<String, String> oldDealValues,
            final DataContainer sourceContainer) {
        final String titleKey = DealFields.TITLE;
        final String pipelineKey = DealFields.PIPELINE;
        final String pipelineStageKey = DealFields.PIPELINE_STAGE;
        final String dealClosedOnKey = DealFields.DEAL_CLOSED_ON;
        final String wonReasonField = DealFields.WON_REASON;
        final String lostReasonField = DealFields.LOST_REASON;
        final String primaryContactKey = DealFields.PRIMARY_CONTACT;
        final String relatedContactsKey = DealFields.RELATED_CONTACTS;
        final String companyKey = DealFields.COMPANY;
        final String dealValueKey = DealFields.DEAL_VALUE;
        final String salesOwnerKey = DealFields.SALES_OWNER;
        final String expectedCloseDateKey = DealFields.EXPECTED_CLOSE_DATE;
        final String forecastCategoryKey = DealFields.FORECAST_CATEGORY;
        final String sourceKey = DealFields.DEAL_SOURCE;
        final String territoryKey = DealFields.TERRITORY;
        final String descriptionKey = DealFields.DESCRIPTION;
        final String tagsField = DealFields.TAGS;
        final String typeField = DealFields.TYPE;
        final String paymentStatusField = DealFields.PAYMENT_STATUS;
        final Long dealId = oldDealRow.getPKValue();
        final Long orgId = oldDealRow.getOrgId();

        putIfTrackEnabled(config, oldDealRow, oldDealValues, titleKey, MDODEAL.TITLE);
        putIfTrackEnabled(config, oldDealRow, oldDealValues, descriptionKey, MDODEAL.DESCRIPTION);

        if (config.isFieldTracked(pipelineKey)) {
            final Long oldPipelineId = oldDealRow.get(MDODEAL.PIPELINE_ID);

            if (Objects.nonNull(oldPipelineId)) {
                final Row pipelineRow = sourceContainer.getRow(pipelineManager.getTable(), oldPipelineId);

                putIfNonNull(oldDealValues, pipelineKey, Objects.isNull(pipelineRow) ? null : pipelineRow.get(MDOPIPELINE.NAME));
            }
        }

        if (config.isFieldTracked(pipelineStageKey)) {
            final Long oldPipelineStageId = oldDealRow.get(MDODEAL.PIPELINE_STAGE_ID);

            if (Objects.nonNull(oldPipelineStageId)) {
                final Row pipelineStageRow = sourceContainer.getRow(pipelineStageManager.getTable(), oldPipelineStageId);

                putIfNonNull(oldDealValues, pipelineStageKey, Objects.isNull(pipelineStageRow) ? null : pipelineStageRow.get(MDOPIPELINESTAGE.NAME));
            }
        }

        if (config.isFieldTracked(dealClosedOnKey)) {
            final DateTime closedTime = oldDealRow.get(MDODEAL.CLOSED_TIME);

            if (Objects.nonNull(closedTime)) {
                putIfNonNull(oldDealValues, dealClosedOnKey, closedTime.getTimeStamp());
            }
        }

        if (config.isFieldTracked(primaryContactKey)) {
            final Long oldContactId = oldDealRow.get(MDODEAL.CONTACT_ID);

            if (Objects.nonNull(oldContactId)) {
                final Row contactRow = sourceContainer.getRow(contactManager.getTable(), oldContactId);

                putIfNonNull(oldDealValues, primaryContactKey, Objects.isNull(contactRow) ? null : Contact.getFullName(contactRow.get(MCOCONTACT.FIRST_NAME), contactRow.get(MCOCONTACT.LAST_NAME)));
            }
        }

        if (config.isFieldTracked(dealValueKey)) {
            final Long monetaryId = oldDealRow.get(MDODEAL.MONETARY_ID);

            if (Objects.nonNull(monetaryId)) {
                final Table table = monetaryManager.getTable();
                final Row monetaryRow = sourceContainer.getRow(table, monetaryId);

                if (Objects.nonNull(monetaryRow)) {
                    final Monetary monetary = monetaryManager.convertRowToModel(monetaryRow);

                    if (Objects.nonNull(monetary)) {
                        putIfNonNull(oldDealValues, dealValueKey, Monetary.getValueWithSymbol(monetary.getCurrency().getSymbol(), monetary.getValue().doubleValue()));
                    }
                }
            }
        }

        if (config.isFieldTracked(salesOwnerKey)) {
            final Long oldOwnerId = oldDealRow.get(MDODEAL.OWNER_ID);

            if (Objects.nonNull(oldOwnerId)) {
                putIfNonNull(oldDealValues, salesOwnerKey, userService.getUser(orgId, oldOwnerId).getFullName());
            }
        }

        if (config.isFieldTracked(expectedCloseDateKey)) {
            final Date expectedCloseDate = oldDealRow.get(MDODEAL.EXPECTED_CLOSE_DATE);

            if (Objects.nonNull(expectedCloseDate)) {
                putIfNonNull(oldDealValues, expectedCloseDateKey, expectedCloseDate.getTimeStamp());
            }
        }

        if (config.isFieldTracked(territoryKey)) {
            final Long territoryId = oldDealRow.get(MDODEAL.TERRITORY_ID);

            if (Objects.nonNull(territoryId)) {
                final Table territoryTable = Table.get(OMCRMTERRITORY.TABLE);
                final Row territoryRow = sourceContainer.getRow(territoryTable, new WhereClause(Criteria.eq(territoryTable.getPKColumn(), territoryId)));

                putIfNonNull(oldDealValues, territoryKey, Objects.isNull(territoryRow) ? null : territoryRow.get(OMCRMTERRITORY.NAME));
            }
        }

        if (config.isFieldTracked(forecastCategoryKey)) {
            final Long forecastCategoryId = oldDealRow.get(MDODEAL.FORECAST_CATEGORY_ID);

            if (Objects.nonNull(forecastCategoryId)) {
                final Table forecastCategoryTable = Table.get(OMQFFORECASTCATEGORY.TABLE);
                final Row forecastCategoryRow = sourceContainer.getRow(forecastCategoryTable, new WhereClause(Criteria.eq(forecastCategoryTable.getPKColumn(), forecastCategoryId)));

                putIfNonNull(oldDealValues, forecastCategoryKey, Objects.isNull(forecastCategoryRow) ? null : forecastCategoryRow.get(OMQFFORECASTCATEGORY.NAME));
            }
        }
        final List<String> trackedComplexFields = new ArrayList<>();

        addFieldIfTrackEnabled(config, relatedContactsKey, trackedComplexFields);
        addFieldIfTrackEnabled(config, companyKey, trackedComplexFields);
        addFieldIfTrackEnabled(config, wonReasonField, trackedComplexFields);
        addFieldIfTrackEnabled(config, lostReasonField, trackedComplexFields);
        addFieldIfTrackEnabled(config, tagsField, trackedComplexFields);
        addFieldIfTrackEnabled(config, sourceKey, trackedComplexFields);
        addFieldIfTrackEnabled(config, typeField, trackedComplexFields);
        addFieldIfTrackEnabled(config, paymentStatusField, trackedComplexFields);

        if (trackedComplexFields.isEmpty()) {
            return;
        }
        final Map<String, FieldInfo> fieldInfoMap = fieldService.groupDefaultFieldsByAliasName(orgId, Deal.ENTITY_TYPE, trackedComplexFields);

        if (fieldInfoMap.containsKey(relatedContactsKey)) {
            final Table participantTable = Table.get(MDOPARTICIPANT.TABLE);
            final String resolvedNames = sourceContainer.getRows(participantTable, new WhereClause(Criteria.eq(participantTable.getColumn(MDOPARTICIPANT.DEAL_ID), dealId)))
                    .filter(row -> Objects.nonNull(row) && !Objects.equals(row.get(MDOPARTICIPANT.CONTACT_ID), oldDealRow.get(MDODEAL.CONTACT_ID)))
                    .sorted(Comparator.comparing(row -> row.get(MDOPARTICIPANT.CONTACT_ID)))
                    .map(row -> {
                        final Row contactRow = sourceContainer.getRow(contactManager.getTable(), (Long) row.get(MDOPARTICIPANT.CONTACT_ID));

                        return Objects.isNull(contactRow) ? null : Contact.getFullName(contactRow.get(MCOCONTACT.FIRST_NAME), contactRow.get(MCOCONTACT.LAST_NAME));
                    }).filter(Objects::nonNull).collect(Collectors.joining(","));

            if (!resolvedNames.isEmpty()) {
                oldDealValues.put(relatedContactsKey, resolvedNames);
            }
        }

        if (fieldInfoMap.containsKey(companyKey)) {
            final Table dealCompanyTable = getDealCompanyTable();
            final Row dealCompanyRow = sourceContainer.getRow(dealCompanyTable, new WhereClause(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID), dealId))
                    .and(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.FIELD_ID), fieldInfoMap.get(companyKey).id())));

            if (Objects.nonNull(dealCompanyRow)) {
                final Long companyId = dealCompanyRow.get(MDOCOMPANYFIELDS.COMPANY_ID);

                if (Objects.nonNull(companyId)) {
                    final Row companyRow = sourceContainer.getRow(companyManager.getTable(), companyId);

                    putIfNonNull(oldDealValues, companyKey, Objects.isNull(companyRow) ? null : companyRow.get(MCOCOMPANY.NAME));
                }
            }
        }

        if (fieldInfoMap.containsKey(tagsField)) {
            final Table dealTagTable = SourceTagManager.getInstance().getDealTagTable();
            final Table tagTable = SourceTagManager.getInstance().getTable();
            final List<Long> tagIds = sourceContainer.getRows(dealTagTable, new WhereClause(Criteria.eq(dealTagTable.getColumn(MTDEALTAG.DEAL_ID),
                    dealId))).filter(Objects::nonNull).map(row -> (Long) row.get(MTDEALTAG.TAG_ID)).toList();

            if (!tagIds.isEmpty()) {
                putIfNonNull(oldDealValues, tagsField, sourceContainer.getRows(tagTable, new WhereClause(Criteria.in(tagTable.getColumn(OMTTAG.ID), tagIds)))
                        .filter(Objects::nonNull).sorted(Comparator.comparing(Row::getPKValue))
                        .map(row -> (String) row.get(OMTTAG.NAME)).collect(Collectors.joining(",")));
            }
        }
        final Table dealDropdownTable = Table.get(MDODROPDOWNFIELDS.TABLE);

        getDealSystemOptionFieldOldValue(oldDealValues, sourceContainer, dealDropdownTable, dealId, orgId, wonReasonField, fieldInfoMap);
        getDealSystemOptionFieldOldValue(oldDealValues, sourceContainer, dealDropdownTable, dealId, orgId, lostReasonField, fieldInfoMap);
        getDealSystemOptionFieldOldValue(oldDealValues, sourceContainer, dealDropdownTable, dealId, orgId, sourceKey, fieldInfoMap);
        getDealSystemOptionFieldOldValue(oldDealValues, sourceContainer, dealDropdownTable, dealId, orgId, typeField, fieldInfoMap);
        getDealSystemOptionFieldOldValue(oldDealValues, sourceContainer, dealDropdownTable, dealId, orgId, paymentStatusField, fieldInfoMap);
    }

    /**
     * <p>
     * Builds a map of current deal system field values for field edit history comparison.
     * </p>
     */
    private Map<String, String> buildNewDealSystemFieldValues(final Deal deal, final FieldTrackingConfig config) {
        final Long orgId = deal.orgId();
        final Map<String, String> newValues = new HashMap<>();
        final CrmUser salesOwner = deal.getSalesOwner();
        final Territory territory = deal.getTerritory();
        final Monetary monetary = deal.getDealValue();
        final Pipeline pipeline = deal.getPipeline();
        final PipelineStage pipelineStage = deal.getPipelineStage();
        final Option wonReason = deal.getWonReason();
        final Option lostReason = deal.getLostReason();
        final DateTime closedTime = deal.getClosedTime();
        final Contact primaryContact = deal.getPrimaryContact();
        final Collection<Participant> participants = deal.getParticipants();
        final Company company = deal.getCompany();
        final ForecastCategory forecastCategory = deal.getForecastCategory();
        final Option source = deal.getSource();
        final Monetary expectedDealValue = deal.getExpectedDealValue();
        final Date expectedCloseDate = deal.getExpectedCloseDate();
        final Option type = deal.getType();
        final Option paymentStatus = deal.getPaymentStatus();
        final String pipelineKey = DealFields.PIPELINE;
        final String pipelineStageKey = DealFields.PIPELINE_STAGE;
        final String primaryContactKey = DealFields.PRIMARY_CONTACT;
        final String relatedContactsKey = DealFields.RELATED_CONTACTS;
        final String companyKey = DealFields.COMPANY;
        final String salesOwnerKey = DealFields.SALES_OWNER;
        final String forecastCategoryKey = DealFields.FORECAST_CATEGORY;
        final String territoryKey = DealFields.TERRITORY;
        final String tagsKey = DealFields.TAGS;

        putIfNonNull(newValues, DealFields.TITLE, deal.getTitle());
        putIfNonNull(newValues, DealFields.WON_REASON, Objects.isNull(wonReason) ? null : resolveOptionName(orgId, wonReason));
        putIfNonNull(newValues, DealFields.LOST_REASON, Objects.isNull(lostReason) ? null : resolveOptionName(orgId, lostReason));
        putIfNonNull(newValues, DealFields.DEAL_CLOSED_ON, Objects.isNull(closedTime) ? null : closedTime.getTimeStamp());
        putIfNonNull(newValues, DealFields.DEAL_SOURCE, Objects.isNull(source) ? null : resolveOptionName(orgId, source));
        putIfNonNull(newValues, DealFields.EXPECTED_CLOSE_DATE, Objects.isNull(expectedCloseDate) ? null : expectedCloseDate.getTimeStamp());
        putIfNonNull(newValues, DealFields.DESCRIPTION, deal.getDescription());
        putIfNonNull(newValues, DealFields.TYPE, Objects.isNull(type) ? null : resolveOptionName(orgId, type));
        putIfNonNull(newValues, DealFields.PAYMENT_STATUS, Objects.isNull(paymentStatus) ? null : resolveOptionName(orgId, paymentStatus));
        final SummaryInfo summaryInfo = deal.getSummaryInfo();

        if (Objects.nonNull(summaryInfo) && config.isFieldTracked(tagsKey)) {
            putIfNonNull(newValues, tagsKey, resolveTagNames(orgId, summaryInfo));
        }

        if (config.isFieldTracked(pipelineKey) && Objects.nonNull(pipeline)) {
            final String pipelineName = pipeline.getName();

            putIfNonNull(newValues, pipelineKey, Objects.isNull(pipelineName) ? pipelineManager.partialGet(orgId, pipeline.id()).getName() : pipelineName);
        }

        if (config.isFieldTracked(pipelineStageKey) && Objects.nonNull(pipelineStage)) {
            final String pipelineStageName = pipelineStage.getName();

            putIfNonNull(newValues, pipelineStageKey, Objects.isNull(pipelineStageName) ? pipelineStageManager.getPipelineStageName(orgId, pipelineStage.id()) : pipelineStageName);
        }

        if (config.isFieldTracked(primaryContactKey) && Objects.nonNull(primaryContact)) {
            final String fullName = primaryContact.getFullName();

            putIfNonNull(newValues, primaryContactKey, Objects.isNull(fullName) ? contactManager.getName(orgId, primaryContact.id()) : fullName);
        }

        if (config.isFieldTracked(relatedContactsKey) && Objects.nonNull(participants) && !participants.isEmpty()) {
            final String resolvedNames = participants.stream()
                    .map(Participant::getContact)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(Contact::id))
                    .map(contact ->  Objects.isNull(contact.getFullName()) ? contactManager.getName(orgId, contact.id()) : contact.getFullName())
                    .collect(Collectors.joining(","));

            putIfNonNull(newValues, relatedContactsKey, resolvedNames);
        }

        if (config.isFieldTracked(companyKey) && Objects.nonNull(company)) {
            final String companyName = company.getName();

            putIfNonNull(newValues, companyKey, Objects.isNull(companyName) ? companyManager.getName(orgId, company.id()) : companyName);
        }

        if (config.isFieldTracked(salesOwnerKey) && Objects.nonNull(salesOwner)) {
            final String ownerName = salesOwner.getFullName();

            putIfNonNull(newValues, salesOwnerKey, Objects.isNull(ownerName) ? userService.getUser(orgId, salesOwner.id()).getFullName() : ownerName);
        }

        if (config.isFieldTracked(forecastCategoryKey) && Objects.nonNull(forecastCategory)) {
            final String forecastCategoryName = forecastCategory.getName();

            putIfNonNull(newValues, forecastCategoryKey, Objects.isNull(forecastCategoryName) ? resolveForecastCategoryName(orgId, forecastCategory.id()) : forecastCategoryName);
        }

        if (config.isFieldTracked(territoryKey) && Objects.nonNull(territory)) {
            final String territoryName = territory.getName();

            putIfNonNull(newValues, territoryKey, Objects.isNull(territoryName) ? resolveTerritoryName(orgId, territory.id()) : territoryName);
        }

        if (Objects.nonNull(monetary)) {
            putIfNonNull(newValues, DealFields.DEAL_VALUE, Monetary.getValueWithSymbol(monetary.getCurrency().getSymbol(), monetary.getValue().doubleValue()));
        }

        if (Objects.nonNull(expectedDealValue)) {
            putIfNonNull(newValues, DealFields.EXPECTED_DEAL_VALUE, Monetary.getValueWithSymbol(expectedDealValue.getCurrency().getSymbol(), expectedDealValue.getValue().doubleValue()));
        }
        return newValues;
    }

    /**
     * Resolves the forecast category name for the given forecast category id.
     */
    private String resolveForecastCategoryName(final Long orgId, final Long forecastCategoryId) {
        if (Objects.isNull(forecastCategoryId)) {
            return null;
        }
        final Table forecastCategoryTable = Table.get(OMQFFORECASTCATEGORY.TABLE);
        final Row row = orgDataStore(orgId).get(forecastCategoryTable, forecastCategoryId, List.of(forecastCategoryTable.getColumn(OMQFFORECASTCATEGORY.NAME)));

        return Objects.isNull(row) ? null : row.get(OMQFFORECASTCATEGORY.NAME);
    }

    /**
     * Fetches and puts the old option name for the given field into the old values map if the field is tracked and has a value.
     */
    private void getDealSystemOptionFieldOldValue(final Map<String, String> oldValues, final DataContainer sourceContainer,
            final Table dealDropdownTable, final Long dealId, final Long orgId, final String field, final Map<String, FieldInfo> fieldInfoMap) {
        if (fieldInfoMap.containsKey(field)) {
            final Long fieldId = fieldInfoMap.get(field).id();
            final Row dropdownRow = sourceContainer.getRow(dealDropdownTable, new WhereClause(Criteria.eq(dealDropdownTable.getColumn(MDODROPDOWNFIELDS.ENTITY_ID), dealId))
                            .and(Criteria.eq(dealDropdownTable.getColumn(MDODROPDOWNFIELDS.FIELD_ID), fieldId)));

            if (Objects.nonNull(dropdownRow)) {
                putIfNonNull(oldValues, field, optionService.get(orgId, fieldId, dropdownRow.get(MDODROPDOWNFIELDS.OPTION_ID)).getOption());
            }
        }
    }
}