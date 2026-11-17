package com.tlc.crm.core.api.contact.manager;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.attachment.model.profilePicture.ProfilePicture;
import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.iam.api.manager.TerritoryManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;

import com.tlc.crm.core.action.service.EmailSummaryActionService;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.insights.service.EntityInsightsToSummaryInfoMapper;
import com.tlc.crm.core.api.summaryInfo.model.SummaryInfo;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.entity.followers.records.UserFollowerUpdateRecords;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.features.api.followers.source.manager.SourceFollowersManager;
import com.tlc.crm.core.features.api.followers.source.model.SourceFollowers;
import com.tlc.crm.core.features.api.tag.SourceTagManager;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.core.api.deal.model.DealStatistics;
import com.tlc.crm.core.audit.AuditLogger;
import com.tlc.crm.core.field.contact.CompanyFields;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.field.fieldAudit.resolver.CompanyFieldAuditResolver;
import com.tlc.crm.core.internal.contact.status.ContactErrorCodes;
import com.tlc.crm.core.sql.resource.MACCOMPANYACTIVITY;
import com.tlc.crm.core.sql.resource.MCOCOMPANY;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MCOMADDRESSFIELDS;
import com.tlc.crm.core.sql.resource.MCOMCOMPANYFIELDS;
import com.tlc.crm.core.sql.resource.OMDPDISPLAYPICTURE;
import com.tlc.crm.core.sql.resource.MCOMDISPLAYPICTURE;
import com.tlc.crm.core.sql.resource.MCOMMONETARYFIELDS;
import com.tlc.crm.core.sql.resource.MCOMDROPDOWNFIELDS;
import com.tlc.crm.core.sql.resource.MCOMPHONEFIELDS;
import com.tlc.crm.core.sql.resource.MCONCOMPANYFIELDS;
import com.tlc.crm.core.sql.resource.MDOCOMPANYFIELDS;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.OMCOMFOLLOWERS;
import com.tlc.crm.entity.audit.log.model.AuditAction;
import com.tlc.crm.entity.audit.log.model.AuditLogData;
import com.tlc.crm.entity.audit.log.model.AuditSource;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.crm.entity.handler.EntityHandler;
import com.tlc.crm.entity.manager.EntityConfigManager;
import com.tlc.crm.entity.manager.field.type.AddressManager;
import com.tlc.crm.entity.manager.field.type.MonetaryManager;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.models.internal.DataLoaderValues;
import com.tlc.crm.entity.models.type.Address;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.OptionService;
import com.tlc.crm.entity.sql.resource.MTLCOMPANYTIMELINE;
import com.tlc.crm.entity.timeline.api.CommonTimelineManager;
import com.tlc.crm.entity.timeline.field.TimelineActionFields;
import com.tlc.crm.entity.fieldaudit.FieldAuditHandler;
import com.tlc.crm.entity.fieldaudit.models.FieldAuditInfo;
import com.tlc.crm.entity.timeline.model.FieldTimeline;
import com.tlc.crm.entity.timeline.model.Timeline;
import com.tlc.crm.entity.timeline.model.TimelineType;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.sql.resource.OMETADDRESS;
import com.tlc.crm.security.sql.resource.OMCRMUSER;
import com.tlc.i18n.I18nKey;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.ds.OrgDataStore;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.model.ErrorDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Implements the {@link Company} related DB actions
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 */
public class CompanyManager extends EntityConfigManager<Company> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompanyManager.class);

    private final String companyNotFoundI18NKey = "i18n.company.not.found";
    private final EntityHandler entityHandler;
    private final MonetaryManager monetaryManager;
    private final AddressManager addressManager;
    private final CommonTimelineManager commonTimelineManager;
    private final OptionService optionService;
    private final UserService userService;
    private final AuditLogger auditLogger;
    private final SourceFollowersManager sourceFollowersManager;
    private final TerritoryManager territoryManager;
    private final FieldAuditHandler fieldAuditHandler;
    private final CompanyFieldAuditResolver fieldAuditResolver;
    private final EntityInsightsToSummaryInfoMapper entityInsightsToSummaryInfoMapper;

    private static class Instance {
        private static final CompanyManager INSTANCE = new CompanyManager();
    }

    private CompanyManager() {
        entityHandler = EntityHandler.getInstance();
        monetaryManager = MonetaryManager.getInstance();
        addressManager = AddressManager.getInstance();
        commonTimelineManager = CommonTimelineManager.getInstance();
        userService = UserService.getInstance();
        optionService = OptionService.getInstance();
        auditLogger = AuditLogger.getInstance();
        sourceFollowersManager = SourceFollowersManager.getInstance();
        territoryManager = TerritoryManager.getInstance();
        fieldAuditHandler = FieldAuditHandler.getInstance();
        fieldAuditResolver = CompanyFieldAuditResolver.getInstance();
        entityInsightsToSummaryInfoMapper = EntityInsightsToSummaryInfoMapper.getInstance();
    }

    public static CompanyManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getTable() {
        return Table.get(MCOCOMPANY.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @return Company follower table.
     */
    @Override
    protected Table getFollowersTable() {
        return Table.get(OMCOMFOLLOWERS.TABLE);
    }

    /**
     * Gets the company option table
     *
     * @return {@link Table}
     */
    public Table getCompanyOptionTable() {
        return Table.get(MCOMDROPDOWNFIELDS.TABLE);
    }

    /**
     * Gets the company monetary table
     *
     * @return {@link Table}
     */
    public Table getCompanyMonetaryTable() {
        return Table.get(MCOMMONETARYFIELDS.TABLE);
    }

    /**
     * Gets the company address table
     *
     * @return {@link Table}
     */
    public Table getCompanyAddressTable() {
        return Table.get(MCOMADDRESSFIELDS.TABLE);
    }

    /**
     * Gets the company phone table
     *
     * @return {@link Table}
     */
    public Table getCompanyPhoneTable() {
        return Table.get(MCOMPHONEFIELDS.TABLE);
    }

    /**
     * <p>
     * Gets the company fields table
     * </p>
     *
     * @return {@link Table} of company fields
     */
    public Table getCompanyFieldsTable() {
        return Table.get(MCOMCOMPANYFIELDS.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link EntityHandler}
     */
    @Override
    protected EntityHandler getEntityHandler() {
        return entityHandler;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Column}
     */
    @Override
    protected Optional<Column> getTerritoryColumn() {
        return Optional.ofNullable(getTable().getColumn(MCOCOMPANY.TERRITORY_ID));
    }

    /**
     * <p>
     * Fetches the {@link Company} that has one of the id value mentioned in the parameter
     * </p>
     *
     * @param orgId Organization id
     * @param ids   Companies id
     * @return Collection of companies
     */
    @Override
    public Collection<Company> get(Long orgId, Collection<Long> ids) {
        final Table companyTable = getTable();
        final DataContainer dataContainer = getCompanyDataContainer(orgId, ids);
        final Stream<Row> rows = Objects.nonNull(dataContainer) ? dataContainer.getRows(companyTable) : null;

        if (Objects.isNull(rows)) {
            throw ErrorCode.get(ContactErrorCodes.COMPANY_NOT_FOUND, companyNotFoundI18NKey);
        } else {
            return rows.map(row -> convertRowToModel(row, false, dataContainer)).toList();
        }
    }

    /**
     * Gets the {@link SummaryInfo} of a company.
     *
     * @param orgId   Refers the organization id.
     * @param company Refers the {@link Company} for which the summary details is to be fetched.
     */
    public final void loadSummaryDetails(final Long orgId, final Company company) {
        final Long companyId = company.id();

        if (Objects.nonNull(companyId)) {
            final SummaryInfo summaryInfo = new SummaryInfo();
            final String entityType = Company.ENTITY_TYPE;

            summaryInfo.setEmailSummary(EmailSummaryActionService.getInstance().get(orgId, companyId, Source.COMPANY));
            summaryInfo.setActivitySummary(SourceActivityManager.getInstance().getActivitySummary(orgId, companyId, entityType));
            summaryInfo.setDealStatistics(getDealStatistics(orgId, companyId));
            summaryInfo.setTags(SourceTagManager.getInstance().getMappedTags(orgId, companyId, entityType));
            summaryInfo.setDealsByState(DealManager.getInstance().getCompanyDeals(orgId, companyId));

            company.setSummaryInfo(summaryInfo);
        }
    }

    /**
     * <p>
     * Loads the summary info data
     * </p>
     *
     * @param orgId   Represents the organization id.
     * @param company Represents the company whose summary info will be set.
     */
    public final void loadSummaryInfo(final Long orgId, final Company company) {
        final Long companyId = company.id();

        if (Objects.nonNull(companyId)) {
            final SummaryInfo summaryInfo = entityInsightsToSummaryInfoMapper.buildSummaryInfo(orgId, companyId, Source.COMPANY);

            summaryInfo.setTags(SourceTagManager.getInstance().getMappedTags(orgId, companyId, Company.ENTITY_TYPE));
            summaryInfo.setDealsByState(DealManager.getInstance().getCompanyDeals(orgId, companyId));

            company.setSummaryInfo(summaryInfo);
        }
    }

    /**
     * Fetches the {@link DataContainer} that contains the company data
     *
     * @param orgId      Represents the organisation ID
     * @param companyIds Which contains the {@link Collection} of company IDs
     * @return {@link DataContainer} that contains the company data
     */
    public DataContainer fetchCompanyContainer(final Long orgId, final Collection<Long> companyIds) {
        return getCompanyDataContainer(orgId, companyIds);
    }

    /**
     * Gets the company dataContainer
     *
     * @param orgId Represents the organisation id
     * @param ids   Collection of ids that have to be gets the company details
     * @return the company {@link DataContainer}
     */
    public DataContainer getCompanyDataContainer(final Long orgId, final Collection<Long> ids) {
        if (isEntityIdCollectionEmpty(ids)) {
            return DataContainer.create();
        }
        final Table companyTable = getTable();
        final Collection<FieldInfo> companyFields = secureFieldService.getFieldsBySource(orgId, Company.ENTITY_TYPE);
        final SelectQuery selectQuery = SelectQuery.get(companyTable);

        selectQuery.addSelectClause(companyTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(companyTable.getPKColumn(), ids)));
        SourceTagManager.getInstance().loadJoinsAndSelectClause(Source.COMPANY, companyTable, selectQuery);

        return getEntityContainer(orgId, ids, selectQuery, companyFields);
    }

    /**
     * Gets the company dataContainer
     *
     * @param orgId Represents the organisation id
     * @return the company {@link DataContainer}
     */
    public DataContainer getCompanyDataContainer(final Long orgId, final SelectQuery selectQuery, final Collection<Long> ids) {
        if (isEntityIdCollectionEmpty(ids)) {
            return DataContainer.create();
        }
        SourceTagManager.getInstance().loadJoinsAndSelectClause(Source.COMPANY, getTable(), selectQuery);
        final Collection<FieldInfo> companyFields = secureFieldService.getFieldsBySource(orgId, Company.ENTITY_TYPE);

        return getEntityContainer(orgId, ids, selectQuery, companyFields);
    }

    /**
     * <p>
     * Fetches the {@link Company} for the given id.
     * </p>
     *
     * @param orgId  Represents the organisation id.
     * @param id     Company id.
     * @param fields Required fields.
     * @return {@link Company} holds the details of the company.
     */
    public Company get(final Long orgId, final Long id, final Collection<FieldInfo> fields) {
        final Table table = getTable();
        final DataContainer dataContainer = getEntityContainer(orgId, List.of(id), fields);
        final Row row = dataContainer.getRow(table);

        return Objects.isNull(row) ? null : convertRowToModel(row, true, dataContainer);
    }

    /**
     * <p>
     * Fetches the user defined entity of an organization along with system entity
     * </p>
     *
     * @param orgId Organization id
     * @return Collection of entity
     */
    private Collection<FieldInfo> getCompanyFields(final Long orgId) {
        return secureFieldService.getFieldsBySource(orgId, Company.ENTITY_TYPE);
    }

    /**
     * <p>
     * Saves the attributes of {@link Company} into the database
     * </p>
     *
     * @param company An entity that holds the company attributes which have to be saved into database
     */
    @Override
    public Company create(Company company) {
        final long orgId = company.orgId();
        final DataContainer targetContainer = DataContainer.create();

        loadCompanyIntoContainer(true, company, targetContainer);
        sourceFollowersManager.loadFollowerIfExists(orgId, company.getSalesOwner(), company.id(), Source.COMPANY, targetContainer);
        orgDataStore(orgId).commitChanges(targetContainer);

        auditLogger.log(orgId, List.of(new AuditLogData(company.getCreatedBy().id(), company.id(), company, AuditSource.COMPANY, AuditAction.ADDED)));

        return company;
    }

    /**
     * <p>
     * Loads the company details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param company         An entity that holds the company attributes
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    public void loadCompanyIntoContainer(final boolean isCreateEvent, final Company company, final DataContainer targetContainer) {
        final Long orgId = company.orgId();
        final Long id = company.id();
        final Collection<FieldInfo> fields = getCompanyFields(orgId);
        final DataContainer entityContainer = isCreateEvent ? null
                : entityLoader.getEntityContainerByIds(orgId, getTable(), fields, List.of(id));

        loadCompanyIntoContainer(isCreateEvent, fields, company, entityContainer, targetContainer);
    }

    /**
     * <p>
     * Loads the company details into container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param fields          Company entity that has to be loaded into the container
     * @param company         An entity that holds the company attributes
     * @param sourceContainer Holds the user defined and system field values of existing company
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    public void loadCompanyIntoContainer(final boolean isCreateEvent, final Collection<FieldInfo> fields, final Company company, final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table companyTable = getTable();
        final Long id = company.id();
        final Long orgId = company.orgId();
        final Row existingCompanyRow = Objects.isNull(sourceContainer) ? null : sourceContainer.getRow(companyTable, id);
        final Row companyRow = isCreateEvent ? new Row(companyTable) : Objects.isNull(existingCompanyRow) ? null : new Row(existingCompanyRow);
        final Company parentCompany = company.getParentCompany();
        final CrmUser salesOwner = company.getSalesOwner();
        final long currentTime = Instant.now().toEpochMilli();
        final SourceTagManager sourceTagManager = SourceTagManager.getInstance();
        final Source source = Source.COMPANY;
        final Map<String, String> existingCompanyValues = new HashMap<>();
        final DateTime createdTime = company.getCreatedTime();

        if (!isCreateEvent && Objects.isNull(companyRow)) {
            throw ErrorCode.get(ContactErrorCodes.COMPANY_NOT_FOUND, companyNotFoundI18NKey);
        }

        if (companyRow.isNewRow()) {
            companyRow.setOrgId(company.orgId());
            companyRow.set(MCOCOMPANY.CREATED_TIME, Objects.isNull(createdTime) ? currentTime : createdTime);
            companyRow.set(MCOCOMPANY.LAST_ASSIGNED_AT, currentTime);
            companyRow.set(MCOCONTACT.CREATED_BY, company.getCreatedBy().id());
            company.setId(companyRow.getPKValue());
            company.setEntityState(Entity.State.NEW);
        } else {
            loadExistingValues(existingCompanyValues, companyRow, sourceContainer);
            company.setEntityState(Entity.State.EXISTING);

            if (Objects.nonNull(createdTime)) {
                companyRow.setIfModified(MCOCOMPANY.CREATED_TIME, createdTime);
            }
        }

        if (!isCreateEvent) {
            loadFieldAudit(company, targetContainer, sourceContainer);
        }
        final CrmUser updatedBy = company.getUpdatedBy();
        final Territory territory = company.getTerritory();

        companyRow.set(MCOCOMPANY.MODIFIED_TIME, Instant.now().toEpochMilli());
        companyRow.set(MCOCOMPANY.NAME, company.getName());
        companyRow.set(MCOCOMPANY.WEBSITE, company.getWebsite());
        companyRow.set(MCOCOMPANY.NUMBER_OF_EMPLOYEE, company.getEmployeeCount());
        companyRow.set(MCOCOMPANY.SIC_CODE, company.getSicCode());
        companyRow.set(MCOCOMPANY.LINKEDIN, company.getLinkedIn());
        companyRow.set(MCOCOMPANY.TWITTER, company.getTwitter());
        companyRow.set(MCOCOMPANY.FACEBOOK, company.getFacebook());
        companyRow.set(MCOCOMPANY.OWNER_ID, Objects.isNull(salesOwner) ? null : salesOwner.id());
        companyRow.set(MCOCOMPANY.UPDATED_BY, Objects.isNull(updatedBy) ? null : updatedBy.id());
        companyRow.set(MCOCOMPANY.TERRITORY_ID, Objects.isNull(territory) ? null : territory.id());
        companyRow.set(MCOCOMPANY.DESCRIPTION, company.getDescription());
        defaultFieldsLoader.loadData(new DataLoaderValues<>(isCreateEvent, companyTable, company, fields,
                sourceContainer, targetContainer));

        final Long companyId = company.id();
        final boolean isParentCompanyNull = Objects.isNull(parentCompany);

        if (!isParentCompanyNull) {
            loadCompanyFieldsIntoContainer(orgId, company, isCreateEvent, targetContainer);
        }
        final SummaryInfo summaryInfo = company.getSummaryInfo();
        final Collection<Tag> tags = Objects.nonNull(summaryInfo) ? summaryInfo.getTags() : Collections.emptyList();

        if (!tags.isEmpty()) {
            sourceTagManager.mapTagsWithSource(isCreateEvent, orgId, companyRow.getPKValue(), tags, source,
                    sourceContainer, targetContainer);
        }

        loadCompanyTimelineIntoContainer(isCreateEvent, company, companyRow, sourceContainer, targetContainer, existingCompanyValues);

        if (!isCreateEvent) {
            if (isParentCompanyNull) {
                deleteUnmappedParentCompany(orgId, companyId, targetContainer);
            }
            sourceTagManager.deleteMappedTags(companyId, tags, source, sourceContainer, targetContainer);
            final SourceFollowers sourceFollowers = new SourceFollowers();

            sourceFollowers.setOrgId(orgId);
            sourceFollowers.setSource(Source.COMPANY);
            sourceFollowers.setEntityId(id);
            sourceFollowersManager.removeSourceFollower(sourceFollowers, company.getOwnerId(), targetContainer);
        }
        final String nameKey = CompanyFields.NAME;
        final FieldInfo fieldInfo = fieldService.groupDefaultFieldsByAliasName(Company.ENTITY_TYPE, nameKey).get(nameKey);

        deleteDismissedValues(orgId, fieldInfo.sourceId(), fieldInfo.id(), company.getName(), targetContainer);

        targetContainer.updateRow(companyRow);
        entityLoader.buildEntity(isCreateEvent, company, fields, companyTable, sourceContainer, targetContainer);
    }

    /**
     * <p>
     * Loads the company fields into container
     * </p>
     *
     * @param company         An entity that holds the company attributes
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    private void loadFieldAudit(final Company company, final DataContainer targetContainer, final DataContainer sourceContainer) {
        try {
            fieldAuditResolver.loadFieldAudit(company, sourceContainer.getRow(getTable(), company.id()), sourceContainer, targetContainer);
        } catch (Exception exception) {
            LOGGER.error("Field edit history : Error while loading history for company id: {}", company.id(), exception);
        }
    }

    /**
     * <p>
     * Loads the existing values of the company fields.
     * </p>
     *
     * @param existingCompanyRow    Represents the existing company row data
     * @param existingCompanyValues Holds the existing values of company fields
     */
    private void loadExistingValues(final Map<String, String> existingCompanyValues, final Row existingCompanyRow, final DataContainer sourceContainer) {
        final DateTime createdTime = existingCompanyRow.get(MCOCOMPANY.CREATED_TIME);

        if (Objects.nonNull(createdTime)) {
            existingCompanyValues.put(CompanyFields.CREATED_AT, String.valueOf(createdTime.getTimeStamp()));
        }
    }

    /**
     * <p>
     * Loads the {@link Company} timeline into target {@link DataContainer}.
     * </p>
     *
     * @param isCreateEvent   A flag which represents the event is create or not
     * @param company         Represents the {@link Company}
     * @param companyRow      Represents the company row
     * @param sourceContainer Holds the existing company details in the source {@link DataContainer}
     * @param targetContainer Holds the updated company details into the target {@link DataContainer}
     */
    private void loadCompanyTimelineIntoContainer(final boolean isCreateEvent, final Company company, final Row companyRow,
                                                  final DataContainer sourceContainer, final DataContainer targetContainer,
                                                  final Map<String, String> oldValues) {
        final CrmUser salesOwner = company.getSalesOwner();
        final String nameKey = CompanyFields.NAME;
        final String salesOwnerKey = CompanyFields.SALES_OWNER;
        final String createdAtKey = CompanyFields.CREATED_AT;
        final Map<String, FieldInfo> companyFieldInfo = fieldService.groupDefaultFieldsByAliasName(company.orgId(),
                Company.ENTITY_TYPE, List.of(nameKey, salesOwnerKey, createdAtKey));
        final Row existingCompanyRow = Objects.isNull(sourceContainer) ? null : sourceContainer.getRow(getTable(), company.id());
        final String oldCompanyName = Objects.isNull(existingCompanyRow) ? null : existingCompanyRow.get(MCOCOMPANY.NAME);
        final String companyName = company.getName();
        final DateTime createdTime = company.getCreatedTime();
        final String oldCreatedTime = oldValues.get(CompanyFields.CREATED_AT);
        final String newCreatedTime = Objects.isNull(createdTime) ? null : createdTime.getTimeStamp().toString();

        if (isCreateEvent) {
            final Timeline timeline = buildTimeline(true, companyRow, company);
            final FieldTimeline fieldTimeline = new FieldTimeline(Collections.emptyList(), companyName,
                    companyFieldInfo.get(nameKey).i18nName());

            timeline.setFieldTimeline(fieldTimeline);
            loadCompanyTimelineIntoContainer(timeline, companyRow.getPKValue(), targetContainer);
        } else {

            if (Objects.nonNull(salesOwner) && Objects.nonNull(existingCompanyRow)) {
                final Long existingOwnerId = existingCompanyRow.get(MCOCOMPANY.OWNER_ID);
                final Long updatedOwnerId = salesOwner.id();

                if (!Objects.equals(existingOwnerId, updatedOwnerId)) {
                    companyRow.set(MCOCOMPANY.LAST_ASSIGNED_AT, Instant.now().toEpochMilli());

                    buildAndLoadTimeline(companyRow, company, String.valueOf(existingOwnerId), String.valueOf(updatedOwnerId),
                            companyFieldInfo.get(salesOwnerKey).i18nName(), salesOwnerKey, targetContainer);
                }
            }

            if (Objects.nonNull(companyName) && !Objects.equals(oldCompanyName, companyName)) {
                buildAndLoadTimeline(companyRow, company, oldCompanyName, companyName,
                        companyFieldInfo.get(nameKey).i18nName(), nameKey, targetContainer);
            }

            if (Objects.nonNull(newCreatedTime) && !Objects.equals(oldCreatedTime, newCreatedTime)) {
                buildAndLoadTimeline(companyRow, company, oldCreatedTime, newCreatedTime,
                        companyFieldInfo.get(createdAtKey).i18nName(), createdAtKey, targetContainer);
            }
        }
    }

    /**
     * <p>
     * Builds and loads the company timeline.
     * </p>
     *
     * @param companyRow      Represents the company row
     * @param company         Represents the {@link Company}
     * @param oldValue        Represents the old value
     * @param newValue        Represents the new value
     * @param fieldName       Represents the field name
     * @param companyField    Represents the company field
     * @param targetContainer Holds the updated company details into the target {@link DataContainer}
     */
    private void buildAndLoadTimeline(final Row companyRow, final Company company, final String oldValue,
                                      final String newValue, final I18nKey fieldName,
                                      final String companyField, final DataContainer targetContainer) {
        final Timeline timeline = buildTimeline(false, companyRow, company);

        switch (companyField) {
            case "name" -> {
                if (Objects.nonNull(oldValue)) {
                    timeline.setTimelineType(TimelineType.COMPANY_NAME_UPDATED);
                    timeline.setAction(TimelineActionFields.UPDATED);
                }
            } case "salesOwner" -> {
                timeline.setTimelineType(TimelineType.COMPANY_SALES_OWNER_REASSIGNED);
                timeline.setAction(TimelineActionFields.REASSIGNED);
            }
            case "createdAt" -> {
                timeline.setTimelineType(TimelineType.COMPANY_CREATED_TIME_CHANGED);
                timeline.setAction(TimelineActionFields.UPDATED);
            }
        }

        if (!Objects.equals(oldValue, newValue)) {
            final Collection<String> oldValues = Optional.ofNullable(oldValue).map(Collections::singletonList).orElseGet(Collections::emptyList);
            final FieldTimeline fieldTimeline = new FieldTimeline(oldValues, newValue, fieldName);

            timeline.setFieldTimeline(fieldTimeline);
        }

        loadCompanyTimelineIntoContainer(timeline, companyRow.getPKValue(), targetContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents the organisation ID
     * @param companies Which contains the collection of {@link Company}
     */
    @Override
    public final void delete(final Long orgId, final Collection<Company> companies) {
        if (Objects.nonNull(companies) && !companies.isEmpty()) {
            final Map<Integer, Collection<FieldInfo>> groupedFields = fieldService.groupAllFieldsByType(orgId, Company.ENTITY_TYPE);
            final Table companyTable = getTable();
            final Set<Long> entityIds = new HashSet<>();
            final Collection<ValueHolder> valueHolders = new ArrayList<>();
            final Set<Long> annualRevenueIds = new HashSet<>();
            final Set<Long> addressIds = new HashSet<>();
            final AuditSource auditSource = AuditSource.COMPANY;
            final AuditAction auditAction = AuditAction.DELETED;
            final Collection<AuditLogData> auditLogDataList = new ArrayList<>();

            for (final Company company : companies) {
                final Monetary annualRevenue = company.getAnnualRevenue();
                final Address address = company.getAddress();

                entityIds.add(company.id());
                valueHolders.add(company.getValueHolder());

                if (Objects.nonNull(annualRevenue)) {
                    annualRevenueIds.add(annualRevenue.id());
                }

                if (Objects.nonNull(address)) {
                    addressIds.add(address.id());
                }
                auditLogDataList.add(new AuditLogData(company.getUpdatedBy().id(), company.id(), company, auditSource, auditAction));
            }
            final List<DeleteQuery> deleteQueries = entityHandler.buildEntityDeleteQueries(companyTable, groupedFields,
                    new WhereClause(Criteria.in(companyTable.getPKColumn(), entityIds)), valueHolders);

            if (!annualRevenueIds.isEmpty()) {
                final Table monetaryTable = monetaryManager.getTable();

                deleteQueries.add(entityHandler.buildEntityDeleteQuery(monetaryTable, new WhereClause(Criteria
                        .in(monetaryTable.getPKColumn(), annualRevenueIds))));
            }

            if (!addressIds.isEmpty()) {
                final Table addressTable = addressManager.getTable();

                deleteQueries.add(entityHandler.buildEntityDeleteQuery(addressTable, new WhereClause(Criteria
                        .in(addressTable.getPKColumn(), addressIds))));
            }

            orgDataStore(orgId).delete(deleteQueries);

            if (!auditLogDataList.isEmpty()) {
                auditLogger.log(orgId, auditLogDataList);
            }
        }
    }

    /**
     * <p>
     * Updates the given companies
     * </p>
     *
     * @param orgId  Organization id
     * @param models Collection of companies that have to be updated
     * @return Updated status
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<Company> models) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Table mainTable = getTable();
        final Collection<FieldInfo> fields = getCompanyFields(orgId);
        final Collection<Long> ids = models.stream().map(Company::id).filter(Objects::nonNull).toList();
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final DataContainer entityContainer = getCompanyDataContainer(orgId, ids);
        final AuditAction auditAction = AuditAction.UPDATED;
        final AuditSource auditSource = AuditSource.COMPANY;
        final Collection<AuditLogData> auditLogDataList = new ArrayList<>();

        models.forEach(company -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadCompanyIntoContainer(false, fields, company, entityContainer, targetContainer);
                orgDataStore.commitChanges(targetContainer);
                processOutcome.addProcessedRecord(company.id());
                final Company oldCompany = convertRowToPartialModel(entityContainer.getRow(mainTable));

                auditLogDataList.add(new AuditLogData(company.getUpdatedBy().id(), company.id(), oldCompany, company, auditSource, auditAction));
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Company update failed Org ID : {} ID : {} reason : {}", orgId, company.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed"), ErrorKeys.UPDATE_ACTION_FAILED, company.id().toString()));
            }
        });

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed")), failedEntities));
        }

        if (!auditLogDataList.isEmpty()) {
            auditLogger.log(orgId, auditLogDataList);
        }

        return processOutcome;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the organization ID.
     * @param userId   Represents the user ID.
     * @param entityId Represents the entity ID.
     * @return A flag that indicates whether the user is the follower.
     */
    @Override
    public boolean isUserFollowingEntity(final long orgId, final long userId, final long entityId) {
        return sourceFollowersManager.isFollowerOfEntity(orgId, userId, entityId, Source.COMPANY);
    }

    /**
     * <p>
     * Using the entity IDs and user ID, checks if the user is the follower of the entities
     * </p>
     *
     * @param orgId     Represents the organisation id
     * @param userId    Represents the user id
     * @param entityIds Represents the entity ids
     * @return The map containing the key as entity id and value is true if the user is the follower, false otherwise
     */
    @Override
    public Map<Long, Boolean> isUserFollowingEntities(final long orgId, final long userId, final Collection<Long> entityIds) {
        return sourceFollowersManager.isUserFollowingEntities(orgId, userId, entityIds, Source.COMPANY);
    }

    /**
     * <p>
     * Converts row into {@link Company}
     * </p>
     *
     * @param row           Company row
     * @param isPartial     A flag that represents the row should be converted as partial or not
     * @param dataContainer A container that holds the user defined field values and system values
     * @return {@link Company}
     */
    public Company convertRowToModel(final Row row, final boolean isPartial, final DataContainer dataContainer) {
        final Company company = new Company();
        final Long orgId = row.getOrgId();
        final Long companyId = row.getPKValue();
        final Long ownerId = row.get(MCOCOMPANY.OWNER_ID);
        final Long createdById = row.get(MCOCOMPANY.CREATED_BY);
        final Long updatedById = row.get(MCOCOMPANY.UPDATED_BY);
        final Long territoryId = row.get(MCOCOMPANY.TERRITORY_ID);
        final Collection<Long> userIds = new HashSet<>();

        if (Objects.nonNull(ownerId)) {
            company.setSalesOwner(userService.getUser(orgId, ownerId));
            userIds.add(ownerId);
            company.setOwnerId(ownerId);
        }

        if (Objects.nonNull(createdById)) {
            company.setCreatedBy(userService.getUser(orgId, createdById));
            userIds.add(createdById);
        }

        if (Objects.nonNull(updatedById)) {
            company.setUpdatedBy(userService.getUser(orgId, updatedById));
            userIds.add(updatedById);
        }

        if (Objects.nonNull(territoryId)) {
            company.setTerritory(buildPartialTerritory(territoryId, dataContainer));
        }

        company.setId(companyId);
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
        company.setDescription(row.get(MCOCOMPANY.DESCRIPTION));
        company.setLastAssignedAt(row.get(MCOCOMPANY.LAST_ASSIGNED_AT));
        company.setParentCompany(getParentCompany(orgId, companyId));
        company.setRecentNote(row.get(MCOCOMPANY.RECENT_NOTE));

        if (Objects.nonNull(dataContainer) && !dataContainer.isEmpty()) {
            final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Company.ENTITY_TYPE);

            convertCompanyOptionRowsToModel(fields, company, dataContainer);
            convertCompanyAddressRowsToModel(fields.get(CompanyFields.ADDRESS).id(), company, dataContainer);
            convertCompanyMonetaryRowsToModel(fields.get(CompanyFields.ANNUAL_REVENUE).id(), company, dataContainer);
        }

        if (!isPartial) {
            entityLoader.setEntityValues(company, getTable(), dataContainer);
        }
        final ValueHolder valueHolder = company.getValueHolder();

        if (Objects.nonNull(valueHolder)) {
            userIds.addAll(valueHolder.getUserValues().stream().map(CrmUser::id).collect(Collectors.toSet()));
        }
        company.setUserIds(userIds);
        loadSummaryDetails(orgId, company);
        return company;
    }

    /**
     * Converts the company monetary rows into model
     *
     * @param annualRevenueId Represents the monetary field id
     * @param company         An entity that holds the company attributes
     * @param dataContainer   A container that holds the user defined field values and system values
     */
    private void convertCompanyMonetaryRowsToModel(final Long annualRevenueId, final Company company,
                                                   final DataContainer dataContainer) {
        final Table monetaryTable = monetaryManager.getTable();
        final Table companyMonetaryTable = getCompanyMonetaryTable();
        final Stream<Row> companyMonetaryRows = dataContainer.getRows(companyMonetaryTable);

        companyMonetaryRows.forEach(companyMonetaryRow -> {
            final Row monetaryFieldRow = dataContainer.getRow(companyMonetaryTable, new WhereClause(Criteria
                    .eq(companyMonetaryTable.getColumn(MCOMMONETARYFIELDS.MONETARY_ID), companyMonetaryRow.get(MCOMMONETARYFIELDS.MONETARY_ID))));
            final Long fieldId = monetaryFieldRow.get(MCOMMONETARYFIELDS.FIELD_ID);
            final Row monetaryRow = dataContainer.getRow(monetaryTable, (Long) monetaryFieldRow.get(MCOMMONETARYFIELDS.MONETARY_ID));

            if (Objects.equals(annualRevenueId, fieldId) && Objects.nonNull(monetaryRow)) {
                company.setAnnualRevenue(monetaryManager.convertRowToModel(monetaryRow));
            }
        });
    }

    /**
     * Converts the company address rows into model
     *
     * @param addressId     Represents the address field id
     * @param company       An entity that holds the company attributes
     * @param dataContainer A container that holds the user defined field values and system values
     */
    private void convertCompanyAddressRowsToModel(final Long addressId, final Company company,
                                                  final DataContainer dataContainer) {
        final Table companyAddressTable = getCompanyAddressTable();
        final Table addressTable = addressManager.getTable();
        final Stream<Row> companyAddressRows = dataContainer.getRows(companyAddressTable);

        companyAddressRows.forEach(companyAddressRow -> {
            final Row addressFieldRow = dataContainer.getRow(companyAddressTable, new WhereClause(Criteria
                    .eq(companyAddressTable.getColumn(MCOMADDRESSFIELDS.ADDRESS_ID), companyAddressRow.get(MCOMADDRESSFIELDS.ADDRESS_ID))));
            final Long fieldId = addressFieldRow.get(MCOMADDRESSFIELDS.FIELD_ID);
            final Row addressRow = dataContainer.getRow(addressTable, new WhereClause(Criteria.eq(addressTable.getPKColumn(),
                    addressFieldRow.get(MCOMADDRESSFIELDS.ADDRESS_ID))));

            if (Objects.equals(addressId, fieldId)) {
                company.setAddress(addressManager.convertRowToModel(addressRow));
            }
        });
    }

    /**
     * Converts the company option rows into model
     *
     * @param fields        Company entity that has to be loaded into the container
     * @param company       An entity that holds the company attributes
     * @param dataContainer A container that holds the user defined field values and system values
     */
    private void convertCompanyOptionRowsToModel(final Map<String, FieldInfo> fields, final Company company,
                                                 final DataContainer dataContainer) {
        final String industryTypeKey = CompanyFields.INDUSTRY_TYPE;
        final String organisationStatusKey = CompanyFields.ORGANIZATION_STATUS;
        final String businessTypeKey = CompanyFields.BUSINESS_TYPE;

        final Long industryTypeId = fields.containsKey(industryTypeKey) ? fields.get(industryTypeKey).id() : null;
        final Long organisationStatusId = fields.containsKey(organisationStatusKey) ? fields.get(organisationStatusKey).id() : null;
        final Long businessTypeId = fields.containsKey(businessTypeKey) ? fields.get(businessTypeKey).id() : null;
        final Table companyOptionTable = getCompanyOptionTable();
        final Stream<Row> companyOptionRows = dataContainer.getRows(companyOptionTable, new WhereClause(Criteria.eq(companyOptionTable.getColumn(MCOMDROPDOWNFIELDS.ENTITY_ID), company.id())));

        companyOptionRows.forEach(companyOptionRow -> {
            final Option option = optionService.get(company.orgId(), companyOptionRow.get(MCOMDROPDOWNFIELDS.FIELD_ID),
                    companyOptionRow.get(MCOMDROPDOWNFIELDS.OPTION_ID));
            final Long fieldId = option.getFieldId();

            if (Objects.equals(industryTypeId, fieldId)) {
                company.setIndustryType(option);
            } else if (Objects.equals(organisationStatusId, fieldId)) {
                company.setOrganizationStatus(option);
            } else if (Objects.equals(businessTypeId, fieldId)) {
                company.setBusinessType(option);
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId represents the organisation ID
     * @param ids   represents the collection of company IDs
     * @return {@link Company} which contains the partial data
     */
    @Override
    public Collection<Company> partialGet(final Long orgId, final Collection<Long> ids) {
        final Table companyTable = getTable();
        final SelectQuery query = SelectQuery.get(companyTable);

        query.addSelectClause(List.of(companyTable.getPKColumn(), companyTable.getColumn(MCOCOMPANY.ORG_ID),
                companyTable.getColumn(MCOCOMPANY.NAME), companyTable.getColumn(MCOCOMPANY.OWNER_ID),
                companyTable.getColumn(MCOCOMPANY.CREATED_BY), companyTable.getColumn(MCOCOMPANY.UPDATED_BY)));
        query.setWhereClause(new WhereClause(Criteria.in(companyTable.getPKColumn(), ids)));

        final Stream<Row> rows = orgDataStore(orgId).get(query).getRows(companyTable);

        return Objects.nonNull(rows) ? rows.map(this::convertRowToPartialModel).collect(Collectors.toUnmodifiableSet())
                : Collections.emptySet();
    }

    /**
     * Fetches the {@link Company} for the dropdown actions.
     *
     * @param orgId    Organization id.
     * @param limit    Which holds the limit configuration.
     * @param criteria Which configure the criteria.
     * @return Collection of companies.
     */
    public Collection<Company> getCompanyDropdownList(final Long orgId, final DropdownLimit limit, final DropdownCriteria criteria) {
        return getCompanyDropdownList(orgId, false, limit, criteria);
    }

    /**
     * Fetches the {@link Company} for the dropdown actions.
     *
     * @param orgId          Organization id.
     * @param includeAddress A flag that indicate whether address include or not
     * @param limit          Which holds the limit configuration.
     * @param criteria       Which configure the criteria.
     * @return Collection of companies.
     */
    public Collection<Company> getCompanyDropdownList(final Long orgId, final boolean includeAddress,
                                                      final DropdownLimit limit, final DropdownCriteria criteria) {
        final Table companyTable = getTable();
        final Column companyNameColumn = companyTable.getColumn(MCOCOMPANY.NAME);
        final SelectQuery selectQuery = SelectQuery.get(companyTable);

        selectQuery.addSelectClause(companyNameColumn);
        selectQuery.setWhereClause(new WhereClause(criteria.build(companyNameColumn)));
        selectQuery.addOrderByClause(new OrderByClause(companyTable.getPKColumn()));
        selectQuery.setLimitClause(limit.getLimitClause());

        if (includeAddress) {
            final Table companyAddressTable = getCompanyAddressTable();
            final Table addressTable = addressManager.getTable();

            selectQuery.addSelectClause(addressTable, companyAddressTable);
            selectQuery.addJoinClause(List.of(
                    new JoinClause(companyTable.getPKColumn(), companyAddressTable.getColumn(MCOMADDRESSFIELDS.ENTITY_ID), JoinClause.JoinType.LEFT),
                    new JoinClause(companyAddressTable.getColumn(MCOMADDRESSFIELDS.ADDRESS_ID), addressTable.getPKColumn(), JoinClause.JoinType.LEFT)));
        }

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Stream<Row> rows = dataContainer.getRows(companyTable);

        if (Objects.isNull(rows)) {
            throw ErrorCode.get(ContactErrorCodes.COMPANY_NOT_FOUND, companyNotFoundI18NKey);
        } else {
            final List<Company> companyList = new ArrayList<>();

            rows.forEach(row -> {
                final Company company = new Company();

                company.setOrgId(orgId);
                company.setId(row.getPKValue());
                company.setName(row.get(MCOCOMPANY.NAME));

                if (includeAddress) {
                    final Table companyAddressTable = getCompanyAddressTable();
                    final Table addressTable = addressManager.getTable();
                    final Long companyId = row.getPKValue();
                    final Column addressPkColumn = addressTable.getPKColumn();
                    final Row addressCompanyRow = dataContainer.getRow(companyAddressTable,
                            new WhereClause(Criteria.eq(companyAddressTable.getColumn(MCOMADDRESSFIELDS.ENTITY_ID), companyId)));

                    if (Objects.nonNull(addressCompanyRow)) {
                        final Row addressRow = dataContainer.getRow(addressTable,
                                new WhereClause(Criteria.eq(addressPkColumn, addressCompanyRow.get(MCOMADDRESSFIELDS.ADDRESS_ID))));

                        if (Objects.nonNull(addressRow)) {
                            company.setAddress(addressManager.convertRowToModel(addressRow));
                        }
                    }
                }

                companyList.add(company);
            });

            return companyList;
        }
    }

    /**
     * Gets the company data by providing the name.
     *
     * @param orgId       Organisation Id
     * @param companyName Name of the company
     * @return {@link Company}
     */
    public Company getExistingCompany(final Long orgId, final Long companyId, final String companyName) {
        final Table companyTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(companyTable);

        selectQuery.addSelectClause(List.of(companyTable.getPKColumn(), companyTable.getColumn(MCOCOMPANY.ORG_ID), companyTable.getColumn(MCOCOMPANY.OWNER_ID)));
        selectQuery.setWhereClause(getCompanyFilter(companyId, companyName, companyTable));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row companyRow = dataContainer.getRow(companyTable);

        return Objects.nonNull(companyRow) ? convertRowToModel(companyRow, true, dataContainer) : null;
    }

    /**
     * Gets the collection of companies by names
     *
     * @param orgId              Represents the organisation ID
     * @param companyNames       Which contains the collection of company names
     * @param excludedCompanyIds Which contains the collection of company IDs to be excluded
     * @return {@link Collection} of companies
     */
    public Collection<Company> getCompanies(final Long orgId, final Collection<String> companyNames, final Collection<Long> excludedCompanyIds) {
        final Table companyTable = getTable();
        final Column companyNameColumn = companyTable.getColumn(MCOCOMPANY.NAME);
        final WhereClause companyNameCriteria = new WhereClause(Criteria.in(companyNameColumn, companyNames));
        final WhereClause companyWhereClause = excludedCompanyIds.isEmpty() ? companyNameCriteria
                : companyNameCriteria.and(Criteria.notIn(companyTable.getPKColumn(), excludedCompanyIds));
        final DataContainer dataContainer = getEntityContainer(orgId, companyWhereClause, secureFieldService.getFieldsBySource(orgId, Company.ENTITY_TYPE));

        return dataContainer.getRows(companyTable).map(row -> convertRowToModel(row, false, dataContainer)).toList();
    }

    /**
     * Gets the collection of company IDs by names
     *
     * @param orgId              Represents the organisation ID
     * @param companyNames       Which contains the collection of company names
     * @param excludedCompanyIds Which contains the collection of company IDs to be excluded
     * @return {@link Map} of company name as keys and its id as values
     */
    public Map<String, Long> getCompanyIdsByName(final Long orgId, final Collection<String> companyNames, final Collection<Long> excludedCompanyIds) {
        final Table companyTable = getTable();
        final Column companyNameColumn = companyTable.getColumn(MCOCOMPANY.NAME);

        final SelectQuery selectQuery = SelectQuery.get(companyTable);

        selectQuery.addSelectClause(List.of(companyTable.getPKColumn().min(), companyNameColumn));
        selectQuery.addGroupByClause(new GroupByClause(companyNameColumn));

        final WhereClause companyNameCriteria = new WhereClause(Criteria.in(companyNameColumn, companyNames));
        final WhereClause companyWhereClause = excludedCompanyIds.isEmpty() ? companyNameCriteria
                : companyNameCriteria.and(Criteria.notIn(companyTable.getPKColumn(), excludedCompanyIds));
        selectQuery.setWhereClause(companyWhereClause);

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(companyTable).collect(Collectors.toMap(row -> row.get(companyNameColumn), Row::getPKValue));
    }

    /**
     * Constructs the where clause to filter the company by its name and id
     *
     * @param companyId company ID
     * @param name      Represents the company name
     * @param table     Represents the company table
     * @return {@link WhereClause} which contains the criteria to filter the company
     */
    private WhereClause getCompanyFilter(final Long companyId, final String name, final Table table) {
        final WhereClause condition = new WhereClause(Criteria.eq(table.getColumn(MCOCOMPANY.NAME), name));

        return Objects.nonNull(companyId) ? condition.and(Criteria.notEq(table.getPKColumn(), companyId)) : condition;
    }

    /**
     * <p>
     * Builds the company related timeline
     * </p>
     *
     * @param isCreateEvent A flag which represents the event is create or not
     * @param companyRow    Represents the row of a company table
     * @param company       An entity that holds the company attributes
     * @return {@link Timeline}
     */
    private Timeline buildTimeline(final boolean isCreateEvent, final Row companyRow, final Company company) {
        final Timeline timeline = new Timeline();

        timeline.setAction(TimelineActionFields.CREATED);
        timeline.setCategory(Timeline.Category.COMPANY);
        timeline.setOrgId(company.orgId());
        timeline.setOwnerId(isCreateEvent ? company.getCreatedBy().id() : company.getUpdatedBy().id());
        timeline.setTimelineType(TimelineType.COMPANY_CREATED);
        timeline.setAuditedTime(companyRow.get(MCOCOMPANY.MODIFIED_TIME));

        return timeline;
    }

    /**
     * <p>
     * Loads the company timeline row into the container
     * </p>
     *
     * @param companyId     Represents the company id
     * @param timeline      An entity that holds the timeline attributes
     * @param dataContainer Holds the updated information of user defined and system field values
     */
    public void loadCompanyTimelineIntoContainer(final Timeline timeline, final Long companyId,
                                                  final DataContainer dataContainer) {
        commonTimelineManager.loadTimelineRowIntoContainer(timeline, new Row(commonTimelineManager.getTable()), dataContainer);
        commonTimelineManager.loadSourceTimelineRowIntoContainer(timeline.id(), companyId, MTLCOMPANYTIMELINE.COMPANY_ID, MTLCOMPANYTIMELINE.TIMELINE_ID,
                new Row(commonTimelineManager.getCompanyTimelineTable()), dataContainer);
    }

    /**
     * Converts partial row to {@link Company}
     *
     * @param companyRow Represents company row
     * @return {@link Company}
     */
    public Company convertRowToPartialModel(final Row companyRow) {
        final Company company = new Company();
        final Long orgId = companyRow.getOrgId();

        company.setId(companyRow.getPKValue());
        company.setOrgId(orgId);
        company.setName(companyRow.get(MCOCOMPANY.NAME));
        company.setOwnerId(companyRow.get(MCOCOMPANY.OWNER_ID));
        company.setFacebook(companyRow.get(MCOCOMPANY.FACEBOOK));
        company.setTwitter(companyRow.get(MCOCOMPANY.TWITTER));
        company.setLinkedIn(companyRow.get(MCOCOMPANY.LINKEDIN));
        final String website = companyRow.get(MCOCOMPANY.WEBSITE);
        final Long updateById = companyRow.get(MCOCOMPANY.UPDATED_BY);
        final Long ownerId = companyRow.get(MCOCOMPANY.OWNER_ID);
        final Long territoryId = companyRow.get(MCOCOMPANY.TERRITORY_ID);

        if (Objects.nonNull(website)) {
            company.setWebsite(website);
        }

        if (Objects.nonNull(updateById)) {
            company.setUpdatedBy(userService.getUser(orgId, updateById));
        }

        if (Objects.nonNull(ownerId)) {
            company.setSalesOwner(userService.getUser(orgId, ownerId));
        }

        if (Objects.nonNull(territoryId)) {
            final Territory territory = new Territory();

            territory.setId(territoryId);
            company.setTerritory(territory);
        }

        return company;
    }

    /**
     * <p>
     * Gets the deal count by company based on state
     * </p>
     *
     * @param orgId     Represents an organisation id
     * @param companyId Represents the company id
     * @return {@link Long} of deal count of company
     */
    public DealStatistics getDealStatistics(final long orgId, final Long companyId) {
        final DealStatistics dealStatistics = new DealStatistics();
        final Table dealCompanyTable = Table.get(MDOCOMPANYFIELDS.TABLE);
        final Table dealTable = Table.get(MDODEAL.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(dealCompanyTable);
        final Column dealStateColumn = dealTable.getColumn(MDODEAL.STATE);
        final Column dealPkColumn = dealTable.getPKColumn();
        final String companyKey = DealFields.COMPANY;
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Source.DEAL.getName(), companyKey);
        final Long companyFieldId = fields.get(companyKey).id();

        selectQuery.addSelectClause(List.of(dealPkColumn.count(), dealStateColumn));
        selectQuery.addJoinClause(new JoinClause(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID), dealPkColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.COMPANY_ID), companyId))
                .and(new WhereClause(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.FIELD_ID), companyFieldId))));
        selectQuery.addGroupByClause(new GroupByClause(dealStateColumn));
        final List<Map<String, Object>> rows = orgDataStore(orgId).getData(selectQuery);

        rows.forEach(row -> {
            final Integer stateId = (Integer) row.get(MDODEAL.STATE);
            final Long count = (Long) row.get(MDODEAL.ID);

            if (Objects.nonNull(stateId)) {
                switch (stateId) {
                    case 1 -> dealStatistics.setOpenDealsCount(count);
                    case 2 -> dealStatistics.setWonDealsCount(count);
                    case 3 -> dealStatistics.setLostDealsCount(count);
                }
            }
        });

        return dealStatistics;
    }

    /**
     * <p>
     * Gets the parent company details mapped for the given company id
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param companyId Represents the company id
     * @return {@link Company}
     */
    private Company getParentCompany(final long orgId, final Long companyId) {
        final String parentCompanyKey = CompanyFields.PARENT_COMPANY;
        final Map<String, FieldInfo> companyFields = secureFieldService.groupDefaultFieldsByAliasName(Company.ENTITY_TYPE, parentCompanyKey);

        if (companyFields.containsKey(parentCompanyKey)) {
            final Table companyTable = getTable();
            final Table companyFieldsTable = getCompanyFieldsTable();
            final SelectQuery innerSelectQuery = SelectQuery.get(companyFieldsTable);
            final SelectQuery selectQuery = SelectQuery.get(companyTable);

            innerSelectQuery.setWhereClause(new WhereClause(Criteria.eq(companyFieldsTable.getColumn(MCOMCOMPANYFIELDS.FIELD_ID),
                    companyFields.get(parentCompanyKey).id()))
                    .and(new WhereClause(Criteria.eq(companyFieldsTable.getColumn(MCOMCOMPANYFIELDS.ENTITY_ID), companyId))));
            innerSelectQuery.addSelectClause(companyFieldsTable.getColumn(MCOMCOMPANYFIELDS.COMPANY_ID));
            selectQuery.setWhereClause(new WhereClause(Criteria.eq(companyTable.getPKColumn(), innerSelectQuery)));
            selectQuery.addSelectClause(companyTable.getColumn(MCOCOMPANY.NAME));
            final Row row = orgDataStore(orgId).get(selectQuery).getRow(companyTable);

            if (Objects.isNull(row)) {
                return null;
            } else {
                final Company company = new Company();

                company.setId(row.getPKValue());
                company.setName(row.get(MCOCOMPANY.NAME));

                return company;
            }
        }
        return null;
    }

    /**
     * <p>
     * Retrieves a table containing company field based on the provided field info
     * </p>
     *
     * @param fieldInfo Refers the {@link FieldInfo}
     * @param table     Refers the field table
     * @return {@link Table}
     */
    public Table getCompanyFieldTable(final FieldInfo fieldInfo, final Table table) {
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(MDOCOMPANYFIELDS.FIELD_ID), fieldInfo.id())));

        return DerivedTable.get(table.getName(), table.getAlias(), selectQuery);
    }

    /**
     * <p>
     * Loads the company fields row into the data container
     * </p>
     *
     * @param orgId           Represents the org id
     * @param company         Represents the company to be updated
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    private void loadCompanyFieldsIntoContainer(final Long orgId, final Company company, final boolean isCreateEvent,
                                                final DataContainer targetContainer) {
        final String parentCompanyKey = CompanyFields.PARENT_COMPANY;
        final Map<String, FieldInfo> companyFields = secureFieldService.groupDefaultFieldsByAliasName(Company.ENTITY_TYPE, parentCompanyKey);
        final Long companyId = company.id();
        final Company parentCompany = company.getParentCompany();
        final Long parentCompanyId = parentCompany.id();

        if (companyFields.containsKey(parentCompanyKey)) {
            final Long companyFieldId = companyFields.get(parentCompanyKey).id();
            final Row row;
            final Table companyFieldsTable = getCompanyFieldsTable();

            if (isCreateEvent) {
                row = new Row(companyFieldsTable);
            } else {
                final Row companyFieldsRow = getCompanyFieldRow(orgId, true, companyId, companyFieldId);

                row = Objects.isNull(companyFieldsRow) ? new Row(companyFieldsTable) : companyFieldsRow;
            }

            if (row.isNewRow()) {
                row.set(MCOMCOMPANYFIELDS.ENTITY_ID, companyId);
                row.set(MCOMCOMPANYFIELDS.FIELD_ID, companyFieldId);
            }

            if (Objects.nonNull(parentCompanyId)) {
                row.set(MCOMCOMPANYFIELDS.COMPANY_ID, parentCompanyId);
            } else {
                loadCompanyIntoContainer(true, parentCompany, targetContainer);
                row.set(MCOMCOMPANYFIELDS.COMPANY_ID, parentCompany.id());
            }

            targetContainer.updateRow(row);
        }
    }

    /**
     * <p>
     * Gets the company field row based on entity id provided.
     * </p>
     *
     * @param orgId            Represents an organization's id.
     * @param areAllRowsNeeded Flag to determine whether full row is needed.
     * @param entityId         Represents the company id.
     * @return {@link Row}.
     */
    private Row getCompanyFieldRow(final Long orgId, final boolean areAllRowsNeeded, final Long entityId, final Long companyFieldId) {
        final Table companyFieldsTable = getCompanyFieldsTable();
        final SelectQuery selectQuery = SelectQuery.get(companyFieldsTable);

        if (areAllRowsNeeded) {
            selectQuery.addSelectClause(companyFieldsTable);
        } else {
            selectQuery.addSelectClause(companyFieldsTable.getPKColumn());
        }
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(companyFieldsTable.getColumn(MCOMCOMPANYFIELDS.ENTITY_ID), entityId)).
                and(Criteria.eq(companyFieldsTable.getColumn(MCOMCOMPANYFIELDS.FIELD_ID), companyFieldId)));

        final DataContainer sourceContainer = orgDataStore(orgId).get(selectQuery);

        return sourceContainer.getRow(companyFieldsTable);
    }

    /**
     * <p>
     * Deletes the unmapped parent company rows for the given company
     * </p>
     *
     * @param orgId           Represents the organization's id.
     * @param companyId       Represents the company's id.
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    private void deleteUnmappedParentCompany(final Long orgId, final Long companyId, final DataContainer targetContainer) {
        final String parentCompanyKey = CompanyFields.PARENT_COMPANY;
        final Map<String, FieldInfo> companyFields = secureFieldService.groupDefaultFieldsByAliasName(Company.ENTITY_TYPE, parentCompanyKey);

        if (companyFields.containsKey(parentCompanyKey)) {
            final Long companyFieldId = companyFields.get(parentCompanyKey).id();
            final Row companyFieldsRow = getCompanyFieldRow(orgId, false, companyId, companyFieldId);

            if (Objects.nonNull(companyFieldsRow)) {
                targetContainer.deleteRow(companyFieldsRow);
            }
        }
    }

    /**
     * <p>
     * Gets the company name for the given org and given company id.
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param companyId Represents the company id
     * @return the company name
     */
    public String getCompanyName(final long orgId, final Long companyId) {
        final Table table = getTable();
        final Optional<Row> companyRow = Optional.ofNullable(orgDataStore(orgId).get(table,
                new WhereClause(Criteria.eq(table.getPKColumn(), companyId))).getRow(table));

        return companyRow.map(row -> (String) row.get(MCOCOMPANY.NAME)).orElseGet(() -> null);
    }

    /**
     * <p>
     * Fetches a collection of {@link Company} for the given owner ids
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param ownerIds Which contains the collection of owner ids
     * @return The collection of {@link Company}
     */
    public Collection<Company> getCompanies(final long orgId, final Collection<Long> ownerIds) {
        final Table companyTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(companyTable);
        final Column ownerIdColumn = companyTable.getColumn(MCOCOMPANY.OWNER_ID);

        selectQuery.addSelectClause(List.of(companyTable.getColumn(MCOCOMPANY.NAME), ownerIdColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(ownerIdColumn, ownerIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Collections.emptyList();
        }
        return dataContainer.getRows(companyTable).map(companyRow -> {
            final Company company = new Company();

            company.setId(companyRow.getPKValue());
            company.setOrgId(orgId);
            company.setName(companyRow.get(MCOCOMPANY.NAME));
            company.setOwnerId(companyRow.get(MCOCOMPANY.OWNER_ID));
            return company;
        }).toList();
    }

    /**
     * <p>
     * Links the given {@link Collection} of contact ids to given company id
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param companyId  Represents the company id
     * @param contactIds Represents the {@link Collection} of contact ids
     */
    public void linkContactsToCompany(final long orgId, final Long companyId, final Collection<Long> contactIds) {
        final Table contactCompanyTable = Table.get(MCONCOMPANYFIELDS.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(contactCompanyTable);
        final String companyKey = ContactFields.COMPANY;
        final long companyFieldId = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, companyKey).get(companyKey).id();
        final DataContainer targetContainer = DataContainer.create();

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.COMPANY_ID), companyId))
                .and(new WhereClause(Criteria.eq(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.FIELD_ID), companyFieldId))));

        orgDataStore(orgId).get(selectQuery).getRows(contactCompanyTable).forEach(row -> {
            final Long contactId = row.get(MCONCOMPANYFIELDS.ENTITY_ID);

            if (!contactIds.contains(contactId)) {
                targetContainer.deleteRow(row);
            }
            contactIds.remove(contactId);
        });


        for (final Long contactId : contactIds) {
            final Row contactCompanyRow = new Row(contactCompanyTable);

            contactCompanyRow.set(MCONCOMPANYFIELDS.COMPANY_ID, companyId);
            contactCompanyRow.set(MCONCOMPANYFIELDS.FIELD_ID, companyFieldId);
            contactCompanyRow.set(MCONCOMPANYFIELDS.ENTITY_ID, contactId);

            targetContainer.addNewRow(contactCompanyRow);
        }

        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Retrieves the total company count
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param dropdownCriteria Represents a dropdown criteria
     * @return The count of entities
     */
    public long getTotalCount(final Long orgId, final DropdownCriteria dropdownCriteria) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        if (Objects.nonNull(dropdownCriteria)) {
            countQuery.setWhereClause(new WhereClause(dropdownCriteria.build(table.getColumn(MCOCOMPANY.NAME))));
        }

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Gets the {@link Map} contains the companies with their deal id
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param dealIds Represents the {@link Collection} of deal ids
     * @return {@link Map} of companies with deal id
     */
    public Map<Long, Company> getDealCompanies(final long orgId, final Collection<Long> dealIds) {

        if (!dealIds.isEmpty()) {
            final Optional<FieldInfo> fieldInfo = secureFieldService.getDefaultFieldByAliasName(Source.DEAL, DealFields.COMPANY);

            if (fieldInfo.isPresent()) {
                final Table dealCompanyTable = Table.get(MDOCOMPANYFIELDS.TABLE);
                final Table companyTable = getTable();
                final Column dealCompanyEntityIdColumn = dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID);
                final SelectQuery companySelectQuery = SelectQuery.get(dealCompanyTable);
                final Column dealCompanyIdColumn = dealCompanyTable.getColumn(MDOCOMPANYFIELDS.COMPANY_ID);
                final Map<Long, Company> companies = new HashMap<>();

                companySelectQuery.addSelectClause(List.of(dealCompanyEntityIdColumn, dealCompanyIdColumn, companyTable.getColumn(MCOCOMPANY.NAME)));
                companySelectQuery.addJoinClause(new JoinClause(dealCompanyIdColumn, companyTable.getPKColumn()));
                companySelectQuery.setWhereClause(new WhereClause(Criteria.in(dealCompanyEntityIdColumn, dealIds))
                        .and(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.FIELD_ID), fieldInfo.get().id())));
                final DataContainer dataContainer = orgDataStore(orgId).get(companySelectQuery);

                dataContainer.getRows(dealCompanyTable).forEach(dealCompanyRow -> {
                    final Company company = new Company();
                    final Long companyId = dealCompanyRow.get(MDOCOMPANYFIELDS.COMPANY_ID);

                    company.setName(dataContainer.getRow(getTable(), companyId).get(MCOCOMPANY.NAME));
                    company.setOrgId(orgId);
                    company.setId(companyId);
                    companies.put(dealCompanyRow.get(MDOCOMPANYFIELDS.ENTITY_ID), company);
                });
                return companies;
            }
        }

        return Collections.emptyMap();
    }

    /**
     * <p>
     * Gets the company profile details
     * </p>
     *
     * @param orgId      Represents the organisation Id
     * @param companyId  Represents the company Id
     * @return The {@link Company}
     */
    public Company getProfile(final Long orgId, final Long companyId) {
        final Table companyTable = getTable();
        final Table companyDisplayPicTable = Table.get(MCOMDISPLAYPICTURE.TABLE);
        final Table displayPicTable = Table.get(OMDPDISPLAYPICTURE.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(companyTable);
        final Column companyPkColumn = companyTable.getPKColumn();
        final Column displayPicPkColumn = displayPicTable.getPKColumn();
        final JoinClause.JoinType joinType = JoinClause.JoinType.LEFT;

        selectQuery.addSelectClause(List.of(companyTable.getColumn(MCOCOMPANY.ORG_ID), companyTable.getColumn(MCOCOMPANY.NAME),
                companyTable.getColumn(MCOCOMPANY.WEBSITE), companyTable.getColumn(MCOCOMPANY.TWITTER),
                companyTable.getColumn(MCOCOMPANY.LINKEDIN), companyTable.getColumn(MCOCOMPANY.FACEBOOK),
                displayPicPkColumn, displayPicTable.getColumn(OMDPDISPLAYPICTURE.ORIGINAL_SIZE), displayPicTable.getColumn(OMDPDISPLAYPICTURE.ORIGINAL_EXTENSION),
                companyDisplayPicTable.getPKColumn(), companyDisplayPicTable.getColumn(MCOMDISPLAYPICTURE.DISPLAY_PICTURE_ID)));

        selectQuery.addJoinClause(List.of(new JoinClause(companyPkColumn, companyDisplayPicTable.getColumn(MCOMDISPLAYPICTURE.COMPANY_ID), joinType),
                new JoinClause(companyDisplayPicTable.getColumn(MCOMDISPLAYPICTURE.DISPLAY_PICTURE_ID), displayPicPkColumn, joinType)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(companyPkColumn, companyId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return convertRowToCompanyProfile(dataContainer.getRow(companyTable), dataContainer);
    }

    /**
     * Converts the given {@link Row} data into a {@link Company} for profile.
     *
     * @param companyRow    The row containing company data to be converted.
     * @param dataContainer The container providing additional reference or lookup data.
     * @return A {@link Company} object populated with values from the given row.
     */
    private Company convertRowToCompanyProfile(final Row companyRow, final DataContainer dataContainer) {
        final Company company = new Company();
        final Long pkValue = companyRow.getPKValue();
        final Long orgId = companyRow.getOrgId();
        final Long updateById = companyRow.get(MCOCOMPANY.UPDATED_BY);
        final Long ownerId = companyRow.get(MCOCOMPANY.OWNER_ID);

        company.setId(pkValue);
        company.setOrgId(orgId);
        company.setName(companyRow.get(MCOCOMPANY.NAME));
        company.setOwnerId(companyRow.get(MCOCOMPANY.OWNER_ID));
        company.setFacebook(companyRow.get(MCOCOMPANY.FACEBOOK));
        company.setTwitter(companyRow.get(MCOCOMPANY.TWITTER));
        company.setLinkedIn(companyRow.get(MCOCOMPANY.LINKEDIN));
        company.setWebsite(companyRow.get(MCOCOMPANY.WEBSITE));

        if (Objects.nonNull(updateById)) {
            company.setUpdatedBy(userService.getUser(orgId, updateById));
        }

        if (Objects.nonNull(ownerId)) {
            company.setSalesOwner(userService.getUser(orgId, ownerId));
        }
        final Table companyDisplayPicTable = Table.get(MCOMDISPLAYPICTURE.TABLE);
        final Table displayPicTable = Table.get(OMDPDISPLAYPICTURE.TABLE);

        Optional.ofNullable(dataContainer.getRow(companyDisplayPicTable)).ifPresent(row -> {
            final Row displayPicRow = dataContainer.getRow(displayPicTable, new WhereClause(Criteria.eq(displayPicTable.getPKColumn(), row.get(MCOMDISPLAYPICTURE.DISPLAY_PICTURE_ID))));
            final DisplayPicture displayPicture = new DisplayPicture(ProfilePicture.Quality.ORIGINAL);

            displayPicture.setSize(displayPicRow.get(OMDPDISPLAYPICTURE.ORIGINAL_SIZE));
            displayPicture.setExtension(ProfilePicture.Extension.get((short) displayPicRow.get(OMDPDISPLAYPICTURE.ORIGINAL_EXTENSION)));

            company.setOriginalPicture(displayPicture);
        });

        return company;
    }

    /**
     * <p>
     * Gets the activity company mapping table
     * </p>
     *
     * @return The activity company mapping {@link Table}
     */
    @Override
    public Table getActivityEntityTable() {
        return Table.get(MACCOMPANYACTIVITY.TABLE);
    }

    /**
     * <p>
     * Gets the total number of companies matching the given dropdown criteria.
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param fieldInfo        Represents the {@link FieldInfo}
     * @param fieldSubType     Represents the {@link FieldSubType}
     * @param dropdownCriteria Represents the {@link DropdownCriteria}
     * @return The company total count
     */
    public long getCompanyDropdownCount(final Long orgId, final FieldInfo fieldInfo, final FieldSubType fieldSubType,
                              final DropdownCriteria dropdownCriteria) {
        final Table companyTable = getTable();
        final Table companyAddressTable = getCompanyAddressTable();
        final Table addressTable = addressManager.getTable();
        final CountQuery countQuery = CountQuery.get(companyTable);

        if (Objects.nonNull(fieldInfo) && CompanyFields.ADDRESS.equals(fieldInfo.aliasName())) {
            countQuery.addJoinClause(List.of(new JoinClause(companyTable.getPKColumn(), companyAddressTable.getColumn(MCOMADDRESSFIELDS.ENTITY_ID), JoinClause.JoinType.LEFT),
                    new JoinClause(companyAddressTable.getColumn(MCOMADDRESSFIELDS.ADDRESS_ID), addressTable.getPKColumn())));
            countQuery.setWhereClause(buildAddressWhereClause(companyAddressTable, addressTable, fieldInfo, fieldSubType, dropdownCriteria));
        } else {
            countQuery.setWhereClause(new WhereClause(dropdownCriteria.build(companyTable.getColumn(MCOCOMPANY.NAME))));
        }
        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Gets the collection of company for the given org id
     * </p>
     *
     * @param orgId         Represents an org id
     * @param fieldInfo     Represents the {@link FieldInfo}
     * @param fieldSubType  Represents the {@link FieldSubType}
     * @param dropdownLimit Represents the {@link DropdownLimit}
     * @param criteria      Holds the {@link DropdownCriteria}
     * @return The collection of {@link Company}
     */
    public Collection<Company> getCompanyDropdownList(final Long orgId, final FieldInfo fieldInfo, final FieldSubType fieldSubType,
                                                      final DropdownLimit dropdownLimit, final DropdownCriteria criteria) {
        final Table companyTable = getTable();
        final Table companyAddressTable = getCompanyAddressTable();
        final Table addressTable = addressManager.getTable();
        final Column companyNameColumn = companyTable.getColumn(MCOCOMPANY.NAME);
        final SelectQuery selectQuery = SelectQuery.get(companyTable);
        final Column companyPkColumn = companyTable.getPKColumn();
        final Column addressPkColumn = addressTable.getPKColumn();

        if (Objects.nonNull(fieldInfo) && CompanyFields.ADDRESS.equals(fieldInfo.aliasName())) {
            selectQuery.setWhereClause(buildAddressWhereClause(companyAddressTable, addressTable, fieldInfo, fieldSubType, criteria));
            selectQuery.addOrderByClause(new OrderByClause(addressPkColumn));
        } else {
            selectQuery.setWhereClause(new WhereClause(criteria.build(companyNameColumn)));
            selectQuery.addOrderByClause(new OrderByClause(companyPkColumn));
        }
        selectQuery.addSelectClause(companyNameColumn);
        selectQuery.addSelectClause(addressTable, companyAddressTable);
        selectQuery.addJoinClause(List.of(new JoinClause(companyPkColumn, companyAddressTable.getColumn(MCOMADDRESSFIELDS.ENTITY_ID),
                JoinClause.JoinType.LEFT), new JoinClause(companyAddressTable.getColumn(MCOMADDRESSFIELDS.ADDRESS_ID), addressPkColumn,
                JoinClause.JoinType.LEFT)));
        selectQuery.setLimitClause(dropdownLimit.getLimitClause());
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(companyTable).map(companyRow -> {
            final Long companyId = companyRow.getPKValue();
            final Company company = new Company();
            final Row addressCompanyRow = dataContainer.getRow(companyAddressTable,
                    new WhereClause(Criteria.eq(companyAddressTable.getColumn(MCOMADDRESSFIELDS.ENTITY_ID), companyId)));

            if (Objects.nonNull(addressCompanyRow)) {
                company.setAddress(addressManager.convertRowToModel(dataContainer.getRow(addressTable,
                        new WhereClause(Criteria.eq(addressPkColumn, addressCompanyRow.get(MCOMADDRESSFIELDS.ADDRESS_ID))))));
            }
            company.setId(companyId);
            company.setName(companyRow.get(MCOCOMPANY.NAME));

            return company;
        }).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * <p>
     * Builds the address criteria for the provided details
     * </p>
     *
     * @param companyAddressTable Represents the company address table
     * @param addressTable        Represents the address table
     * @param fieldInfo           Represents the {@link FieldInfo}
     * @param fieldSubType        Represents the {@link FieldSubType}
     * @param dropdownCriteria    Holds the dropdown criteria
     * @return The address criteria
     */
    private WhereClause buildAddressWhereClause(final Table companyAddressTable, final Table addressTable,
                                                final FieldInfo fieldInfo, final FieldSubType fieldSubType,
                                                final DropdownCriteria dropdownCriteria) {
        final WhereClause addressWhereClause = new WhereClause(Criteria.eq(companyAddressTable.getColumn(MCOMADDRESSFIELDS.FIELD_ID), fieldInfo.id()));

        if (Objects.nonNull(fieldSubType)) {
            final Column subFieldColumn = switch (fieldSubType.storeSubType()) {
                case ADDRESS_LINE1 -> addressTable.getColumn(OMETADDRESS.ADDRESS_LINE1);
                case ADDRESS_LINE2 -> addressTable.getColumn(OMETADDRESS.ADDRESS_LINE2);
                default -> null;
            };

            if (Objects.nonNull(subFieldColumn)) {
                return addressWhereClause.and(dropdownCriteria.build(subFieldColumn));
            }
        }
        return addressWhereClause;
    }

    /**
     * <p>
     * Gets the company profile details
     * </p>
     *
     * @param orgId     Represents the organisation Id
     * @param companyId Represents the company Id
     * @return The {@link Company}
     */
    public Company getSocialProfile(final Long orgId, final Long companyId) {
        final Table companyTable = getTable();

        final Row companyRow = orgDataStore(orgId).get(companyTable, companyId, List.of(companyTable.getColumn(MCOCOMPANY.ORG_ID),
                companyTable.getColumn(MCOCOMPANY.FACEBOOK), companyTable.getColumn(MCOCOMPANY.LINKEDIN),
                companyTable.getColumn(MCOCOMPANY.TWITTER)));

        return convertSocialProfile(companyRow);
    }

    /**
     * Converts partial row into {@link Company}
     *
     * @param companyRow Represents the company row
     * @return The {@link Company}
     */
    private Company convertSocialProfile(final Row companyRow) {
        final Company company = new Company();
        final Long orgId = companyRow.getOrgId();

        company.setId(companyRow.getPKValue());
        company.setOrgId(orgId);
        company.setLinkedIn(companyRow.get(MCOCOMPANY.LINKEDIN));
        company.setTwitter(companyRow.get(MCOCOMPANY.TWITTER));
        company.setFacebook(companyRow.get(MCOCOMPANY.FACEBOOK));

        return company;
    }

    /**
     * Updates the company owner and (optionally) its territory, and timeline.
     *
     * @param orgId     The organization in which the update is applied.
     * @param id        The identifier of the record being updated.
     * @param timeline  The timeline entry representing the assignment update.
     * @param ownerId   The newly assigned owner for the record.
     * @param territory The resolved territory to assign; may be {@code null} if no territory update is required.
     */
    public void updateOwnerAndTerritory(final long orgId, final Long id, final Timeline timeline,
                                        final long ownerId, final Territory territory) {
        final Table table = getTable();
        final OrgDataStore orgDataStore = orgDataStore(orgId);

        Optional.ofNullable(orgDataStore.get(table, id)).ifPresent(row -> {
            final DataContainer targetContainer = DataContainer.create();
            final Long existingOwnerId = row.get(MCOCOMPANY.OWNER_ID);
            final Long existingTerritoryId = row.get(MCOCOMPANY.TERRITORY_ID);

            row.set(MCOCOMPANY.OWNER_ID, ownerId);

            if (Objects.nonNull(territory)) {
                row.set(MCOCOMPANY.TERRITORY_ID, territory.id());
            }
            targetContainer.updateRow(row);

            if (Objects.nonNull(timeline)) {
                loadCompanyTimelineIntoContainer(timeline, id, targetContainer);
                final FieldAuditInfo editContext = timeline.getFieldAuditInfo();

                fieldAuditHandler.loadFieldAuditEntry(orgId, Source.COMPANY, id, null, editContext, CompanyFields.SALES_OWNER,
                        Objects.isNull(existingOwnerId) ? null : String.valueOf(existingOwnerId), String.valueOf(ownerId), targetContainer);

                if (Objects.nonNull(territory)) {
                    fieldAuditHandler.loadFieldAuditEntry(orgId, Source.COMPANY, id, null, editContext, CompanyFields.TERRITORY,
                            Objects.isNull(existingTerritoryId) ? null : territoryManager.getName(orgId, existingTerritoryId),
                            territory.getName(), targetContainer);
                }
            }
            orgDataStore.commitChanges(targetContainer);
        });
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents the org id
     * @param companyId Represents the company id
     * @return the user ids
     */
    @Override
    public Collection<Long> getRestrictedIamUserIdsForEntity(final Long orgId, final Long companyId) {
        final Table userTable = Table.get(OMCRMUSER.TABLE);
        final Table companyTable = getTable();
        final Column userTablePkColumn = userTable.getPKColumn();
        final Column iamUserIdColumn = userTable.getColumn(OMCRMUSER.IAM_USER_ID);
        final SelectQuery selectQuery = SelectQuery.get(userTable);
        final Collection<Long> userIdsWithAccess = getUserIdsWithEntityAccess(orgId, companyId, companyTable, companyTable.getColumn(MCOCOMPANY.TERRITORY_ID), SubCategory.Type.COMPANIES);

        userIdsWithAccess.addAll(sourceFollowersManager.getFollowerIdsByEntityId(orgId, companyId, Source.COMPANY));

        selectQuery.addSelectClause(List.of(userTablePkColumn, iamUserIdColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.notIn(userTablePkColumn, userIdsWithAccess)));

        return orgDataStore(orgId).get(selectQuery).getRows(userTable).map(userRow -> (Long) userRow.get(OMCRMUSER.IAM_USER_ID)).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Gets the collection of {@link Company} given by the IDs.
     *
     * @param orgId      Represents the organization ID
     * @param companyIds Represents the collection of company IDs.
     * @return the collection of {@link Company}
     */
    public Collection<Company> getCompaniesByIds(final Long orgId, final Set<Long> companyIds) {
        final Table companyTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(companyTable);

        selectQuery.addSelectClause(companyTable.getColumn(MCOCOMPANY.NAME));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(companyTable.getPKColumn(), companyIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(companyTable).map(companyRow -> {
            final Company company = new Company();

            company.setId(companyRow.getPKValue());
            company.setName(companyRow.get(MCOCOMPANY.NAME));

            return company;
        }).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents the organisation id
     * @param entityId  Represents the entity id
     * @return The entity name
     */
    @Override
    public String getName(final Long orgId, final Long entityId) {
        final Table table = getTable();
        final Row row = orgDataStore(orgId).get(table, entityId, List.of(table.getColumn(MCOCOMPANY.NAME)));

        return Objects.isNull(row) ? null : row.get(MCOCOMPANY.NAME);
    }

    /**
     * {@inheritDoc}

     /**
     * <p>
     * Ensures that a company is associated with the specified deal.
     * If the company is not already associated, it will be added.
     * </p>
     *
     * @param orgId     the organization ID
     * @param dealId    the deal ID to associate with
     * @param companyId the company ID to associate
     * @return {@code true} if the company is now associated with the deal
     */
    @Override
    public void associateEntityWithDeal(final long orgId, final long companyId, final long dealId) {
        if (isCompanyAssociatedWithDeal(orgId, dealId, companyId)) {
            return;
        }
        final Map<String, FieldInfo> fields = secureFieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, DealFields.COMPANY);
        final Table dealCompanyTable = DealManager.getInstance().getDealCompanyTable();
        final Long dealCompanyFieldId = fields.get(DealFields.COMPANY).id();
        final Row dealCompanyRow = new Row(dealCompanyTable);
        final DataContainer targetContainer = DataContainer.create();

        dealCompanyRow.set(MDOCOMPANYFIELDS.ENTITY_ID, dealId);
        dealCompanyRow.set(MDOCOMPANYFIELDS.FIELD_ID, dealCompanyFieldId);
        dealCompanyRow.set(MDOCOMPANYFIELDS.COMPANY_ID, companyId);

        targetContainer.addNewRow(dealCompanyRow);
        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Loads the companies preview data such as company name into the container
     * </p>
     *
     * @param orgId         Represents an organization ID
     * @param ids           Represents the collection of company IDs
     * @param dataContainer Represents the data container to load the company preview data
     */
    public void loadCompanyPreviewDataIntoContainer(final long orgId, final Collection<Long> ids,
                                                    final DataContainer dataContainer) {
        final Table companyTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(companyTable);

        selectQuery.addSelectClause(companyTable.getColumn(MCOCOMPANY.NAME));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(companyTable.getPKColumn(), ids)));

        dataContainer.append(orgDataStore(orgId).get(selectQuery));
    }

    /**
     * <p>
     * Checks whether the given company is associated with the specified deal.
     * </p>
     *
     * @param orgId     the organization ID
     * @param dealId    the deal ID
     * @param companyId the company ID to validate
     * @return {@code true} if the company is associated with the deal, {@code false} otherwise
     */
    private boolean isCompanyAssociatedWithDeal(final long orgId, final long dealId, final long companyId) {
        final Map<String, FieldInfo> fields = secureFieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, DealFields.COMPANY);

        if (!fields.containsKey(DealFields.COMPANY)) {
            return false;
        }
        final Table dealCompanyTable = DealManager.getInstance().getDealCompanyTable();
        final Long companyFieldId = fields.get(DealFields.COMPANY).id();
        final CountQuery countQuery = CountQuery.get(dealCompanyTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID), dealId))
                .and(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.FIELD_ID), companyFieldId))
                .and(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.COMPANY_ID), companyId)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Gets the companies (id, name and address) for the given company ids
     * </p>
     *
     * @param orgId Represents an organization id
     * @param ids   Represents the collection of company ids
     * @return The collection of {@link Company} holding id, name and address
     */
    public Collection<Company> getCompaniesWithAddress(final Long orgId, final Collection<Long> ids) {
        if (isEntityIdCollectionEmpty(ids)) {
            return Collections.emptyList();
        }
        final Table companyTable = getTable();
        final Table companyAddressTable = getCompanyAddressTable();
        final Table addressTable = addressManager.getTable();
        final Column companyPkColumn = companyTable.getPKColumn();
        final Column addressPkColumn = addressTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(companyTable);
        final Map<String, FieldInfo> fields = secureFieldService.groupDefaultFieldsByAliasName(Company.ENTITY_TYPE, CompanyFields.ADDRESS);
        final FieldInfo addressField = fields.get(CompanyFields.ADDRESS);

        selectQuery.addSelectClause(companyTable.getColumn(MCOCOMPANY.NAME));
        selectQuery.addSelectClause(addressTable, companyAddressTable);
        selectQuery.addJoinClause(List.of(
                new JoinClause(companyPkColumn, companyAddressTable.getColumn(MCOMADDRESSFIELDS.ENTITY_ID), JoinClause.JoinType.LEFT),
                new JoinClause(companyAddressTable.getColumn(MCOMADDRESSFIELDS.ADDRESS_ID), addressPkColumn, JoinClause.JoinType.LEFT)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(companyPkColumn, ids)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(companyTable).map(companyRow -> {
            final Long companyId = companyRow.getPKValue();
            final Company company = new Company();
            WhereClause addressCriteria = new WhereClause(Criteria.eq(companyAddressTable.getColumn(MCOMADDRESSFIELDS.ENTITY_ID), companyId));

            if (Objects.nonNull(addressField)) {
                addressCriteria = addressCriteria.and(Criteria.eq(companyAddressTable.getColumn(MCOMADDRESSFIELDS.FIELD_ID), addressField.id()));
            }
            final Row companyAddressRow = dataContainer.getRow(companyAddressTable, addressCriteria);

            if (Objects.nonNull(companyAddressRow)) {
                final Row addressRow = dataContainer.getRow(addressTable,
                        new WhereClause(Criteria.eq(addressPkColumn, companyAddressRow.get(MCOMADDRESSFIELDS.ADDRESS_ID))));

                if (Objects.nonNull(addressRow)) {
                    company.setAddress(addressManager.convertRowToModel(addressRow));
                }
            }
            company.setOrgId(orgId);
            company.setId(companyId);
            company.setName(companyRow.get(MCOCOMPANY.NAME));

            return company;
        }).collect(Collectors.toCollection(ArrayList::new));
    }

}