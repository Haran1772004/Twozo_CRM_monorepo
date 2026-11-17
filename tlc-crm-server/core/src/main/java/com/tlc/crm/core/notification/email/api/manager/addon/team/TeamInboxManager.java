package com.tlc.crm.core.notification.email.api.manager.addon.team;

import com.tlc.commons.code.ErrorCode;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.api.manager.TeamManager;
import com.tlc.crm.common.iam.api.manager.TerritoryManager;
import com.tlc.crm.common.iam.api.manager.impl.CrmUserManagerImpl;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.sql.resource.MCRMUSERTEAM;
import com.tlc.crm.common.sql.resource.MCRMUSERTERRITORY;
import com.tlc.crm.common.sql.resource.OMCRMTEAM;
import com.tlc.crm.common.sql.resource.OMCRMTERRITORY;

import com.tlc.crm.core.notification.email.api.manager.CoreMailManager;
import com.tlc.crm.core.notification.email.api.manager.CrmEmailDomainManager;
import com.tlc.crm.core.notification.email.api.manager.impl.CrmEmailDomainManagerImpl;
import com.tlc.crm.core.notification.email.api.manager.addon.AbstractEmailAddOnManager;
import com.tlc.crm.core.notification.email.api.model.email.EmailVariant;
import com.tlc.crm.core.sql.resource.OMEMTEAMINBOXTHREADS;
import com.tlc.crm.email.notification.email.api.manager.MailManager;
import com.tlc.crm.email.notification.email.api.model.address.FromAddressInfo;
import com.tlc.crm.email.notification.email.status.EmailErrorCodes;
import com.tlc.crm.core.notification.email.inbox.api.model.TeamInbox;
import com.tlc.crm.core.notification.email.inbox.status.TeamInboxErrorCodes;
import com.tlc.crm.email.notification.sql.resource.MEMParentAutoForwardingAddress;
import com.tlc.crm.email.notification.sql.resource.OMEMAutoForwardingAddress;
import com.tlc.crm.email.notification.sql.resource.OMEMCONVERSATIONS;
import com.tlc.crm.email.notification.sql.resource.OMEMFROMADDRESS;
import com.tlc.crm.email.notification.sql.resource.OMEMTEAMINBOXUSER;
import com.tlc.crm.email.notification.sql.resource.OMEMADDRESS;
import com.tlc.crm.email.notification.sql.resource.OMEMTEAMINBOX;
import com.tlc.crm.email.notification.sql.resource.OMEMTEAMINBOXTERRITORY;
import com.tlc.crm.email.notification.sql.resource.OMEMTEAMINBOXTEAM;

import com.tlc.email.common.model.address.EmailAddress;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tlc.crm.core.notification.email.inbox.api.model.TeamInbox.State.ACTIVE;

/**
 * <p>
 * Implements the {@link TeamInbox} related DB actions
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public class TeamInboxManager extends AbstractEmailAddOnManager<TeamInbox> {

    private final MailManager mailManager;
    private final TeamManager teamManager;
    private final TerritoryManager territoryManager;
    private final CrmUserManagerImpl crmUserManager;
    private final UserService userService;
    private final CrmEmailDomainManager crmDomainManager;
    private final CoreMailManager coreMailManager;

    private static class Instance {
        private static final TeamInboxManager INSTANCE = new TeamInboxManager();
    }

    private TeamInboxManager() {
        mailManager = MailManager.getInstance();
        teamManager = TeamManager.getInstance();
        territoryManager = TerritoryManager.getInstance();
        crmUserManager = CrmUserManagerImpl.getInstance();
        userService = UserService.getInstance();
        crmDomainManager = CrmEmailDomainManagerImpl.getInstance();
        this.coreMailManager = CoreMailManager.getInstance();
    }

    /**
     * Gets the {@link AdminDataStore}
     *
     * @return the {@link AdminDataStore}
     */
    protected AdminDataStore adminDataStore() {
        return SqlServiceProvider.get().getAdminDataStore();
    }

    /**
     * <p>
     * Gets the {@link TeamInboxManager} Instance
     * </p>
     *
     * @return {@link TeamInboxManager}
     */
    public static TeamInboxManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the Team inbox table
     * </p>
     *
     * @return {@link Table} of team inbox
     */
    @Override
    public Table getTable() {
        return Table.get(OMEMTEAMINBOX.TABLE);
    }

    /**
     * <p>
     * Gets the table representing team inbox threads in the datastore.
     * </p>
     *
     * @return {@link Table} corresponding to the team inbox thread table
     */
    public Table getTeamInboxThreadTable() {
        return Table.get(OMEMTEAMINBOXTHREADS.TABLE);
    }

    /**
     * <p>
     * Gets the email address table
     * </p>
     *
     * @return {@link Table} of email address
     */
    public Table getEmailAddressTable() {
        return Table.get(OMEMADDRESS.TABLE);
    }

    /**
     * <p>
     * Gets the team inbox users table
     * </p>
     *
     * @return {@link Table} of team inbox users
     */
    private Table getTeamInboxUsersTable() {
        return Table.get(OMEMTEAMINBOXUSER.TABLE);
    }

    /**
     * <p>
     * Gets the team inbox territory table
     * </p>
     *
     * @return {@link Table} of team inbox territory
     */
    private Table getTeamInboxTerritoryTable() {
        return Table.get(OMEMTEAMINBOXTERRITORY.TABLE);
    }

    /**
     * <p>
     * Gets the team inbox team table
     * </p>
     *
     * @return {@link Table} of team inbox team
     */
    private Table getTeamInboxTeamTable() {
        return Table.get(OMEMTEAMINBOXTEAM.TABLE);
    }

    /**
     * <p>
     * Gets the table representing parent addresses associated with a team inbox.
     * </p>
     *
     * @return {@link Table} corresponding to the parent team inbox address table
     */
    @Override
    protected Table getParentAddressTable() {
        return Table.get(MEMParentAutoForwardingAddress.TABLE);
    }

    /**
     * <p>
     * Gets the table representing addresses of a team inbox.
     * </p>
     *
     * @return {@link Table} corresponding to the team inbox address table
     */
    protected Table getAutoForwardingAddressTable() {
        return Table.get(OMEMAutoForwardingAddress.TABLE);
    }

    /**
     * <p>
     * Loads a new row representing a team inbox address into the specified data container
     * and commits it to the datastore.
     * </p>
     *
     * @param orgId         Represents the organization id
     * @param emailId       Represents the email id to be added to the team inbox
     * @param parentEmailId Represents the parent inbox id associated with the email
     * @param dataContainer The container where the new row will be added and committed
     */
    @Override
    protected void loadRowIntoContainer(final Long orgId, final Long emailId,
                                        final Long parentEmailId, final DataContainer dataContainer) {
        final Row row = new Row(getAutoForwardingAddressTable());

        row.set(OMEMAutoForwardingAddress.PARENT_INBOX_ID, parentEmailId);
        row.set(OMEMAutoForwardingAddress.EMAIL_ID, emailId);
        row.setOrgId(orgId);

        dataContainer.addNewRow(row);

        orgDataStore(orgId).commitChanges(dataContainer);
    }

    /**
     * <p>
     * Creation of team inbox for an organization
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param teamInboxes Collection of team inbox
     * @return {@link Map} of response of create action
     */
    @Override
    public ProcessOutcome create(final Long orgId, final Collection<TeamInbox> teamInboxes) {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntries = new ArrayList<>();

        teamInboxes.forEach(teamInbox -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadTeamInboxIntoContainer(orgId, teamInbox, targetContainer);
                orgDataStore(orgId).commitChanges(targetContainer);
                processOutcome.addProcessedRecord(teamInbox.id());
            } catch (ErrorCode errorCode) {
                LOGGER.error("Create action: Team inbox create failed Org ID : {} reason : {}", orgId, errorCode.getMessage(), errorCode);
                failedEntries.add(new ErrorDescriptor(ErrorCode.get(TeamInboxErrorCodes.CREATE_ACTION_FAILED,
                        "i18n.common.create.failed")));
            }
        });

        if (!failedEntries.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(TeamInboxErrorCodes.CREATE_ACTION_FAILED,
                    "i18n.common.create.failed")), failedEntries));
        }

        return processOutcome;
    }

    /**
     * <p>
     * Retrieves a {@link TeamInbox} for the specified organization and ID.
     * </p>
     *
     * @param orgId Represents the organization ID
     * @param id    Represents the Team Inbox ID
     * @return the {@link TeamInbox} corresponding to the provided ID
     */
    @Override
    public TeamInbox get(final Long orgId, final Long id) {
        final DataContainer dataContainer = getTeamInboxContainer(orgId, List.of(id), false);

        return convertRowToModel(dataContainer.getRow(getTable()), dataContainer);
    }

    /**
     * <p>
     * Update action for team inbox for an organization provided
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param teamInboxes Contains the collection of {@link TeamInbox} entity
     * @return The {@link ProcessOutcome}
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<TeamInbox> teamInboxes) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntries = new ArrayList<>();
        final Collection<Long> ids = teamInboxes.stream().map(TeamInbox::id).filter(Objects::nonNull).toList();
        final DataContainer entityContainer = getTeamInboxContainer(orgId, ids, true);

        teamInboxes.forEach(teamInbox -> {
            try {
                final DataContainer targetContainer = DataContainer.create();
                final Long id = teamInbox.id();

                loadTeamInboxIntoContainer(false, teamInbox, entityContainer, targetContainer);
                orgDataStore.commitChanges(targetContainer);
                processOutcome.addProcessedRecord(id);
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Team inbox update failed Org ID : {} ID : {} reason : {}", orgId, teamInbox.id(), errorCode.getMessage(), errorCode);
                failedEntries.add(new ErrorDescriptor(ErrorCode.get(TeamInboxErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.common.update.failed"), ErrorKeys.UPDATE_ACTION_FAILED, teamInbox.id().toString()));
            }
        });

        if (!failedEntries.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(TeamInboxErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.common.update.failed")), failedEntries));
        }

        return processOutcome;
    }

    /**
     * <p>
     * Checks the existence of the provided team inbox id
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param teamInboxId Represents the team inbox id
     * @return {@link Boolean} existence status of team inbox
     */
    @Override
    public boolean exists(final Long orgId, final Long teamInboxId) {
        final Table teamInboxTable = getTable();
        final CountQuery countQuery = CountQuery.get(teamInboxTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(teamInboxTable.getPKColumn(), teamInboxId))
                .and(Criteria.eq(teamInboxTable.getColumn(OMEMTEAMINBOX.IS_ACTIVE), ACTIVE.id())));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Checks whether a team inbox entry exists for the given organization and
     * email address with the specified verification state.
     *
     * @param orgId             The organization identifier.
     * @param emailAddress      The email address to check.
     * @param verificationState The verification state to match.
     * @return {@code true} if a matching record exists, otherwise {@code false}.
     */
    public boolean exists(final Long orgId, final EmailAddress emailAddress,
                          final TeamInbox.VerificationState verificationState) {
        return exists(orgId, emailAddress,
                new WhereClause(Criteria.eq(getTable().getColumn(OMEMTEAMINBOX.EMAIL_VERIFICATION_STATE),
                        verificationState.id())));
    }

    /**
     * <p>
     * Checks whether a team inbox entry exists for the given organization and
     * email address with the specified active status.
     * </p>
     *
     * @param orgId        The organization identifier.
     * @param emailAddress The email address to check.
     * @param state        Represent the team inbox verification state
     * @return {@code true} if a matching record exists, otherwise {@code false}.
     */
    public boolean exists(final Long orgId, final EmailAddress emailAddress, final TeamInbox.State state) {
        return exists(orgId, emailAddress, new WhereClause(Criteria.eq(getTable().getColumn(OMEMTEAMINBOX.IS_ACTIVE), state.id())));
    }

    /**
     * <p>
     * Updates the deletion of a team inbox account
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param teamInboxId Represents the team inbox id to be deleted
     */
    public void delete(final Long orgId, final Long teamInboxId) {
        final Table teamInboxTable = getTable();
        final Table fromAddressTable = mailManager.getFromAddressTable();
        final SelectQuery selectQuery = getTeamInboxSelectQuery();

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(teamInboxTable.getPKColumn(), teamInboxId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final DataContainer targetContainer = DataContainer.create();
        final Row teamInboxRow = dataContainer.getRow(teamInboxTable, teamInboxId);
        final Row fromAddressRow = dataContainer.getRow(fromAddressTable,
                new WhereClause(Criteria.eq(fromAddressTable.getColumn(OMEMFROMADDRESS.EMAIL_ID),
                        teamInboxRow.get(OMEMTEAMINBOX.EMAIL_ID))));

        teamInboxRow.set(OMEMTEAMINBOX.IS_ACTIVE, TeamInbox.State.INACTIVE.id());
        fromAddressRow.set(OMEMFROMADDRESS.IS_ACTIVE, false);

        targetContainer.updateRow(teamInboxRow);
        targetContainer.updateRow(fromAddressRow);

        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Retrieves the count of available team inbox accounts for the provided user
     * </p>
     *
     * @param orgId Represents an organization id
     * @return {@link Long} count for number of team inbox available for the user
     */
    public long getTeamInboxCount(final long orgId) {
        final Table teamInboxTable = getTable();
        final CountQuery countQuery = CountQuery.get(teamInboxTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(teamInboxTable.getColumn(OMEMTEAMINBOX.IS_ACTIVE), ACTIVE.id())));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Retrieves the team inbox id for the given email address if it exists and is active.
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param emailAddress Represents the email address to search for
     * @return the team inbox id associated with the email address
     */
    public Long getInboxIdByAddress(final long orgId, final String emailAddress) {
        final Table teamInboxTable = getTable();
        final Table addressTable = getEmailAddressTable();
        final SelectQuery selectQuery = SelectQuery.get(teamInboxTable);

        selectQuery.addSelectClause(teamInboxTable.getPKColumn());
        selectQuery.addJoinClause(
                new JoinClause(teamInboxTable.getColumn(OMEMTEAMINBOX.EMAIL_ID), addressTable.getPKColumn())
        );
        selectQuery.setWhereClause(new WhereClause(
                Criteria.eq(addressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), emailAddress))
                .and(Criteria.eq(teamInboxTable.getColumn(OMEMTEAMINBOX.IS_ACTIVE), ACTIVE.id()))
        );
        final Row teamInboxRow = orgDataStore(orgId).get(selectQuery).getRow(teamInboxTable);

        return Objects.isNull(teamInboxRow) ? null : teamInboxRow.getPKValue();
    }

    /**
     * <p>
     * Fetches the team inbox list available for the provided user based on the provided pagination limits
     * </p>
     *
     * @param orgId Represents an organization id
     * @param start Start value represents the lower bound of team inbox value to be fetched
     * @param limit Limit value represents the upper bound of team inbox value to be fetched
     * @return Collection of {@link TeamInbox}
     */
    public Collection<TeamInbox> getTeamInboxList(final Long orgId, final int start, final int limit) {
        return getTeamInboxList(orgId, start, limit, null);
    }

    /**
     * <p>
     * Fetches the team inbox list associated with the specified user within the provided organization
     * based on the given pagination limits.
     * </p>
     *
     * @param orgId  Represents an organization ID
     * @param userId Represents a user ID used to filter team inbox records
     * @param start  Start value represents the lower bound of team inbox values to be fetched
     * @param limit  Limit value represents the upper bound of team inbox values to be fetched
     * @return Collection of {@link TeamInbox}
     */
    public Collection<TeamInbox> getTeamInboxList(final Long orgId, final Long userId, final int start, final int limit) {
        return getTeamInboxList(orgId, start, limit, getCriteria(getTable().getPKColumn(), userId));
    }

    /**
     * <p>
     * Retrieves the {@link TeamInbox} entries associated with the given collection of email addresses.
     * </p>
     *
     * @param emailAddress Represents the collection of email addresses to search for.
     * @return A {@link Map} where the key is the email address and the value is a list of {@link TeamInbox} objects.
     */
    public Map<String, List<TeamInbox>> getTeamInbox(final Collection<String> emailAddress) {
        if (Objects.isNull(emailAddress) || emailAddress.isEmpty()) {
            return Collections.emptyMap();
        }
        final Table teamInboxTable = getTable();
        final Table emailAddressTable = getEmailAddressTable();

        final Column teamInboxEmailIdColumn = teamInboxTable.getColumn(OMEMTEAMINBOX.EMAIL_ID);
        final Column emailAddressColumn = emailAddressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS);

        final SelectQuery selectQuery = SelectQuery.get(teamInboxTable);

        selectQuery.addSelectClause(teamInboxTable);
        selectQuery.addSelectClause(emailAddressColumn);
        selectQuery.addJoinClause(new JoinClause(teamInboxEmailIdColumn, emailAddressTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(emailAddressColumn, emailAddress)));
        final DataContainer dataContainer = adminDataStore().get(selectQuery);

        return dataContainer.getRows(teamInboxTable).collect(
                Collectors.groupingBy(
                        row -> dataContainer.getRow(emailAddressTable, (Long) row.get(OMEMTEAMINBOX.EMAIL_ID))
                                .get(emailAddressColumn),
                        Collectors.mapping(
                                row -> convertRowToModel(row, dataContainer),
                                Collectors.toList()
                        )
                )
        );
    }

    /**
     * <p>
     * Fetches the territories associated with the given team inbox id.
     * </p>
     *
     * @param orgId       Represents an organization id.
     * @param teamInboxId Represents the {@link TeamInbox} id
     * @return collection of {@link Territory}
     */
    public Collection<Territory> fetchAssociatedTerritories(final long orgId, final long teamInboxId) {
        final Table teamInboxTerritoryTable = getTeamInboxTerritoryTable();
        final Table territoryTable = Table.get(OMCRMTERRITORY.TABLE);
        final SelectQuery territorySelectQuery = SelectQuery.get(teamInboxTerritoryTable);

        territorySelectQuery.addJoinClause(
                new JoinClause(teamInboxTerritoryTable.getColumn(OMEMTEAMINBOXTERRITORY.TERRITORY_ID), territoryTable.getPKColumn()));
        territorySelectQuery.setWhereClause(new WhereClause(Criteria.eq(teamInboxTerritoryTable.getColumn(OMEMTEAMINBOXTERRITORY.TEAM_INBOX_ID), teamInboxId)));
        territorySelectQuery.addSelectClause(territoryTable.getColumn(OMCRMTERRITORY.NAME));

        final DataContainer dataContainer = orgDataStore(orgId).get(territorySelectQuery);

        if (dataContainer.isEmpty()) {
            return Collections.emptyList();
        } else {
            final Collection<Row> territoryRows = dataContainer.getRows(territoryTable).toList();
            final Collection<Territory> territories = new HashSet<>();

            for (final Row territoryRow : territoryRows) {
                final Territory territory = new Territory();

                territory.setId(territoryRow.getPKValue());
                territory.setName(territoryRow.get(OMCRMTERRITORY.NAME));

                territories.add(territory);
            }

            return territories;
        }
    }

    /**
     * <p>
     * Fetches the teams associated with the given team inbox id.
     * </p>
     *
     * @param orgId       Represents an organization id.
     * @param teamInboxId Represents the {@link TeamInbox} id
     * @return collection of {@link Team}
     */
    public Collection<Team> fetchAssociatedTeams(final long orgId, final long teamInboxId) {
        final Table teamInboxTeamTable = getTeamInboxTeamTable();
        final Table teamTable = Table.get(OMCRMTEAM.TABLE);
        final SelectQuery teamSelectQuery = SelectQuery.get(teamInboxTeamTable);

        teamSelectQuery.addJoinClause(
                new JoinClause(teamInboxTeamTable.getColumn(OMEMTEAMINBOXTEAM.TEAM_ID), teamTable.getPKColumn()));
        teamSelectQuery.setWhereClause(new WhereClause(Criteria.eq(teamInboxTeamTable.getColumn(OMEMTEAMINBOXTEAM.TEAM_INBOX_ID), teamInboxId)));
        teamSelectQuery.addSelectClause(teamTable.getColumn(OMCRMTEAM.NAME));

        final DataContainer dataContainer = orgDataStore(orgId).get(teamSelectQuery);

        if (dataContainer.isEmpty()) {
            return Collections.emptyList();
        } else {
            final Collection<Row> teamRows = dataContainer.getRows(teamTable).toList();
            final Collection<Team> teams = new HashSet<>();

            for (final Row teamRow : teamRows) {
                final Team team = new Team();

                team.setId(teamRow.getPKValue());
                team.setName(teamRow.get(OMCRMTEAM.NAME));

                teams.add(team);
            }

            return teams;
        }
    }

    /**
     * <p>
     * Fetches the users associated with the given team inbox id.
     * </p>
     *
     * @param orgId       Represents an organization id.
     * @param teamInboxId Represents the {@link TeamInbox} id
     * @return collection of {@link CrmUser}
     */
    public Collection<CrmUser> fetchAssociatedUsers(final long orgId, final long teamInboxId) {
        final Table teamInboxUsersTable = getTeamInboxUsersTable();
        final SelectQuery usersSelectquery = SelectQuery.get(teamInboxUsersTable);

        usersSelectquery.setWhereClause(new WhereClause(Criteria.eq(teamInboxUsersTable.getColumn(OMEMTEAMINBOXUSER.TEAM_INBOX_ID), teamInboxId)));

        final DataContainer dataContainer = orgDataStore(orgId).get(usersSelectquery);

        if (dataContainer.isEmpty()) {
            return Collections.emptyList();
        } else {
            final Stream<Row> userRows = dataContainer.getRows(teamInboxUsersTable);
            final Collection<Long> userIds = userRows.map(row -> (Long) row.get(OMEMTEAMINBOXUSER.USER_ID)).collect(Collectors.toUnmodifiableSet());

            return userService.getUsers(orgId, userIds);
        }
    }

    /**
     * <p>
     * Loads a team inbox thread into a specified target container.
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param date            Represents the date to be updated
     * @param teamInboxId     Represents the team inbox id
     * @param threadId        Represents the thread id
     * @param targetContainer Holds the updated information of team inbox attributes
     */
    public void loadTeamInboxThreadIntoContainer(final long orgId, final long date, final long teamInboxId, final long threadId,
                                                 final DataContainer targetContainer) {
        final Table teamInboxThreadTable = getTeamInboxThreadTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(teamInboxThreadTable.getColumn(OMEMTEAMINBOXTHREADS.TEAM_INBOX_ID), teamInboxId))
                .and(Criteria.eq(teamInboxThreadTable.getColumn(OMEMTEAMINBOXTHREADS.THREAD_ID), threadId));
        final Row row = Optional.ofNullable(targetContainer.getRow(teamInboxThreadTable, whereClause))
                .or(() -> Optional.ofNullable(orgDataStore(orgId).get(teamInboxThreadTable, whereClause).getRow(teamInboxThreadTable)))
                .orElseGet(() -> new Row(teamInboxThreadTable));

        if (row.isNewRow()) {
            row.setOrgId(orgId);
            row.set(OMEMTEAMINBOXTHREADS.TEAM_INBOX_ID, teamInboxId);
            row.set(OMEMTEAMINBOXTHREADS.THREAD_ID, threadId);
        }
        targetContainer.updateRow(row);
        updateTeamInbox(orgId, teamInboxId, date, targetContainer);
    }

    /**
     * <p>
     * Checks if a given thread belongs to a team inbox. First verifies the presence
     * of the thread in the provided {@link DataContainer}, and if not found,
     * queries the datastore.
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param threadId        Represents the thread id to check
     * @param targetContainer Holds the team inbox data to check against
     * @return {@code true} if the thread is a team inbox thread, {@code false} otherwise
     */
    public boolean isTeamInboxThread(final long orgId, final Long threadId, final DataContainer targetContainer) {
        final Table teamInboxTable = getTeamInboxThreadTable();

        return Objects.nonNull(targetContainer.getRow(teamInboxTable,
                new WhereClause(Criteria.eq(teamInboxTable.getColumn(OMEMTEAMINBOXTHREADS.THREAD_ID), threadId))))
                || areTeamInboxThreads(orgId, List.of(threadId));
    }

    /**
     * <p>
     * Checks if a conversation is an active team inbox conversation for the given organization.
     * Queries the datastore to verify that the conversation exists, is active, and belongs
     * to the team inbox variant.
     * </p>
     *
     * @param orgId Represents an organization id
     * @param id    Represents the conversation id to check
     * @return {@code true} if the conversation is an active team inbox conversation, {@code false} otherwise
     */
    public boolean isActiveTeamInboxConversation(final long orgId, final long id) {
        final Table conversationTable = coreMailManager.getConversationsTable();
        final CountQuery countQuery = CountQuery.get(conversationTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(conversationTable.getPKColumn(), id))
                .and(Criteria.eq(conversationTable.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), true))
                .and(Criteria.eq(conversationTable.getColumn(OMEMCONVERSATIONS.VARIANT), EmailVariant.Type.TEAM_INBOX.getValue())));

        return orgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * <p>
     * Checks if any of the given thread IDs belong to team inbox threads. Queries the datastore
     * for the presence of the thread IDs in the team inbox thread table.
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param threadIds Represents the collection of thread ids to check
     * @return all the team inbox thread Ids
     */
    public Set<Long> filterTeamInboxThreadIds(final long orgId, final Collection<Long> threadIds) {
        final Table teamInboxThreadTable = getTeamInboxThreadTable();
        final SelectQuery selectQuery = SelectQuery.get(teamInboxThreadTable);

        selectQuery.addSelectClause(teamInboxThreadTable.getColumn(OMEMTEAMINBOXTHREADS.THREAD_ID));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(teamInboxThreadTable.getColumn(OMEMTEAMINBOXTHREADS.THREAD_ID), threadIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(teamInboxThreadTable).map(row ->
                (Long) row.get(OMEMTEAMINBOXTHREADS.THREAD_ID)).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Fetches the smart bcc address for the given organization id
     * </p>
     *
     * @param orgId Represents an organization id
     * @return the smart bcc address
     */
    public String fetchAutoForwardingAddress(final Long orgId) {
        final Table emailAddressTable = getEmailAddressTable();
        final Table teamInboxAddressTable = getAutoForwardingAddressTable();
        final SelectQuery selectQuery = SelectQuery.get(emailAddressTable);

        selectQuery.addJoinClause(List.of(
                new JoinClause(emailAddressTable.getPKColumn(), teamInboxAddressTable.getColumn(OMEMAutoForwardingAddress.EMAIL_ID)),
                new JoinClause(teamInboxAddressTable.getColumn(OMEMAutoForwardingAddress.PARENT_INBOX_ID), getParentAddressTable().getPKColumn())));
        selectQuery.addSelectClause(emailAddressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS));
        final Row row = orgDataStore(orgId).get(selectQuery).getRow(emailAddressTable);

        return Objects.isNull(row) ? null : row.get(OMEMADDRESS.EMAIL_ADDRESS);
    }

    /**
     * <p>
     * Gets the team inbox select query
     * </p>
     *
     * @return Returns the select query.
     */
    private SelectQuery getTeamInboxSelectQuery() {
        final Table teamInboxTable = getTable();
        final Table emailTable = mailManager.getEmailAddressTable();
        final Table fromAddressTable = mailManager.getFromAddressTable();
        final Column emailPkColumn = emailTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(teamInboxTable);

        selectQuery.addJoinClause(
                List.of(
                        new JoinClause(teamInboxTable.getColumn(OMEMTEAMINBOX.EMAIL_ID), emailPkColumn),
                        new JoinClause(emailPkColumn, fromAddressTable.getColumn(OMEMFROMADDRESS.EMAIL_ID))
                )
        );

        return selectQuery;
    }

    /**
     * <p>
     * Checks whether a given email address is in active for a specific organization.
     * </p>
     *
     * @param orgId        Represents the organization ID
     * @param emailAddress Represents the email address to check
     * @return true if the email address is active, false otherwise
     */
    public boolean isInActive(final Long orgId, final EmailAddress emailAddress) {
        final Table teamInboxTable = getTable();
        final Table emailTable = mailManager.getEmailAddressTable();
        final CountQuery countQuery = CountQuery.get(teamInboxTable);

        countQuery.addJoinClause(new JoinClause(teamInboxTable.getColumn(OMEMTEAMINBOX.EMAIL_ID), emailTable.getPKColumn()));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(emailTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), emailAddress.getEmailAddress()))
                .and(Criteria.eq(teamInboxTable.getColumn(OMEMTEAMINBOX.IS_ACTIVE), TeamInbox.State.INACTIVE.id())));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Updates the state of a team inbox email address to ACTIVE for a given organization.
     * </p>
     *
     * @param orgId        Represents the organization ID
     * @param emailAddress Represents the email address to update
     */
    public void updateState(final Long orgId, final EmailAddress emailAddress) {
        final Table teamInboxTable = getTable();
        final Table emailTable = mailManager.getEmailAddressTable();
        final Table fromAddressTable = mailManager.getFromAddressTable();
        final SelectQuery selectQuery = getTeamInboxSelectQuery();

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(emailTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), emailAddress.getEmailAddress())));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        Optional.ofNullable(dataContainer.getRow(teamInboxTable))
                .ifPresent(row -> {
                    row.set(OMEMTEAMINBOX.IS_ACTIVE, ACTIVE.id());
                    final Row fromAddressRow = dataContainer.getRow(fromAddressTable,
                            new WhereClause(Criteria.eq(fromAddressTable.getColumn(OMEMFROMADDRESS.EMAIL_ID), row.get(OMEMTEAMINBOX.EMAIL_ID))));

                    fromAddressRow.set(OMEMFROMADDRESS.IS_ACTIVE, TeamInbox.VerificationState.get(row.get(OMEMTEAMINBOX.EMAIL_VERIFICATION_STATE)).isVerified());

                    dataContainer.updateRow(row);
                    dataContainer.updateRow(fromAddressRow);

                    orgDataStore(orgId).commitChanges(dataContainer);
                });
    }

    /**
     * <p>
     * Builds the filtering criteria to determine which team inbox records
     * a user has access to based on their direct association, team membership,
     * or assigned territory.
     * </p>
     *
     * @param userId the ID of the user whose accessible team inboxes are evaluated
     * @return a {@link WhereClause} representing the combined access criteria
     */
    public WhereClause getCriteria(final Column column, final Long userId) {
        return new WhereClause(
                Criteria.in(column, getUserQuery(userId))
        ).or(
                Criteria.in(column, getTeamQuery(userId))
        ).or(
                Criteria.in(column, getTerritoryQuery(userId))
        );
    }

    /**
     * <p>
     * Gets the domain id by teamInbox id.
     * </p>
     *
     * @param orgId       Represents the orgId.
     * @param teamInboxId Represents the team inbox id.
     * @return a {@link WhereClause} representing the combined access criteria
     */
    public Long getDomainId(final Long orgId, final Long teamInboxId) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table.getColumn(OMEMTEAMINBOX.DOMAIN_ID));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), teamInboxId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row row = dataContainer.getRow(table, teamInboxId);

        return Objects.isNull(row) ? null : row.get(OMEMTEAMINBOX.DOMAIN_ID);
    }

    /**
     * <p>
     * Checks directly in the datastore if the given thread IDs belong to a team inbox.
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param threadIds Represents the collection of thread ids to check
     * @return {@code true} if all thread IDs exist in the team inbox thread table, {@code false} otherwise
     */
    public boolean areTeamInboxThreads(final long orgId, final Collection<Long> threadIds) {
        final Table teamInboxThreadTable = getTeamInboxThreadTable();
        final CountQuery countQuery = CountQuery.get(teamInboxThreadTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(teamInboxThreadTable.getColumn(OMEMTEAMINBOXTHREADS.THREAD_ID), threadIds)));

        return orgDataStore(orgId).get(countQuery) == threadIds.size();
    }

    /**
     * <p>
     * Checks whether a conversation is an active team inbox conversation.
     * </p>
     *
     * @param orgId          Represents the organization id
     * @param userId         Represents the user id associated with the conversation
     * @param conversationId Represents the conversation id to check
     * @param isActive       Represents the active status flag to filter the conversation
     * @return {@code true} if the conversation exists for the user or is an active team inbox conversation,
     *         {@code false} otherwise
     */
    public boolean isActiveTeamInboxConversation(final Long orgId, final Long userId, final Long conversationId,
                                                 final boolean isActive) {
        return coreMailManager.isConversationExists(orgId, userId, conversationId, isActive) ||
                isActiveTeamInboxConversation(orgId, conversationId);
    }

    /**
     * <p>
     * Updates the team inbox status with the recently received mail time
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param timeStamp       Represents the time stamp to be updated
     * @param teamInboxId     Represents the team inbox id
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void updateTeamInbox(final long orgId, final long timeStamp, final long teamInboxId, final DataContainer targetContainer) {
        final Table teamInboxTable = getTable();
        final Row existingRow = targetContainer.getRow(teamInboxTable, new WhereClause(Criteria.eq(teamInboxTable.getPKColumn(), teamInboxId)));
        final Row row = Objects.isNull(existingRow) ? getTeamInboxRow(orgId, teamInboxId) : existingRow;

        if (Objects.nonNull(row)) {
            row.set(OMEMTEAMINBOX.AUTO_FORWARDED_STATUS_TIME, timeStamp);

            targetContainer.updateRow(row);
        }
    }

    /**
     * <p>
     * Retrieves a row representing a team inbox from the datastore for a given organization.
     * </p>
     *
     * @param orgId       Represents the organization id
     * @param teamInboxId Represents the team inbox id to fetch
     * @return The {@link Row} corresponding to the specified team inbox id, or {@code null} if no such row exists
     */
    private Row getTeamInboxRow(final long orgId, final Long teamInboxId) {
        final Table teamInboxTable = getTable();

        final SelectQuery selectQuery = SelectQuery.get(teamInboxTable);
        final WhereClause whereClause = new WhereClause(Criteria.eq(teamInboxTable.getPKColumn(), teamInboxId));

        selectQuery.setWhereClause(whereClause);

        return orgDataStore(orgId).get(selectQuery).getRow(teamInboxTable);
    }

    /**
     * <p>
     * Loads email address of the team inbox
     * </p>
     *
     * @param orgId           Represents the organization id
     * @param teamInbox       An entity that holds team inbox related attributes
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadTeamInboxIntoContainer(final Long orgId, final TeamInbox teamInbox, final DataContainer targetContainer) {
        final EmailAddress emailAddress = teamInbox.getAddress();
        final Long userId = teamInbox.getOwnerId();

        mailManager.loadEmailAddress(orgId, userId, emailAddress, targetContainer);
        final Long emailAddressId = emailAddress.id();

        mailManager.loadFromAddressIntoContainer(orgId, userId, false, FromAddressInfo.Type.TEAM_INBOX, emailAddressId, targetContainer);
        final DataContainer dataContainer = getDataContainer(orgId, emailAddressId);

        loadTeamInboxIntoContainer(true, teamInbox, dataContainer, targetContainer);
    }

    /**
     * <p>
     * Fetches the data container for the related team inbox email id and users
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param emailId Represents an email id
     * @return {@link DataContainer}
     */
    private DataContainer getDataContainer(final Long orgId, final Long emailId) {
        final Table teamInboxTable = getTable();
        final Table teamInboxUsersTable = getTeamInboxUsersTable();
        final SelectQuery selectQuery = SelectQuery.get(teamInboxTable);

        selectQuery.addSelectClause(teamInboxTable, teamInboxUsersTable);
        selectQuery.addJoinClause(new JoinClause(teamInboxTable.getColumn(OMEMTEAMINBOX.ID),
                teamInboxUsersTable.getColumn(OMEMTEAMINBOXUSER.TEAM_INBOX_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(teamInboxTable.getColumn(OMEMTEAMINBOX.EMAIL_ID), emailId)));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Updates the verification state of the team inbox for the provided organization and email address
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param emailAddress Represents the email address of team inbox
     */
    public void update(final long orgId, final String emailAddress) {
        if (Objects.nonNull(emailAddress)) {
            final Table teamInboxTable = getTable();
            final Table fromAddressTable = mailManager.getFromAddressTable();
            final SelectQuery selectQuery = getTeamInboxSelectQuery();

            selectQuery.setWhereClause(new WhereClause(Criteria.eq(mailManager.getEmailAddressTable().getColumn(OMEMADDRESS.EMAIL_ADDRESS), emailAddress))
                    .and(Criteria.eq(teamInboxTable.getColumn(OMEMTEAMINBOX.IS_ACTIVE), ACTIVE.id())));

            final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

            if (!dataContainer.isEmpty()) {
                final DataContainer targetContainer = DataContainer.create();
                final Row teamInboxRow = dataContainer.getRow(teamInboxTable);
                final Long emailId = teamInboxRow.get(OMEMTEAMINBOX.EMAIL_ID);

                teamInboxRow.set(OMEMTEAMINBOX.EMAIL_VERIFICATION_STATE, TeamInbox.VerificationState.VERIFIED.id());
                targetContainer.updateRow(teamInboxRow);
                final WhereClause fromAddressCriteria = new WhereClause(Criteria.eq(fromAddressTable.getColumn(OMEMFROMADDRESS.EMAIL_ID), emailId))
                        .and(Criteria.eq(fromAddressTable.getColumn(OMEMFROMADDRESS.TYPE), FromAddressInfo.Type.TEAM_INBOX.getId()));
                final Row fromAddressRow = dataContainer.getRow(fromAddressTable, fromAddressCriteria);

                fromAddressRow.set(OMEMFROMADDRESS.IS_ACTIVE, true);
                targetContainer.updateRow(fromAddressRow);

                orgDataStore(orgId).commitChanges(targetContainer);
            }
        }
    }

    /**
     * <p>
     * Loads the email address into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create event or update event
     * @param teamInbox       An entity that holds team inbox related attributes
     * @param sourceContainer Holds the user defined values of existing team inbox entity
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadTeamInboxIntoContainer(final boolean isCreateEvent, final TeamInbox teamInbox, final DataContainer sourceContainer,
                                            final DataContainer targetContainer) {
        final Table teamInboxTable = getTable();
        final Long orgId = teamInbox.orgId();
        final TeamInbox.VisibilityStatus visibilityStatus = teamInbox.getVisibilityStatus();
        final Row teamInboxRow = sourceContainer.isEmpty() ? new Row(teamInboxTable) : sourceContainer.getRow(teamInboxTable);

        if (!isCreateEvent && Objects.isNull(teamInboxRow)) {
            throw ErrorCode.get(EmailErrorCodes.EMAIL_NOT_FOUND, String.format("orgId %d Id %d", orgId, teamInbox.id()));
        }

        if (teamInboxRow.isNewRow()) {
            teamInboxRow.setOrgId(orgId);
            teamInboxRow.set(OMEMTEAMINBOX.EMAIL_ID, teamInbox.getAddress().id());
        }
        teamInboxRow.set(OMEMTEAMINBOX.DOMAIN_ID, teamInbox.getDomain().id());
        teamInboxRow.set(OMEMTEAMINBOX.IS_ACTIVE, teamInbox.getState().id());
        teamInboxRow.set(OMEMTEAMINBOX.OWNER_ID, teamInbox.getOwnerId());
        teamInboxRow.set(OMEMTEAMINBOX.VISIBILITY_STATUS, visibilityStatus.getId());
        teamInbox.setId(teamInboxRow.getPKValue());

        targetContainer.updateRow(teamInboxRow);
        loadTeamInboxUsersIntoContainer(isCreateEvent, teamInbox, sourceContainer, targetContainer);
    }

    /**
     * <p>
     * Loads the team inbox users into container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param teamInbox       An entity that holds team inbox related attributes
     * @param entityContainer Holds the user defined values of existing team inbox action
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadTeamInboxUsersIntoContainer(final boolean isCreateEvent, final TeamInbox teamInbox, final DataContainer entityContainer, final DataContainer targetContainer) {
        final Long orgId = teamInbox.orgId();
        final Long teamInboxId = teamInbox.id();
        final Collection<Team> teams = teamInbox.getTeams();
        final Collection<Territory> territories = teamInbox.getTerritories();
        final Collection<CrmUser> crmUsers = teamInbox.getUsers();
        final Collection<Long> teamIds = Objects.isNull(teams) ? new HashSet<>()
                : teams.stream().filter(Objects::nonNull).map(Team::id).collect(Collectors.toUnmodifiableSet());
        final Collection<Long> territoryIds = Objects.isNull(territories) ? new HashSet<>()
                : territories.stream().filter(Objects::nonNull).map(Territory::id).collect(Collectors.toUnmodifiableSet());
        final Collection<Long> userIds = Objects.isNull(crmUsers) ? new HashSet<>()
                : crmUsers.stream().filter(Objects::nonNull).map(CrmUser::id).collect(Collectors.toUnmodifiableSet());

        if (!teamIds.isEmpty()) {

            if (teamManager.exists(orgId, teamIds)) {
                loadTeamsIntoContainer(orgId, teamInboxId, teamIds, entityContainer, targetContainer);
            } else {
                throw ErrorCode.get(TeamInboxErrorCodes.TEAM_NOT_FOUND, "i18n.notifications.team.inbox.team.not.found");
            }
        }

        if (!territoryIds.isEmpty()) {

            if (territoryManager.exists(orgId, territoryIds)) {
                loadTeamInboxTerritories(orgId, teamInboxId, territoryIds, entityContainer, targetContainer);
            } else {
                throw ErrorCode.get(TeamInboxErrorCodes.TERRITORY_NOT_FOUND, "i18n.notifications.team.inbox.territory.not.found");
            }
        }

        if (Objects.nonNull(crmUsers) && !crmUsers.isEmpty()) {

            if (crmUserManager.exists(orgId, userIds)) {
                loadTeamInboxUsers(orgId, teamInboxId, userIds, entityContainer, targetContainer);
            } else {
                throw ErrorCode.get(TeamInboxErrorCodes.USER_NOT_FOUND, "i18n.notifications.team.inbox.user.not.found");
            }
        }

        if (!isCreateEvent) {
            deleteUnmappedUsers(teamInboxId, userIds, entityContainer, targetContainer);
            deleteUnmappedTerritories(teamInboxId, territoryIds, entityContainer, targetContainer);
            deleteUnmappedTeams(teamInboxId, teamIds, entityContainer, targetContainer);
        }
    }

    /**
     * <p>
     * Loads all the users who have access to the team inbox id into the container
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param teamInboxId     Represents the team inbox id
     * @param users           Represents the collection of users
     * @param entityContainer Holds the user defined values of existing team inbox action
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadTeamInboxUsers(final long orgId, final long teamInboxId, final Collection<Long> users,
                                    final DataContainer entityContainer, final DataContainer targetContainer) {
        final Table teamInboxUsersTable = getTeamInboxUsersTable();

        for (final Long userId : users) {
            final Row existingRow = Objects.isNull(entityContainer) ? null : entityContainer.getRow(teamInboxUsersTable,
                    new WhereClause(Criteria.eq(teamInboxUsersTable.getColumn(OMEMTEAMINBOXUSER.USER_ID), userId)));
            final Row row = Objects.isNull(existingRow) ? new Row(teamInboxUsersTable) : existingRow;

            if (row.isNewRow()) {
                row.setOrgId(orgId);
                row.set(OMEMTEAMINBOXUSER.USER_ID, userId);
                row.set(OMEMTEAMINBOXUSER.TEAM_INBOX_ID, teamInboxId);
            }
            targetContainer.updateRow(row);
        }
    }

    /**
     * <p>
     * Load all the territory which have access to the team inbox id into the container
     * </p>
     *
     * @param orgId           Represents an organization id.
     * @param teamInboxId     Represents the team inbox id
     * @param territoryIds    Represents the collection of territory id.
     * @param entityContainer Holds the user defined values of existing team inbox action
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadTeamInboxTerritories(final long orgId, final long teamInboxId, final Collection<Long> territoryIds, final DataContainer entityContainer, final DataContainer targetContainer) {
        final Table teamInboxTerritoryTable = getTeamInboxTerritoryTable();

        for (final Long territoryId : territoryIds) {
            final Row existingRow = Objects.isNull(entityContainer) ? null : entityContainer.getRow(teamInboxTerritoryTable,
                    new WhereClause(Criteria.eq(teamInboxTerritoryTable.getColumn(OMEMTEAMINBOXTERRITORY.TERRITORY_ID), territoryId)));
            final Row row = Objects.isNull(existingRow) ? new Row(teamInboxTerritoryTable) : existingRow;

            if (row.isNewRow()) {
                row.set(OMEMTEAMINBOXTERRITORY.TERRITORY_ID, territoryId);
                row.set(OMEMTEAMINBOXTERRITORY.TEAM_INBOX_ID, teamInboxId);
                row.setOrgId(orgId);
            }
            targetContainer.updateRow(row);
        }
    }

    /**
     * <p>
     * Load all the team which have access to the team inbox id into the container
     * </p>
     *
     * @param orgId           Represents an organization id.
     * @param teamInboxId     Represents the team inbox id
     * @param teamIds         Represents the collection of team id.
     * @param entityContainer Holds the user defined values of existing team inbox action
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadTeamsIntoContainer(final long orgId, final long teamInboxId, final Collection<Long> teamIds, final DataContainer entityContainer, final DataContainer targetContainer) {
        final Table teamTable = getTeamInboxTeamTable();

        for (final Long teamId : teamIds) {
            final Row existingRow = Objects.isNull(entityContainer) ? null : entityContainer.getRow(teamTable,
                    new WhereClause(Criteria.eq(teamTable.getColumn(OMEMTEAMINBOXTEAM.TEAM_ID), teamId)));
            final Row row = Objects.isNull(existingRow) ? new Row(teamTable) : existingRow;

            if (row.isNewRow()) {
                row.set(OMEMTEAMINBOXTEAM.TEAM_ID, teamId);
                row.set(OMEMTEAMINBOXTEAM.TEAM_INBOX_ID, teamInboxId);
                row.setOrgId(orgId);
            }
            targetContainer.updateRow(row);
        }
    }

    /**
     * <p>
     * Deletes the users that are not associated with the team inbox id
     * </p>
     *
     * @param teamInboxId     Represents the team inbox id
     * @param userIds         Represents Collection of {@link Long} user ids
     * @param dataContainer   Holds the field values of existing team inbox
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void deleteUnmappedUsers(final Long teamInboxId, final Collection<Long> userIds, final DataContainer dataContainer,
                                     final DataContainer targetContainer) {
        final Table teamInboxUsersTable = getTeamInboxUsersTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(teamInboxUsersTable.getColumn(OMEMTEAMINBOXUSER.TEAM_INBOX_ID), teamInboxId));
        final WhereClause usersWhereClause = Objects.nonNull(userIds) && !userIds.isEmpty()
                ? whereClause.and(Criteria.notIn(teamInboxUsersTable.getColumn(OMEMTEAMINBOXUSER.USER_ID), userIds)) : whereClause;
        final Stream<Row> userRows = dataContainer.getRows(teamInboxUsersTable, usersWhereClause);

        userRows.forEach(targetContainer::deleteRow);
    }

    /**
     * <p>
     * Deletes the territories that are not associated with the team inbox id
     * </p>
     *
     * @param teamInboxId     Represents the team inbox id
     * @param territoryIds    Represents Collection of {@link Long} territory ids
     * @param dataContainer   Holds the field values of existing team inbox
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void deleteUnmappedTerritories(final long teamInboxId, final Collection<Long> territoryIds,
                                           final DataContainer dataContainer, final DataContainer targetContainer) {
        final Table teamInboxTerritoryTable = getTeamInboxTerritoryTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(teamInboxTerritoryTable.getColumn(OMEMTEAMINBOXTERRITORY.TEAM_INBOX_ID), teamInboxId));
        final WhereClause territoryWhereClause = Objects.nonNull(territoryIds) && !territoryIds.isEmpty() ?
                whereClause.and(Criteria.notIn(teamInboxTerritoryTable.getColumn(OMEMTEAMINBOXTERRITORY.TERRITORY_ID), territoryIds)) : whereClause;
        final Collection<Row> territoryRows = dataContainer.getRows(teamInboxTerritoryTable, territoryWhereClause).toList();

        territoryRows.forEach(targetContainer::deleteRow);
    }

    /**
     * <p>
     * Deletes the teams that are not associated with the team inbox id
     * </p>
     *
     * @param teamInboxId     Represents the team inbox id
     * @param teamIds         Represents Collection of {@link Long} team ids
     * @param dataContainer   Holds the field values of existing team inbox
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void deleteUnmappedTeams(final long teamInboxId, final Collection<Long> teamIds,
                                     final DataContainer dataContainer, final DataContainer targetContainer) {
        final Table teamTable = getTeamInboxTeamTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(teamTable.getColumn(OMEMTEAMINBOXTEAM.TEAM_INBOX_ID), teamInboxId));
        final WhereClause teamsWhereClause = Objects.nonNull(teamIds) && !teamIds.isEmpty() ?
                whereClause.and(Criteria.notIn(teamTable.getColumn(OMEMTEAMINBOXTEAM.TEAM_ID), teamIds)) : whereClause;
        final Collection<Row> teamRows = dataContainer.getRows(teamTable, teamsWhereClause).toList();

        teamRows.forEach(targetContainer::deleteRow);
    }

    /**
     * <p>
     * Generates a team inbox entity using the acquired row's data
     * </p>
     *
     * @param row           Represents the team inbox row
     * @param dataContainer Container which holds the field values related to team inbox row fetched
     * @return {@link TeamInbox} entity
     */
    private TeamInbox convertRowToModel(final Row row, final DataContainer dataContainer) {
        final Short visibilityStatus = row.get(OMEMTEAMINBOX.VISIBILITY_STATUS);
        final TeamInbox teamInbox = new TeamInbox();
        final Table emailAddressTable = mailManager.getEmailAddressTable();
        final Short verificationState = row.get(OMEMTEAMINBOX.EMAIL_VERIFICATION_STATE);
        final Row emailRow = dataContainer.getRow(emailAddressTable, new WhereClause(Criteria.eq(emailAddressTable.getPKColumn(),
                row.get(OMEMTEAMINBOX.EMAIL_ID))));

        teamInbox.setId(row.getPKValue());
        teamInbox.setOrgId(row.getOrgId());
        teamInbox.setOwnerId(row.get(OMEMTEAMINBOX.OWNER_ID));

        if (Objects.nonNull(emailRow)) {
            teamInbox.setAddress(new EmailAddress(emailRow.get(OMEMADDRESS.EMAIL_ADDRESS)));
        }

        if (Objects.nonNull(visibilityStatus)) {
            teamInbox.setVisibilityStatus(TeamInbox.VisibilityStatus.get(visibilityStatus));
        }

        if (Objects.nonNull(verificationState)) {
            teamInbox.setVerificationState(TeamInbox.VerificationState.get(verificationState));
        }
        crmDomainManager.getDomain(row.get(OMEMTEAMINBOX.DOMAIN_ID), dataContainer).ifPresent(teamInbox::setDomain);
        teamInbox.setAutoForwardedTime(row.get(OMEMTEAMINBOX.AUTO_FORWARDED_STATUS_TIME));

        teamInbox.setTeams(convertRowToTeams(dataContainer));
        teamInbox.setTerritories(convertRowToTerritory(dataContainer));
        teamInbox.setUsers(convertRowToUsers(dataContainer));

        return teamInbox;
    }

    /**
     * <p>
     * Converts rows from the team inbox-team mapping table into a collection of {@link Team} objects.
     * </p>
     *
     * @param dataContainer The {@link DataContainer} holding table rows
     * @return A collection of {@link Team} objects
     */
    private Collection<Team> convertRowToTeams(final DataContainer dataContainer) {
        final Table teamTable = teamManager.getTable();
        final Table teamInboxTeamTable = getTeamInboxTeamTable();

        final Collection<Team> teams = new HashSet<>();

        dataContainer.getRows(teamInboxTeamTable)
                .forEach(teamInboxTeamRow -> {
                    final Team team = new Team();
                    final Long pkValue = teamInboxTeamRow.get(OMEMTEAMINBOXTEAM.TEAM_ID);

                    team.setId(pkValue);
                    team.setName(dataContainer.getRow(teamTable, pkValue).get(OMCRMTEAM.NAME));

                    teams.add(team);
                });

        return teams.isEmpty() ? null : teams;
    }

    /**
     * <p>
     * Converts rows from the team inbox-territory mapping table into a collection of {@link Territory} objects.
     * </p>
     *
     * @param dataContainer The {@link DataContainer} holding table rows
     * @return A collection of {@link Territory} objects
     */
    private Collection<Territory> convertRowToTerritory(final DataContainer dataContainer) {
        final Table territoryTable = territoryManager.getTable();
        final Table teamInboxTerritoryTable = getTeamInboxTerritoryTable();

        final Collection<Territory> territories = new HashSet<>();

        dataContainer.getRows(teamInboxTerritoryTable)
                .forEach(teamInboxTerritoryRow -> {
                    final Territory territory = new Territory();
                    final Long pkValue = teamInboxTerritoryRow.get(OMEMTEAMINBOXTERRITORY.TERRITORY_ID);

                    territory.setId(pkValue);
                    territory.setName(dataContainer.getRow(territoryTable, pkValue).get(OMCRMTERRITORY.NAME));

                    territories.add(territory);
                });

        return territories.isEmpty() ? null : territories;
    }

    /**
     * <p>
     * Converts rows from the team inbox-user mapping table into a collection of {@link CrmUser} objects.
     * </p>
     *
     * @param dataContainer The {@link DataContainer} holding table rows
     * @return A collection of {@link CrmUser} objects
     */
    private Collection<CrmUser> convertRowToUsers(final DataContainer dataContainer) {
        final Table teamInboxUserTable = getTeamInboxUsersTable();
        final Table userTable = crmUserManager.getTable();

        final Collection<CrmUser> crmUsers = new HashSet<>();

        dataContainer.getRows(teamInboxUserTable)
                .forEach(teamInboxUserRow -> {
                    final Row userRow = dataContainer.getRow(userTable, (Long) teamInboxUserRow.get(OMEMTEAMINBOXUSER.USER_ID));

                    if (Objects.nonNull(userRow)) {
                        crmUsers.add(crmUserManager.convertRowToModel(userRow));
                    }
                });

        return crmUsers.isEmpty() ? null : crmUsers;
    }

    /**
     * <p>
     * Fetches the team inbox container with the associated users of those provided team inbox ids
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param teamInboxIds Represents the collection of team inbox ids
     * @return {@link DataContainer} with team inbox, users associated data
     */
    private DataContainer getTeamInboxContainer(final Long orgId, final Collection<Long> teamInboxIds, final boolean requiredPartialData) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);

        final Table teamInboxTable = getTable();
        final Table teamInboxUsersTable = getTeamInboxUsersTable();
        final Table teamInboxTeamTable = getTeamInboxTeamTable();
        final Table teamInboxTerritoryTable = getTeamInboxTerritoryTable();

        final Table addressTable = getEmailAddressTable();
        final Table userTable = crmUserManager.getTable();
        final Table teamTable = teamManager.getTable();
        final Table territoryTable = territoryManager.getTable();

        final SelectQuery teamInboxSelectQuery = SelectQuery.get(teamInboxTable);
        final SelectQuery usersSelectQuery = SelectQuery.get(teamInboxUsersTable);
        final SelectQuery teamsSelectQuery = SelectQuery.get(teamInboxTeamTable);
        final SelectQuery territoriesSelectQuery = SelectQuery.get(teamInboxTerritoryTable);

        if (!requiredPartialData) {
            teamInboxSelectQuery.addJoinClause(new JoinClause(teamInboxTable.getColumn(OMEMTEAMINBOX.EMAIL_ID), addressTable.getPKColumn()));
            usersSelectQuery.addJoinClause(new JoinClause(teamInboxUsersTable.getColumn(OMEMTEAMINBOXUSER.USER_ID), userTable.getPKColumn()));
            teamsSelectQuery.addJoinClause(new JoinClause(teamInboxTeamTable.getColumn(OMEMTEAMINBOXTEAM.TEAM_ID), teamTable.getPKColumn()));
            territoriesSelectQuery.addJoinClause(new JoinClause(teamInboxTerritoryTable.getColumn(OMEMTEAMINBOXTERRITORY.TERRITORY_ID), territoryTable.getPKColumn()));
        }

        teamInboxSelectQuery.setWhereClause(new WhereClause(Criteria.in(teamInboxTable.getPKColumn(), teamInboxIds)));
        usersSelectQuery.setWhereClause(new WhereClause(Criteria.in(teamInboxUsersTable.getColumn(OMEMTEAMINBOXUSER.TEAM_INBOX_ID), teamInboxIds)));
        teamsSelectQuery.setWhereClause(new WhereClause(Criteria.in(teamInboxTeamTable.getColumn(OMEMTEAMINBOXTEAM.TEAM_INBOX_ID), teamInboxIds)));
        territoriesSelectQuery.setWhereClause(new WhereClause(Criteria.in(teamInboxTerritoryTable.getColumn(OMEMTEAMINBOXTERRITORY.TEAM_INBOX_ID), teamInboxIds)));

        final DataContainer teamInboxDatacontainer = orgDataStore.get(teamInboxSelectQuery);
        final DataContainer usersDataContainer = orgDataStore.get(usersSelectQuery);
        final DataContainer teamsDataContainer = orgDataStore.get(teamsSelectQuery);
        final DataContainer territoriesDatacontainer = orgDataStore.get(territoriesSelectQuery);

        teamInboxDatacontainer.append(usersDataContainer);
        teamInboxDatacontainer.append(teamsDataContainer);
        teamInboxDatacontainer.append(territoriesDatacontainer);

        return teamInboxDatacontainer;
    }

    /**
     * <p>
     * Builds a {@link SelectQuery} to retrieve team inbox IDs accessible to a user.
     * </p>
     *
     * @param userId       The ID of the user
     * @param joinClauses  The list of join conditions to apply in the query
     * @param whereClause  An optional additional filter (maybe {@code null})
     * @param userIdColumn The column representing the user association
     * @return a {@link SelectQuery} selecting distinct team inbox IDs
     */
    private SelectQuery getQuery(final Long userId, final List<JoinClause> joinClauses,
                                 final WhereClause whereClause, final Column userIdColumn) {
        final Table teamInboxTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(teamInboxTable);

        selectQuery.addSelectClause(teamInboxTable.getPKColumn().distinct());
        selectQuery.addJoinClause(joinClauses);
        selectQuery.setWhereClause(
                WhereClauseUtil.connect(
                        whereClause,
                        new WhereClause(Criteria.eq(userIdColumn, userId))
                                .and(Criteria.eq(teamInboxTable.getColumn(OMEMTEAMINBOX.IS_ACTIVE), ACTIVE.id())),
                        Connector.OR
                )
        );

        return selectQuery;
    }

    /**
     * <p>
     * A convenience overload of {@link #getQuery(Long, List, WhereClause, Column)}
     * that omits the additional {@code whereClause}.
     * </p>
     *
     * @param userId       The ID of the user
     * @param joinClauses  The list of join conditions to apply in the query
     * @param userIdColumn The column representing the user association
     * @return a {@link SelectQuery} selecting distinct team inbox IDs
     */
    private SelectQuery getQuery(final Long userId, final List<JoinClause> joinClauses, final Column userIdColumn) {
        return getQuery(userId, joinClauses, null, userIdColumn);
    }

    /**
     * <p>
     * Builds a {@link SelectQuery} to retrieve team inboxes visible to the user
     * either because they are marked as visible to everyone or explicitly
     * assigned to the user.
     * </p>
     *
     * @param userId The ID of the user
     * @return a {@link SelectQuery} selecting distinct team inbox IDs accessible through direct user association
     */
    private SelectQuery getUserQuery(final Long userId) {
        final Table teamInboxTable = getTable();
        final Table teamInboxUserTable = getTeamInboxUsersTable();

        return getQuery(
                userId,
                List.of(
                        new JoinClause(teamInboxTable.getPKColumn(), teamInboxUserTable.getColumn(OMEMTEAMINBOXUSER.TEAM_INBOX_ID), JoinClause.JoinType.LEFT)
                ),
                new WhereClause(Criteria.eq(teamInboxTable.getColumn(OMEMTEAMINBOX.VISIBILITY_STATUS), TeamInbox.VisibilityStatus.EVERYONE.getId())),
                teamInboxUserTable.getColumn(OMEMTEAMINBOXUSER.USER_ID)
        );
    }

    /**
     * <p>
     * Builds a {@link SelectQuery} to retrieve team inboxes accessible to the useR through team membership.
     * </p>
     *
     * @param userId The ID of the user
     * @return a {@link SelectQuery} selecting distinct team inbox IDs
     * accessible through team membership
     */
    private SelectQuery getTeamQuery(final Long userId) {
        final Table teamInboxTeamTable = getTeamInboxTeamTable();
        final Table userTeamTable = teamManager.getUserTeamTable();
        final Table teamTable = teamManager.getTable();

        final Column teamPkColumn = teamTable.getPKColumn();

        return getQuery(
                userId,
                List.of(
                        new JoinClause(getTable().getPKColumn(), teamInboxTeamTable.getColumn(OMEMTEAMINBOXTEAM.TEAM_INBOX_ID)),
                        new JoinClause(teamInboxTeamTable.getColumn(OMEMTEAMINBOXTEAM.TEAM_ID), teamPkColumn),
                        new JoinClause(teamPkColumn, userTeamTable.getColumn(MCRMUSERTEAM.TEAM_ID))
                ),
                userTeamTable.getColumn(MCRMUSERTEAM.USER_ID)
        );
    }

    /**
     * <p>
     * Builds a {@link SelectQuery} to retrieve team inboxes accessible to the user through territory assignments.
     * </p>
     *
     * @param userId The ID of the user
     * @return a {@link SelectQuery} selecting distinct team inbox IDs  accessible through territory association
     */
    private SelectQuery getTerritoryQuery(final Long userId) {
        final Table teamInboxTerritoryTable = getTeamInboxTerritoryTable();
        final Table userTerritoryTable = territoryManager.getUserTerritoryTable();
        final Table territoryTable = territoryManager.getTable();

        final Column territoryPkColumn = territoryTable.getPKColumn();

        return getQuery(
                userId,
                List.of(
                        new JoinClause(getTable().getPKColumn(), teamInboxTerritoryTable.getColumn(OMEMTEAMINBOXTERRITORY.TEAM_INBOX_ID)),
                        new JoinClause(teamInboxTerritoryTable.getColumn(OMEMTEAMINBOXTERRITORY.TERRITORY_ID), territoryPkColumn),
                        new JoinClause(territoryPkColumn, userTerritoryTable.getColumn(MCRMUSERTERRITORY.TERRITORY_ID))
                ),
                userTerritoryTable.getColumn(MCRMUSERTERRITORY.USER_ID)
        );
    }

    /**
     * <p>
     * Fetches the team inbox list for the given organization based on the provided criteria and pagination limits.
     * This method constructs and executes the underlying query that joins the team inbox and email address tables,
     * applies the active state filter, and retrieves the resulting records.
     * </p>
     *
     * @param orgId    Represents an organization ID
     * @param start    Represents the start
     * @param limit    Represents the limit
     * @param criteria Represents additional filtering conditions for fetching team inbox data;
     *                 can be {@code null} if no user-specific filter is applied
     * @return Collection of {@link TeamInbox}
     */
    private Collection<TeamInbox> getTeamInboxList(final Long orgId, final int start, final int limit, final WhereClause criteria) {
        final Table teamInboxTable = getTable();
        final Table domainTable = crmDomainManager.getTable();
        final Table emailTable = mailManager.getEmailAddressTable();

        final SelectQuery selectQuery = SelectQuery.get(teamInboxTable);
        final Column teamInboxEmailIdColumn = teamInboxTable.getColumn(OMEMTEAMINBOX.EMAIL_ID);

        selectQuery.addSelectClause(teamInboxTable, domainTable, emailTable);
        selectQuery.addJoinClause(
                List.of(
                        new JoinClause(teamInboxEmailIdColumn, emailTable.getPKColumn()),
                        new JoinClause(teamInboxTable.getColumn(OMEMTEAMINBOX.DOMAIN_ID), domainTable.getPKColumn(), JoinClause.JoinType.LEFT)
                )
        );
        selectQuery.setWhereClause(
                WhereClauseUtil.connect(
                        criteria,
                        new WhereClause(Criteria.eq(teamInboxTable.getColumn(OMEMTEAMINBOX.IS_ACTIVE), ACTIVE.id())),
                        Connector.AND
                )
        );
        selectQuery.setLimitClause(new LimitClause(start, limit));
        selectQuery.addOrderByClause(new OrderByClause(teamInboxTable.getPKColumn()));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(teamInboxTable).map(row -> convertRowToModel(row, dataContainer)).toList();
    }

    /**
     * <p>
     * Checks the existence of the provided id
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param emailAddress Represents the email address
     * @param whereClause  Represents the criteria
     * @return {@link Boolean} existence status
     */
    private boolean exists(final Long orgId, final EmailAddress emailAddress, final WhereClause whereClause) {
        final Table teamInboxTable = getTable();
        final Table emailTable = mailManager.getEmailAddressTable();

        final CountQuery countQuery = CountQuery.get(teamInboxTable);

        countQuery.addJoinClause(new JoinClause(teamInboxTable.getColumn(OMEMTEAMINBOX.EMAIL_ID), emailTable.getPKColumn()));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(emailTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), emailAddress.getEmailAddress()))
                .and(whereClause));

        return orgDataStore(orgId).get(countQuery) > 0;
    }
}