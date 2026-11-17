package com.tlc.crm.common.iam.resolver;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.resolver.internal.UserResolverImpl;
import com.tlc.crm.common.models.CriteriaDetailsHolder;
import com.tlc.crm.common.models.LimitConfig;
import com.tlc.iam.client.dropdown.SearchFilter;

import java.util.Collection;

/**
 * <p>
 * A resolver that resolves the {@link CrmUser} related details
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public interface UserResolver {

    /**
     * <p>
     * Gets the instance of {@link UserResolver}
     * </p>
     *
     * @return The instance of {@link UserResolver}
     */
    static UserResolver getInstance() {
        return UserResolverImpl.getInstance();
    }

    /**
     * <p>
     * Gets the collection of {@link CrmUser} by org id and user ids
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param userIds Contains the collection of user ids
     * @return The collection of {@link CrmUser}
     */
    Collection<CrmUser> getUsers(final Long orgId, final Collection<Long> userIds);

    /**
     * <p>
     * Gets the {@link CrmUser} given by org and user id
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return The {@link CrmUser} details
     */
    CrmUser getUser(final Long orgId, final Long userId);

    /**
     * <p>
     * Gets the cmr user id given by org and iam user id
     * </p>
     *
     * @param orgId Represents an organization id
     * @param iamId Represents the iam id
     * @return The crm user id
     */
    Long getUserId(final Long orgId, final Long iamId);

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