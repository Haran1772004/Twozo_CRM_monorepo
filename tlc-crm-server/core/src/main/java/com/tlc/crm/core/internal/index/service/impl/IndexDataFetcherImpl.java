package com.tlc.crm.core.internal.index.service.impl;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.manager.PipelineManager;
import com.tlc.crm.core.api.deal.manager.PipelineStageManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.features.api.followers.source.manager.SourceFollowersManager;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.internal.index.service.IndexDataFetcher;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MCOCOMPANY;
import com.tlc.crm.core.sql.resource.MCONADDRESSFIELDS;
import com.tlc.crm.core.sql.resource.MCONCOMPANYFIELDS;
import com.tlc.crm.core.sql.resource.MCONEMAILFIELDS;
import com.tlc.crm.core.sql.resource.MCONPHONEFIELDS;
import com.tlc.crm.core.sql.resource.MCOMADDRESSFIELDS;
import com.tlc.crm.core.sql.resource.MDOPIPELINE;
import com.tlc.crm.core.sql.resource.MDOPIPELINESTAGE;
import com.tlc.crm.core.sql.resource.OMCONFOLLOWERS;
import com.tlc.crm.entity.features.api.activity.manager.ActivityTypeManager;
import com.tlc.crm.entity.features.api.activity.model.ActivityType;
import com.tlc.crm.entity.features.field.activity.ActivityFields;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.manager.field.type.AddressManager;
import com.tlc.crm.entity.manager.field.type.EmailManager;
import com.tlc.crm.entity.manager.field.type.PhoneManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.search.fields.CommonSearchFields;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.sql.resource.OMACACTIVITYTYPE;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;

import java.util.Objects;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.List;

/**
 * <p>
 * Facilitates to handle index manager's fetch requirements.
 * Index-related database access is confined to this class.
 * </p>
 *
 * @author Ajay
 * @version 1.0
 */
public class IndexDataFetcherImpl implements IndexDataFetcher {

    private static final JoinClause.JoinType LEFT = JoinClause.JoinType.LEFT;

    private final FieldService fieldService;
    private final DealManager dealManager;
    private final PipelineManager pipelineManager;
    private final PipelineStageManager pipelineStageManager;
    private final ActivityTypeManager activityTypeManager;
    private final ContactManager contactManager;
    private final CompanyManager companyManager;
    private final SourceFollowersManager sourceFollowersManager;
    private final EmailManager emailManager;
    private final PhoneManager phoneManager;
    private final AddressManager addressManager;
    private final ReadableDataStore readableDataStore;

    private IndexDataFetcherImpl() {
        fieldService = FieldService.getInstance();
        dealManager = DealManager.getInstance();
        pipelineManager = PipelineManager.getInstance();
        pipelineStageManager = PipelineStageManager.getInstance();
        activityTypeManager = ActivityTypeManager.getInstance();
        contactManager = ContactManager.getInstance();
        companyManager = CompanyManager.getInstance();
        sourceFollowersManager = SourceFollowersManager.getInstance();
        emailManager = EmailManager.getInstance();
        phoneManager = PhoneManager.getInstance();
        addressManager = AddressManager.getInstance();
        readableDataStore = SqlServiceProvider.get().getAdminDataStore();
    }

    private static class Instance {
        private static final IndexDataFetcherImpl INSTANCE = new IndexDataFetcherImpl();
    }

    /**
     * <p>
     * Gets the {@link IndexDataFetcherImpl} Instance
     * </p>
     *
     * @return {@link IndexDataFetcherImpl}
     */
    public static IndexDataFetcherImpl getInstance() {

        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Fetches the {@link Contact} for the given id.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @param id    To fetch the {@link Contact}.
     * @return {@link Contact}.
     */
    @Override
    public Contact fetchContact(final Long orgId, final Long id) {
        final Table contactTable = contactManager.getTable();
        final Table emailTable = emailManager.getTable();
        final Table phoneTable = phoneManager.getTable();
        final Table addressTable = addressManager.getTable();
        final Table contactEmailTable = contactManager.getContactEmailTable();
        final Table contactPhoneTable = contactManager.getContactPhoneTable();
        final Table contactAddressTable = contactManager.getContactAddressTable();
        final Table contactCompanyTable = contactManager.getContactCompanyTable();
        final Column contactPkColumn = contactTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);
        final Collection<Table> selectedTables = selectQuery.getTables();
        final Map<String, FieldInfo> contactMapperFields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE,
                ContactFields.ADDRESS, ContactFields.PHONES, ContactFields.EMAILS, ContactFields.COMPANY);

        contactManager.loadEntityMapperTableSelectAndJoinClause(contactAddressTable, selectedTables, selectQuery, contactPkColumn,
                contactMapperFields.get(ContactFields.ADDRESS).id());
        contactManager.loadEntityMapperTableSelectAndJoinClause(contactPhoneTable, selectedTables, selectQuery, contactPkColumn,
                contactMapperFields.get(ContactFields.PHONES).id());
        contactManager.loadEntityMapperTableSelectAndJoinClause(contactEmailTable, selectedTables, selectQuery, contactPkColumn,
                contactMapperFields.get(ContactFields.EMAILS).id());
        contactManager.loadEntityMapperTableSelectAndJoinClause(contactCompanyTable, selectedTables, selectQuery, contactPkColumn,
                contactMapperFields.get(ContactFields.COMPANY).id());

        selectQuery.addSelectClause(List.of(contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME),
                contactTable.getColumn(MCOCONTACT.OWNER_ID), contactTable.getColumn(MCOCONTACT.TERRITORY_ID),
                contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL), contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE),
                contactTable.getColumn(MCOCONTACT.FACEBOOK), contactTable.getColumn(MCOCONTACT.LINKEDIN),
                contactTable.getColumn(MCOCONTACT.ORG_ID), contactCompanyTable.getColumn(MCONCOMPANYFIELDS.COMPANY_ID)));
        selectQuery.addSelectClause(emailTable, phoneTable, addressTable);
        selectQuery.addJoinClause(List.of(
                new JoinClause(contactAddressTable.getColumn(MCONADDRESSFIELDS.ADDRESS_ID), addressTable.getPKColumn(), LEFT),
                new JoinClause(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), emailTable.getPKColumn(), LEFT),
                new JoinClause(contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID), phoneTable.getPKColumn(), LEFT)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(contactPkColumn, id)));
        final DataContainer dataContainer = readableDataStore.get(selectQuery);

        if (dataContainer.isEmpty()) {
            return null;
        }

        final Row contactCompanyRow = dataContainer.getRow(contactCompanyTable);
        final Contact contact = convertRowToContact(dataContainer.getRow(contactTable), dataContainer);

        if (Objects.nonNull(contactCompanyRow)) {
            contact.setCompany(fetchNestedCompany(contactCompanyRow.get(MCONCOMPANYFIELDS.COMPANY_ID)));
        }

        return contact;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id.
     * @param id    To fetch the {@link Company}.
     * @return {@link Company}.
     */
    @Override
    public Company fetchCompany(final Long orgId, final Long id) {
        final Table companyTable = companyManager.getTable();
        final Table addressTable = addressManager.getTable();
        final Table companyAddressTable = companyManager.getCompanyAddressTable();
        final Column companyPkColumn = companyTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(companyTable);
        final Map<String, FieldInfo> companyMapperFields = fieldService.groupDefaultFieldsByAliasName(Company.ENTITY_TYPE,
                CommonSearchFields.ADDRESS, CommonSearchFields.WEBSITE);

        companyManager.loadEntityMapperTableSelectAndJoinClause(companyAddressTable, selectQuery.getTables(), selectQuery, companyPkColumn,
                companyMapperFields.get(CommonSearchFields.ADDRESS).id());
        selectQuery.addSelectClause(List.of(companyTable.getColumn(MCOCOMPANY.NAME), companyTable.getColumn(MCOCOMPANY.OWNER_ID),
                companyTable.getColumn(MCOCOMPANY.WEBSITE), companyTable.getColumn(MCOCOMPANY.TERRITORY_ID),
                companyTable.getColumn(MCOCOMPANY.ORG_ID)));
        selectQuery.addJoinClause(new JoinClause(companyAddressTable.getColumn(
                MCONADDRESSFIELDS.ADDRESS_ID), addressTable.getPKColumn(), LEFT));
        selectQuery.addSelectClause(addressTable, companyAddressTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(companyPkColumn, id)));
        final DataContainer dataContainer = readableDataStore.get(selectQuery);

        if (dataContainer.isEmpty()) {
            return null;
        }
        final Row companyRow = dataContainer.getRow(companyManager.getTable());
        final Row companyAddressRow = dataContainer.getRow(companyAddressTable, new WhereClause(Criteria.eq(
                companyAddressTable.getColumn(MCONCOMPANYFIELDS.FIELD_ID), companyMapperFields.get(CommonSearchFields.ADDRESS).id())));
        final Row addressRow = Objects.isNull(companyAddressRow) ? null : dataContainer.getRow(addressTable, new WhereClause(Criteria.eq(
                addressTable.getPKColumn(), companyAddressRow.get(MCOMADDRESSFIELDS.ADDRESS_ID))));

        return convertRowToCompany(companyRow, addressRow, dataContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id.
     * @param id    To fetch the {@link Deal}.
     * @return {@link Deal}.
     */
    @Override
    public Deal fetchDeal(final Long orgId, final Long id) {

        return dealManager.getDealSummary(orgId, id);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id.
     * @param id    To fetch the {@link Pipeline}.
     * @return {@link Pipeline}.
     */
    @Override
    public Pipeline fetchPipeline(final Long orgId, final Long id) {
        final Table pipelineTable = pipelineManager.getTable();
        final SelectQuery query = SelectQuery.get(pipelineTable);

        query.addSelectClause(List.of(pipelineTable.getPKColumn(), pipelineTable.getColumn(MDOPIPELINE.NAME)));
        query.setWhereClause(new WhereClause(Criteria.eq(pipelineTable.getPKColumn(), id)));
        final DataContainer dataContainer = orgDataStore(orgId).get(query);
        final Row pipelineRow = dataContainer.getRow(pipelineTable);

        if (Objects.isNull(pipelineRow)) {
            return null;
        }

        return pipelineManager.convertRowToModel(pipelineRow, null, true);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id.
     * @param id    To fetch the {@link PipelineStage}.
     * @return {@link PipelineStage}.
     */
    @Override
    public PipelineStage fetchPipelineStage(final Long orgId, final Long id) {
        final Table pipelineStageTable = pipelineStageManager.getTable();
        final SelectQuery query = SelectQuery.get(pipelineStageTable);

        query.addSelectClause(List.of(pipelineStageTable.getPKColumn(), pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME)));
        query.setWhereClause(new WhereClause(Criteria.eq(pipelineStageTable.getPKColumn(), id)));
        final DataContainer dataContainer = orgDataStore(orgId).get(query);
        final Row pipelineStageRow = dataContainer.getRow(pipelineStageTable);

        if (Objects.isNull(pipelineStageRow)) {
            return null;
        }

        return pipelineStageManager.convertPartialRowToModel(pipelineStageRow);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id.
     * @param id    To fetch the {@link ActivityType}.
     * @return {@link ActivityType}.
     */
    @Override
    public ActivityType fetchActivityType(final Long orgId, final Long id) {
        final Table activityTypeTable = activityTypeManager.getTable();
        final SelectQuery query = SelectQuery.get(activityTypeTable);
        final Column pkColumn = activityTypeTable.getPKColumn();

        query.addSelectClause(List.of(pkColumn, activityTypeTable.getColumn(OMACACTIVITYTYPE.NAME)));
        query.setWhereClause(new WhereClause(Criteria.eq(pkColumn, id)));
        final DataContainer dataContainer = orgDataStore(orgId).get(query);
        final Row activityTypeRow = dataContainer.getRow(activityTypeTable);

        if (Objects.isNull(activityTypeRow)) {
            return null;
        }

        final ActivityType activityType = new ActivityType();

        activityType.setId(activityTypeRow.getPKValue());
        activityType.setName(activityTypeRow.get(OMACACTIVITYTYPE.NAME));

        return activityType;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents the organization id.
     * @param entityId   Refers the id of the entity.
     * @param source     Refers the type of the entity.
     * @return Entity value.
     */
    @Override
    public Map.Entry<String, Object> fetchEntityValue(final Long orgId, final Long entityId, final String source) {
        final String lowerCaseSource = source.toLowerCase();

        return switch (lowerCaseSource) {
            case "contact" -> Map.entry(lowerCaseSource, fetchContact(orgId, entityId));
            case "company" -> Map.entry(lowerCaseSource, fetchCompany(orgId, entityId));
            case "deal" -> Map.entry(lowerCaseSource, fetchDeal(orgId, entityId));
            default -> null;
        };
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents the organization id.
     * @param contactIds Contact ids.
     * @return Map of contact's owner and territory id field and ids pair.
     */
    @Override
    public Map<String, Collection<Long>> getContactPermissionRelatedIdsMap(final Long orgId, final Collection<Long> contactIds) {
        final Table contactTable = contactManager.getTable();
        final Table contactFollowersTable = sourceFollowersManager.getMapperTable(Source.CONTACT);
        final Column contactPkColumn = contactTable.getPKColumn();
        final SelectQuery contactQuery = SelectQuery.get(contactTable);
        final Collection<Long> ownerIds = new HashSet<>();
        final Collection<Long> territoryIds = new HashSet<>();

        contactQuery.addJoinClause(new JoinClause(contactPkColumn, contactFollowersTable.getColumn(
                OMCONFOLLOWERS.CONTACT_ID), LEFT));
        contactQuery.addSelectClause(List.of(contactPkColumn, contactTable.getColumn(MCOCONTACT.OWNER_ID),
                contactTable.getColumn(MCOCONTACT.TERRITORY_ID), contactFollowersTable.getColumn(OMCONFOLLOWERS.FOLLOWER_ID)));
        contactQuery.setWhereClause(new WhereClause(Criteria.in(contactPkColumn, contactIds)));
        final DataContainer dataContainer = readableDataStore.get(contactQuery);

        dataContainer.getRows(contactTable).forEach(contactRow -> {
            final Long territoryId = contactRow.get(MCOCONTACT.TERRITORY_ID);

            if (Objects.nonNull(territoryId)) {
                territoryIds.add(territoryId);
            }
            ownerIds.add(contactRow.get(MCOCONTACT.OWNER_ID));
        });

        dataContainer.getRows(contactFollowersTable).forEach(
                followerRow -> ownerIds.add(followerRow.get(OMCONFOLLOWERS.FOLLOWER_ID)));

        return Map.of(ActivityFields.OWNER_IDS.getName(), ownerIds, ActivityFields.TERRITORY_IDS.getName(), territoryIds);
    }

    /**
     * <p>
     * Loads a nested {@link Company} snapshot for index-related nested documents.
     * </p>
     *
     * @param companyId Company id.
     * @return Thin {@link Company} or {@code null}.
     */
    private Company fetchNestedCompany(final Long companyId) {
        final Table companyTable = companyManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(companyTable);

        selectQuery.addSelectClause(List.of(companyTable.getColumn(MCOCOMPANY.NAME), companyTable.getColumn(MCOCOMPANY.OWNER_ID),
                companyTable.getColumn(MCOCOMPANY.TERRITORY_ID), companyTable.getColumn(MCOCOMPANY.ORG_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(companyTable.getPKColumn(), companyId)));
        final DataContainer dataContainer = readableDataStore.get(selectQuery);

        return dataContainer.isEmpty() ? null : buildPartialCompany(dataContainer.getRow(companyTable), dataContainer);
    }

    /**
     * <p>
     * Builds a nested company snapshot from the given row.
     * </p>
     *
     * @param companyRow    Company row.
     * @param dataContainer {@link DataContainer}.
     * @return {@link Company}.
     */
    private Company buildPartialCompany(final Row companyRow, final DataContainer dataContainer) {
        final Company company = new Company();
        final Long territoryId = companyRow.get(MCOCOMPANY.TERRITORY_ID);

        company.setId(companyRow.getPKValue());
        company.setOrgId(companyRow.getOrgId());
        company.setName(companyRow.get(MCOCOMPANY.NAME));
        company.setOwnerId(companyRow.get(MCOCOMPANY.OWNER_ID));

        if (Objects.nonNull(territoryId)) {
            company.setTerritory(companyManager.buildPartialTerritory(territoryId, dataContainer));
        }

        return company;
    }

    /**
     * <p>
     * Converts row into {@link Company}.
     * </p>
     *
     * @param companyRow    Row containing company details.
     * @param addressRow    Row containing address details.
     * @param dataContainer {@link DataContainer}.
     * @return {@link Company}.
     */
    private Company convertRowToCompany(final Row companyRow, final Row addressRow, final DataContainer dataContainer) {
        final Company company = new Company();
        final Long territoryId = companyRow.get(MCOCOMPANY.TERRITORY_ID);

        company.setId(companyRow.getPKValue());
        company.setOrgId(companyRow.getOrgId());
        company.setOwnerId(companyRow.get(MCOCOMPANY.OWNER_ID));
        company.setName(companyRow.get(MCOCOMPANY.NAME));
        company.setWebsite(companyRow.get(MCOCOMPANY.WEBSITE));

        if (Objects.nonNull(territoryId)) {
            company.setTerritory(companyManager.buildPartialTerritory(territoryId, dataContainer));
        }

        if (Objects.nonNull(addressRow)) {
            company.setAddress(addressManager.convertRowToModel(addressRow));
        }

        return company;
    }

    /**
     * <p>
     * Converts row into {@link Contact}.
     * </p>
     *
     * @param contactRow    Row containing contact details.
     * @param dataContainer {@link DataContainer}.
     * @return {@link Contact}.
     */
    private Contact convertRowToContact(final Row contactRow, final DataContainer dataContainer) {
        final Contact contact = new Contact();
        final Long contactId = contactRow.getPKValue();
        final Long territoryId = contactRow.get(MCOCONTACT.TERRITORY_ID);
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE,
                ContactFields.PHONES, ContactFields.EMAILS, ContactFields.ADDRESS);

        contact.setId(contactId);
        contact.setOrgId(contactRow.getOrgId());
        contact.setFirstName(contactRow.get(MCOCONTACT.FIRST_NAME));
        contact.setLastName(contactRow.get(MCOCONTACT.LAST_NAME));
        contact.setOwnerId(contactRow.get(MCOCONTACT.OWNER_ID));
        contact.setFacebook(contactRow.get(MCOCONTACT.FACEBOOK));
        contact.setLinkedIn(contactRow.get(MCOCONTACT.LINKEDIN));
        contact.setAddress(contactManager.buildAddress(contactId, fields.get(ContactFields.ADDRESS).id(), dataContainer));
        contactManager.buildPhones(contactId, contact, contactRow, fields.get(ContactFields.PHONES).id(), dataContainer);
        contactManager.buildEmails(contactId, contact, contactRow, fields.get(ContactFields.EMAILS).id(), dataContainer);

        if (Objects.nonNull(territoryId)) {
            contact.setTerritory(contactManager.buildPartialTerritory(territoryId, dataContainer));
        }

        return contact;
    }

    /**
     * <p>
     * Gets {@link OrgDataStore}.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @return {@link OrgDataStore}.
     */
    private OrgDataStore orgDataStore(final Long orgId) {

        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }
}