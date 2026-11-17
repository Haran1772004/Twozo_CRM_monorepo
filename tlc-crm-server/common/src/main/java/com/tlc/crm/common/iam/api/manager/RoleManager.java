package com.tlc.crm.common.iam.api.manager;

import com.tlc.crm.common.iam.api.manager.impl.RoleManagerImpl;
import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.internal.manager.IamConfigManger;
import com.tlc.crm.common.iam.internal.manager.UserLinkManager;

import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.LimitClause;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Performs the functionality for the {@link RoleManager} DB related actions
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public interface RoleManager extends IamConfigManger<CrmRole>, UserLinkManager<CrmRole> {

    /**
     * <p>
     * Gets the role manager instance
     * </p>
     *
     * @return The {@link RoleManagerImpl}
     */
    static RoleManager getInstance() {
        return RoleManagerImpl.getInstance();
    }

    /**
     * <p>
     * Checks if any one of the roles are having one of the given role names excluding the given ids
     * </p>
     *
     * @param orgId Represents an organization id
     * @param ids   Contains the collection od role ids
     * @param names Contains the collection of role names
     * @return If any role is exists it will return true, otherwise it will return false
     */
    boolean checkRolesAreExists(final Long orgId, final Collection<Long> ids, final Collection<String> names);

    /**
     * <p>
     * Retrieves the roles by given organization id with start and limit criteria
     * </p>
     *
     * @param orgId Represents an organization id
     * @param start Represents the starting index of role to be fetched
     * @param limit Represents the maximum number of role to be fetched
     * @return The collection of {@link CrmRole} details
     */
    Collection<CrmRole> get(final Long orgId, final int start, final int limit);

    /**
     * <p>
     * Retrieves the total number of users.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param roleId Represents the role id
     * @return The total number of users
     */
    long getUsersCount(final Long orgId, final Long roleId);

    /**
     * <p>
     * Retrieves the role by given id with start and limit criteria
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param roleId         Represents the role id
     * @param userStartValue Represents the starting index of user to be fetched
     * @param userLimit      Represents the maximum number of user to be fetched
     * @return The {@link CrmRole} details
     */
    CrmRole get(final Long orgId, final Long roleId, final int userStartValue, final int userLimit);

    /**
     * <p>
     * Checks whether any role is exists with one of the given names
     * </p>
     *
     * @param orgId Represents an organization id
     * @param names Contains the collection of role names
     * @return If any role is exists it will return true, otherwise it will return false
     */
    boolean checkRolesAreExists(final Long orgId, final Collection<String> names);

    /**
     * <p>
     * Retrieves the users by given role id with start and limit criteria
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param roleId         Represents the role id
     * @param userStartValue Represents the starting index of user to be fetched
     * @param userLimit      Represents the maximum number of user to be fetched
     * @return The Collection of {@link CrmUser} details
     */
    Collection<CrmUser> getUsers(final Long orgId, final Long roleId, final int userStartValue, final int userLimit);

    /**
     * <p>
     * Fetches the role dropdown values based on the specified start and limit conditions
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param limitClause Represents the start and limit the role to be fetched
     * @param criteria    Represents the criteria for role filtering
     * @return The collection of {@link CrmRole} details
     */
    Collection<CrmRole> getRoleDropDownList(final Long orgId, final LimitClause limitClause, final Criteria criteria);

    /**
     * <p>
     * Gets the collection of {@link CrmRole} given organization id
     * </p>
     *
     * @param orgId Represents an organization id
     * @param ids   Represents the role ids
     * @return The collection of {@link CrmRole}
     */
    Collection<CrmRole> get(final long orgId, final Collection<Long> ids);

    /**
     * <p>
     * Checks if there are any existing associations between the specified organization id and role ids
     * </p>
     *
     * @param orgId   Represents the organization id
     * @param roleIds Represents the collection of role ids
     * @return True if role ids exist, otherwise false
     */
    boolean exists(final long orgId, final Collection<Long> roleIds);

    /**
     * <p>
     * Gets the {@link CrmRole} details by user ids.
     * </p>
     *
     * @param orgId   Represents an organization id.
     * @param userIds Represents the user id.
     * @return The map of user id and {@link CrmRole}.
     */
    Map<Long, CrmRole> getRolesByUsers(final Long orgId, final Collection<Long> userIds);

    /**
     * <p>
     * Gets the {@link CrmRole} details by user id
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param userId Represents the user id
     * @return The {@link CrmRole} details
     */
    CrmRole getRoleByUser(final Long orgId, final Long userId);

    /**
     * Fetches the roles for the respective users
     *
     * @param orgId   Represent an organization id
     * @param userIds Represents the user ids
     * @return the collection of {@link CrmRole}
     */
    Collection<CrmRole> fetchRolesForUsers(final long orgId, final Collection<Long> userIds);

    /**
     * <p>
     * Fetches a CRM role along with its associated permissions.
     * </p>
     *
     * @param orgId Represents an organization ID.
     * @param id    Represents the role ID.
     * @return the {@link CrmRole} object populated with its details, permissions, and associated users.
     */
    CrmRole getRole(final long orgId, final long id);

    /**
     * <p>
     * Fetches the collection of CRM role ids
     * </p>
     *
     * @param orgId Represents an organization ID.
     * @return the collection of {@link CrmRole}
     */
    Set<Long> getRoleIds(final long orgId);

    /**
     * <p>
     * Fetches the admin user ids of the given organisation id.
     * </p>
     *
     * @param orgId Represents an organisation id.
     * @return Admin user ids.
     */
    Collection<Long> fetchAdminUserIds(final Long orgId);

    /**
     * <p>
     * Checks whether the given role is admin
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param roleId Represents the role id
     * @return true, if admin role is present
     */
    boolean isAdminRole(final long orgId, final long roleId);

    /**
     * <p>
     * Fetches the user IDs of the given role IDs.
     * </p>
     *
     * @param orgId   Represents an organization ID.
     * @param roleIds Represents the role IDs
     * @return Map of role and user IDs.
     */
    Map<Long, Collection<Long>> getUserIds(final Long orgId, final Collection<Long> roleIds);

    /**
     * Fetches the admin role of an organisation.
     *
     * @param orgId Represents the organisation ID.
     * @return Admin role.
     */
    CrmRole getAdminRole(final long orgId);
}
