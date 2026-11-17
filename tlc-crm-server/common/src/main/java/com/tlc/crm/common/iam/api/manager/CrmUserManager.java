package com.tlc.crm.common.iam.api.manager;

import com.tlc.crm.common.iam.api.manager.impl.CrmUserManagerImpl;
import com.tlc.crm.common.iam.api.manager.impl.SecureCrmUserManager;
import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.internal.CrmUserEntity;

import com.tlc.crm.common.models.CriteriaDetailsHolder;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Performs the functionality for the {@link CrmUserManager} DB related actions
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public interface CrmUserManager {

    /**
     * <p>
     * Gets the instance of {@link CrmUserManager}
     * </p>
     *
     * @return {@link CrmUserManager}
     */
    static CrmUserManager getInstance() {
        return CrmUserManagerImpl.getInstance();
    }

    /**
     * <p>
     * Gets the instance of {@link CrmUserManager}
     * </p>
     *
     * @return {@link CrmUserManager}
     */
    static CrmUserManager getSecureInstance() {
        return SecureCrmUserManager.getInstance();
    }

    /**
     * <p>
     * Retrieves a collection of {@link CrmUser} based on the org id and user ids
     * </p>
     *
     * @param orgId Represents an organization id
     * @param ids   Contains the collection of user ids
     * @return The collection of {@link CrmUser} details
     */
    Collection<CrmUser> get(final Long orgId, final Collection<Long> ids);

    /**
     * <p>
     * Gets the {@link CrmUser} based on the user id
     * </p>
     *
     * @param orgId Represents an organization id
     * @param id    Represents the user id
     * @return The {@link CrmUser} details
     */
    CrmUser get(final Long orgId, final Long id);

    /**
     * <p>
     * Checks whether the user ids are exists or not
     * </p>
     *
     * @param orgId Represents an organization id
     * @param ids   Contains the crm user ids
     * @return If all user ids are exists it will return true, otherwise it will return false
     */
    boolean exists(final Long orgId, final Collection<Long> ids);

    /**
     * <p>
     * Checks whether any users are mapped to the given roles.
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param roleIds Contains the collection of role ids
     * @return If any user exists in the given role it will return true, otherwise it will return false.
     */
    boolean rolesHaveUsers(final Long orgId, final Collection<Long> roleIds);

    /**
     * <p>
     * Gets the {@link CrmUser} details using iam user id
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param iamUserIds Represents the iam user id
     * @return The collection of {@link CrmUser} details
     */
    Collection<CrmUser> getUsersByIamId(final Long orgId, final Collection<Long> iamUserIds);

    /**
     * <p>
     * Gets the collection of {@link CrmUser} details with role details using iam user id
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param iamUserIds Represents the iam user id
     * @return The collection of {@link CrmUser} details
     */
    Collection<CrmUser> getUserListByIamId(final Long orgId, final Collection<Long> iamUserIds);

    /**
     * <p>
     * Gets the total number of users
     * </p>
     *
     * @param orgId Represents an organization id
     * @return The total number of users
     */
    Long totalUsers(final Long orgId);

    /**
     * <p>
     * Inserts or Updates {@link CrmUser} information based on the provided flag and {@link CrmUser} object
     * </p>
     *
     * @param isCreateEvent A flag indicating whether the operation is triggered by a create event
     * @param crmUser       An entity that holds the {@link CrmUser} details
     */
    void upsert(final boolean isCreateEvent, final CrmUser crmUser);

    /**
     * <p>
     * Loads the {@link CrmUser} information into the provided {@link DataContainer}
     * based on the provided flag and {@link CrmUser}
     * </p>
     *
     * @param isCreateEvent  A flag indicating whether the operation is triggered by a create event
     * @param crmUser        An entity that holds the {@link CrmUser} details
     * @param targetContainer The {@link DataContainer} to load the user information into
     */
    void loadUserDataIntoContainer(final boolean isCreateEvent, final CrmUser crmUser,
                                   final DataContainer targetContainer);

    /**
     * <p>
     * Saves the given role details into data base
     * </p>
     *
     * @param orgId         Represents an org id
     * @param crmUserEntity Represents the {@link CrmUser} details
     */
    void create(final long orgId, final CrmUserEntity crmUserEntity);

    /**
     * <p>
     * Converts {@link Row} into {@link CrmUser} details
     * </p>
     *
     * @param row A row represents the {@link CrmUser} details
     * @return The {@link CrmUser} details
     */
    CrmUser convertRowToModel(final Row row);

    /**
     * <p>
     * Gets {@link CrmUser} reporting manager id.
     * </p>
     *
     * @param orgId Represents an organization id
     * @param id    Represents the id of the user.
     * @return reporting manager id.
     */
    Long getReportingManagerId(final Long orgId, final Long id);

    /**
     * <p>
     * Gets the user summary based on the user id
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return The {@link CrmUser} summary details
     */
    CrmUser getSummary(final Long orgId, final Long userId);

    /**
     * <p>
     * Fetches the crm user for the given org id and user id
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return The {@link CrmUser}
     */
    CrmUser getCrmUser(final Long orgId, final Long userId);

    /**
     * <p>
     * Partially updates the given user
     * </p>
     *
     * @param crmUser Represents the {@link CrmUser} details
     */
    void partialUpdate(final CrmUser crmUser);

    /**
     * <p>
     * Loads the partial user information into the provided {@link DataContainer} based on the given {@link CrmUser}
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param crmUser         An entity that holds the partial {@link CrmUser} details
     * @param targetContainer The {@link DataContainer} to load the partial user information into
     */
    void loadPartialUserDataIntoContainer(final long orgId, final CrmUser crmUser,
                                          final DataContainer targetContainer);

    /**
     * <p>
     * Gets the collection of {@link CrmRole} given organization id
     * </p>
     *
     * @param orgId Represents an organization id
     * @return The collection of {@link CrmRole}
     */
    Collection<CrmRole> getRoles(final long orgId);

    /**
     * <p>
     * Gets the IAM user id of the org account creator.
     * </p>
     *
     * @param orgId Represents an organization id.
     * @return the IAM user id of the org account creator.
     */
    Long getOrgAccountCreatorId(final long orgId);

    /**
     * Gets the admin user ID for the given organization ID
     *
     * @param orgId Represents an organization id
     * @return the admin user ID
     */
    Long getAdminUserId(final Long orgId);

    /**
     * <p>
     * Checks whether any of the given user is reporting manager or not.
     * </p>
     *
     * @param orgId   Represents an organization id.
     * @param userIds Represents an user ids.
     * @return True, if the given user ids contains reporting manager.
     */
    boolean isReportingManager(final Long orgId, final Collection<Long> userIds);

    /**
     * <p>
     * Gets the map of primary user id of the provided org ids. Primary user refers to the first user of an organization
     * </p>
     *
     * @param orgIds Represents the collection of org id.
     * @return the map of primary user id (crm user)
     */
    Map<Long, Long> getPrimaryUserMap(final Collection<Long> orgIds);

    /**
     * <p>
     * Gets the user ids from the provided org id
     * </p>
     *
     * @param orgId Represents the org id
     * @return the collection of user id
     */
    Collection<Long> getUserIds(final Long orgId);

    /**
     * <p>
     * Gets the collection of {@link CrmUser} by user names
     * </p>
     * <p><b>⚠ Caution:</b> This method may also return <i>inactive users</i>
     * @param orgId      Represents an organization id
     * @param userNames  Contains the collection of user names
     * @return The collection of {@link CrmUser} details
     */
    Collection<CrmUser> getUsersByNames(final Long orgId, final Collection<String> userNames);

    /**
     * <p>
     * Gets the total number of CRM users matching the filter criteria
     * </p>
     *
     * @param orgId                  Represents an organization id
     * @param criteriaDetailsHolder  Holds the filtering criteria (pagination is not applied)
     * @param excludeCrmUserIds      CRM user ids to exclude from the result
     * @return The total number of matching users
     */
    long getUsersCount(final long orgId, final CriteriaDetailsHolder criteriaDetailsHolder, final Collection<Long> excludeCrmUserIds);

    /**
     * <p>
     * Gets IAM user ids for CRM users matching the filter criteria, ordered by CRM user id
     * </p>
     *
     * @param orgId                  Represents an organization id
     * @param criteriaDetailsHolder  Holds the filtering criteria (pagination is not applied)
     * @param excludeCrmUserIds      CRM user ids to exclude from the result
     * @return IAM user ids in stable order
     */
    Collection<Long> getIamUserIds(final long orgId, final CriteriaDetailsHolder criteriaDetailsHolder, final Collection<Long> excludeCrmUserIds);
}