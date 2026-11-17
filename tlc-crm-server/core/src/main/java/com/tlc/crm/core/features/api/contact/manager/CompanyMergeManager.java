package com.tlc.crm.core.features.api.contact.manager;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.JavaUtil;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.features.api.merge.EntityMergeManager;
import com.tlc.crm.core.field.contact.CompanyFields;
import com.tlc.crm.core.internal.contact.status.ContactErrorCodes;
import com.tlc.crm.core.sql.resource.MCOCOMPANY;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MCOMDROPDOWNFIELDS;
import com.tlc.crm.core.sql.resource.MCONCOMPANYFIELDS;
import com.tlc.crm.core.sql.resource.MDOCOMPANYFIELDS;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.sql.resource.MTLCOMPANYTIMELINE;
import com.tlc.crm.entity.sql.resource.OMTLTIMELINE;
import com.tlc.crm.entity.timeline.api.CommonTimelineManager;
import com.tlc.crm.entity.timeline.field.TimelineActionFields;
import com.tlc.crm.entity.timeline.model.FieldTimeline;
import com.tlc.crm.entity.timeline.model.Timeline;
import com.tlc.crm.entity.timeline.model.TimelineType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.i18n.I18nKey;
import com.tlc.l10n.temporal.model.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.HavingClause;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * <p>
 * Manages the duplicate company list and merge company related DB actions
 * </p>
 *
 * @author EswariNivethaVU
 * @version 1.0
 */
public class CompanyMergeManager extends EntityMergeManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompanyMergeManager.class);

    private final ContactManager contactManager;
    private final DealManager dealManager;
    private final CompanyManager companyManager;
    private final CommonTimelineManager commonTimelineManager;
    private final FieldService fieldService;
    private final int companyBatchFetchSize;

    private static class Instance {
        private static final CompanyMergeManager INSTANCE = new CompanyMergeManager();
    }

    /**
     * <p>
     * Initializes all required manager and service instances used across company merge operations.
     * </p>
     */
    private CompanyMergeManager() {
        fieldService = FieldService.getSecureInstance();
        contactManager = ContactManager.getInstance();
        dealManager = DealManager.getInstance();
        companyManager = CompanyManager.getInstance();
        commonTimelineManager = CommonTimelineManager.getInstance();
        this.companyBatchFetchSize = 10;
    }

    /**
     * <p>
     * Fetches the {@link CompanyMergeManager} Instance
     * </p>
     *
     * @return {@link CompanyMergeManager}
     */
    public static CompanyMergeManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the company preview details based on the primary and secondary company ids.
     * </p>
     *
     * @param orgId       Represents the organization id.
     * @param primaryId   Represents the primary company id.
     * @param secondaryId Represents the secondary company id.
     * @return The {@link Company} preview details.
     */
    public Company getCompanyPreviewDetails(final Long orgId, final Long primaryId, final Long secondaryId) {
        final Table companyTable = companyManager.getTable();
        final Set<Long> companyIds = Set.of(primaryId, secondaryId);
        final DataContainer dataContainer = getDataContainer(orgId, companyIds);
        final Company primaryCompany = getCompanyDetails(primaryId, companyTable, dataContainer);
        final Company secondaryCompany = getCompanyDetails(secondaryId, companyTable, dataContainer);

        if (Objects.nonNull(primaryCompany) && Objects.nonNull(secondaryCompany)) {
            companyManager.loadSummaryDetails(orgId, primaryCompany);
            companyManager.loadSummaryDetails(orgId, secondaryCompany);

            mergeCompaniesForPreview(primaryCompany, secondaryCompany);
        }

        return primaryCompany;
    }

    /**
     * <p>
     * Gets the {@link Company} details.
     * </p>
     *
     * @param orgId     Represents the organization id.
     * @param companyId Represents the company id.
     * @return The {@link Company} details.
     */
    public Company get(final Long orgId, final Long companyId) {
        final Table companyTable = companyManager.getTable();
        final DataContainer dataContainer = getDataContainer(orgId, List.of(companyId));

        return getCompanyDetails(companyId, companyTable, dataContainer);
    }

    /**
     * <p>
     * Gets the company details based on company id.
     * </p>
     *
     * @param companyId     Represents the company id.
     * @param companyTable  Represents the company table.
     * @param dataContainer Holds the updated information of user defined and system field values.
     * @return The {@link Company} details.
     */
    private Company getCompanyDetails(final Long companyId, final Table companyTable, final DataContainer dataContainer) {
        final Row companyRow = dataContainer.getRow(companyTable, new WhereClause(Criteria.eq(companyTable.getPKColumn(), companyId)));

        return Objects.isNull(companyRow) ? null : companyManager.convertRowToModel(companyRow, false, dataContainer);
    }

    /**
     * <p>
     * Merges the companies for preview details.
     * </p>
     *
     * @param primaryCompany   An entity that holds the attributes of primary company.
     * @param secondaryCompany An entity that holds the attributes of secondary company.
     */
    private void mergeCompaniesForPreview(final Company primaryCompany, final Company secondaryCompany) {
        primaryCompany.setWebsite(preferPrimaryValue(primaryCompany.getWebsite(), secondaryCompany.getWebsite()));
        primaryCompany.setEmployeeCount(preferPrimaryValue(primaryCompany.getEmployeeCount(), secondaryCompany.getEmployeeCount()));
        primaryCompany.setFacebook(preferPrimaryValue(primaryCompany.getFacebook(), secondaryCompany.getFacebook()));
        primaryCompany.setLinkedIn(preferPrimaryValue(primaryCompany.getLinkedIn(), secondaryCompany.getLinkedIn()));
        primaryCompany.setTwitter(preferPrimaryValue(primaryCompany.getTwitter(), secondaryCompany.getTwitter()));
        primaryCompany.setSicCode(preferPrimaryValue(primaryCompany.getSicCode(), secondaryCompany.getSicCode()));
        primaryCompany.setAddress(preferPrimaryValue(primaryCompany.getAddress(), secondaryCompany.getAddress()));
        primaryCompany.setAnnualRevenue(preferPrimaryValue(primaryCompany.getAnnualRevenue(), secondaryCompany.getAnnualRevenue()));
        primaryCompany.setBusinessType(preferPrimaryValue(primaryCompany.getBusinessType(), secondaryCompany.getBusinessType()));
        primaryCompany.setIndustryType(preferPrimaryValue(primaryCompany.getIndustryType(), secondaryCompany.getIndustryType()));
        primaryCompany.setOrganizationStatus(preferPrimaryValue(primaryCompany.getOrganizationStatus(), secondaryCompany.getOrganizationStatus()));
        primaryCompany.setDescription(preferPrimaryValue(primaryCompany.getDescription(), secondaryCompany.getDescription()));
        primaryCompany.setParentCompany(preferPrimaryValue(primaryCompany.getParentCompany(), secondaryCompany.getParentCompany()));
        primaryCompany.setSalesOwner(preferPrimaryValue(primaryCompany.getSalesOwner(), secondaryCompany.getSalesOwner()));
        primaryCompany.setCreatedBy(preferPrimaryValue(primaryCompany.getCreatedBy(), secondaryCompany.getCreatedBy()));
        primaryCompany.setUpdatedBy(preferPrimaryValue(primaryCompany.getUpdatedBy(), secondaryCompany.getUpdatedBy()));
        primaryCompany.setOwnerId(preferPrimaryValue(primaryCompany.getOwnerId(), secondaryCompany.getOwnerId()));
        primaryCompany.setTerritory(preferPrimaryValue(primaryCompany.getTerritory(), secondaryCompany.getTerritory()));
        primaryCompany.setRecentNote(preferPrimaryValue(primaryCompany.getRecentNote(), secondaryCompany.getRecentNote()));
        primaryCompany.setSummaryInfo(mergeSummaryInfoForPreview(primaryCompany.getSummaryInfo(), secondaryCompany.getSummaryInfo()));
        primaryCompany.setDealCount(sumNullableLong(primaryCompany.getDealCount(), secondaryCompany.getDealCount()));
        primaryCompany.setContactCount(sumNullableLong(primaryCompany.getContactCount(), secondaryCompany.getContactCount()));
        primaryCompany.setActivityCount(sumNullableLong(primaryCompany.getActivityCount(), secondaryCompany.getActivityCount()));
        mergeCustomFields(primaryCompany, secondaryCompany);
    }

    /**
     * <p>
     * Gets the company data container.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param companyIds Contains the collection of company ids.
     * @return The {@link DataContainer}.
     */
    private DataContainer getDataContainer(final Long orgId, final Collection<Long> companyIds) {
        return companyManager.getCompanyDataContainer(orgId, companyIds);
    }

    /**
     * Fetches the {@link OrgDataStore}
     *
     * @param orgId Represents the organization id
     * @return {@link OrgDataStore}
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * <p>
     * Merges the given duplicate companies with primary company
     * </p>
     *
     * @param orgId               Represents the organization id
     * @param userId              Represents the user id
     * @param primaryCompanyId    Represents the primary company id
     * @param duplicateCompanyIds {@link Collection<Long>} of duplicate company ids
     * @param companyIds          {@link Collection<Long>} of company ids
     * @return response of merged status
     */
    public Map<String, Object> mergeCompanies(final Long orgId, final long userId, final Long primaryCompanyId, final Collection<Long> duplicateCompanyIds,
                                              final Collection<Long> companyIds) {
        final Map<String, Object> mergedData = new HashMap<>();
        final Table companyTable = companyManager.getTable();
        final Table contactCompanyTable = contactManager.getContactCompanyTable();
        final Table dealCompanyTable = dealManager.getDealCompanyTable();
        final Table companyTimeLineTable = commonTimelineManager.getCompanyTimelineTable();
        final Table companyOptionTable = companyManager.getCompanyOptionTable();
        final Column companyIdColumn = companyTable.getPKColumn();
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Collection<Collection<Long>> companyBatchedIds = JavaUtil.splitCollection(companyIds, companyBatchFetchSize);
        final DataContainer dataContainer = DataContainer.create();

        for (final Collection<Long> companyIdBatch : companyBatchedIds) {
            dataContainer.append(companyManager.getCompanyDataContainer(orgId, companyIdBatch));
            dataContainer.append(getCompanyTimelineDataContainer(orgId, companyIdBatch, companyTable, companyTimeLineTable));
        }
        final Row primaryCompanyRow = dataContainer.getRow(companyTable, new WhereClause(Criteria.eq(companyIdColumn, primaryCompanyId)));
        final Collection<Row> duplicateCompanyRows = dataContainer.getRows(companyTable, new WhereClause(
                Criteria.in(companyIdColumn, duplicateCompanyIds))).sorted(Comparator.comparing((row) ->
                ((DateTime) row.get(MCOCONTACT.CREATED_TIME)).getTimeStamp())).toList();
        final Collection<String> columnNames = companyTable.getTableDefinition().getEditableColumns().keySet();

        for (final String columnName : columnNames) {

            if (Objects.isNull(primaryCompanyRow.get(columnName))) {
                final Optional<Row> duplicateCompanyRow = duplicateCompanyRows.stream().filter(row ->
                        Objects.nonNull(row.get(columnName))).findFirst();

                duplicateCompanyRow.ifPresent(row -> primaryCompanyRow.set(columnName, row.get(columnName)));
            }
        }
        final Company company = convertRowToModel(primaryCompanyRow);

        try {
            final DataContainer targetContainer = DataContainer.create();
            final Long companyId = company.id();
            final String entityType = Company.ENTITY_TYPE;
            final Collection<FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(entityType).values();
            final Collection<FieldInfo> orgFields = fieldService.getOrgFields(orgId).stream().filter(fieldInfo -> fieldInfo.storeType() == StoreType.MULTI_SELECT).toList();

            for (final FieldInfo crmField : fields) {

                if (crmField.isActive() && StoreType.isModelStore(crmField.storeType()) && !crmField.isPersistent()) {
                    loadDefaultFieldRowsIntoContainer(orgId, crmField, primaryCompanyId, duplicateCompanyIds, dataContainer, targetContainer);
                }
            }

            for (final FieldInfo crmField : orgFields) {

                if (StoreType.MULTI_SELECT == crmField.storeType() && !crmField.isPersistent()) {
                    loadMultiSelectFieldsIntoContainer(companyId, crmField.id(), Objects.nonNull(crmField.presetConfig().dependsOn()), companyIds, companyOptionTable, dataContainer, targetContainer);
                }
            }

            loadCustomFieldRowsIntoContainer(company, duplicateCompanyIds, dataContainer, targetContainer, Company.ENTITY_TYPE);
            loadStandardMergeRowsIntoContainer(orgId, Source.COMPANY, companyId, duplicateCompanyIds, targetContainer, List.of(), List.of());
            loadMappingTableRowsIntoContainer(orgId, companyId, duplicateCompanyIds, List.of(
                            new MapperTableTransferSpec(dealCompanyTable, MDOCOMPANYFIELDS.COMPANY_ID),
                            new MapperTableTransferSpec(contactCompanyTable, MCONCOMPANYFIELDS.COMPANY_ID)),
                    targetContainer);
            loadCompanyOptionRowIntoContainer(primaryCompanyId, duplicateCompanyIds, dataContainer, targetContainer);
            targetContainer.updateRow(primaryCompanyRow);

            loadCompanyTimelineIntoContainer(orgId, userId, companyId, duplicateCompanyIds, dataContainer, targetContainer);
            duplicateCompanyRows.forEach(targetContainer::deleteRow);
            orgDataStore.commitChanges(targetContainer);
        } catch (ErrorCode errorCode) {
            LOGGER.info("Company merge failed for orgId: {}, primaryCompanyId: {}, duplicateCompanyIds: {} reason: {}", orgId, primaryCompanyId, duplicateCompanyIds, errorCode.getMessage());
            throw ErrorCode.get(ContactErrorCodes.MERGE_ACTION_FAILED);
        }
        mergedData.put(CompanyFields.MERGED_RECORDS, duplicateCompanyIds);
        mergedData.put(CompanyFields.COMPANY, company);

        return mergedData;
    }

    /**
     * <p>
     * Fetches timeline mapping rows and created timeline rows for the given companies.
     * </p>
     *
     * @param orgId                Represents an organization id
     * @param companyIds           Represents company ids
     * @param companyTable         Represents company table
     * @param companyTimelineTable Represents company timeline table
     * @return The timeline related {@link DataContainer}
     */
    private DataContainer getCompanyTimelineDataContainer(final Long orgId, final Collection<Long> companyIds,
                                                          final Table companyTable, final Table companyTimelineTable) {
        final Table timelineTable = commonTimelineManager.getTable();
        final SelectQuery timelineSelectQuery = SelectQuery.get(companyTable);

        timelineSelectQuery.addJoinClause(List.of(
                new JoinClause(companyTable.getPKColumn(), companyTimelineTable.getColumn(MTLCOMPANYTIMELINE.COMPANY_ID)),
                new JoinClause(companyTimelineTable.getColumn(MTLCOMPANYTIMELINE.TIMELINE_ID), timelineTable.getPKColumn())));
        timelineSelectQuery.addSelectClause(companyTimelineTable, timelineTable);
        timelineSelectQuery.setWhereClause(new WhereClause(Criteria.in(companyTable.getPKColumn(), companyIds))
                .and(Criteria.eq(timelineTable.getColumn(OMTLTIMELINE.ACTION), TimelineActionFields.CREATED.toLowerCase()))
                .and(Criteria.eq(timelineTable.getColumn(OMTLTIMELINE.TYPE), TimelineType.COMPANY_CREATED.getId()))
                .and(Criteria.eq(timelineTable.getColumn(OMTLTIMELINE.CATEGORY), Timeline.Category.COMPANY.name())));

        return orgDataStore(orgId).get(timelineSelectQuery);
    }

    /**
     * <p>
     * Updates the company related timelines into the target {@link DataContainer}.
     * </p>
     *
     * @param orgId               Represents an organization id
     * @param userId              Represents the user id
     * @param companyId           Represents the primary company id
     * @param duplicateCompanyIds Represents the secondary/duplicate company ids
     * @param dataContainer       Holds the information of existing company attributes
     * @param targetContainer     Holds the updated information of company attributes
     */
    private void loadCompanyTimelineIntoContainer(final long orgId, final long userId, final Long companyId, final Collection<Long> duplicateCompanyIds,
                                                  final DataContainer dataContainer, final DataContainer targetContainer) {
        final Table companyTable = companyManager.getTable();
        final Row primaryRow = dataContainer.getRow(companyTable, companyId);

        if (Objects.nonNull(primaryRow)) {
            final Collection<String> duplicateCompanyNames = dataContainer.getRows(companyTable, new WhereClause(Criteria.in(companyTable.getPKColumn(),
                    duplicateCompanyIds))).filter(Objects::nonNull).map(row -> (String) row.get(MCOCOMPANY.NAME)).toList();

            loadMergeTimelineIntoContainer(orgId, userId, Timeline.Category.COMPANY, duplicateCompanyIds, companyId,
                    new FieldTimeline(duplicateCompanyNames, (String) primaryRow.get(MCOCOMPANY.NAME), new I18nKey("i18n.company.fields.name")),
                    dataContainer, targetContainer);
        }
    }

    /**
     * <p>
     * Before merging need to update all duplicate company options with primary company
     * </p>
     *
     * @param primaryCompanyId    Represents the company id
     * @param duplicateCompanyIds {@link Collection<Long>} of duplicate company ids
     * @param dataContainer       A container that holds the user defined field values and system values
     * @param targetContainer     Holds the updated information of user defined and system field values
     */
    private void loadCompanyOptionRowIntoContainer(final Long primaryCompanyId, final Collection<Long> duplicateCompanyIds,
                                                   final DataContainer dataContainer, final DataContainer targetContainer) {
        final Collection<FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Company.ENTITY_TYPE,
                CompanyFields.INDUSTRY_TYPE, CompanyFields.ORGANIZATION_STATUS,
                CompanyFields.BUSINESS_TYPE).values();

        for (final FieldInfo fieldInfo : fields) {
            final Long fieldId = fieldInfo.id();
            final Table companyOptionTable = companyManager.getCompanyOptionTable();
            final Column entityIdColumn = companyOptionTable.getColumn(MCOMDROPDOWNFIELDS.ENTITY_ID);
            final Column fieldIdColumn = companyOptionTable.getColumn(MCOMDROPDOWNFIELDS.FIELD_ID);
            final Row companyOptionRow = dataContainer.getRow(companyOptionTable, new WhereClause(Criteria.eq(entityIdColumn, primaryCompanyId)).and(Criteria.eq(fieldIdColumn, fieldId)));

            if (Objects.isNull(companyOptionRow)) {

                for (final Long duplicateCompanyId : duplicateCompanyIds) {
                    final Row duplicateCompanyOptionRow = dataContainer.getRow(companyOptionTable, new WhereClause(Criteria.eq(entityIdColumn, duplicateCompanyId)).and(Criteria.eq(fieldIdColumn, fieldId)));

                    if (Objects.nonNull(duplicateCompanyOptionRow)) {
                        duplicateCompanyOptionRow.set(entityIdColumn, primaryCompanyId);
                        targetContainer.updateRow(duplicateCompanyOptionRow);
                        break;
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Converts row into {@link Company}
     * </p>
     *
     * @param row Company row
     * @return {@link Company}
     */
    private Company convertRowToModel(final Row row) {
        final Company company = new Company();
        final Long salesOwnerId = row.get(MCOCOMPANY.OWNER_ID);
        final Long createdById = row.get(MCOCOMPANY.CREATED_BY);
        final Long updatedById = row.get(MCOCOMPANY.UPDATED_BY);
        final Long orgId = row.getOrgId();
        final Collection<Long> userIds = collectUserIds(createdById, updatedById, salesOwnerId);

        company.setId(row.getPKValue());
        company.setOrgId(orgId);
        company.setName(row.get(MCOCOMPANY.NAME));
        company.setModifiedTime(row.get(MCOCOMPANY.MODIFIED_TIME));
        company.setCreatedTime(row.get(MCOCOMPANY.CREATED_TIME));
        company.setWebsite(row.get(MCOCOMPANY.WEBSITE));
        company.setEmployeeCount(row.get(MCOCOMPANY.NUMBER_OF_EMPLOYEE));
        company.setSicCode(row.get(MCOCOMPANY.SIC_CODE));
        company.setTwitter(row.get(MCOCOMPANY.TWITTER));
        company.setLinkedIn(row.get(MCOCOMPANY.LINKEDIN));
        company.setFacebook(row.get(MCOCOMPANY.FACEBOOK));
        company.setParentCompany(row.get(MCOCOMPANY.PARENT_COMPANY));
        company.setOwnerId(salesOwnerId);
        company.setRecentNote(row.get(MCOCOMPANY.RECENT_NOTE));
        company.setTerritory(buildPartialTerritory(row.get(MCOCOMPANY.TERRITORY_ID)));

        company.setUserIds(userIds);
        setCompanyUsers(company, salesOwnerId, createdById, updatedById, getUsersById(orgId, userIds));

        return company;
    }

    /**
     * Sets createdBy/updatedBy/salesOwner from a pre-fetched users map.
     */
    private void setCompanyUsers(final Company company, final Long salesOwnerId, final Long createdById,
                                 final Long updatedById, final Map<Long, CrmUser> usersById) {
        company.setSalesOwner(usersById.get(salesOwnerId));
        company.setCreatedBy(usersById.get(createdById));
        company.setUpdatedBy(usersById.get(updatedById));
    }

    /**
     * <p>
     * Fetches the collection of possible duplicate records by primary company name and website
     * </p>
     *
     * @param orgId          Represents the organization id
     * @param primaryCompany An entity that holds the company attributes
     * @param start          Represents the start value
     * @param limit          Represents the limit value
     * @return {@link Collection} of duplicate companies by primary company values
     */
    public Collection<Company> getDuplicateCompanies(final Long orgId, final Company primaryCompany, final int start,
                                                     final int limit) {
        final Table companyTable = companyManager.getTable();
        final Column createdTimeColumn = companyTable.getColumn(MCOCOMPANY.CREATED_TIME);
        final SelectQuery selectQuery = SelectQuery.get(companyTable);

        selectQuery.addSelectClause(companyTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(companyTable.getColumn(MCOCOMPANY.NAME), primaryCompany.getName())));
        selectQuery.setLimitClause(new LimitClause(start, limit));
        selectQuery.addOrderByClause(new OrderByClause(createdTimeColumn));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(companyTable).map(this::convertRowToModel).toList();
    }

    /**
     * <p>
     * Fetches the primary company values for given company id
     * </p>
     *
     * @param orgId     Represents the organization id
     * @param companyId Represents the company id
     * @return {@link Company} which contains primary company value
     */
    public Company getPrimaryCompany(final Long orgId, final Long companyId) {
        final Table companyTable = companyManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(companyTable);

        selectQuery.addSelectClause(companyTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(companyTable.getPKColumn(), companyId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return convertRowToModel(dataContainer.getRow(companyTable));
    }

    /**
     * <p>
     * Retrieves the total duplicate company count by using primary company name and website
     * </p>
     *
     * @param orgId          Represents the organization id
     * @param primaryCompany An entity that holds the company attributes
     * @return the {@link Long} of duplicate company count
     */
    public Long getDuplicateCompanyCount(final Long orgId, final Company primaryCompany) {
        final Table companyTable = companyManager.getTable();
        final CountQuery countQuery = CountQuery.get(companyTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(companyTable.getColumn(MCOCOMPANY.NAME), primaryCompany.getName())));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * Fetches the primary and secondary company using company name
     *
     * @param orgId Represents the organization id
     * @param limit Represents the limit value
     * @param start Represents the start value
     * @return {@link List} of companies by duplicate company name
     */
    public Collection<List<Company>> getCompaniesByDuplicateName(final Long orgId, final int limit, final int start) {
        final List<List<Company>> companyList = new ArrayList<>();
        final Table companyTable = companyManager.getTable();
        final Column companyNameColumn = companyTable.getColumn(MCOCOMPANY.NAME);
        final String nameKey = CompanyFields.NAME;
        final FieldInfo fieldInfo = fieldService.groupDefaultFieldsByAliasName(Company.ENTITY_TYPE, nameKey).get(nameKey);
        final Collection<String> dismissedCompanies = getDismissedValues(orgId, fieldInfo.sourceId(), fieldInfo.id());
        final WhereClause whereClause = new WhereClause(Criteria.eq(companyTable.getColumn(MCOCOMPANY.ORG_ID), orgId));
        final Column createdTimeColumn = companyTable.getColumn(MCOCOMPANY.CREATED_TIME);
        final SelectQuery duplicateQuery = SelectQuery.get(companyTable);
        final Column maxCreatedTimeColumn = createdTimeColumn.max();

        duplicateQuery.addSelectClause(List.of(companyNameColumn, companyTable.getPKColumn().min(), maxCreatedTimeColumn));
        duplicateQuery.addGroupByClause(new GroupByClause(companyNameColumn));
        duplicateQuery.addHavingClause(new HavingClause(Criteria.gt(companyNameColumn.count(), 1)));
        duplicateQuery.addOrderByClause(new OrderByClause(maxCreatedTimeColumn));
        duplicateQuery.setLimitClause(new LimitClause(start, limit));

        if (!dismissedCompanies.isEmpty()) {
            duplicateQuery.setWhereClause(whereClause.and(Criteria.notIn(companyNameColumn, dismissedCompanies)));
        }

        final List<String> companyNames = orgDataStore(orgId).get(duplicateQuery)
                .getRows(companyTable).map(row -> (String) row.get(MCOCOMPANY.NAME)).toList();

        if (!companyNames.isEmpty()) {
            final SelectQuery selectQuery = SelectQuery.get(companyTable);

            selectQuery.addSelectClause(List.of(companyNameColumn, companyTable.getColumn(MCOCOMPANY.OWNER_ID), companyTable.getColumn(MCOCOMPANY.ORG_ID)));
            selectQuery.addOrderByClause(new OrderByClause(createdTimeColumn));
            selectQuery.setWhereClause(new WhereClause(Criteria.in(companyNameColumn, companyNames)));
            final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

            if (!dataContainer.isEmpty()) {
                final Set<String> companySet = new HashSet<>();
                final Stream<Row> companyRows = dataContainer.getRows(companyTable);

                companyRows.filter(Objects::nonNull).forEach(row -> buildCompany(row, dataContainer, companyNameColumn,
                        companyList, companySet));
            }
        }

        return companyList;
    }

    /**
     * <p>
     * Fetches the duplicate companies count present in an organization
     * </p>
     *
     * @param orgId Represents an organization id
     * @return the total records
     */
    public int getDuplicateCompaniesCount(final long orgId) {
        final Table companyTable = companyManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(companyTable);
        final Column companyNameColumn = companyTable.getColumn(MCOCOMPANY.NAME);
        final Column companyNameCountColumn = companyNameColumn.count();
        final String nameKey = CompanyFields.NAME;
        final FieldInfo fieldInfo = fieldService.groupDefaultFieldsByAliasName(Company.ENTITY_TYPE, nameKey).get(nameKey);
        final Collection<String> dismissedCompanies = getDismissedValues(orgId, fieldInfo.sourceId(), fieldInfo.id());

        selectQuery.addSelectClause(companyNameCountColumn);
        selectQuery.addGroupByClause(new GroupByClause(companyNameColumn));
        selectQuery.addHavingClause(new HavingClause(Criteria.gt(companyNameCountColumn, 1)));

        if (!dismissedCompanies.isEmpty()) {
            selectQuery.setWhereClause(new WhereClause(Criteria.notIn(companyTable.getColumn(MCOCOMPANY.NAME), dismissedCompanies)));
        }
        return orgDataStore(orgId).getData(selectQuery).size();
        // TODO replace with column-receiver
//        final Counter duplicateCounter = new Counter();
//
//        orgDataStore(orgId).fetchData(selectQuery, (ColumnReceiver) value -> duplicateCounter.increment());
//
//        return duplicateCounter.getCount();
    }

    /**
     * <p>
     * Builds the company fields
     * </p>
     *
     * @param row                Represents the Company row
     * @param dataContainer      A container that holds the user defined field values and system values
     * @param companyValueColumn Represents the company column
     * @param companyList        {@link Collection} of list contains company details
     * @param companySet         {@link Collection} of set contains company column value
     */
    private void buildCompany(final Row row, final DataContainer dataContainer, final Column companyValueColumn,
                              final Collection<List<Company>> companyList, final Collection<String> companySet) {
        final String value = row.get(companyValueColumn);
        final String companyName = value.toLowerCase();

        if (!companySet.contains(companyName)) {
            final Collection<Row> indexedCompanyRows = dataContainer.getRows(companyManager.getTable(), new WhereClause(Criteria
                    .eq(companyValueColumn, companyName))).limit(2).toList();

            if (indexedCompanyRows.size() > 1) {
                final List<Company> companies = new ArrayList<>();

                for (final Row companyRow : indexedCompanyRows) {
                    final Company company = new Company();
                    final Long orgId = companyRow.getOrgId();

                    company.setId(companyRow.getPKValue());
                    company.setOrgId(orgId);
                    company.setOwnerId(companyRow.get(MCOCOMPANY.OWNER_ID));
                    company.setName(companyRow.get(MCOCOMPANY.NAME));
                    companies.add(company);
                    companySet.add(companyName);
                }
                companyList.add(companies);
            }
        }
    }
}
