package com.tlc.crm.core.features.quote.manager.impl;

import com.tlc.cache.remote.CacheService;
import com.tlc.cache.remote.DistributedAtomicCounter;
import com.tlc.cache.service.provider.CacheServiceProvider;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;

import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.features.quote.manager.QuotationManager;
import com.tlc.crm.core.features.quote.model.Quotation;
import com.tlc.crm.core.features.quote.model.QuotationTemplate;
import com.tlc.crm.core.internal.quote.status.QuotationErrorCodes;
import com.tlc.crm.core.sql.resource.MQNQUOTATIONTEMPLATE;
import com.tlc.crm.core.sql.resource.OMQNCOMPANYQUOTATION;
import com.tlc.crm.core.sql.resource.OMQNCONTACTQUOTATION;
import com.tlc.crm.core.sql.resource.OMQNQUOTATION;
import com.tlc.crm.core.sql.resource.OMQNQUOTATIONTEMPLATE;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.manager.field.type.MonetaryManager;
import com.tlc.crm.entity.models.type.Monetary;

import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.DateTime;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Implements the {@link Quotation} related DB actions, covering full CRUD and clone operations.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class QuotationManagerImpl implements QuotationManager {

    public static final String NAME_CLONE_FORMAT = "%s - Copy";
    private static final String QUOTATION_NUMBER_COUNTER_KEY_FORMAT = "quotation:number:%d";

    private final UserService userService;
    private final MonetaryManager monetaryManager;
    private final AdminDataStore adminDataStore;
    private final CacheService cacheService;
    private final DealManager dealManager;

    private static class Instance {
        private static final QuotationManagerImpl INSTANCE = new QuotationManagerImpl();
    }

    private QuotationManagerImpl() {
        this.userService = UserService.getInstance();
        this.monetaryManager = MonetaryManager.getInstance();
        this.adminDataStore = DataStoreProvider.getAdminDataStore();
        this.cacheService = CacheServiceProvider.get();
        this.dealManager = DealManager.getInstance();
    }

    /**
     * Returns the singleton {@link QuotationManagerImpl} instance.
     *
     * @return singleton instance
     */
    public static QuotationManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    // ────────────────────────── TABLE HELPERS ─────────────────────────────────

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getTable() {
        return Table.get(OMQNQUOTATION.TABLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getContactQuotationTable() {
        return Table.get(OMQNCONTACTQUOTATION.TABLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getCompanyQuotationTable() {
        return Table.get(OMQNCOMPANYQUOTATION.TABLE);
    }

    /**
     * Returns the organisation-specific quotation template mapping table.
     *
     * @return the {@link Table} representing the org-to-template mapping
     */
    private Table getOrgQuotationTemplateTable() {
        return Table.get(OMQNQUOTATIONTEMPLATE.TABLE);
    }

    /**
     * Returns the master quotation template table.
     *
     * @return the {@link Table} representing the global quotation templates
     */
    private Table getQuotationTemplateTable() {
        return Table.get(MQNQUOTATIONTEMPLATE.TABLE);
    }

    /**
     * Returns the secure organisation-scoped data store for the given organisation.
     *
     * @param orgId the organisation ID
     * @return the {@link OrgDataStore} instance
     */
    private OrgDataStore secureOrgDataStore(final long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * Returns the organisation-scoped data store for the given organisation.
     *
     * @param orgId the organisation ID
     * @return the {@link OrgDataStore} instance
     */
    private OrgDataStore orgDataStore(final long orgId) {
        return DataStoreProvider.getOrgDataStore(orgId);
    }

    // ─────────────────────────────── CREATE ───────────────────────────────────

    /**
     * {@inheritDoc}
     */
    @Override
    public void create(final Quotation quotation) {
        bulkCreate(quotation.orgId(), List.of(quotation));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void bulkCreate(final long orgId, final Collection<Quotation> quotations) {
        if (quotations.isEmpty()) {
            return;
        }
        final DataContainer dataContainer = DataContainer.create();

        for (final Quotation quotation : quotations) {
            loadRowIntoContainer(true, quotation, null, dataContainer);
        }

        secureOrgDataStore(orgId).commitChanges(dataContainer);
    }

    // ─────────────────────────────── READ ─────────────────────────────────────

    /**
     * {@inheritDoc}
     */
    @Override
    public Quotation get(final long orgId, final long id) {
        return get(orgId, Set.of(id)).stream().findAny()
                .orElseThrow(() -> ErrorCode.get(QuotationErrorCodes.QUOTATION_NOT_FOUND, "i18n.quotation.not.found"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Quotation> get(final long orgId, final Collection<Long> ids) {
        final Table quotationTable = getTable();
        final Table contactQuotationTable = getContactQuotationTable();
        final Table companyQuotationTable = getCompanyQuotationTable();
        final SelectQuery selectQuery = buildSelectQuery(
                new WhereClause(Criteria.in(quotationTable.getPKColumn(), ids)));
        final DataContainer dataContainer = secureOrgDataStore(orgId).get(selectQuery);
        final Set<Long> quotationIds = dataContainer.getRows(quotationTable).map(Row::getPKValue).collect(Collectors.toSet());
        final DataContainer companyAndContactsContainer = fetchCompanyAndContactsContainer(orgId, quotationIds);

        return dataContainer.getRows(quotationTable).map(row -> {
            final Quotation quotation = convertRowToQuotation(row, dataContainer);
            final long companiesCount = companyAndContactsContainer.getRows(companyQuotationTable,
                    new WhereClause(Criteria.eq(companyQuotationTable.getColumn(OMQNCOMPANYQUOTATION.QUOTATION_ID), quotation.id()))).count();
            final long contactsCount = companyAndContactsContainer.getRows(contactQuotationTable,
                    new WhereClause(Criteria.eq(contactQuotationTable.getColumn(OMQNCONTACTQUOTATION.QUOTATION_ID), quotation.id()))).count();

            quotation.setActualContactsCount(Long.valueOf(contactsCount).intValue());
            quotation.setCompanyPresent(companiesCount > 0);
            return quotation;
        }).toList();
    }

    // ─────────────────────────────── UPDATE ───────────────────────────────────

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(final Quotation quotation) {
        bulkUpdate(quotation.orgId(), Set.of(quotation));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void bulkUpdate(final long orgId, final Collection<Quotation> quotations) {
        if (quotations.isEmpty()) {
            return;
        }
        final Table quotationTable = getTable();
        final OrgDataStore orgDataStore = secureOrgDataStore(orgId);

        final Collection<Long> quotationIds = quotations.stream()
                .map(Quotation::id)
                .collect(Collectors.toList());

        final SelectQuery selectQuery = buildSelectQuery(
                new WhereClause(Criteria.in(quotationTable.getPKColumn(), quotationIds)));
        final DataContainer sourceContainer = orgDataStore.get(selectQuery);
        final DataContainer targetContainer = DataContainer.create();

        for (final Quotation quotation : quotations) {
            loadRowIntoContainer(false, quotation, sourceContainer, targetContainer);
        }

        orgDataStore.commitChanges(targetContainer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateStatus(final long orgId, final long userId, final long id, final Quotation.Status status) {
        final Table quotationTable = getTable();
        final OrgDataStore orgDataStore = secureOrgDataStore(orgId);
        final Row quotationRow = orgDataStore.get(quotationTable, id);

        if (Objects.isNull(quotationRow)) {
            return;
        }
        final DataContainer targetContainer = DataContainer.create();

        quotationRow.set(OMQNQUOTATION.STATUS, status.getId());
        quotationRow.set(OMQNQUOTATION.UPDATED_TIME, TemporalServiceProvider.get().getUtcDateTime());
        quotationRow.set(OMQNQUOTATION.UPDATED_BY, userId);
        targetContainer.updateRow(quotationRow);

        orgDataStore.commitChanges(targetContainer);
    }

    // ─────────────────────────────── DELETE ───────────────────────────────────

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final long orgId, final long id) {
        secureOrgDataStore(orgId).delete(getTable(), id);
    }

    // ─────────────────────────────── CLONE ────────────────────────────────────

    /**
     * {@inheritDoc}
     */
    @Override
    public Quotation clone(final long orgId, final long id) {
        final Quotation quotation = get(orgId, id);
        final String quoteJsonAsString = quotation.getQuoteJsonAsString();
        final Monetary amount = quotation.getAmount();

        quotation.setId(null);
        quotation.setName(String.format(NAME_CLONE_FORMAT, quotation.getName()));
        quotation.setUuid(UUID.randomUUID().toString());
        quotation.setStatus(Quotation.Status.DRAFT);
        quotation.setNumber(generateQuotationNumber(orgId));

        if (Objects.nonNull(quoteJsonAsString)) {
            quotation.setQuoteJsonAsString(quoteJsonAsString);
        }

        if (Objects.nonNull(amount)) {
            amount.setId(null);
        }

        create(quotation);
        return quotation;
    }

    // ─────────────────────────── TEMPLATE HELPERS ────────────────────────────

    /**
     * {@inheritDoc}
     */
    @Override
    public void createDefaultTemplateMapping(final long orgId, final long userId, final Collection<Long> templateIds) {
        final DataContainer dataContainer = DataContainer.create();
        final Table templateMappingTable = getOrgQuotationTemplateTable();
        final long currentTime = TemporalServiceProvider.get().getUtcDateTime();
        boolean isDefault = true;

        for (final Long templateId : templateIds) {
            final Row templateMappingRow = new Row(templateMappingTable);

            templateMappingRow.setOrgId(orgId);
            templateMappingRow.set(OMQNQUOTATIONTEMPLATE.TEMPLATE_ID, templateId);
            templateMappingRow.set(OMQNQUOTATIONTEMPLATE.IS_DEFAULT, isDefault);
            templateMappingRow.set(OMQNQUOTATIONTEMPLATE.CREATED_BY, userId);
            templateMappingRow.set(OMQNQUOTATIONTEMPLATE.UPDATED_BY, userId);
            templateMappingRow.set(OMQNQUOTATIONTEMPLATE.CREATED_TIME, currentTime);
            templateMappingRow.set(OMQNQUOTATIONTEMPLATE.UPDATED_TIME, currentTime);

            dataContainer.addNewRow(templateMappingRow);
            isDefault = false;
        }
        secureOrgDataStore(orgId).commitChanges(dataContainer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<QuotationTemplate> getDefaultTemplates() {
        final Table templateTable = getQuotationTemplateTable();
        final SelectQuery selectQuery = SelectQuery.get(templateTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(templateTable.getColumn(MQNQUOTATIONTEMPLATE.IS_DEFAULT), true)));
        final DataContainer dataContainer = adminDataStore.get(selectQuery);

        return dataContainer.getRows(templateTable).map(templateRow -> convertRowToQuotationTemplate(templateRow, dataContainer)).toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<QuotationTemplate> getDefaultTemplate(final long orgId) {
        final Table templateTable = getQuotationTemplateTable();
        final Table orgTemplateTable = getOrgQuotationTemplateTable();
        final SelectQuery selectQuery = SelectQuery.get(templateTable);

        selectQuery.addJoinClause(new JoinClause(templateTable.getPKColumn(), orgTemplateTable.getColumn(OMQNQUOTATIONTEMPLATE.TEMPLATE_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(orgTemplateTable.getColumn(OMQNQUOTATIONTEMPLATE.IS_DEFAULT), true)));
        final DataContainer dataContainer = secureOrgDataStore(orgId).get(selectQuery);

        return Optional.ofNullable(dataContainer.getRow(templateTable)).map(templateRow -> convertRowToQuotationTemplate(templateRow, dataContainer));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadStatusUpdateIntoContainer(final long orgId, final long id, final Quotation.Status status,
                                              final DataContainer targetContainer) {
        final Row quotationRow = secureOrgDataStore(orgId).get(getTable(), id);

        if (Objects.isNull(quotationRow)) {
            return;
        }
        quotationRow.set(OMQNQUOTATION.STATUS, status.getId());
        quotationRow.set(OMQNQUOTATION.UPDATED_TIME, TemporalServiceProvider.get().getUtcDateTime());

        targetContainer.updateRow(quotationRow);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Quotation.Status> getStatus(final long orgId, final long quotationId) {
        final Row quotationRow = secureOrgDataStore(orgId).get(getTable(), quotationId);

        if (Objects.nonNull(quotationRow)) {
            final Short status = quotationRow.get(OMQNQUOTATION.STATUS);

            if (Objects.nonNull(status)) {
                return Optional.of(Quotation.Status.get(status));
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves the quote file ID associated with the given organization and quote ID.
     *
     * @param orgId the organization identifier
     * @param id    the quote identifier
     * @return an {@link Optional} containing the quote file ID if available, otherwise empty
     */
    @Override
    public Collection<Long> getQuoteFileIds(final long orgId, final long id) {
        final Table table = getTable();
        final DataContainer dataContainer = secureOrgDataStore(orgId).get(table,
                new WhereClause(Criteria.eq(table.getPKColumn(), id)),
                List.of(table.getColumn(OMQNQUOTATION.HTML_FILE_ID), table.getColumn(OMQNQUOTATION.PDF_FILE_ID))
        );

        return Optional.ofNullable(dataContainer.getRow(table)).map(row -> {
            final Long htmlFileId = row.get(OMQNQUOTATION.HTML_FILE_ID);
            final Long pdfFileId = row.get(OMQNQUOTATION.PDF_FILE_ID);

            return Stream.of(htmlFileId, pdfFileId).filter(Objects::nonNull).collect(Collectors.toSet());
        }).orElseGet(Collections::emptySet);
    }

    /**
     * Retrieves the associated company and contact IDs for each of the given quotations,
     * grouped by their {@link Source}.
     *
     * <p>
     * The returned map is keyed by quotation ID. Each value is a nested map whose keys
     * are {@link Source} values (e.g., company source, contact source) and whose values
     * are the collections of entity IDs belonging to that source for the respective quotation.
     * </p>
     *
     * @param orgId        the organisation ID
     * @param quotationIds the collection of quotation IDs for which to fetch associations
     * @return a map of quotation ID → (source → collection of associated entity IDs);
     * never {@code null}, but may be empty if no associations are found
     */
    @Override
    public Map<Long, Map<Source, Collection<Long>>> getCompanyAndContactIds(final long orgId, final Collection<Long> quotationIds) {
        final Table companyQuotationTable = getCompanyQuotationTable();
        final Table contactQuotationTable = getContactQuotationTable();
        final DataContainer dataContainer = fetchCompanyAndContactsContainer(orgId, quotationIds);
        final Map<Long, Map<Source, Collection<Long>>> companyAndContactsMap = new HashMap<>();

        dataContainer.getRows(companyQuotationTable).forEach(companyQuoteRow -> {
            final Map<Source, Collection<Long>> quoteEntityIds = companyAndContactsMap.computeIfAbsent(companyQuoteRow.get(OMQNCOMPANYQUOTATION.QUOTATION_ID), quoteId -> new HashMap<>());
            final Collection<Long> companyIds = quoteEntityIds.computeIfAbsent(Source.COMPANY, sourceId -> new HashSet<>());

            companyIds.add(companyQuoteRow.get(OMQNCOMPANYQUOTATION.COMPANY_ID));
        });

        dataContainer.getRows(contactQuotationTable).forEach(contactQuoteRow -> {
            final Map<Source, Collection<Long>> quoteEntityIds = companyAndContactsMap.computeIfAbsent(contactQuoteRow.get(OMQNCONTACTQUOTATION.QUOTATION_ID), quoteId -> new HashMap<>());
            final Collection<Long> contactIds = quoteEntityIds.computeIfAbsent(Source.CONTACT, sourceId -> new HashSet<>());

            contactIds.add(contactQuoteRow.get(OMQNCONTACTQUOTATION.CONTACT_ID));
        });

        return companyAndContactsMap;
    }

    // ─────────────────────────────── HELPERS ──────────────────────────────────

    /**
     * Executes a LEFT JOIN query against the quotation, company-quotation, and
     * contact-quotation tables to fetch all company and contact association rows
     * for the specified quotations in a single round-trip.
     *
     * <p>
     * The resulting {@link DataContainer} contains rows from both join tables
     * ({@code OMQNCOMPANYQUOTATION} and {@code OMQNCONTACTQUOTATION}) keyed by
     * quotation ID, allowing callers to filter and count associations per quotation
     * without issuing additional queries.
     * </p>
     *
     * @param orgId        the organisation ID used to scope the data store lookup
     * @param quotationIds the set of quotation IDs whose associations should be fetched
     * @return a {@link DataContainer} holding the joined company and contact rows
     *         for all requested quotations; rows are absent (not {@code null}) when
     *         no association exists for a given quotation
     */
    private DataContainer fetchCompanyAndContactsContainer(final long orgId, final Collection<Long> quotationIds) {
        final Table quotationTable = getTable();
        final Table companyQuotationTable = getCompanyQuotationTable();
        final Table contactQuotationTable = getContactQuotationTable();
        final Column quotationPkColumn = quotationTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(quotationTable);

        selectQuery.addSelectClause(companyQuotationTable, contactQuotationTable);
        selectQuery.addJoinClause(new JoinClause(quotationPkColumn, companyQuotationTable.getColumn(OMQNCOMPANYQUOTATION.QUOTATION_ID), JoinClause.JoinType.LEFT));
        selectQuery.addJoinClause(new JoinClause(quotationPkColumn, contactQuotationTable.getColumn(OMQNCONTACTQUOTATION.QUOTATION_ID), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(quotationPkColumn, quotationIds)));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * Builds a {@link SelectQuery} for the quotation table with the given
     * filter, joining the contact and company association tables.
     *
     * @param whereClause the filter criteria
     * @return the constructed {@link SelectQuery}
     */
    private SelectQuery buildSelectQuery(final WhereClause whereClause) {
        final Table quotationTable = getTable();
        final Table monetaryTable = monetaryManager.getTable();
        final Table contactQuotationTable = getContactQuotationTable();
        final Table companyQuotationTable = getCompanyQuotationTable();
        final SelectQuery selectQuery = SelectQuery.get(quotationTable);

        selectQuery.addSelectClause(quotationTable, contactQuotationTable, companyQuotationTable, monetaryTable);
        selectQuery.addJoinClause(new JoinClause(quotationTable.getColumn(OMQNQUOTATION.AMOUNT_ID), monetaryTable.getPKColumn()));
        selectQuery.addJoinClause(new JoinClause(
                quotationTable.getPKColumn(),
                contactQuotationTable.getColumn(OMQNCONTACTQUOTATION.QUOTATION_ID),
                JoinClause.JoinType.LEFT));
        selectQuery.addJoinClause(new JoinClause(
                quotationTable.getPKColumn(),
                companyQuotationTable.getColumn(OMQNCOMPANYQUOTATION.QUOTATION_ID),
                JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(whereClause);

        return selectQuery;
    }

    /**
     * Populates a {@link DataContainer} with the rows needed to persist
     * (create or update) a {@link Quotation}, including its contact and
     * company associations.
     *
     * @param isCreateEvent   {@code true} for a create operation, {@code false} for update
     * @param quotation       the quotation model to persist
     * @param sourceContainer existing data fetched from the DB (used for update only; {@code null} on create)
     * @param targetContainer the container that will be committed to the DB
     */
    private void loadRowIntoContainer(final boolean isCreateEvent, final Quotation quotation,
                                      final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table quotationTable = getTable();
        final Long id = quotation.id();
        final Row quotationRow = isCreateEvent ? new Row(quotationTable) : sourceContainer.getRow(quotationTable, id);

        if (!isCreateEvent && Objects.isNull(quotationRow)) {
            throw ErrorCode.get(QuotationErrorCodes.QUOTATION_NOT_FOUND,
                    String.format("Org ID: %s, Quotation ID: %s", quotation.orgId(), id));
        }

        final long currentTime = TemporalServiceProvider.get().getUtcDateTime();

        if (quotationRow.isNewRow()) {
            final Long quotationId = quotationRow.getPKValue();
            final CrmUser createdBy = quotation.getCreatedBy();
            final JsonObject quoteJson = quotation.getQuoteJson();

            quotationRow.set(OMQNQUOTATION.NUMBER, quotation.getNumber());
            quotationRow.set(OMQNQUOTATION.CREATED_TIME, currentTime);
            quotationRow.set(OMQNQUOTATION.UPDATED_TIME, currentTime);
            quotationRow.set(OMQNQUOTATION.TEMPLATE_ID, quotation.getTemplateId());
            quotationRow.set(OMQNQUOTATION.UUID, quotation.getUuid());

            quotation.setId(quotationId);
            quotation.setQuoteJson(quoteJson);

            quotationRow.set(OMQNQUOTATION.CREATED_BY, createdBy.id());
            quotationRow.set(OMQNQUOTATION.UPDATED_BY, createdBy.id());
        } else {
            final CrmUser updatedBy = quotation.getUpdatedBy();

            quotationRow.set(OMQNQUOTATION.UPDATED_TIME, currentTime);
            quotationRow.set(OMQNQUOTATION.UPDATED_BY, updatedBy.id());
            quotation.setUpdatedAt(new DateTime(currentTime));
        }
        final String quoteJsonAsString = quotation.getQuoteJsonAsString();
        final Monetary amount = quotation.getAmount();
        final CrmUser crmUser = quotation.getOwner();
        final Deal deal = quotation.getDeal();
        final DateTime expirationTime = quotation.getExpirationTime();
        final Collection<Contact> contacts = quotation.getContacts();
        final Long htmlFileId = quotation.getHtmlFileId();
        final Long pdfFileId = quotation.getPdfFileId();

        quotationRow.set(OMQNQUOTATION.NAME, quotation.getName());
        quotationRow.set(OMQNQUOTATION.STATUS, quotation.getStatus().getId());

        if (Objects.nonNull(quoteJsonAsString)) {
            quotationRow.set(OMQNQUOTATION.SCHEMA_WITH_VALUE_JSON, quoteJsonAsString);
        }

        if (Objects.nonNull(amount)) {
            monetaryManager.loadMonetaryIntoContainer(isCreateEvent, amount, sourceContainer, targetContainer);
            quotationRow.set(OMQNQUOTATION.AMOUNT_ID, amount.id());
        }

        if (Objects.nonNull(crmUser)) {
            quotationRow.set(OMQNQUOTATION.OWNER_ID, crmUser.id());
        }

        if (Objects.nonNull(deal)) {
            quotationRow.set(OMQNQUOTATION.DEAL_ID, deal.id());
        }

        if (Objects.nonNull(expirationTime)) {
            quotationRow.set(OMQNQUOTATION.EXPIRATION_TIME, expirationTime);
        }

        if (Objects.nonNull(htmlFileId)) {
            quotationRow.set(OMQNQUOTATION.HTML_FILE_ID, htmlFileId);
        }

        if (Objects.nonNull(pdfFileId)) {
            quotationRow.set(OMQNQUOTATION.PDF_FILE_ID, pdfFileId);
        }

        targetContainer.updateRow(quotationRow);
        loadContactDetailsIntoContainer(quotation.orgId(), quotation.id(), Objects.isNull(contacts) ? Collections.emptyList() : contacts, sourceContainer, targetContainer);
        loadCompanyDetailsIntoContainer(quotation.id(), quotation.orgId(), quotation.getCompany(), sourceContainer, targetContainer);
    }


    /**
     * Generates the next sequential quotation number for the given organisation
     * using a Redis-backed distributed atomic counter to ensure uniqueness across
     * multiple threads and server instances.
     * <p>
     * On first invocation (counter value is {@code -1} sentinel), the counter is
     * seeded from the database's current maximum quotation number using
     * {@code compareAndSet} to prevent initialization races. The sentinel value
     * {@code -1} distinguishes an uninitialized counter from a legitimately zero-valued one.
     * </p>
     *
     * @param orgId the organisation ID
     * @return the next quotation number as an integer
     */
    @Override
    public int generateQuotationNumber(final long orgId) {
        final String counterKey = String.format(QUOTATION_NUMBER_COUNTER_KEY_FORMAT, orgId);
        final DistributedAtomicCounter counter = cacheService.getOrInitAtomicCounter(counterKey);
        final long currentValue = counter.get();

        if (currentValue == 0) {
            final long currentMax = fetchCurrentMaxSequence(orgId);

            counter.compareAndSet(0, currentMax);
        }
        final long nextSequence = counter.incrementAndGet();

        return (int) nextSequence;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Performs an efficient existence check for the specified quotation id
     * scoped to the provided organisation by issuing a count query against the
     * quotation table. Returns {@code true} when at least one row matches.</p>
     *
     * @param orgId   the organisation id used to scope the lookup; may be {@code null}
     *                if calling context expects an unscoped check
     * @param quoteId the quotation id to check; must not be {@code null}
     * @return {@code true} if a quotation with the given id exists for the
     *         organisation, otherwise {@code false}
     */
    @Override
    public boolean exists(final Long orgId, final Long quoteId) {
        final Table quotationTable = getTable();
        final CountQuery countQuery = CountQuery.get(quotationTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(quotationTable.getPKColumn(), quoteId)));
        return secureOrgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * Fetches the current maximum quotation sequence number from the database
     * for counter initialization.
     *
     * @param orgId the organisation ID
     * @return the current max sequence number, or {@code 0} if none exist
     */
    private long fetchCurrentMaxSequence(final long orgId) {
        final Table quotationTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(quotationTable);

        selectQuery.addSelectClause(quotationTable.getColumn(OMQNQUOTATION.NUMBER));
        selectQuery.addOrderByClause(new OrderByClause(quotationTable.getPKColumn(), OrderByClause.OrderType.DESCENDING));
        selectQuery.setLimitClause(new LimitClause(1));
        final DataContainer dataContainer = secureOrgDataStore(orgId).get(selectQuery);
        final Row row = dataContainer.getRow(quotationTable);

        if (Objects.nonNull(row)) {
            final Integer maxNumber = row.get(OMQNQUOTATION.NUMBER);

            if (Objects.nonNull(maxNumber)) {
                return maxNumber;
            }
        }

        return 0;
    }

    /**
     * Converts a raw database {@link Row} into a fully populated {@link Quotation} model.
     *
     * @param row           the raw SQL row
     * @param dataContainer contextual data container (used to resolve joined rows)
     * @return the converted {@link Quotation}
     */
    private Quotation convertRowToQuotation(final Row row, final DataContainer dataContainer) {
        final Quotation quotation = new Quotation();
        final Long quotationId = row.getPKValue();
        final Long orgId = row.getOrgId();
        final Short status = row.get(OMQNQUOTATION.STATUS);
        final Long amountId = row.get(OMQNQUOTATION.AMOUNT_ID);
        final String quoteJson = row.get(OMQNQUOTATION.SCHEMA_WITH_VALUE_JSON);
        final Long createdById = row.get(OMQNQUOTATION.CREATED_BY);
        final Long updatedById = row.get(OMQNQUOTATION.UPDATED_BY);
        final Long ownerId = row.get(OMQNQUOTATION.OWNER_ID);
        final Long dealId = row.get(OMQNQUOTATION.DEAL_ID);
        final Set<Long> userIds = new HashSet<>();
        final int number = row.get(OMQNQUOTATION.NUMBER);

        quotation.setId(quotationId);
        quotation.setOrgId(orgId);
        quotation.setName(row.get(OMQNQUOTATION.NAME));
        quotation.setNumber(number);
        quotation.setUuid(row.get(OMQNQUOTATION.UUID));
        quotation.setTemplateId(row.get(OMQNQUOTATION.TEMPLATE_ID));
        quotation.setHtmlFileId(row.get(OMQNQUOTATION.HTML_FILE_ID));
        quotation.setPdfFileId(row.get(OMQNQUOTATION.PDF_FILE_ID));

        if (Objects.nonNull(status)) {
            quotation.setStatus(Quotation.Status.get(status));
        }

        if (Objects.nonNull(amountId) && Objects.nonNull(dataContainer)) {
            final Row monetaryRow = dataContainer.getRow(monetaryManager.getTable(), amountId);

            if (Objects.nonNull(monetaryRow)) {
                quotation.setAmount(monetaryManager.convertRowToModel(monetaryRow));
            }
        }

        if (Objects.nonNull(quoteJson)) {
            quotation.setQuoteJsonAsString(quoteJson);
        }

        quotation.setCreatedAt(row.get(OMQNQUOTATION.CREATED_TIME));
        quotation.setUpdatedAt(row.get(OMQNQUOTATION.UPDATED_TIME));
        quotation.setExpirationTime(row.get(OMQNQUOTATION.EXPIRATION_TIME));

        if (Objects.nonNull(createdById)) {
            userIds.add(createdById);
        }

        if (Objects.nonNull(updatedById)) {
            userIds.add(updatedById);
        }

        if (Objects.nonNull(ownerId)) {
            userIds.add(ownerId);
        }

        if (!userIds.isEmpty()) {
            final Map<Long, CrmUser> users = userService.getUsers(orgId, userIds)
                    .stream()
                    .collect(Collectors.toMap(CrmUser::id, Function.identity()));

            if (Objects.nonNull(createdById)) {
                quotation.setCreatedBy(users.get(createdById));
            }

            if (Objects.nonNull(updatedById)) {
                quotation.setUpdatedBy(users.get(updatedById));
            }

            if (Objects.nonNull(ownerId)) {
                final CrmUser owner = users.get(ownerId);

                if (Objects.nonNull(owner)) {
                    quotation.setOwner(owner);
                }
            }
        }

        if (Objects.nonNull(dealId)) {
            final Deal deal = new Deal();

            deal.setId(dealId);
            dealManager.getPrimaryContact(orgId, dealId).ifPresent(deal::setPrimaryContact);
            quotation.setDeal(deal);
        }

        if (Objects.nonNull(dataContainer)) {
            final Table contactQuotationTable = getContactQuotationTable();
            final Table companyQuotationTable = getCompanyQuotationTable();
            final Collection<Contact> contacts = dataContainer.getRows(contactQuotationTable,
                            new WhereClause(Criteria.eq(contactQuotationTable.getColumn(OMQNCONTACTQUOTATION.QUOTATION_ID), quotationId)))
                    .map(contactRow -> {
                        final Contact contact = new Contact();

                        contact.setId(contactRow.get(OMQNCONTACTQUOTATION.CONTACT_ID));
                        return contact;
                    }).collect(Collectors.toList());

            quotation.setContacts(contacts);

            dataContainer.getRows(companyQuotationTable,
                            new WhereClause(Criteria.eq(companyQuotationTable.getColumn(OMQNCOMPANYQUOTATION.QUOTATION_ID), quotationId)))
                    .findFirst().ifPresent(companyRow -> {
                        final Company company = new Company();

                        company.setId(companyRow.get(OMQNCOMPANYQUOTATION.COMPANY_ID));
                        quotation.setCompany(company);
                    });
        }

        return quotation;
    }

    /**
     * Loads the contact-quotation join rows by diffing the provided contact
     * collection against what is already persisted.
     *
     * @param orgId           the organisation ID
     * @param quotationId     the quotation ID
     * @param contacts        the current set of contacts
     * @param sourceContainer existing data from the DB (may be {@code null} on create)
     * @param targetContainer the container to stage changes for commit
     */
    private void loadContactDetailsIntoContainer(final long orgId, final long quotationId, final Collection<Contact> contacts,
                                                 final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table contactQuotationTable = getContactQuotationTable();
        final Map<Long, Contact> contactMap = contacts.stream().collect(Collectors.toMap(Contact::id, Function.identity()));

        final Stream<Row> existingRows = Objects.isNull(sourceContainer) ? Stream.empty() : sourceContainer.getRows(contactQuotationTable,
                new WhereClause(Criteria.eq(contactQuotationTable.getColumn(OMQNCONTACTQUOTATION.QUOTATION_ID), quotationId)));

        existingRows.forEach(existingRow -> {
            final Long contactId = existingRow.get(OMQNCONTACTQUOTATION.CONTACT_ID);

            if (Objects.isNull(contactMap.remove(contactId))) {
                targetContainer.deleteRow(existingRow);
            }
        });

        for (final Contact contact : contactMap.values()) {
            final Row contactQuotationRow = new Row(contactQuotationTable);

            contactQuotationRow.setOrgId(orgId);
            contactQuotationRow.set(OMQNCONTACTQUOTATION.QUOTATION_ID, quotationId);
            contactQuotationRow.set(OMQNCONTACTQUOTATION.CONTACT_ID, contact.id());
            targetContainer.addNewRow(contactQuotationRow);
        }
    }

    /**
     * Loads the company-quotation join row. Removes any stale row and
     * inserts the new one when the company has changed.
     *
     * @param quotationId     the quotation ID
     * @param orgId           the organisation ID
     * @param company         the company to associate
     * @param sourceContainer existing data from the DB (may be {@code null} on create)
     * @param targetContainer the container to stage changes for commit
     */
    private void loadCompanyDetailsIntoContainer(final long quotationId, final long orgId, final Company company,
                                                 final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table companyQuotationTable = getCompanyQuotationTable();

        if (Objects.isNull(company)) {
            final Row existingCompanyQuotationRow = Objects.nonNull(sourceContainer) ?
                    sourceContainer.getRow(companyQuotationTable,
                            new WhereClause(Criteria.eq(companyQuotationTable.getColumn(OMQNCOMPANYQUOTATION.QUOTATION_ID), quotationId))) : null;

            if (Objects.nonNull(existingCompanyQuotationRow)) {
                targetContainer.deleteRow(existingCompanyQuotationRow);
            }
            return;
        }
        final Long companyId = company.id();
        final Row companyQuotationRow = Objects.isNull(sourceContainer) ? new Row(companyQuotationTable)
                : Optional.ofNullable(sourceContainer.getRow(companyQuotationTable,
                        new WhereClause(Criteria.eq(companyQuotationTable.getColumn(OMQNCOMPANYQUOTATION.QUOTATION_ID), quotationId))
                                .and(Criteria.eq(companyQuotationTable.getColumn(OMQNCOMPANYQUOTATION.COMPANY_ID), companyId))))
                .orElseGet(() -> new Row(companyQuotationTable));

        if (companyQuotationRow.isNewRow()) {
            companyQuotationRow.setOrgId(orgId);
            companyQuotationRow.set(OMQNCOMPANYQUOTATION.QUOTATION_ID, quotationId);
            companyQuotationRow.set(OMQNCOMPANYQUOTATION.COMPANY_ID, companyId);
            targetContainer.addNewRow(companyQuotationRow);
        }
    }

    /**
     * Converts a raw database {@link Row} from the master quotation template table
     * into a fully populated {@link QuotationTemplate} model, including organization-specific
     * metadata when available.
     *
     * @param templateRow   the raw SQL row from MQNQUOTATIONTEMPLATE table
     * @param dataContainer contextual data container containing joined org-specific data
     * @return the fully populated {@link QuotationTemplate} model
     */
    private QuotationTemplate convertRowToQuotationTemplate(final Row templateRow, final DataContainer dataContainer) {
        final QuotationTemplate template = new QuotationTemplate();
        final Long templateId = templateRow.getPKValue();
        final String name = templateRow.get(MQNQUOTATIONTEMPLATE.NAME);
        final String schemaJson = templateRow.get(MQNQUOTATIONTEMPLATE.SCHEMA_JSON);
        final Boolean isDefault = templateRow.get(MQNQUOTATIONTEMPLATE.IS_DEFAULT);

        template.setId(templateId);
        template.setName(name);
        
        if (Objects.nonNull(schemaJson)) {
            template.setSchemaJsonAsString(schemaJson);
        }
        
        if (Objects.nonNull(isDefault)) {
            template.setDefault(isDefault);
        }

        if (Objects.nonNull(dataContainer)) {
            final Table orgTemplateTable = getOrgQuotationTemplateTable();
            final Row orgTemplateRow = dataContainer.getRow(orgTemplateTable,
                    new WhereClause(Criteria.eq(orgTemplateTable.getColumn(OMQNQUOTATIONTEMPLATE.TEMPLATE_ID), templateId)));
            
            if (Objects.nonNull(orgTemplateRow)) {
                final Long orgId = orgTemplateRow.getOrgId();
                final Long createdById = orgTemplateRow.get(OMQNQUOTATIONTEMPLATE.CREATED_BY);
                final Long updatedById = orgTemplateRow.get(OMQNQUOTATIONTEMPLATE.UPDATED_BY);
                final DateTime createdTime = orgTemplateRow.get(OMQNQUOTATIONTEMPLATE.CREATED_TIME);
                final DateTime updatedTime = orgTemplateRow.get(OMQNQUOTATIONTEMPLATE.UPDATED_TIME);
                final Set<Long> userIds = new HashSet<>();

                template.setOrgId(orgId);
                template.setDefault(orgTemplateRow.get(OMQNQUOTATIONTEMPLATE.IS_DEFAULT));
                
                if (Objects.nonNull(createdTime)) {
                    template.setCreatedTime(createdTime);
                }
                
                if (Objects.nonNull(updatedTime)) {
                    template.setUpdatedTime(updatedTime);
                }

                if (Objects.nonNull(createdById)) {
                    userIds.add(createdById);
                }
                
                if (Objects.nonNull(updatedById)) {
                    userIds.add(updatedById);
                }

                if (!userIds.isEmpty() && Objects.nonNull(orgId)) {
                    final Map<Long, CrmUser> users = userService.getUsers(orgId, userIds)
                            .stream()
                            .collect(Collectors.toMap(CrmUser::id, Function.identity()));

                    if (Objects.nonNull(createdById)) {
                        template.setCreatedBy(users.get(createdById));
                    }

                    if (Objects.nonNull(updatedById)) {
                        template.setUpdatedBy(users.get(updatedById));
                    }
                }
            }
        }

        return template;
    }
}
