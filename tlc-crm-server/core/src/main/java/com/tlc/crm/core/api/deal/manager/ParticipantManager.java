package com.tlc.crm.core.api.deal.manager;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.ContactDealInfo;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.Participant;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.internal.deal.status.DealErrorCodes;
import com.tlc.crm.core.sql.resource.MCOCOMPANY;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MCONCOMPANYFIELDS;
import com.tlc.crm.core.sql.resource.MCONEMAILFIELDS;
import com.tlc.crm.core.sql.resource.MCONPHONEFIELDS;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.MDOPARTICIPANT;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.manager.field.type.EmailManager;
import com.tlc.crm.entity.manager.field.type.PhoneManager;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.sql.resource.MTLDEALTIMELINE;
import com.tlc.crm.entity.timeline.api.CommonTimelineManager;
import com.tlc.crm.entity.timeline.field.TimelineActionFields;
import com.tlc.crm.entity.timeline.model.FieldTimeline;
import com.tlc.crm.entity.timeline.model.Timeline;
import com.tlc.crm.entity.timeline.model.TimelineType;
import com.tlc.crm.fields.sql.resource.OMETEMAIL;
import com.tlc.crm.fields.sql.resource.OMETPHONE;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.i18n.I18nKey;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.With;
import com.tlc.sql.common.api.ds.OrgDataStore;

import com.tlc.sql.common.api.meta.DatabaseType;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages the {@link Participant} related actions
 *
 * @author Sundar
 * @version 1.0
 */
public class ParticipantManager extends AbstractConfigManager<Participant> {

    private final ContactManager contactManager;
    private final CompanyManager companyManager;
    private final PhoneManager phoneManager;
    private final EmailManager emailManager;
    private final FieldService fieldService;
    private final CommonTimelineManager commonTimelineManager;

    private static class Instance {
        private static final ParticipantManager INSTANCE = new ParticipantManager();
    }

    private ParticipantManager() {
        contactManager = ContactManager.getInstance();
        companyManager = CompanyManager.getInstance();
        phoneManager = PhoneManager.getInstance();
        emailManager = EmailManager.getInstance();
        fieldService = FieldService.getSecureInstance();
        commonTimelineManager = CommonTimelineManager.getInstance();
    }

    /**
     * Gets the {@link ParticipantManager} instance
     *
     * @return the {@link ParticipantManager} instance
     */
    public static ParticipantManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link Table} of participant
     */
    @Override
    public Table getTable() {
        return Table.get(MDOPARTICIPANT.TABLE);
    }

    /**
     * <p>
     * Fetches the {@link Participant} that has one of the id value mentioned in the parameter
     * </p>
     *
     * @param orgId Organization id
     * @param ids   Participants id
     * @return Collection of Participants
     */
    @Override
    public Collection<Participant> get(Long orgId, Collection<Long> ids) {
        final Table table = getTable();
        final Stream<Row> rows = orgDataStore(orgId).get(table, ids).getRows(table);

        if (Objects.isNull(rows)) {
            throw ErrorCode.get(DealErrorCodes.PARTICIPANT_NOT_FOUND, "i18n.participant.not.found");
        } else {
            return rows.map(this::convertRowToModel).toList();
        }
    }

    /**
     * <p>
     * Saves the attributes of {@link Participant} into the database
     * </p>
     *
     * @param participant An entity that holds the participant attributes which have to be saved into database
     */
    @Override
    public Participant create(Participant participant) {
        final DataContainer targetContainer = DataContainer.create();

        loadParticipantIntoContainer(true, true, participant, targetContainer);
        orgDataStore(participant.orgId()).commitChanges(targetContainer);

        return participant;
    }

    /**
     * <p>
     * Updates the given participants
     * </p>
     *
     * @param orgId  Organization id
     * @param models Collection of participants that has to be updated
     * @return Updated status
     */
    @Override
    public ProcessOutcome update(Long orgId, Collection<Participant> models) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Collection<Long> ids = models.stream().map(Participant::id).filter(Objects::nonNull).toList();
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final DataContainer entityContainer = orgDataStore.get(getTable(), ids);

        models.forEach(participant -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadParticipantIntoContainer(false, participant, false, entityContainer, targetContainer);
                orgDataStore.commitChanges(targetContainer);
                processOutcome.addProcessedRecord(participant.id());
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Participant update failed Org ID : {} ID : {} reason : {}", orgId, participant.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed"), ErrorKeys.UPDATE_ACTION_FAILED, participant.id().toString()));
            }
        });

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed")), failedEntities));
        }

        return processOutcome;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents an organization id
     * @param participants Which contains collection of {@link Participant}
     */
    @Override
    public void delete(final Long orgId, final Collection<Participant> participants) {
        final DataContainer sourceContainer = orgDataStore(orgId).get(getTable(), participants.stream()
                .map(Participant::id).filter(Objects::nonNull).collect(Collectors.toSet()));

        if (!sourceContainer.isEmpty()) {
            final DataContainer targetContainer = DataContainer.create();
            final Table table = getTable();

            sourceContainer.getRows(table).forEach(row -> {
                targetContainer.deleteRow(row);

                loadRelatedContactsIntoTimeline(orgId, row.get(MDOPARTICIPANT.CONTACT_ID), row.get(MDOPARTICIPANT.DEAL_ID),
                        TimelineType.DEAL_RELATED_CONTACTS_REMOVED, TimelineActionFields.RELATED_CONTACTS_REMOVED, targetContainer);
            });
            orgDataStore(orgId).commitChanges(targetContainer);
        }
    }

    /**
     * <p>
     * Loads the participant details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param isTimelineRequired A boolean flag indicating whether the timeline is required or not
     * @param participant     An entity that holds the participant attributes
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    public void loadParticipantIntoContainer(final boolean isCreateEvent, final boolean isTimelineRequired, final Participant participant, final DataContainer targetContainer) {
        final DataContainer entityContainer = isCreateEvent ? null :
                orgDataStore(participant.orgId()).get(getTable(), List.of(participant.id()));

        loadParticipantIntoContainer(isCreateEvent, participant, isTimelineRequired, entityContainer, targetContainer);
    }

    /**
     * <p>
     * Loads the participant details into the target container
     * </p>
     *
     * @param isCreateEvent      A flag which indicates whether the action is related to create or update event
     * @param participant        An entity that holds the participant attributes
     * @param isTimelineRequired A boolean flag indicating whether the timeline is required or not
     * @param sourceContainer    Holds the user defined and system field values of existing participants
     * @param targetContainer    Holds the updated information of user defined and system field values
     */
    public void loadParticipantIntoContainer(final boolean isCreateEvent, final Participant participant, final boolean isTimelineRequired,
                                             final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table participantTable = getTable();
        final Long id = participant.id();
        final Row participantRow = isCreateEvent ? new Row(participantTable)
                : sourceContainer.getRow(participantTable, id);
        final Long dealId = participant.getDealId();
        final Long contactId = participant.getContactId();
        final Long orgId = participant.orgId();

        if (!isCreateEvent && Objects.isNull(participantRow)) {
            throw ErrorCode.get(DealErrorCodes.PARTICIPANT_NOT_FOUND, "i18n.participant.not.found");
        }

        if (participantRow.isNewRow()) {
            participantRow.setOrgId(orgId);
            participant.setId(participantRow.getPKValue());
        }

        participantRow.set(MDOPARTICIPANT.DEAL_ID, dealId);
        participantRow.set(MDOPARTICIPANT.CONTACT_ID, contactId);

        if (isTimelineRequired) {
            loadRelatedContactsIntoTimeline(orgId, contactId, dealId, TimelineType.DEAL_RELATED_CONTACTS_ADDED,
                    TimelineActionFields.RELATED_CONTACTS_ADDED, targetContainer);
        }
        targetContainer.updateRow(participantRow);
    }

    /**
     * <p>
     * Converts row into {@link Participant}
     * </p>
     *
     * @param row Participant row to be converted
     * @return {@link Participant}
     */
    private Participant convertRowToModel(final Row row) {
        final Participant participant = new Participant();

        participant.setId(row.getPKValue());
        participant.setContactId(row.get(MDOPARTICIPANT.CONTACT_ID));
        participant.setDealId(row.get(MDOPARTICIPANT.DEAL_ID));
        participant.setOrgId(row.getOrgId());

        return participant;
    }

    /**
     * <p>
     * Gets the collection of participants
     * </p>
     *
     * @param orgId  Represents the organisation id
     * @param dealId Represents the deal id
     * @param start  Start value represents the lower bound of data to be fetched
     * @param limit  Limit value represents the upper bound of data to be fetched
     * @return the {@link Collection} of Participants
     */
    public Collection<Participant> getParticipantsByDeal(final Long orgId, final Long dealId, final int start, final int limit) {
        final ContactManager contactManager = ContactManager.getInstance();
        final Table participantTable = getTable();
        final Table dealTable = DealManager.getInstance().getTable();
        final Table contactTable = contactManager.getTable();
        final Column contactPKColumn = contactTable.getPKColumn();
        final Column participantPKColumn = participantTable.getPKColumn();
        final Column participantDealIdColumn = participantTable.getColumn(MDOPARTICIPANT.DEAL_ID);
        final Table emailTable = emailManager.getTable();
        final Table phoneTable = phoneManager.getTable();
        final Table contactEmailTable = contactManager.getContactEmailTable();
        final Table contactPhoneTable = contactManager.getContactPhoneTable();
        final Table contactCompanyTable = contactManager.getContactCompanyTable();
        final Table companyTable = companyManager.getTable();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final Column participantContactIdColumn = participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID);
        final Column dealPkColumn = dealTable.getPKColumn();
        final Column companyPkColumn = companyTable.getPKColumn();
        final SelectQuery primaryContactSelectQuery = SelectQuery.get(dealTable);
        final Column contactEmailEntityIdColumn = contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID);
        final Column contactPhoneEntityIdColumn = contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID);
        final Column contactCompanyEntityIdColumn = contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID);
        final String cteAlias = "filtered_participants";

        primaryContactSelectQuery.addSelectClause(dealTable.getColumn(MDODEAL.CONTACT_ID));
        primaryContactSelectQuery.setWhereClause(new WhereClause(Criteria.eq(dealPkColumn, dealId))
                .and(Criteria.eq(dealTable.getColumn(MDODEAL.ORG_ID), orgId)));

        final SelectQuery filteredParticipantsQuery = SelectQuery.get(participantTable);

        filteredParticipantsQuery.addSelectClause(List.of(participantPKColumn, participantContactIdColumn, participantDealIdColumn));
        filteredParticipantsQuery.addJoinClause(List.of(new JoinClause(participantContactIdColumn, contactPKColumn)));
        filteredParticipantsQuery.setWhereClause(new WhereClause(Criteria.eq(participantDealIdColumn, dealId))
                .and(Criteria.notEq(participantContactIdColumn, primaryContactSelectQuery)));
        filteredParticipantsQuery.setLimitClause(new LimitClause(start, limit));
        filteredParticipantsQuery.addOrderByClause(new OrderByClause(participantPKColumn));

        final DerivedTable filteredParticipantsCte = DerivedTable.get(DatabaseType.OLTP, participantTable.getName(), cteAlias, filteredParticipantsQuery);

        filteredParticipantsCte.setWithClauseCTE(true);

        final SelectQuery participantSelectQuery = SelectQuery.get(filteredParticipantsCte);
        final Column cteContactIdColumn = filteredParticipantsCte.getColumn(MDOPARTICIPANT.CONTACT_ID, MDOPARTICIPANT.CONTACT_ID);

        participantSelectQuery.addJoinClause(List.of(new JoinClause(cteContactIdColumn, contactEmailEntityIdColumn, leftJoin),
                new JoinClause(cteContactIdColumn, contactCompanyEntityIdColumn, leftJoin),
                new JoinClause(cteContactIdColumn, contactPhoneEntityIdColumn, leftJoin)));

        final With.Builder withBuilder = With.Builder.create();

        withBuilder.addDerivedTable(filteredParticipantsCte);
        withBuilder.selectQuery(participantSelectQuery);

        final DataContainer participantDataContainer = orgDataStore(orgId).get(withBuilder.build());

        if (!participantDataContainer.isEmpty() && participantDataContainer.size(filteredParticipantsCte) > 0) {
            final Collection<Participant> participants = new ArrayList<>();
            final SelectQuery innerSelectQuery = SelectQuery.get(contactTable);
            final SelectQuery contactSelectQuery = SelectQuery.get(contactTable);

            innerSelectQuery.addSelectClause(contactPKColumn);
            innerSelectQuery.setWhereClause(new WhereClause(Criteria.in(contactPKColumn,
                    participantDataContainer.getRows(filteredParticipantsCte).map(row -> row.get(MDOPARTICIPANT.CONTACT_ID)).collect(Collectors.toUnmodifiableSet()))));

            if (participantDataContainer.size(contactEmailTable) > 0) {
                contactSelectQuery.addSelectClause(contactEmailTable);
                contactSelectQuery.addSelectClause(List.of(emailTable.getColumn(OMETEMAIL.VALUE),
                        emailTable.getColumn(OMETEMAIL.TYPE)));
                contactSelectQuery.addJoinClause(List.of(new JoinClause(contactPKColumn, contactEmailEntityIdColumn, leftJoin),
                        new JoinClause(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), emailTable.getPKColumn(), leftJoin)));
            }

            if (participantDataContainer.size(contactPhoneTable) > 0) {
                contactSelectQuery.addSelectClause(contactPhoneTable);
                contactSelectQuery.addSelectClause(List.of(phoneTable.getColumn(OMETPHONE.NUMBER),
                        phoneTable.getColumn(OMETPHONE.TYPE)));
                contactSelectQuery.addJoinClause(List.of(new JoinClause(contactPKColumn, contactPhoneEntityIdColumn, leftJoin),
                        new JoinClause(contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID), phoneTable.getPKColumn(), leftJoin)));
            }

            if (participantDataContainer.size(contactCompanyTable) > 0) {
                contactSelectQuery.addSelectClause(contactCompanyTable);
                contactSelectQuery.addSelectClause(List.of(companyTable.getColumn(MCOCOMPANY.NAME),
                        companyTable.getColumn(MCOCOMPANY.ORG_ID), companyPkColumn));
                contactSelectQuery.addJoinClause(List.of(new JoinClause(contactPKColumn, contactCompanyEntityIdColumn, leftJoin),
                        new JoinClause(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.COMPANY_ID), companyPkColumn, leftJoin)));
            }
            contactSelectQuery.addSelectClause(List.of(contactPKColumn, contactTable.getColumn(MCOCONTACT.OWNER_ID),
                    contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME),
                    contactTable.getColumn(MCOCONTACT.ORG_ID), contactTable.getColumn(MCOCONTACT.FACEBOOK),
                    contactTable.getColumn(MCOCONTACT.LINKEDIN), contactTable.getColumn(MCOCONTACT.TWITTER),
                    contactTable.getColumn(MCOCONTACT.DESIGNATION)));
            contactSelectQuery.setWhereClause(new WhereClause(Criteria.in(contactPKColumn, innerSelectQuery)));
            final DataContainer dataContainer = orgDataStore(orgId).get(contactSelectQuery);

            dataContainer.append(participantDataContainer);
            final Stream<Row> participantRows = dataContainer.getRows(filteredParticipantsCte);

            participantRows.filter(Objects::nonNull).forEach(participantRow ->
                    participants.add(convertRowToModel(participantRow, dataContainer)));

            return participants;
        }
        return Collections.emptyList();
    }

    /**
     * Checks whether the participant is already mapped or not
     *
     * @param orgId            represents the organisation ID
     * @param dealParticipants which contains the participants grouped by deal Ids
     * @return true if the participant is already exists
     */
    public boolean isAlreadyMapped(final Long orgId, final Map<Long, List<Participant>> dealParticipants) {
        if (Objects.nonNull(dealParticipants) && !dealParticipants.isEmpty()) {
            final Table participantTable = getTable();
            final CountQuery countQuery = CountQuery.get(participantTable);
            final Column dealColumn = participantTable.getColumn(MDOPARTICIPANT.DEAL_ID);
            final Column contactColumn = participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID);
            final AtomicReference<WhereClause> whereClause = new AtomicReference<>();

            dealParticipants.forEach((dealId, participants) -> {
                final WhereClause condition = whereClause.get();
                final WhereClause dealCriteria = new WhereClause(Criteria.eq(dealColumn, dealId))
                        .and(Criteria.in(contactColumn, participants.stream().map(Participant::getContactId)
                                .collect(Collectors.toSet())));

                whereClause.set(Objects.nonNull(condition) ? condition.or(dealCriteria) : dealCriteria);
            });
            countQuery.setWhereClause(whereClause.get());

            return orgDataStore(orgId).get(countQuery) > 0;
        }
        return false;
    }

    /**
     * <p>
     * Converts row into {@link Participant}
     * </p>
     *
     * @param row           participants related row to be converted
     * @param dataContainer Holds the user defined and system field values of existing participants
     * @return {@link Participant}
     */
    private Participant convertRowToModel(final Row row, final DataContainer dataContainer) {
        final ContactManager contactManager = ContactManager.getInstance();
        final Participant participant = new Participant();
        final Contact contact = new Contact();
        final Table contactCompanyTable = contactManager.getContactCompanyTable();
        final Table contactEmailTable = contactManager.getContactEmailTable();
        final Table contactPhoneTable = contactManager.getContactPhoneTable();
        final Long contactId = row.get(MDOPARTICIPANT.CONTACT_ID);
        final Table contactTable = contactManager.getTable();
        final Table phoneTable = phoneManager.getTable();
        final Table emailTable = emailManager.getTable();
        final Row contactCompanyRow = dataContainer.getRow(contactCompanyTable, new WhereClause(Criteria
                .eq(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID), contactId)));
        final Stream<Row> contactEmailRows = dataContainer.getRows(contactEmailTable,
                new WhereClause(Criteria.eq(contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID), contactId)));
        final Stream<Row> contactPhoneRows = dataContainer.getRows(contactPhoneTable,
                new WhereClause(Criteria.eq(contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID), contactId)));
        final Stream<Row> emailRows = dataContainer.getRows(emailTable,
                new WhereClause(Criteria.in(emailTable.getPKColumn(), contactEmailRows.map(contactEmailRow ->
                        contactEmailRow.get(MCONEMAILFIELDS.EMAIL_ID)).collect(Collectors.toSet()))));
        final Stream<Row> phoneRows = dataContainer.getRows(phoneTable,
                new WhereClause(Criteria.in(phoneTable.getPKColumn(), contactPhoneRows.map(contactPhoneRow ->
                        contactPhoneRow.get(MCONPHONEFIELDS.PHONE_ID)).collect(Collectors.toSet()))));
        final Row contactRow = dataContainer.getRow(contactTable,
                new WhereClause(Criteria.eq(contactTable.getPKColumn(), contactId)));

        if (Objects.nonNull(contactRow)) {
            final Long orgId = contactRow.getOrgId();

            contact.setId(contactId);
            contact.setOrgId(orgId);
            contact.setOwnerId(contactRow.get(MCOCONTACT.OWNER_ID));
            contact.setFirstName(contactRow.get(MCOCONTACT.FIRST_NAME));
            contact.setLastName(contactRow.get(MCOCONTACT.LAST_NAME));
            contact.setFacebook(contactRow.get(MCOCONTACT.FACEBOOK));
            contact.setLinkedIn(contactRow.get(MCOCONTACT.LINKEDIN));
            contact.setTwitter(contactRow.get(MCOCONTACT.TWITTER));
            contact.setDesignation(contactRow.get(MCOCONTACT.DESIGNATION));
            contact.setEmails(emailRows.filter(Objects::nonNull).map(emailManager::convertRowToModel).toList());
            contact.setPhones(phoneRows.filter(Objects::nonNull).map(phoneManager::convertRowToModel).toList());
        }

        if (Objects.nonNull(contactCompanyRow)) {
            final Company company = new Company();
            final Long companyId = contactCompanyRow.get(MCONCOMPANYFIELDS.COMPANY_ID);
            final Row companyRow = dataContainer.getRow(companyManager.getTable(), companyId);

            if (Objects.nonNull(companyRow)) {
                company.setId(companyId);
                company.setName(companyRow.get(MCOCOMPANY.NAME));
                contact.setCompany(company);
            }
        }
        participant.setId(row.getPKValue());
        participant.setContact(contact);

        return participant;
    }

    /**
     * <p>
     * Retrieves the number of participants mapped given deal id
     * </p>
     *
     * @param orgId  Represents the Organization id
     * @param dealId Represents the deal id
     * @return the {@link Long} of participants mapped with deal count
     */
    public Long getDealParticipantCount(final Long orgId, final Long dealId) {
        final Table participantTable = getTable();
        final Table dealTable = DealManager.getInstance().getTable();
        final Column dealPkColumn = dealTable.getPKColumn();
        final Column participantDealIdColumn = participantTable.getColumn(MDOPARTICIPANT.DEAL_ID);
        final CountQuery countQuery = CountQuery.get(participantTable);
        final SelectQuery primaryContactSelectQuery = SelectQuery.get(dealTable);

        primaryContactSelectQuery.addSelectClause(dealTable.getColumn(MDODEAL.CONTACT_ID));
        primaryContactSelectQuery.setWhereClause(new WhereClause(Criteria.eq(dealPkColumn, dealId)));

        countQuery.addJoinClause(new JoinClause(participantDealIdColumn, dealPkColumn));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(participantDealIdColumn, dealId))
                .and(Criteria.notEq(participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID), primaryContactSelectQuery)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId organisation ID
     * @param ids   which contains the collection of participant IDs
     * @return the {@link Participant}
     */
    @Override
    public Collection<Participant> partialGet(Long orgId, Collection<Long> ids) {
        return null;
    }

    /**
     * <p>
     * Updates the {@link Collection} of related contacts into deal
     * </p>
     *
     * @param orgId                      Represents an organization id
     * @param dealId                     Represents the deal id
     * @param contactIds                 Represents the {@link Collection} of contact ids
     * @param removeExistingParticipants Represents flag to determine whether to remove existing participants or not
     */
    public void updateParticipants(final long orgId, final Long dealId, final Collection<Long> contactIds, final boolean removeExistingParticipants) {
        final Table participantTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(participantTable);
        final DataContainer targetContainer = DataContainer.create();
        final DealManager dealManager = DealManager.getInstance();
        final Column contactIdColumn = dealManager.getTable().getColumn(MDODEAL.CONTACT_ID);
        final Column participantContactIdColumn = participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID);

        selectQuery.addJoinClause(new JoinClause(participantContactIdColumn, contactIdColumn, JoinClause.JoinType.LEFT));
        selectQuery.addSelectClause(participantContactIdColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(participantTable.getColumn(MDOPARTICIPANT.DEAL_ID), dealId))
                .and(Criteria.notIn(participantContactIdColumn, dealManager.getPrimaryContactSelectQuery(dealId))));

        orgDataStore(orgId).get(selectQuery).getRows(participantTable).forEach(row -> {
            final Long contactId = row.get(MDOPARTICIPANT.CONTACT_ID);

            if (!contactIds.remove(contactId) && removeExistingParticipants) {
                targetContainer.deleteRow(row);
            }
        });

        for (final Long contactId : contactIds) {
            final Row participantRow = new Row(participantTable);

            participantRow.set(MDOPARTICIPANT.CONTACT_ID, contactId);
            participantRow.set(MDOPARTICIPANT.DEAL_ID, dealId);
            participantRow.setOrgId(orgId);
            targetContainer.addNewRow(participantRow);
        }

        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Gets the {@link Collection} {@link Contact} with deals mapped for the given organization id
     * </p>
     *
     * @param orgId            Represents the organization id
     * @param start            Represents the starting index of contacts to be fetched
     * @param limit            Represents the maximum number of contacts to be fetched
     * @param dropdownCriteria Represents the {@link DropdownCriteria}
     * @return {@link Collection} of {@link ContactDealInfo}
     */
    public Collection<ContactDealInfo> getContactDealInfoList(final long orgId, final long dealId, final int start, final int limit, final DropdownCriteria dropdownCriteria) {
        final Table contactTable = contactManager.getTable();
        final Column firstNameColumn = contactTable.getColumn(MCOCONTACT.FIRST_NAME);
        final Column lastNameColumn = contactTable.getColumn(MCOCONTACT.LAST_NAME);
        final Table participantTable = getTable();
        final Collection<ContactDealInfo> contactDealInfoList = new ArrayList<>();
        final Long dealParticipantCount = getDealParticipantCount(orgId, dealId);
        final Column participantContactIdColumn = participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID);

        if (dealParticipantCount >= start) {
            final DataContainer participantsContainer = getParticipantsContainer(orgId, dealId, start, limit, dropdownCriteria);

            participantsContainer.getRows(contactTable).forEach(row -> {
                final Stream<Row> participantsRows = participantsContainer.getRows(participantTable, new WhereClause(Criteria.eq(participantContactIdColumn, row.getPKValue())));
                final ContactDealInfo contactDealInfo = new ContactDealInfo();

                contactDealInfo.setContact(contactManager.convertRowToPartialModel(row));
                contactDealInfo.setDealIds(participantsRows.map(participantRow -> (Long) participantRow.get(MDOPARTICIPANT.DEAL_ID)).toList());
                contactDealInfoList.add(contactDealInfo);
            });
        }

        final int contactDealInfoSize = contactDealInfoList.size();
        final boolean hasContactDealInfo = contactDealInfoSize > 0;
        final int nonParticipantContactsLimit = hasContactDealInfo ? limit - contactDealInfoSize : limit;
        final int nonParticipantContactsStart = hasContactDealInfo ? contactDealInfoSize + 1 : start;

        if (nonParticipantContactsStart > 0 && nonParticipantContactsLimit > 0) {
            final SelectQuery selectQuery = SelectQuery.get(contactTable);
            final Column contactPkColumn = contactTable.getPKColumn();
            final SelectQuery participantSelectQuery = SelectQuery.get(participantTable);

            participantSelectQuery.addSelectClause(participantContactIdColumn);
            participantSelectQuery.setWhereClause(new WhereClause(Criteria.eq(participantTable.getColumn(MDOPARTICIPANT.DEAL_ID), dealId)));
            participantSelectQuery.setLimitClause(new LimitClause(nonParticipantContactsStart, nonParticipantContactsLimit));

            selectQuery.addSelectClause(List.of(firstNameColumn, lastNameColumn, contactTable.getColumn(MCOCONTACT.ORG_ID)));
            selectQuery.setWhereClause(new WhereClause(dropdownCriteria.build(firstNameColumn)).or(dropdownCriteria.build(lastNameColumn)).and(Criteria.notIn(contactPkColumn, participantSelectQuery)));
            selectQuery.setLimitClause(new LimitClause(nonParticipantContactsStart, nonParticipantContactsLimit));
            selectQuery.addOrderByClause(new OrderByClause(contactTable.getColumn(MCOCONTACT.CREATED_TIME)));
            final Stream<Row> contactRows = orgDataStore(orgId).get(selectQuery).getRows(contactTable);

            contactRows.forEach(row -> {
                final ContactDealInfo contactDealInfo = new ContactDealInfo();

                contactDealInfo.setContact(contactManager.convertRowToPartialModel(row));
                contactDealInfoList.add(contactDealInfo);
            });
        }

        return contactDealInfoList;
    }

    /**
     * <p>
     * Gets the {@link DataContainer} for the participants mapped for the given deal id
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param dealId           Represents the deal id
     * @param start            Represents the starting index of the contacts to be fetched
     * @param limit            Represents the maximum number of the contacts to be fetched
     * @param dropdownCriteria Represents the {@link DropdownCriteria}
     * @return the {@link DataContainer}
     */
    private DataContainer getParticipantsContainer(final long orgId, final long dealId, final int start, final int limit, final DropdownCriteria dropdownCriteria) {
        final Table contactTable = contactManager.getTable();
        final Table participantTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(participantTable);
        final Column firstNameColumn = contactTable.getColumn(MCOCONTACT.FIRST_NAME);
        final Column lastNameColumn = contactTable.getColumn(MCOCONTACT.LAST_NAME);
        final Column participantContactIdColumn = participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID);
        final Column contactPkColumn = contactTable.getPKColumn();
        final WhereClause whereClause = new WhereClause(Criteria.eq(participantTable.getColumn(MDOPARTICIPANT.DEAL_ID), dealId))
                .and(Criteria.notIn(contactPkColumn, DealManager.getInstance().getPrimaryContactSelectQuery(dealId)));

        selectQuery.addSelectClause(List.of(firstNameColumn, lastNameColumn, participantContactIdColumn,
                participantTable.getColumn(MDOPARTICIPANT.DEAL_ID), contactTable.getColumn(MCOCONTACT.ORG_ID)));
        selectQuery.addJoinClause(new JoinClause(participantContactIdColumn, contactPkColumn));
        selectQuery.setWhereClause(new WhereClause(dropdownCriteria.build(firstNameColumn)).or(dropdownCriteria.build(lastNameColumn)).and(whereClause));
        selectQuery.setLimitClause(new LimitClause(start, limit));
        selectQuery.addOrderByClause(new OrderByClause(contactTable.getColumn(MCOCONTACT.CREATED_TIME)));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Retrieves the total count of non primary contact for the deal based on the dropdown criteria
     * </p>
     *
     * @param orgId            Represents the organization ID for which the contacts are counted
     * @param dealId           Represents the deal id
     * @param dropdownCriteria The criteria to filter the contacts (e.g., based on first name, last name).
     * @return The count of entities
     */
    public long getNonPrimaryContactsCount(final long orgId, final long dealId, final DropdownCriteria dropdownCriteria) {
        final Table contactTable = contactManager.getTable();
        final CountQuery countQuery = CountQuery.get(contactTable);

        countQuery.setWhereClause(new WhereClause(dropdownCriteria.build(contactTable.getColumn(MCOCONTACT.FIRST_NAME)))
                .or(dropdownCriteria.build(contactTable.getColumn(MCOCONTACT.LAST_NAME))).
                and(Criteria.notEq(contactTable.getPKColumn(), DealManager.getInstance().getPrimaryContactSelectQuery(dealId))));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Loads the addition and removal of related contact into the timeline
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param contactId       Represents the contact id
     * @param dealId          Represents the deal id
     * @param timelineType    Represents the {@link TimelineType}
     * @param action          Action associated with the timeline update
     * @param targetContainer Holds the updated information participant details
     */
    private void loadRelatedContactsIntoTimeline(final long orgId, final Long contactId, final Long dealId, final TimelineType timelineType,
                                                 final String action, final DataContainer targetContainer) {
        final String newValue = Json.object().put(action, Json.array(List.of(contactManager.getContactFullName(orgId, contactId)))).toString();
        final I18nKey i18nKey = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE).get(DealFields.RELATED_CONTACTS).i18nName();
        final Timeline timeline = new Timeline();

        timeline.setAuditedTime(new DateTime(Instant.now().toEpochMilli()));
        timeline.setOrgId(orgId);
        timeline.setCategory(Timeline.Category.DEAL);
        timeline.setOwnerId(LoggedInUserInfo.getUser().id());
        timeline.setTimelineType(timelineType);
        timeline.setAction(action);
        timeline.setFieldTimeline(new FieldTimeline(newValue, i18nKey));

        commonTimelineManager.loadTimelineRowIntoContainer(timeline, new Row(commonTimelineManager.getTable()), targetContainer);
        commonTimelineManager.loadSourceTimelineRowIntoContainer(timeline.id(), dealId, MTLDEALTIMELINE.DEAL_ID, MTLDEALTIMELINE.TIMELINE_ID,
                new Row(commonTimelineManager.getDealTimelineTable()), targetContainer);
    }

    /**
     * <p>
     * Gets the deal participant ids
     * </p>
     *
     * @param orgId      Represents the organisation id
     * @param dealId     Represents the deal id
     * @param contactIds Represents the contact ids
     * @return The collection of participant ids
     */
    public Collection<Long> getParticipantIds(final Long orgId, final Long dealId, final Collection<Long> contactIds) {
        final Table participantTable = getTable();
        final WhereClause whereClause = new WhereClause(Criteria.in(participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID), contactIds))
                .and(Criteria.eq(participantTable.getColumn(MDOPARTICIPANT.DEAL_ID), dealId));
        final DataContainer dataContainer = orgDataStore(orgId).get(participantTable, whereClause, List.of(participantTable.getPKColumn()));

        return dataContainer.getRows(participantTable).filter(Objects::nonNull).map(Row::getPKValue).collect(Collectors.toSet());
    }
}
