package com.tlc.crm.common.iam.service;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.internal.UserServiceImpl;
import com.tlc.crm.common.models.CriteriaDetailsHolder;
import com.tlc.crm.common.models.LimitConfig;

import java.util.Collection;
import java.util.Optional;

/**
 * <p>
 * The {@link UserService} provides the service for the crm user
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public interface UserService {

    /**
     * <p>
     * Gets the instance of the {@link UserService}
     * </p>
     *
     * @return The {@link UserServiceImpl} instance
     */
    static UserService getInstance() {
        return UserServiceImpl.getInstance();
    }

    /**
     * <p>
     * Gets the collection of {@link CrmUser} given by orgId and user ids
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param userIds Contains the collection of user ids
     * @return The collection of {@link CrmUser} details
     */
    Collection<CrmUser> getUsers(final Long orgId, final Collection<Long> userIds);

    /**
     * <p>
     * Gets the {@link CrmUser} given by orgId and user id
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return The {@link CrmUser} details
     */
    CrmUser getUser(final Long orgId, final Long userId);

    /**
     * <p>
     * Gets the crm user id
     * </p>
     *
     * @param orgId Represents an organization id
     * @param id    Represents the iam user id
     * @return The crm user id
     */
    Long getUserId(final Long orgId, final Long id);

    /**
     * <p>
     * Gets the collection of {@link CrmUser} by iam user ids
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param iamUserIds Contains the collection of iam user ids
     * @return The collection of {@link CrmUser} details
     */
    Collection<CrmUser> getUsersByIamId(final Long orgId, final Collection<Long> iamUserIds);

    /**
     * <p>
     * Gets the {@link CrmUser} by iam user id.
     * </p>
     *
     * @param orgId     Represents an organization id.
     * @param iamUserId Refers the iam user ids.
     * @return The optional of {@link CrmUser} details.
     */
    Optional<CrmUser> getUserByIamId(final Long orgId, final Long iamUserId);

    /**
     * Gets the Admin user ID of the given organization ID
     *
     * @param orgId Represents the organization ID
     * @return the Admin user ID
     */
    Long getAdminUserId(final long orgId);

    /**
     * Gets the admin {@code CrmUser} for the given orgId.
     *
     * @param orgId Represents an organization id.
     * @return The admin {@code CrmUser}.
     */
    CrmUser getAdminUser(final long orgId);

    /**
     * Counts CRM users matching the filter.
     *
     * @param orgId                  CRM organization id
     * @param criteriaDetailsHolder  filter criteria (pagination is not applied)
     * @param excludeCrmUserIds      CRM user ids to exclude
     * @return total active users matching the filter
     */
    long getUsersCount(final long orgId, final CriteriaDetailsHolder criteriaDetailsHolder, final Collection<Long> excludeCrmUserIds);

    /**
     * Returns a page of CRM users matching the filter.
     *
     * @param orgId                  CRM organization id
     * @param criteriaDetailsHolder  filter criteria
     * @param  limitConfig           contains start and limit values for pagination
     * @param excludeCrmUserIds      CRM user ids to exclude
     * @return ordered list of active users for the requested page
     */
    Collection<CrmUser> getUsers(final long orgId, final CriteriaDetailsHolder criteriaDetailsHolder,
                                 final LimitConfig limitConfig, final Collection<Long> excludeCrmUserIds);
}