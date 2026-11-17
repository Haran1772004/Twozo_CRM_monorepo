package com.tlc.iam.server.internal.api.manager;

import com.tlc.commons.code.ErrorCode;
import com.tlc.iam.common.models.Organization;
import com.tlc.iam.common.models.Organization.IndustryType;
import com.tlc.iam.common.models.Organization.OrgSize;
import com.tlc.iam.common.models.Organization.UsersSize;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.status.IamErrorCodes;
import com.tlc.iam.server.internal.status.IamServerErrorCodes;
import com.tlc.iam.server.sql.resource.MIAMORGANIZATION;
import com.tlc.iam.server.sql.resource.MIAMOUSER;
import com.tlc.sql.common.SQLService;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;

import com.tlc.sql.common.provider.SqlServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A manager that manages the {@link Organization} related DB actions
 *
 * @author Selvakumar
 * @version 1.0
 */
public class OrganizationManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrganizationManager.class);

    private final SQLService sqlService;
    private final UserManager userManager;

    private static class Instance {
        private static final OrganizationManager INSTANCE = new OrganizationManager();
    }

    private OrganizationManager() {
        sqlService = SqlServiceProvider.get();
        userManager = UserManager.getInstance();
    }

    public static OrganizationManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Returns Org table
     *
     * @return Organization table
     */
    public Table getOrgTable() {
        return Table.get(MIAMORGANIZATION.TABLE);
    }

    /**
     * Checks whether any organization is exists by the given id
     *
     * @param orgId ID of the organization
     * @return True, if any organization has the given ID
     */
    public boolean isExists(final Long orgId) {
        final Table table = getOrgTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(MIAMORGANIZATION.ID), orgId)));

        return sqlService.getAdminDataStore().get(countQuery) > 0;
    }

    /**
     * Checks whether any organization is exists by the given name
     *
     * @param orgName Name of the organization
     * @return True, if any organization has the given name
     */
    public boolean isExists(final String orgName) {
        final Table table = getOrgTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(MIAMORGANIZATION.NAME), orgName)));

        return sqlService.getAdminDataStore().get(countQuery) > 0;
    }

    /**
     * Fetches {@link Organization} details by its id
     *
     * @param id Organization id
     * @return {@link Organization}
     */
    public Organization get(final long id) {
        final Row row = sqlService.getAdminDataStore().get(getOrgTable(), id);

        return Objects.nonNull(row) ? convertRowToOrg(row) : null;
    }

    /**
     * Converts {@link Row} details into {@link Organization}
     *
     * @param row Holds all the column details of {@link Organization} row
     * @return {@link Organization}
     */
    public Organization convertRowToOrg(final Row row) {
        final Organization organization = new Organization();
        final Short userSize = row.get(MIAMORGANIZATION.USERS_SIZE);
        final Short orgSize = row.get(MIAMORGANIZATION.ORG_SIZE);
        final Short industryType = row.get(MIAMORGANIZATION.INDUSTRY_TYPE);
        final Short state = row.get(MIAMORGANIZATION.STATE);

        organization.setId(row.getPKValue());
        organization.setName(row.get(MIAMORGANIZATION.NAME));
        organization.setActive(row.get(MIAMORGANIZATION.IS_ACTIVE));
        organization.setUsersSize(Objects.nonNull(userSize) ? Organization.UsersSize.valueOf(userSize) : null);
        organization.setOrgSize(Objects.nonNull(orgSize) ? Organization.OrgSize.valueOf(orgSize) : null);
        organization.setOwnerId(row.get(MIAMORGANIZATION.OWNER_ID));
        organization.setIndustryType(Objects.nonNull(industryType) ? Organization.IndustryType.valueOf(industryType) : null);
        organization.setOrgState(Objects.nonNull(state) ? Organization.State.valueOf(state) : null);
        organization.setCreatedTime(row.get(MIAMORGANIZATION.CREATED_TIME));
        organization.setStateUpdatedTime(row.get(MIAMORGANIZATION.STATE_UPDATED_TIME));

        return organization;
    }

    /**
     * Saves an organization to the datastore
     *
     * @param organization An entity that holds org details
     */
    public void save(final Organization organization) {
        final DataContainer dataContainer = DataContainer.create();
        final User adminUser = organization.getAdminUser();
        final Row row = new Row(getOrgTable());
        final Long currentTime = System.currentTimeMillis();

        row.set(MIAMORGANIZATION.IS_ACTIVE, organization.isActive());
        row.set(MIAMORGANIZATION.NAME, organization.getName());
        row.set(MIAMORGANIZATION.CREATED_TIME, currentTime);
        row.set(MIAMORGANIZATION.MODIFIED_TIME, currentTime);
        final Organization.UsersSize usersSize = organization.getUsersSize();
        final Organization.OrgSize orgSize = organization.getOrgSize();
        final Organization.IndustryType industryType = organization.getIndustryType();

        if (Objects.nonNull(usersSize)) {
            row.set(MIAMORGANIZATION.USERS_SIZE, usersSize.id());
        }
        row.set(MIAMORGANIZATION.STATE, organization.getOrgState().id());
        row.set(MIAMORGANIZATION.STATE_UPDATED_TIME, currentTime);

        if (Objects.nonNull(orgSize)) {
            row.set(MIAMORGANIZATION.ORG_SIZE, orgSize.id());
        }

        if (Objects.nonNull(industryType)) {
            row.set(MIAMORGANIZATION.INDUSTRY_TYPE, organization.getIndustryType().id());
        }
        organization.setId(row.getPKValue());
        adminUser.setOrgId(row.getPKValue());

        userManager.loadUserIntoContainer(true, organization.getAdminUser(), null, dataContainer);
        row.set(MIAMORGANIZATION.OWNER_ID, organization.getAdminUser().id());
        dataContainer.addNewRow(row);
        sqlService.getAdminDataStore().commitChanges(dataContainer);
    }

    /**
     * Updates the organization details into database
     *
     * @param organization An entity that holds organization details
     */
    public void update(final Organization organization) {
        final DataContainer dataContainer = DataContainer.create();
        final Row row = sqlService.getAdminDataStore().get(getOrgTable(), organization.id());
        final Long currentTime = Instant.now().toEpochMilli();

        if (Objects.isNull(row)) {
            throw ErrorCode.get(IamServerErrorCodes.ORG_NOT_FOUND);
        }

        row.set(MIAMORGANIZATION.NAME, organization.getName());
        row.set(MIAMORGANIZATION.IS_ACTIVE, organization.isActive());
        row.set(MIAMORGANIZATION.MODIFIED_TIME, Instant.now().toEpochMilli());
        final Organization.UsersSize usersSize = organization.getUsersSize();
        final Organization.OrgSize orgSize = organization.getOrgSize();
        final Organization.IndustryType industryType = organization.getIndustryType();

        if (Objects.isNull(usersSize) && Objects.isNull(orgSize) && Objects.isNull(industryType)) {
            throw ErrorCode.get(IamErrorCodes.REQUIRED_ORGANIZATION_FIELDS);
        }

        if (Objects.nonNull(usersSize)) {
            row.set(MIAMORGANIZATION.USERS_SIZE, usersSize.id());
        }

        if (Objects.nonNull(orgSize)) {
            row.set(MIAMORGANIZATION.ORG_SIZE, orgSize.id());
        }

        if (Objects.nonNull(industryType)) {
            row.set(MIAMORGANIZATION.INDUSTRY_TYPE, organization.getIndustryType().id());
        }
        row.set(MIAMORGANIZATION.STATE, organization.getOrgState().id());
        row.set(MIAMORGANIZATION.STATE_UPDATED_TIME, currentTime);
        dataContainer.updateRow(row);

        sqlService.getAdminDataStore().commitChanges(dataContainer);
    }

    /**
     * Fetches the organization properties based on user
     *
     * @param id     Represents the organization id
     * @param userId Represents the user id
     * @return {@link Organization}
     */
    public Organization getUserOrganization(final Long id, final Long userId) {
        final Table organizationTable = getOrgTable();
        final Row row = sqlService.getAdminDataStore().get(organizationTable, new WhereClause(Criteria.eq(organizationTable.getPKColumn(), id))
                .and(Criteria.eq(organizationTable.getColumn(MIAMORGANIZATION.OWNER_ID), userId))).getRow(organizationTable);

        return Objects.isNull(row) ? null : convertRowToOrg(row);
    }
    /**
     * <p>
     * Update the active status of the provided organization
     * </p>
     *
     * @param id       Represents the id of organization
     * @param isActive A flag refers the org is active or not.
     * @return {@link Boolean} status of deactivation
     */
    public boolean updateState(final Long id, final boolean isActive) {
        final Table orgTable = getOrgTable();
        final Row row = sqlService.getAdminDataStore().get(orgTable, id);

        if (Objects.isNull(row)) {
            throw ErrorCode.get(IamServerErrorCodes.ORG_NOT_FOUND);
        }

        final UpdateQuery updateQuery = UpdateQuery.get(orgTable);

        updateQuery.addUpdateColumn(orgTable.getColumn(MIAMORGANIZATION.IS_ACTIVE), isActive);
        updateQuery.addUpdateColumn(orgTable.getColumn(MIAMORGANIZATION.STATE_UPDATED_TIME), Instant.now().toEpochMilli());
        updateQuery.addUpdateColumn(orgTable.getColumn(MIAMORGANIZATION.STATE),
                isActive ? Organization.State.ACTIVE.id() : Organization.State.INACTIVE.id());
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(orgTable.getPKColumn(), id)));

        try {
            sqlService.getAdminDataStore().update(updateQuery);
            return true;
        } catch (final Exception exception) {
            LOGGER.error("Org Update: Organization state update failed. Reason: {}", exception.getMessage(), exception);
            return false;
        }
    }

    /**
     * Updates the signup details for the given organization.
     *
     * @param organization The organization entity whose signup details need to be updated.
     */
    public final void updateSignupDetails(final Organization organization) {
        final Table orgTable = getOrgTable();
        final Table userTable = Table.get(MIAMOUSER.TABLE);
        final User user = organization.getAdminUser();
        final Long currentTime = Instant.now().toEpochMilli();
        final List<UpdateQuery> updateQueries = new ArrayList<>();
        final UpdateQuery userUpdateQuery = UpdateQuery.get(userTable);
        final UpdateQuery orgUpdateQuery = UpdateQuery.get(orgTable);
        final IndustryType industryType = organization.getIndustryType();
        final UsersSize usersSize = organization.getUsersSize();
        final OrgSize orgSize = organization.getOrgSize();

        orgUpdateQuery.addUpdateColumn(orgTable.getColumn(MIAMORGANIZATION.NAME), organization.getName());
        orgUpdateQuery.addUpdateColumn(orgTable.getColumn(MIAMORGANIZATION.MODIFIED_TIME), currentTime);

        if (Objects.nonNull(industryType)) {
            orgUpdateQuery.addUpdateColumn(orgTable.getColumn(MIAMORGANIZATION.INDUSTRY_TYPE), industryType.id());
        }

        if  (Objects.nonNull(usersSize)) {
            orgUpdateQuery.addUpdateColumn(orgTable.getColumn(MIAMORGANIZATION.USERS_SIZE), usersSize.id());
        }

        if  (Objects.nonNull(orgSize)) {
            orgUpdateQuery.addUpdateColumn(orgTable.getColumn(MIAMORGANIZATION.ORG_SIZE), orgSize.id());
        }
        orgUpdateQuery.setWhereClause(new WhereClause(Criteria.eq(orgTable.getPKColumn(), organization.id())));

        userUpdateQuery.addUpdateColumn(userTable.getColumn(MIAMOUSER.JOB_TITLE), user.getJobTitle());
        userUpdateQuery.setWhereClause(new WhereClause(Criteria.eq(userTable.getPKColumn(), user.id())));

        updateQueries.add(orgUpdateQuery);
        updateQueries.add(userUpdateQuery);

        sqlService.getOrgDataStore(user.orgId()).update(updateQueries);
    }

}
