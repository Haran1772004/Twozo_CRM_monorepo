package com.tlc.iam.server.internal.api.manager;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.attachment.model.profilePicture.ProfilePicture;
import com.tlc.commons.code.ErrorCode;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.common.models.Organization;
import com.tlc.iam.common.models.UserProfilePicture;
import com.tlc.iam.common.models.User;
import com.tlc.iam.server.internal.dropdown.SearchCriteria;
import com.tlc.iam.server.internal.status.IamServerErrorCodes;
import com.tlc.iam.server.sql.resource.MIAMOPROFILEPICTURE;
import com.tlc.iam.server.sql.resource.MIAMORGANIZATION;
import com.tlc.iam.server.sql.resource.MIAMOUSER;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.sql.common.SQLService;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A manager that manages the {@link User} related DB actions
 *
 * @author Selvakumar
 * @version 1.0
 */
public class UserManager extends AbstractIamManager<User> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserManager.class);

    private final AdminDataStore adminDataStore;
    private final SQLService sqlService;

    private static class Instance {
        private static final UserManager INSTANCE = new UserManager();
    }

    private UserManager() {
        sqlService = SqlServiceProvider.get();
        adminDataStore = sqlService.getAdminDataStore();
    }

    public static UserManager getInstance() {
        return Instance.INSTANCE;
    }

    private OrgDataStore orgDataStore(final Long orgId) {
        return sqlService.getOrgDataStore(orgId);
    }

    /**
     * Fetches IAM User table
     *
     * @return IAM user table
     */
    protected final Table getTable() {
        return Table.get(MIAMOUSER.TABLE);
    }

    /**
     * Fetches IAM User table
     *
     * @return IAM user table
     */
    private Table getProfilePictureTable() {
        return Table.get(MIAMOPROFILEPICTURE.TABLE);
    }

    /**
     * Fetches a user by using user id and org id
     *
     * @param userId User id
     * @param orgId  Org id
     * @return {@link User} entity
     */
    public User getUser(final long userId, final long orgId) {
        final Table userTable = getTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(userTable, new WhereClause(Criteria.eq(userTable.getColumn(MIAMOUSER.ID), userId)));
        final Row row = dataContainer.getRow(userTable, userId);

        return Objects.isNull(row) ? null : convertRowToUser(row);
    }

    /**
     * Checks whether any user account exists with the specified email address
     *
     * @param email Email address
     * @return True, if user account exists
     */
    public boolean isEmailAddressExists(final String email) {
        return isEmailAddressExists(email, null);
    }

    /**
     * <p>
     * Checks whether the given email address exists for any user
     * </p>
     *
     * @param email the email address to verify
     * @return true if an active user exists with the specified email address; false otherwise
     */
    public boolean isActiveUserEmailExists(final String email) {
        return isEmailAddressExists(email, new WhereClause(Criteria.eq(getTable().getColumn(MIAMOUSER.STATE), User.State.ACTIVE.id())));
    }

    /**
     * Checks whether any user account exists with the specified mobile
     *
     * @param mobile Mobile number
     * @return True, if user mobile already exists
     */
    public boolean isMobileExists(final String mobile) {
        final Table userTable = getTable();
        final CountQuery countQuery = CountQuery.get(userTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(userTable.getColumn(MIAMOUSER.MOBILE_NUMBER), mobile)));

        return adminDataStore.get(countQuery) > 0;
    }

    /**
     * Fetches a user by using email id
     *
     * @param orgId Org id
     * @param email Email address
     * @return {@link User} entity
     */
    public User getUserByEmail(final long orgId, final String email) {
        final Table userTable = getTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(userTable, new WhereClause(Criteria.eq(userTable.getColumn(MIAMOUSER.EMAIL), email)));
        final Row row = dataContainer.getRow(userTable);

        return Objects.isNull(row) ? null : convertRowToUser(row);
    }

    /**
     * Fetches a user by using email id
     *
     * @param email Email address
     * @return {@link User} entity
     */
    public User getUserByEmail(final String email) {
        final Table userTable = getTable();
        final DataContainer dataContainer = adminDataStore.get(userTable, new WhereClause(Criteria.eq(userTable.getColumn(MIAMOUSER.EMAIL), email)));
        final Row row = dataContainer.getRow(userTable);

        return Objects.isNull(row) ? null : convertRowToUser(row);
    }

    /**
     * Fetches a user details by mobile number
     *
     * @param orgId        Org id
     * @param mobileNumber Mobile number
     * @return {@link User} entity
     */
    public User getUserByMobileNumber(final long orgId, final String mobileNumber) {
        final Table userTable = getTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(userTable, new WhereClause(Criteria.eq(userTable.getColumn(MIAMOUSER.MOBILE_NUMBER), mobileNumber)));
        final Row row = dataContainer.getRow(userTable);

        return Objects.isNull(row) ? null : convertRowToUser(row);
    }

    /**
     * Fetches a user by mobile number across all organizations.
     *
     * @param mobileNumber the mobile number to search for
     * @return the {@link User} entity, or null if not found
     */
    public User getUserByMobileNumber(final String mobileNumber) {
        final Table userTable = getTable();
        final DataContainer dataContainer = adminDataStore.get(userTable, new WhereClause(Criteria.contains(userTable.getColumn(MIAMOUSER.MOBILE_NUMBER), mobileNumber)));
        final Row row = dataContainer.getRow(userTable);

        return Objects.isNull(row) ? null : convertRowToUser(row);
    }

    /**
     * Fetches a user details by mobile number
     *
     * @param orgId         Org id
     * @param mobileNumbers Mobile numbers
     * @return {@link User} entity
     */
    public Collection<User> getUsersByMobileNumbers(final long orgId, final Collection<String> mobileNumbers) {
        final Table userTable = getTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(userTable, new WhereClause(Criteria.in(userTable.getColumn(MIAMOUSER.MOBILE_NUMBER), mobileNumbers)));

        if (dataContainer.isEmpty()) {
            return Collections.emptyList();
        }

        return dataContainer.getRows(userTable).map(this::convertRowToUser).toList();
    }

    /**
     * Fetches a user by mobile number across all organizations.
     *
     * @param mobileNumbers     the mobile numbers to search for
     * @return the {@link User} entity, or null if not found
     */
    public Collection<User> getUsersByMobileNumbers(final Collection<String> mobileNumbers) {
        final Table userTable = getTable();
        final DataContainer dataContainer = adminDataStore.get(userTable, new WhereClause(Criteria.in(userTable.getColumn(MIAMOUSER.MOBILE_NUMBER), mobileNumbers)));

        if (dataContainer.isEmpty()) {
            return Collections.emptyList();
        }

        return dataContainer.getRows(userTable).map(this::convertRowToUser).toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public User get(final Long id) {
        Objects.requireNonNull(id);

        final Table userTable = getTable();
        final Row userRow = adminDataStore.get(userTable, id);

        return Objects.isNull(userRow) ? null : convertRowToUser(userRow);
    }

    /**
     * <p>
     * Gets the {@link User} details based on the org and user id
     * </p>
     *
     * @param orgId Represents an organization id
     * @param id    Represents the user id
     * @return The {@link User} details
     */
    public User get(final Long orgId, final Long id) {
        final Table userTable = getTable();
        final Table orgTable = OrganizationManager.getInstance().getOrgTable();
        final SelectQuery selectQuery = SelectQuery.get(userTable);
        final Table profilePictureTable = getProfilePictureTable();
        final Column userPkColumn = userTable.getPKColumn();

        selectQuery.addJoinClause(List.of(new JoinClause(userTable.getColumn(MIAMOUSER.ORG_ID), orgTable.getPKColumn()),
                new JoinClause(userPkColumn, profilePictureTable.getColumn(MIAMOPROFILEPICTURE.USER_ID), JoinClause.JoinType.LEFT)));
        selectQuery.addSelectClause(userTable, orgTable, profilePictureTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(userPkColumn, id)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row row = dataContainer.getRow(userTable);

        return Objects.isNull(row) ? null : convertRowToModel(row, dataContainer);
    }

    /**
     * <p>
     * Checks whether the user email and password are exists or not
     * </p>
     *
     * @param user Represents the {@link User} details
     * @return If the user is exists it will return true, otherwise it will return fasle
     */
    public boolean exists(final User user) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), user.id())).and(Criteria.eq(table
                .getColumn(MIAMOUSER.EMAIL), user.getEmail())));

        return orgDataStore(user.orgId()).get(countQuery) > 0;
    }

    /**
     * <p>
     * Converts the row to {@link User}
     * </p>
     *
     * @param row           Represents the user row
     * @param dataContainer A container that holds the user defined field values and system values
     * @return The {@link User} details
     */
    private User convertRowToModel(final Row row, final DataContainer dataContainer) {
        final OrganizationManager organizationManager = OrganizationManager.getInstance();
        final User user = new User();
        final Short state = row.get(MIAMOUSER.STATE);
        final Row profilePictureRow = dataContainer.getRow(getProfilePictureTable());

        user.setFirstName(row.get(MIAMOUSER.FIRST_NAME));
        user.setLastName(row.get(MIAMOUSER.LAST_NAME));
        user.setMobile(row.get(MIAMOUSER.MOBILE_NUMBER));
        user.setCreatedTime(row.get(MIAMOUSER.CREATED_TIME));
        user.setId(row.getPKValue());
        user.setType(User.Type.valueOf((short) row.get(MIAMOUSER.USER_TYPE)));
        user.setLocaleString(row.get(MIAMOUSER.LOCALE));
        user.setOrgId(row.getOrgId());
        user.setEmail(row.get(MIAMOUSER.EMAIL));
        user.setJobTitle(row.get(MIAMOUSER.JOB_TITLE));
        user.setOrganization(organizationManager.convertRowToOrg(dataContainer.getRow(organizationManager.getOrgTable())));
        user.setLastLoginTime(row.get(MIAMOUSER.LAST_LOGIN_TIME));

        if (Objects.nonNull(state)) {
            user.setUserState(User.State.get(state));
        }

        if (Objects.nonNull(profilePictureRow)) {
            user.setThumbnail(convertRowToThumbnailPicture(profilePictureRow));
            user.setOriginalPicture(convertRowToOriginalPicture(profilePictureRow));
        }

        return user;
    }

    /**
     * <p>
     * Converts the {@link Row} to {@link DisplayPicture}
     * </p>
     *
     * @param profilePictureRow Represents the {@link Row} of profile picture
     * @return the {@link DisplayPicture}
     */
    private DisplayPicture convertRowToThumbnailPicture(final Row profilePictureRow) {
        final DisplayPicture displayPicture = new DisplayPicture(ProfilePicture.Quality.THUMBNAIL);

        displayPicture.setFileId(profilePictureRow.get(MIAMOPROFILEPICTURE.THUMBNAIL_FILE_ID));
        displayPicture.setSize(profilePictureRow.get(MIAMOPROFILEPICTURE.THUMBNAIL_SIZE));
        displayPicture.setExtension(ProfilePicture.Extension.get((short) profilePictureRow.get(MIAMOPROFILEPICTURE.ORIGINAL_EXTENSION)));

        return displayPicture;
    }

    /**
     * <p>
     * Converts the {@link Row} to {@link DisplayPicture}
     * </p>
     *
     * @param profilePictureRow Represents the {@link Row} of profile picture
     * @return the {@link DisplayPicture}
     */
    private DisplayPicture convertRowToOriginalPicture(final Row profilePictureRow) {
        final DisplayPicture displayPicture = new DisplayPicture(ProfilePicture.Quality.ORIGINAL);

        displayPicture.setFileId(profilePictureRow.get(MIAMOPROFILEPICTURE.ORIGINAL_FILE_ID));
        displayPicture.setSize(profilePictureRow.get(MIAMOPROFILEPICTURE.ORIGINAL_SIZE));
        displayPicture.setExtension(ProfilePicture.Extension.get((short) profilePictureRow.get(MIAMOPROFILEPICTURE.ORIGINAL_EXTENSION)));

        return displayPicture;
    }

    /**
     * <p>
     * Fetches a collection of users by using user id
     * </p>
     *
     * @param orgId         Represents the org id
     * @param ids           Collection of ids that have to be gets the user details
     * @param isOrgRequired A flag indicating whether organization information is required or not for each user
     * @param stateId       Represents the user state id
     * @return The collection of {@link User} entity
     */
    public Collection<User> getUsers(final Long orgId, final Collection<Long> ids, final boolean isOrgRequired, final Short stateId) {
        final Table table = getTable();
        final Table profilePictureTable = getProfilePictureTable();
        final Table orgTable = OrganizationManager.getInstance().getOrgTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Column stateColumn = table.getColumn(MIAMOUSER.STATE);
        final WhereClause whereClause = new WhereClause(Criteria.in(table.getPKColumn(), ids));

        if (isOrgRequired) {
            selectQuery.addSelectClause(orgTable.getColumn(MIAMORGANIZATION.NAME));
            selectQuery.addJoinClause(new JoinClause(table.getColumn(MIAMOUSER.ORG_ID), orgTable.getPKColumn()));
        }

        selectQuery.addSelectClause(List.of(table.getColumn(MIAMOUSER.FIRST_NAME), table.getColumn(MIAMOUSER.LAST_NAME),
                table.getColumn(MIAMOUSER.EMAIL), table.getColumn(MIAMOUSER.MOBILE_NUMBER), stateColumn,
                table.getColumn(MIAMOUSER.JOB_TITLE), profilePictureTable.getColumn(MIAMOPROFILEPICTURE.ID),
                table.getColumn(MIAMOUSER.ORG_ID), table.getColumn(MIAMOUSER.LAST_LOGIN_TIME)));
        selectQuery.addJoinClause(new JoinClause(table.getColumn(MIAMOUSER.ID), profilePictureTable.getColumn(MIAMOPROFILEPICTURE.USER_ID), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(Objects.nonNull(stateId) ? whereClause.and(new WhereClause(Criteria.eq(stateColumn, stateId))) : whereClause);
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Stream<Row> rows = dataContainer.getRows(table);

        return rows.filter(Objects::nonNull).map(row -> convertPartialRowToUser(row, dataContainer)).toList();
    }

    /**
     * <p>
     * Fetches a collection of users by using user id and emails
     * </p>
     *
     * @param ids    Collection of ids that have to be gets the user details
     * @param emails Collection of emails to get user details
     * @return The collection of {@link User} entity
     */
    public Collection<User> getUsers(final Collection<Long> ids, final Collection<String> emails) {
        final Table table = getTable();
        final Column emailColumn = table.getColumn(MIAMOUSER.EMAIL);
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(List.of(table.getColumn(MIAMOUSER.FIRST_NAME), table.getColumn(MIAMOUSER.LAST_NAME),
                table.getColumn(MIAMOUSER.MOBILE_NUMBER), emailColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), ids)).or(Criteria.in(emailColumn, emails)));
        final DataContainer dataContainer = adminDataStore.get(selectQuery);
        final Stream<Row> rows = dataContainer.getRows(table);

        return rows.filter(Objects::nonNull).map(this::convertPartialRowToUser).toList();
    }

    /**
     * <p>
     * Fetches a collection of users by using email addresses
     * </p>
     *
     * @param emails Represents the collection of emails
     * @return the collection of {@link User}
     */
    public Collection<User> getUsersByEmails(final Collection<String> emails) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(List.of(table.getColumn(MIAMOUSER.EMAIL), table.getColumn(MIAMOUSER.ORG_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(table.getColumn(MIAMOUSER.EMAIL), emails)));
        final DataContainer dataContainer = adminDataStore.get(selectQuery);
        final Stream<Row> rows = dataContainer.getRows(table);

        return rows.filter(Objects::nonNull).map(this::convertPartialRowToUser).toList();
    }

    /**
     * <p>
     * Checks whether the user has active password or not
     * </p>
     *
     * @param id   Represents the user ID
     * @return password active status
     */
    public boolean isPasswordActive(final long id) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table.getColumn(MIAMOUSER.HAS_ACTIVE_PASSWORD));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), id)));

        return adminDataStore.get(selectQuery).getRow(table).get(MIAMOUSER.HAS_ACTIVE_PASSWORD);
    }

    /**
     * <p>
     * Fetches a collection of users by using start and limit criteria
     * </p>
     *
     * @param orgId                     Represents an organization id
     * @param isPartial                 A flag is representing the email is to be fetched or not
     * @param start                     Represents the starting index of user to be fetched
     * @param limit                     Represents the maximum number of user to be fetched
     * @param criteria                  Represents the criteria type
     * @param excludeUserIds            Which contains collection of exclude user ids
     * @param isExcludeUserWithoutPhone A flag is representing the exclude users without phone number
     * @return The collection of {@link User} details
     */
    public Collection<User> getUsers(final Long orgId, final boolean isPartial, final int start, final int limit, final SearchCriteria criteria,
                                     final Short stateId, final boolean includeDeactivatedUser, final boolean excludeInvitedUser,
                                     final Collection<Long> excludeUserIds, final boolean isExcludeUserWithoutPhone) {
        final Table table = getTable();
        final Table profilePictureTable = getProfilePictureTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Column pkColumn = table.getPKColumn();
        final WhereClause whereClause = getCriteria(includeDeactivatedUser, excludeInvitedUser, isPartial, criteria);
        final WhereClause excludeUserCriteria = excludeUserIds.isEmpty() ? whereClause : WhereClauseUtil.connect(whereClause,
                new WhereClause(Criteria.notIn(pkColumn, excludeUserIds)), Connector.AND);
        final Collection<Column> columns = new HashSet<>();

        if (!isPartial) {
            columns.add(table.getColumn(MIAMOUSER.EMAIL));
        }
        final WhereClause phoneNumberCriteria = isExcludeUserWithoutPhone ? WhereClauseUtil.connect(excludeUserCriteria, new WhereClause(Criteria.notEq(table.getColumn(MIAMOUSER.MOBILE_NUMBER), null)), Connector.AND) : excludeUserCriteria;

        columns.addAll(List.of(table.getColumn(MIAMOUSER.FIRST_NAME), table.getColumn(MIAMOUSER.LAST_NAME), table.getColumn(MIAMOUSER.MOBILE_NUMBER),
                table.getColumn(MIAMOUSER.STATE), table.getColumn(MIAMOUSER.LAST_LOGIN_TIME), table.getColumn(MIAMOUSER.JOB_TITLE),
                profilePictureTable.getColumn(MIAMOPROFILEPICTURE.ID), table.getColumn(MIAMOUSER.ORG_ID)));
        selectQuery.addSelectClause(columns);
        selectQuery.addOrderByClause(new OrderByClause(pkColumn));
        selectQuery.setLimitClause(new LimitClause(start, limit));
        selectQuery.addJoinClause(new JoinClause(table.getColumn(MIAMOUSER.ID), profilePictureTable.getColumn(MIAMOPROFILEPICTURE.USER_ID), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(Objects.isNull(stateId) ? phoneNumberCriteria : WhereClauseUtil.connect(phoneNumberCriteria,
                new WhereClause(Criteria.eq(table.getColumn(MIAMOUSER.STATE), stateId)), Connector.AND));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(table).filter(Objects::nonNull).map(row -> convertPartialRowToUser(row, dataContainer)).toList();
    }

    /**
     * <p>
     * Fetches the users count based on the criteria
     * </p>
     *
     * @param orgId                     Represents an organization id
     * @param isPartial                 A flag is representing the email is to be fetched or not
     * @param criteria                  Represents the criteria type
     * @param stateId                   Represent the state id
     * @param includeDeactivatedUser    Represent whether to include the deactivated users or not
     * @param excludeInvitedUser        Represent whether to include the invited users or not
     * @param excludeUserIds            Which contains collection of exclude user ids
     * @param isExcludeUserWithoutPhone A flag is representing the exclude users without phone number
     * @return The collection of {@link User} details
     */
    public long getTotalCount(final Long orgId, final boolean isPartial, final SearchCriteria criteria,
                              final Short stateId, final boolean includeDeactivatedUser, final boolean excludeInvitedUser,
                              final Collection<Long> excludeUserIds, final boolean isExcludeUserWithoutPhone) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);
        final WhereClause whereClause = getCriteria(includeDeactivatedUser, excludeInvitedUser, isPartial, criteria);
        final WhereClause excludeUserCriteria = excludeUserIds.isEmpty() ? whereClause : WhereClauseUtil.connect(whereClause,
                new WhereClause(Criteria.notIn(table.getPKColumn(), excludeUserIds)), Connector.AND);
        final WhereClause phoneNumberCriteria = isExcludeUserWithoutPhone ? WhereClauseUtil.connect(excludeUserCriteria, new WhereClause(Criteria.notEq(table.getColumn(MIAMOUSER.MOBILE_NUMBER), null)), Connector.AND) : excludeUserCriteria;

        countQuery.setWhereClause(Objects.isNull(stateId) ? phoneNumberCriteria : WhereClauseUtil.connect(phoneNumberCriteria,
                new WhereClause(Criteria.eq(table.getColumn(MIAMOUSER.STATE), stateId)), Connector.AND));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Fetches the users count based on the criteria
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param criteria Represents the criteria type
     * @param stateId  Represent the state id
     * @return The collection of {@link User} details
     */
    public long getTotalCount(final Long orgId, final Short stateId, final SearchCriteria criteria) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(getCriteria(stateId, criteria));

        return orgDataStore(orgId).get(countQuery);
    }

     /**
      * <p>
      * Fetches the collection of users based on search criteria, state and pagination
      * </p>
      *
      * @param orgId     Represents an organization id
      * @param start     Represents the starting index of user to be fetched
      * @param limit     Represents the maximum number of user to be fetched
      * @param criteria  Represents the search criteria
      * @param stateId   Represents the user state ID
      * @return The collection of {@link User} details
      */
     public Collection<User> getUsers(final Long orgId, final int start, final int limit, final SearchCriteria criteria, final Short stateId, final Collection<Long> excludedUserIds) {
         final Table table = getTable();
         final SelectQuery selectQuery = SelectQuery.get(table);
         final Column pkColumn = table.getPKColumn();
         final WhereClause whereClause = getCriteria(stateId, criteria);

         selectQuery.addSelectClause(List.of(table.getColumn(MIAMOUSER.FIRST_NAME), table.getColumn(MIAMOUSER.LAST_NAME),
                 table.getColumn(MIAMOUSER.EMAIL), table.getColumn(MIAMOUSER.MOBILE_NUMBER), table.getColumn(MIAMOUSER.STATE),
                 table.getColumn(MIAMOUSER.ORG_ID)));
         selectQuery.addOrderByClause(new OrderByClause(pkColumn));
         selectQuery.setLimitClause(new LimitClause(start, limit));
         selectQuery.setWhereClause(Objects.nonNull(excludedUserIds) && !excludedUserIds.isEmpty()
                 ? whereClause.and(Criteria.notIn(pkColumn, excludedUserIds))
                 : whereClause);
         final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

         return dataContainer.getRows(table).filter(Objects::nonNull).map(row -> convertPartialRowToUser(row, dataContainer)).toList();
     }

    /**
     * <p>
     * Fetches a collection of users by using user id
     * </p>
     *
     * @param orgId         Represents the org id
     * @return The collection of {@link User} entity
     */
    public Collection<User> getUsers(final Long orgId, final Collection<Long> userIds) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Column pkColumn = table.getPKColumn();

        selectQuery.addSelectClause(List.of(table.getColumn(MIAMOUSER.FIRST_NAME), table.getColumn(MIAMOUSER.LAST_NAME),
                table.getColumn(MIAMOUSER.EMAIL), table.getColumn(MIAMOUSER.MOBILE_NUMBER), table.getColumn(MIAMOUSER.STATE),
                table.getColumn(MIAMOUSER.ORG_ID)));
        selectQuery.addOrderByClause(new OrderByClause(pkColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(pkColumn, userIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(table).filter(Objects::nonNull).map(row -> convertPartialRowToUser(row, dataContainer)).toList();
    }

    /**
     * <p>
     * Constructs a {@link WhereClause} based on the provided criteria for fetching users.
     * </p>
     *
     * @param includeDeactivatedUser A boolean indicating whether to include users who are deactivated.
     * @param excludeInvitedUser     A boolean indicating whether to exclude invited users from the results.
     * @param isPartial              A boolean indicating whether the search should allow partial matches.
     * @param criteria               An instance of {@link SearchCriteria} containing the search criteria to be applied.
     * @return A {@link WhereClause} representing the constructed criteria for querying users.
     */
    private WhereClause getCriteria(final boolean includeDeactivatedUser, final boolean excludeInvitedUser, final boolean isPartial,
                                    final SearchCriteria criteria) {
        final Table table = getTable();
        final Column stateColumn = table.getColumn(MIAMOUSER.STATE);
        final Column firstNameColumn = table.getColumn(MIAMOUSER.FIRST_NAME);
        final Column lastNameColumn = table.getColumn(MIAMOUSER.LAST_NAME);
        final WhereClause whereClause = new WhereClause(criteria.build(firstNameColumn)).or(criteria.build(lastNameColumn))
                .or(criteria.build(firstNameColumn.concatWithTrim(lastNameColumn.trim()))).or(criteria.build(table.getColumn(MIAMOUSER.EMAIL)));
        final WhereClause deactivatedCondition = includeDeactivatedUser ? whereClause :
                WhereClauseUtil.connect(whereClause, new WhereClause(Criteria.notEq(stateColumn, User.State.DEACTIVATED.id())), Connector.AND);

        return excludeInvitedUser ? WhereClauseUtil.connect(deactivatedCondition,
                new WhereClause(Criteria.notEq(stateColumn, User.State.INVITED.id())), Connector.AND) : deactivatedCondition;
    }

    /**
     * <p>
     * Constructs a {@link WhereClause} based on the provided criteria for fetching users.
     * </p>
     * @param stateId Represents the state id
     * @param criteria               An instance of {@link SearchCriteria} containing the search criteria to be applied.
     * @return A {@link WhereClause} representing the constructed criteria for querying users.
     */
    private WhereClause getCriteria(final Short stateId, final SearchCriteria criteria) {
        final Table table = getTable();
        final Column firstNameColumn = table.getColumn(MIAMOUSER.FIRST_NAME);
        final Column lastNameColumn = table.getColumn(MIAMOUSER.LAST_NAME);
        final WhereClause whereClause = Objects.isNull(criteria) ? null
                : new WhereClause(criteria.build(firstNameColumn)).or(criteria.build(lastNameColumn)).
                or(criteria.build(firstNameColumn.concatWithTrim(lastNameColumn.trim()))).or(criteria.build(table.getColumn(MIAMOUSER.EMAIL)));

        return Objects.isNull(stateId) ? whereClause : WhereClauseUtil.connect(whereClause,
                new WhereClause(Criteria.eq(table.getColumn(MIAMOUSER.STATE), stateId)), Connector.AND);
    }

    /**
     * <p>
     * Fetches the collection of users based on user state
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param start     Represents the starting index of user to be fetched
     * @param limit     Represents the maximum number of user to be fetched
     * @param stateType Represents the user state type
     * @return The collection of {@link User} details
     */
    public Collection<User> getUsers(final Long orgId, final int start, final int limit, final int stateType) {
        final Table table = getTable();
        final Column userStateColumn = table.getColumn(MIAMOUSER.STATE);
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(List.of(table.getColumn(MIAMOUSER.FIRST_NAME), table.getColumn(MIAMOUSER.LAST_NAME),
                table.getColumn(MIAMOUSER.EMAIL), userStateColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(userStateColumn, stateType)));
        selectQuery.setLimitClause(new LimitClause(start, limit));

        return orgDataStore(orgId).get(selectQuery).getRows(table).map(this::convertPartialRowToUser).toList();
    }

    /**
     * Fetches the users by the provided names
     *
     * @param orgId     Represents the organization ID
     * @param userNames Which contains the names of the users
     * @return The {@link Collection} of users that matched with the provided names
     */
    public Collection<User> getUsersByNames(final long orgId, final Collection<String> userNames) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(List.of(table.getColumn(MIAMOUSER.FIRST_NAME), table.getColumn(MIAMOUSER.ORG_ID)));
        /*
            We're only getting a single name from the client's end. Likewise, we have certain scenarios where we need to locate users based on their names.
            Therefore, I'm conducting searches for users using the FIRST_NAME column.
         */
        selectQuery.setWhereClause(new WhereClause(Criteria.in(table.getColumn(MIAMOUSER.FIRST_NAME), userNames))
                .and(Criteria.eq(table.getColumn(MIAMOUSER.ORG_ID), orgId)));
        final DataContainer dataContainer = adminDataStore.get(selectQuery);
        final Stream<Row> rows = dataContainer.getRows(table);

        return rows.filter(Objects::nonNull).map(this::convertPartialRowToUser).toList();
    }

    /**
     * <p>
     * Converts the partial row to model
     * </p>
     *
     * @param row Represents the user row
     * @return The {@link User} entity
     */
    private User convertPartialRowToUser(final Row row) {
        final User user = new User();
        final Short state = row.get(MIAMOUSER.STATE);

        user.setOrgId(row.getOrgId());
        user.setId(row.getPKValue());
        user.setEmail(row.get(MIAMOUSER.EMAIL));
        user.setFirstName(row.get(MIAMOUSER.FIRST_NAME));
        user.setLastName(row.get(MIAMOUSER.LAST_NAME));
        user.setMobile(row.get(MIAMOUSER.MOBILE_NUMBER));
        user.setJobTitle(row.get(MIAMOUSER.JOB_TITLE));
        user.setLastLoginTime(row.get(MIAMOUSER.LAST_LOGIN_TIME));

        if (Objects.nonNull(state)) {
            user.setUserState(User.State.get(state));
        }

        return user;
    }

    /**
     * {@inheritDoc}
     */
    public User convertRowToUser(final Row row) {
        final User user = new User(row.getPKValue(), row.getOrgId(), row.get(MIAMOUSER.FIRST_NAME), row.get(MIAMOUSER.LAST_NAME),
                row.get(MIAMOUSER.EMAIL), row.get(MIAMOUSER.MOBILE_NUMBER), row.get(MIAMOUSER.LOCALE), row.get(MIAMOUSER.PASSWORD),
                User.Type.valueOf((short) row.get(MIAMOUSER.USER_TYPE)));
        final Short state = row.get(MIAMOUSER.STATE);

        user.setPassword(row.get(MIAMOUSER.PASSWORD));
        user.setEmailVerified(row.get(MIAMOUSER.IS_EMAIL_VERIFIED));
        user.setMobileNumberVerified(row.get(MIAMOUSER.IS_MOBILE_NUMBER_VERIFIED));
        user.setUserState(Objects.nonNull(state) ? User.State.get(state) : User.State.ACTIVE);
        user.setCreatedTime(row.get(MIAMOUSER.CREATED_TIME));
        user.setStateUpdatedTime(row.get(MIAMOUSER.STATE_UPDATED_TIME));
        user.setLastLoginTime(row.get(MIAMOUSER.LAST_LOGIN_TIME));
        user.setJobTitle(row.get(MIAMOUSER.JOB_TITLE));
        user.setIdpUserId(row.get(MIAMOUSER.IDP_USER_ID));
        user.setHasActivePassword(row.get(MIAMOUSER.HAS_ACTIVE_PASSWORD));

        return user;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final User user) {
        final DataContainer dataContainer = DataContainer.create();

        loadUserIntoContainer(true, user, null, dataContainer);
        orgDataStore(user.orgId()).commitChanges(dataContainer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> update(final Collection<User> users) {
        final User firstUser = users.stream().findFirst().get();
        final Map<String, Object> response = new HashMap<>();
        final List<Map<String, Object>> failedEntities = new ArrayList<>();
        final List<Long> updatedRecords = new ArrayList<>();
        final Table userTable = getTable();
        final DataContainer sourceContainer = orgDataStore(firstUser.orgId()).get(userTable, new WhereClause(Criteria.in(userTable.getColumn(MIAMOUSER.ID),
                users.stream().map(User::id).filter(Objects::nonNull).toList())));

        for (final User user : users) {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadUserIntoContainer(false, user, sourceContainer, targetContainer);
                orgDataStore(user.orgId()).commitChanges(targetContainer);
                updatedRecords.add(user.id());
            } catch (final ErrorCode errorCode) {
                final Map<String, Object> entity = new HashMap<>();

                entity.put(UserFields.ID, user.id());
                entity.put(IamFields.REASON, "i18n.iam.accounts.user.update.failed");
                failedEntities.add(entity);
            }
        }

        response.put(IamFields.UPDATED_RECORDS, updatedRecords);
        response.put(IamFields.FAILED_RECORDS, failedEntities);
        return response;
    }

    /**
     * Loads the user details into target container
     *
     * @param isCreateEvent   A flag that indicates whether an action is related to create event or update event
     * @param user            An entity that holds user information
     * @param sourceContainer A container that holds the details of exiting users
     * @param targetContainer A container that holds the updated details of user
     */
    public void loadUserIntoContainer(final boolean isCreateEvent, final User user, final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table userTable = getTable();
        final Row userRow = isCreateEvent ? new Row(userTable) : sourceContainer.getRow(userTable, user.id());
        final User.State userState = user.getUserState();

        if (!isCreateEvent && Objects.isNull(userRow)) {
            throw ErrorCode.get(IamServerErrorCodes.IAM_USER_NOT_FOUND);
        }

        userRow.set(MIAMOUSER.FIRST_NAME, user.getFirstName());
        userRow.set(MIAMOUSER.LAST_NAME, user.getLastName());
        userRow.set(MIAMOUSER.MOBILE_NUMBER, user.getMobile());
        userRow.set(MIAMOUSER.LOCALE, user.getLocaleString());
        userRow.set(MIAMOUSER.USER_TYPE, user.getType().id());
        userRow.set(MIAMOUSER.IS_EMAIL_VERIFIED, user.isEmailVerified());
        userRow.set(MIAMOUSER.IS_MOBILE_NUMBER_VERIFIED, user.isMobileNumberVerified());
        userRow.set(MIAMOUSER.STATE_UPDATED_TIME, Instant.now().toEpochMilli());
        userRow.set(MIAMOUSER.JOB_TITLE, user.getJobTitle());
        userRow.set(MIAMOUSER.IDP_USER_ID, user.getIdpUserId());
        userRow.set(MIAMOUSER.HAS_ACTIVE_PASSWORD, user.hasActivePassword());

        if (Objects.nonNull(userState)) {
            userRow.set(MIAMOUSER.STATE, userState.id());
        }

        if (userRow.isNewRow()) {
            userRow.set(MIAMOUSER.CREATED_TIME, Instant.now().toEpochMilli());
            userRow.set(MIAMOUSER.PASSWORD, user.getPasswordHash());
            userRow.set(MIAMOUSER.EMAIL, user.getEmail());
            userRow.setOrgId(user.orgId());
            user.setId(userRow.getPKValue());
        }

        targetContainer.updateRow(userRow);
    }

    /**
     * Updates new password for the user
     *
     * @param email        Represents the email
     * @param hashPassword hash value of given password
     */
    public boolean updatePassword(final String email, final String hashPassword) {
        final Table userTable = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(userTable);

        updateQuery.addUpdateColumn(userTable.getColumn(MIAMOUSER.PASSWORD), hashPassword);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(userTable.getColumn(MIAMOUSER.EMAIL), email)));

        try {
            adminDataStore.update(updateQuery);

            return true;
        } catch (ErrorCode errorCode) {
            return false;
        }
    }

    /**
     * Gets the user password by using given email
     *
     * @param email represents the email address
     * @return {@link String}
     */
    public String getUserPasswordByEmail(final String email) {
        //Todo active user check
        final Table userTable = getTable();

        return adminDataStore.get(userTable, new WhereClause(Criteria.eq(userTable.getColumn(MIAMOUSER.EMAIL), email)),
                List.of(userTable.getColumn(MIAMOUSER.PASSWORD))).getRow(userTable).get(MIAMOUSER.PASSWORD);
    }

    /**
     * <p>
     * Deactivates the user
     * </p>
     *
     * @param id Represents the id of user
     * @return {@link Boolean} status of deactivation of user
     */
    public boolean deactivate(final Long id) {
        final Table table = getTable();
        final Row row = adminDataStore.get(getTable(), id);

        if (Objects.isNull(row)) {
            throw ErrorCode.get(IamServerErrorCodes.IAM_USER_NOT_FOUND);
        }

        if (row.get(MIAMOUSER.STATE).equals(User.State.ACTIVE.id())) {
            final UpdateQuery updateQuery = UpdateQuery.get(table);

            updateQuery.addUpdateColumn(table.getColumn(MIAMOUSER.STATE_UPDATED_TIME), Instant.now().toEpochMilli());
            updateQuery.addUpdateColumn(table.getColumn(MIAMOUSER.STATE), User.State.DEACTIVATED.id());
            updateQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), id)));

            try {
                orgDataStore(row.getOrgId()).update(updateQuery);
                return true;
            } catch (final Exception exception) {
                LOGGER.debug("Stacktrace", exception);
                return false;
            }
        }

        return false;
    }

    /**
     * <p>
     * Checks whether a mobile number exists for a user within a given organization
     * </p>
     *
     * @param userId Represents the user id
     * @param mobile Represents the user mobile
     * @param orgId  Represents the organization id
     * @return true If the mobile number exists, otherwise it will return false
     */
    public boolean isMobileExists(final Long orgId, final Long userId, final String mobile) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(MIAMOUSER.MOBILE_NUMBER), mobile))
                .and(Criteria.notEq(table.getColumn(MIAMOUSER.ID), userId)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * Saves the {@link UserProfilePicture} information of a user to the database
     *
     * @param userProfilePicture Holds the profile picture details
     */
    public void saveProfilePictureInfo(final UserProfilePicture userProfilePicture) {
        final long orgId = userProfilePicture.orgId();
        final long userId = userProfilePicture.userId();
        final Table profilePictureTable = getProfilePictureTable();
        final Row row = getProfilePictureRow(orgId, userId);
        final Row profilePicRow = Objects.isNull(row) ? new Row(profilePictureTable) : row;
        final DataContainer targetContainer = DataContainer.create();
        final DisplayPicture originalPicture = (DisplayPicture) userProfilePicture.getOriginalPicture();
        final DisplayPicture thumbnail = (DisplayPicture) userProfilePicture.getThumbnail();

        profilePicRow.set(MIAMOPROFILEPICTURE.ORIGINAL_SIZE, originalPicture.size());
        profilePicRow.set(MIAMOPROFILEPICTURE.ORIGINAL_EXTENSION, originalPicture.extension().getId());
        profilePicRow.set(MIAMOPROFILEPICTURE.THUMBNAIL_SIZE, thumbnail.size());
        profilePicRow.set(MIAMOPROFILEPICTURE.ORIGINAL_FILE_ID, userProfilePicture.getOriginalFileId());
        profilePicRow.set(MIAMOPROFILEPICTURE.THUMBNAIL_FILE_ID, userProfilePicture.getThumbnailFileId());

        if (profilePicRow.isNewRow()) {
            profilePicRow.set(MIAMOPROFILEPICTURE.USER_ID, userId);
            profilePicRow.setOrgId(orgId);

            userProfilePicture.setId(profilePicRow.getPKValue());
        }

        targetContainer.updateRow(profilePicRow);
        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * Fetches the user profile picture row from the data-container
     *
     * @param orgId  Organization identifier
     * @param userId User identifier
     * @return A row of user profile picture
     */
    private Row getProfilePictureRow(final long orgId, final long userId) {
        final Table profilePictureTable = getProfilePictureTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(profilePictureTable, new WhereClause(Criteria.eq(profilePictureTable.getColumn(MIAMOPROFILEPICTURE.USER_ID), userId)));

        return dataContainer.getRow(profilePictureTable);
    }

    /**
     * Deletes the profile picture information
     *
     * @param orgId  Organization identifier
     * @param userId User identifier
     */
    public void deleteProfilePictureInfo(final long orgId, final long userId) {
        final Table profilePictureTable = getProfilePictureTable();
        final DeleteQuery deleteQuery = DeleteQuery.get(profilePictureTable);

        deleteQuery.setWhereClause(new WhereClause(Criteria.eq(profilePictureTable.getColumn(MIAMOPROFILEPICTURE.USER_ID), userId)));
        orgDataStore(orgId).delete(deleteQuery);
    }

    /**
     * Fetches the information of the specified display picture type
     *
     * @param orgId        Organization identifier
     * @param userId       User identifier
     * @param imageQuality Fetches the type of image has to be fetched
     * @return DisplayPicture information
     */
    public Optional<DisplayPicture> getDisplayPicture(final long orgId, final long userId, final ProfilePicture.Quality imageQuality) {
        final DisplayPicture displayPicture = switch (imageQuality) {
            case THUMBNAIL -> getThumbnailInfo(orgId, userId);
            case ORIGINAL -> getOriginalPicInfo(orgId, userId);
        };

        return Optional.ofNullable(displayPicture);
    }

    /**
     * <p>
     * Fetches the collection of {@link User}
     * </p>
     *
     * @param orgId                  Represents an org id
     * @param isPartial              Indicates whether to retrieve a partial set of user details
     * @param start                  The starting index for the inner query pagination
     * @param limit                  The maximum number of users to retrieve in the inner query
     * @param criteria               The search criteria to filter users
     * @param stateId                The state ID to filter users by status
     * @param includeDeactivatedUser Whether to include deactivated users in the results
     * @param excludeInvitedUser     Whether to exclude users who are only invited but not yet active
     * @param isSearchUser           A flag indicating whether specific users should be explicitly included in the result set
     * @param searchUserIds          A collection of specific user IDs that must be included in the result
     * @return The collection of {@link User}
     */
    public Collection<User> getUsers(final Long orgId, final boolean isPartial, final int start, final int limit, final SearchCriteria criteria,
                                     final Short stateId, final boolean includeDeactivatedUser, final boolean excludeInvitedUser, final boolean isSearchUser, final Collection<Long> searchUserIds) {
        final Table table = getTable();
        final Table profilePictureTable = getProfilePictureTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final SelectQuery innerSelectQuery = SelectQuery.get(table);
        final WhereClause whereClause = getCriteria(includeDeactivatedUser, excludeInvitedUser, false, criteria);
        final Column pkColumn = table.getPKColumn();
        final boolean hasNoSearchUserIds = searchUserIds.isEmpty();
        final WhereClause stateCriteria = Objects.isNull(stateId) ? whereClause : WhereClauseUtil.connect(whereClause,
                new WhereClause(Criteria.eq(table.getColumn(MIAMOUSER.STATE), stateId)), Connector.AND);
        final WhereClause userIdCriteria = new WhereClause(Criteria.in(pkColumn, innerSelectQuery));
        final WhereClause searchUserCriteria = isSearchUser ? hasNoSearchUserIds ? userIdCriteria : WhereClauseUtil.connect(new WhereClause(Criteria.in(pkColumn,
                searchUserIds)).and(stateCriteria), userIdCriteria, Connector.OR) : userIdCriteria;

        innerSelectQuery.addSelectClause(pkColumn);
        innerSelectQuery.setWhereClause(hasNoSearchUserIds ? stateCriteria : WhereClauseUtil.connect(stateCriteria, new WhereClause(Criteria.notIn(pkColumn, searchUserIds)), Connector.AND));
        innerSelectQuery.addOrderByClause(new OrderByClause(pkColumn));
        innerSelectQuery.setLimitClause(new LimitClause(start, limit));
        final Collection<Column> columns = new HashSet<>();

        if (!isPartial) {
            columns.add(table.getColumn(MIAMOUSER.EMAIL));
        }
        columns.addAll(List.of(table.getColumn(MIAMOUSER.FIRST_NAME), table.getColumn(MIAMOUSER.LAST_NAME), table.getColumn(MIAMOUSER.STATE), table.getColumn(MIAMOUSER.LAST_LOGIN_TIME),
                profilePictureTable.getColumn(MIAMOPROFILEPICTURE.ID), table.getColumn(MIAMOUSER.ORG_ID)));
        selectQuery.addSelectClause(columns);
        selectQuery.addJoinClause(new JoinClause(table.getColumn(MIAMOUSER.ID), profilePictureTable.getColumn(MIAMOPROFILEPICTURE.USER_ID), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(searchUserCriteria);
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(table).filter(Objects::nonNull).map(row -> convertPartialRowToUser(row, dataContainer)).toList();
    }

    /**
     * Returns the original profile picture's information
     *
     * @param orgId  Organization identifier
     * @param userId User identifier
     * @return Original picture
     */
    private DisplayPicture getOriginalPicInfo(final long orgId, final long userId) {
        final Row row = getProfilePictureRow(orgId, userId);

        if (Objects.isNull(row)) {
            return null;
        } else {
            final DisplayPicture displayPicture = new DisplayPicture(ProfilePicture.Quality.ORIGINAL);

            displayPicture.setFileId(row.get(MIAMOPROFILEPICTURE.ORIGINAL_FILE_ID));
            displayPicture.setSize(row.get(MIAMOPROFILEPICTURE.ORIGINAL_SIZE));
            displayPicture.setExtension(ProfilePicture.Extension.get((short) row.get(MIAMOPROFILEPICTURE.ORIGINAL_EXTENSION)));

            return displayPicture;
        }
    }

    /**
     * Gets the display picture file id.
     *
     * @param orgId        Refers the organization id.
     * @param userId       Refers the user id.
     * @param imageQuality Refers the {@link ProfilePicture.Quality}.
     * @return The picture file id.
     */
    public final Long getDisplayPictureId(final long orgId, final long userId, final ProfilePicture.Quality imageQuality) {
        final Row row = getProfilePictureRow(orgId, userId);

        if (Objects.isNull(row)) {
            return null;
        } else {
            return switch (imageQuality) {
                case ORIGINAL -> row.get(MIAMOPROFILEPICTURE.ORIGINAL_FILE_ID);
                case THUMBNAIL -> row.get(MIAMOPROFILEPICTURE.THUMBNAIL_FILE_ID);
            };
        }
    }

    /**
     * Gets the display picture ids based on the given details.
     *
     * @param orgId  Represents an organization id.
     * @param userId Represents the user id.
     * @return The {@link DisplayPicture} ids.
     */
    public final Collection<Long> getDisplayPictureIds(final long orgId, final long userId) {
        final Row row = getProfilePictureRow(orgId, userId);

        return Objects.isNull(row) ? Collections.emptySet()
                : Set.of(row.get(MIAMOPROFILEPICTURE.ORIGINAL_FILE_ID), row.get(MIAMOPROFILEPICTURE.THUMBNAIL_FILE_ID));
    }

    /**
     * Returns the profile picture's thumbnail information
     *
     * @param orgId  Organization identifier
     * @param userId User identifier
     * @return Thumbnail of the profile picture
     */
    private DisplayPicture getThumbnailInfo(final long orgId, final long userId) {
        final Row row = getProfilePictureRow(orgId, userId);

        return Objects.isNull(row) ? null : convertRowToThumbnailPicture(row);
    }

    /**
     * <p>
     * Converts the partial row to model
     * </p>
     *
     * @param row           Represents the user row
     * @param dataContainer Represents the {@link DataContainer}
     * @return The {@link User} entity
     */
    private User convertPartialRowToUser(final Row row, final DataContainer dataContainer) {
        final OrganizationManager organizationManager = OrganizationManager.getInstance();
        final User user = new User();
        final Short state = row.get(MIAMOUSER.STATE);
        final Long orgId = row.getOrgId();
        final Long pkValue = row.getPKValue();
        final Row organizationRow = dataContainer.getRow(organizationManager.getOrgTable());

        user.setOrgId(orgId);
        user.setId(pkValue);
        user.setEmail(row.get(MIAMOUSER.EMAIL));
        user.setFirstName(row.get(MIAMOUSER.FIRST_NAME));
        user.setLastName(row.get(MIAMOUSER.LAST_NAME));
        user.setMobile(row.get(MIAMOUSER.MOBILE_NUMBER));
        user.setJobTitle(row.get(MIAMOUSER.JOB_TITLE));
        user.setLastLoginTime(row.get(MIAMOUSER.LAST_LOGIN_TIME));

        if (Objects.nonNull(organizationRow)) {
            final Organization organization = new Organization();

            organization.setId(organizationRow.getPKValue());
            organization.setName(organizationRow.get(MIAMORGANIZATION.NAME));
            user.setOrganization(organization);
        }

        if (Objects.nonNull(state)) {
            user.setUserState(User.State.get(state));
        }

        if (Objects.nonNull(dataContainer.getRow(getProfilePictureTable()))) {
            user.setThumbnail(getThumbnailInfo(orgId, pkValue));
            user.setOriginalPicture(getOriginalPicInfo(orgId, pkValue));
        }

        return user;
    }

    /**
     * <p>
     * Checks whether a mobile number exists for a user within a given organization
     * </p>
     *
     * @param userId Represents the user id
     * @param email  Represents the user email
     * @return true If the mobile number exists, otherwise it will return false
     */
    public boolean isEmailExists(final Long userId, final String email) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(MIAMOUSER.EMAIL), email))
                .and(Criteria.eq(table.getColumn(MIAMOUSER.ID), userId)));

        return adminDataStore.get(countQuery) > 0;
    }

    /**
     * <p>
     * Checks if given user is in the invited state or not
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param userIds Which contains the collection of user ids
     * @return If any user is in invited state it will return true, otherwise it will return false
     */
    public boolean isInvited(final Long orgId, final Collection<Long> userIds) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(MIAMOUSER.STATE), User.State.INVITED.id()))
                .and(Criteria.in(table.getColumn(MIAMOUSER.ID), userIds)));

        try {
            return orgDataStore(orgId).get(countQuery) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * <p>
     * Updates the user state to active state
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param userIds Which contains the collection of user ids
     * @param state   Represents the user state
     */
    public boolean updateState(final Long orgId, final Collection<Long> userIds, final User.State state) {
        final Table table = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(table);

        updateQuery.addUpdateColumn(table.getColumn(MIAMOUSER.STATE), state.id());
        updateQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), userIds)));

        try {
            orgDataStore(orgId).update(updateQuery);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * <p>
     * Updates the password and state by given user id
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents the user id
     * @param stateId  Represents the state id
     * @param password Represents the password
     */
    public void updatePasswordAndState(final long orgId, final long userId, final Short stateId, final String password) {
        final Table userTable = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(userTable);

        if (Objects.nonNull(stateId)) {
            updateQuery.addUpdateColumn(userTable.getColumn(MIAMOUSER.STATE), stateId);
        }
        updateQuery.addUpdateColumn(userTable.getColumn(MIAMOUSER.PASSWORD), password);
        updateQuery.addUpdateColumn(userTable.getColumn(MIAMOUSER.HAS_ACTIVE_PASSWORD),  true);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(userTable.getPKColumn(), userId)));
        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * <p>
     * Updates the account details of the organization.
     * </p>
     *
     * @param user Represents the {@link User}
     */
    public void updateAccountDetails(final User user) {
        final Organization organization = user.getOrganization();
        final Table userTable = getTable();
        final Table orgTable = Table.get(MIAMORGANIZATION.TABLE);
        final List<UpdateQuery> updateQueries = new ArrayList<>();
        final UpdateQuery userUpdateQuery = UpdateQuery.get(userTable);
        final UpdateQuery orgUpdateQuery = UpdateQuery.get(orgTable);

        userUpdateQuery.addUpdateColumn(userTable.getColumn(MIAMOUSER.FIRST_NAME), user.getFirstName());
        userUpdateQuery.addUpdateColumn(userTable.getColumn(MIAMOUSER.MOBILE_NUMBER), user.getMobile());
        userUpdateQuery.setWhereClause(new WhereClause(Criteria.eq(userTable.getPKColumn(), user.id())));
        updateQueries.add(userUpdateQuery);

        if (Objects.nonNull(organization)) {
            orgUpdateQuery.addUpdateColumn(orgTable.getColumn(MIAMORGANIZATION.NAME), organization.getName());
            orgUpdateQuery.setWhereClause(new WhereClause(Criteria.eq(orgTable.getPKColumn(), organization.id())));
            updateQueries.add(orgUpdateQuery);
        }
        orgDataStore(user.orgId()).update(updateQueries);
    }

    /**
     * <p>
     * Checks whether an email address exists
     * </p>
     *
     * @param email Represents the user email
     * @return true If the email exists, otherwise it will return false
     */
    public boolean isEmailExists(final String email) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(MIAMOUSER.EMAIL), email)));

        return adminDataStore.get(countQuery) > 0;
    }

    /**
     * <p>
     * Updates the user last login time.
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param userId        Represents the user id
     * @param lastLoginTime Represents the last login time of user
     */
    public void updateLastLoginTime(final Long orgId, final Long userId, final DateTime lastLoginTime) {
        final Table userTable = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(userTable);

        updateQuery.addUpdateColumn(userTable.getColumn(MIAMOUSER.LAST_LOGIN_TIME), lastLoginTime);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(userTable.getColumn(MIAMOUSER.ORG_ID), orgId))
                .and(Criteria.eq(userTable.getPKColumn(), userId)));
        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * <p>
     * Sets the ID provider user ID
     * </p>
     *
     * @param orgId             Represents an organization ID
     * @param userId            Represents the user ID
     * @param idpUserId         Represents the provider user ID
     */
    public void setIdpUserId(final Long orgId, final Long userId, final String idpUserId) {
        final Table userTable = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(userTable);

        updateQuery.addUpdateColumn(userTable.getColumn(MIAMOUSER.IDP_USER_ID), idpUserId);
        updateQuery.addUpdateColumn(userTable.getColumn(MIAMOUSER.HAS_ACTIVE_PASSWORD), true);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(userTable.getPKColumn(), userId)));
        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * <p>
     * Determines whether all specified users belonging to the given organization
     * are currently in an active state.
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param userIds Which contains collection of user ids
     * @return true if all provided users are active; false otherwise
     */
    public boolean areAllUsersActive(final long orgId, final Collection<Long> userIds) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), userIds)).and(Criteria
                .eq(table.getColumn(MIAMOUSER.STATE), User.State.ACTIVE.id())));

        return orgDataStore(orgId).get(countQuery) == userIds.size();
    }

    /**
     * <p>
     * Determines whether any of the specified users belonging to the given organization
     * are currently in a deactivated state.
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param userIds Which contains collection of user ids
     * @return true if at least one of the provided users is deactivated; false otherwise
     */
    public boolean hasAnyDeactivatedUser(final long orgId, final Collection<Long> userIds) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), userIds)).and(Criteria
                .eq(table.getColumn(MIAMOUSER.STATE), User.State.DEACTIVATED.id())));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * Checks whether any user account exists with the specified email address
     *
     * @param email       Email address
     * @param whereClause Represents the user criteria
     * @return True, if user account exists
     */
    private boolean isEmailAddressExists(final String email, final WhereClause whereClause) {
        final Table userTable = getTable();
        final CountQuery countQuery = CountQuery.get(userTable);
        final WhereClause emailCriteria = new WhereClause(Criteria.eq(userTable.getColumn(MIAMOUSER.EMAIL), email));

        countQuery.setWhereClause(Objects.isNull(whereClause) ? emailCriteria : WhereClauseUtil.connect(emailCriteria, whereClause, Connector.AND));

        return adminDataStore.get(countQuery) > 0;
    }

     /**
      * <p>
      * Groups the provided user IDs by their state
      * </p>
      *
      * @param orgId   Represents the organization ID
      * @param userIds Collection of user IDs to be grouped
      * @return A map with state ID as key and collection of user IDs as value
      */
     public Map<Short, Collection<Long>> groupUserIdsByState(final Long orgId, final Collection<Long> userIds) {
         final Table userTable = getTable();
         final SelectQuery selectQuery = SelectQuery.get(userTable);
         final Column userIdColumn = userTable.getPKColumn();

         selectQuery.addSelectClause(List.of(userIdColumn, userTable.getColumn(MIAMOUSER.STATE)));
         selectQuery.setWhereClause(new WhereClause(Criteria.in(userIdColumn, userIds)));

         return orgDataStore(orgId).get(selectQuery).getRows(userTable).collect(Collectors.groupingBy(row -> row.get(MIAMOUSER.STATE),
                 Collectors.mapping(Row::getPKValue, Collectors.toCollection(HashSet::new))));
     }

    /**
     * <p>
     * Returns the user id with their thumbnails
     * </p>
     *
     * @param orgId   Represents the organization ID
     * @param userIds Collection of user IDs to be grouped
     * @return A map with state ID as key and collection of user IDs as value
     */
    public Map<Long, DisplayPicture> getUsersThumbnails(final long orgId, final Collection<Long> userIds){
        final Table userTable = getTable();
        final Table profilePictureTable = getProfilePictureTable();
        final SelectQuery selectQuery = SelectQuery.get(userTable);

        selectQuery.addSelectClause(List.of(
                userTable.getPKColumn(),
                profilePictureTable.getColumn(MIAMOPROFILEPICTURE.THUMBNAIL_FILE_ID),
                profilePictureTable.getColumn(MIAMOPROFILEPICTURE.THUMBNAIL_SIZE),
                profilePictureTable.getColumn(MIAMOPROFILEPICTURE.ORIGINAL_EXTENSION)
        ));

        selectQuery.addJoinClause(
                new JoinClause(
                        userTable.getColumn(MIAMOUSER.ID),
                        profilePictureTable.getColumn(MIAMOPROFILEPICTURE.USER_ID),
                        JoinClause.JoinType.LEFT
                )
        );

        selectQuery.setWhereClause(
                new WhereClause(Criteria.in(userTable.getPKColumn(), userIds))
                        .and(Criteria.eq(userTable.getColumn(MIAMOUSER.STATE), User.State.ACTIVE.id()))
        );

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Map<Long, DisplayPicture> thumbnails = new HashMap<>();

        dataContainer.getRows(userTable).forEach(row -> {
            final Row profileRow = dataContainer.getRow(profilePictureTable);

            thumbnails.put(row.getPKValue(), Objects.nonNull(profileRow) ? convertRowToThumbnailPicture(profileRow) : null);
        });

        return thumbnails;
    }
 }