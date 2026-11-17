package com.tlc.crm.common.iam.service.internal;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.resolver.UserResolver;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.models.CriteriaDetailsHolder;
import com.tlc.crm.common.models.LimitConfig;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * It provides the implementation for various operations related to the {@link UserService}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class UserServiceImpl implements UserService {

    private final UserResolver userResolver;

    private UserServiceImpl() {
        this.userResolver = UserResolver.getInstance();
    }

    private static class Instance {
        private static final UserServiceImpl INSTANCE = new UserServiceImpl();
    }

    /**
     * <p>
     * Gets the user service instance
     * </p>
     *
     * @return The {@link UserServiceImpl} instance
     */
    public static UserServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents an organization id
     * @param userIds Represents the user ids
     * @return The collection of {@link CrmUser} details
     */
    @Override
    public Collection<CrmUser> getUsers(final Long orgId, final Collection<Long> userIds) {
        return userResolver.getUsers(orgId, userIds);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return The {@link CrmUser} details
     */
    @Override
    public CrmUser getUser(final Long orgId, final Long userId) {
        return userResolver.getUser(orgId, userId);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param iamId Represents the iam user id
     * @return The crm user id
     */
    @Override
    public Long getUserId(final Long orgId, final Long iamId) {
        return userResolver.getUserId(orgId, iamId);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organization id
     * @param iamUserIds Contains the collection of iam user ids
     * @return The collection of {@link CrmUser} details
     */
    @Override
    public Collection<CrmUser> getUsersByIamId(final Long orgId, final Collection<Long> iamUserIds) {
        return userResolver.getUsersByIamId(orgId, iamUserIds);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents an organization id.
     * @param iamUserId Refers the iam user ids.
     * @return The optional of {@link CrmUser} details.
     */
    @Override
    public Optional<CrmUser> getUserByIamId(final Long orgId, final Long iamUserId) {
        final Collection<CrmUser> crmUsers = userResolver.getUsersByIamId(orgId, List.of(iamUserId));

        return Objects.nonNull(crmUsers) && !crmUsers.isEmpty() ? crmUsers.stream().findFirst() : Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization ID
     * @return the Admin user ID
     */
    @Override
    public Long getAdminUserId(final long orgId) {
        return userResolver.getAdminUserId(orgId);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id.
     * @return The admin {@code CrmUser}.
     */
    @Override
    public CrmUser getAdminUser(long orgId) {
        return userResolver.getAdminUser(orgId);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                  CRM organization id
     * @param criteriaDetailsHolder  filter criteria (pagination is not applied)
     * @param excludeCrmUserIds      CRM user ids to exclude
     * @return total active users matching the filter
     */
    @Override
    public long getUsersCount(final long orgId, final CriteriaDetailsHolder criteriaDetailsHolder,
                              final Collection<Long> excludeCrmUserIds) {
        return userResolver.getUsersCount(orgId, criteriaDetailsHolder, excludeCrmUserIds);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                  CRM organization id
     * @param criteriaDetailsHolder  filter criteria
     * @param  limitConfig           contains start and limit values for pagination
     * @param excludeCrmUserIds      CRM user ids to exclude
     * @return ordered list of active users for the requested page
     */
    @Override
    public Collection<CrmUser> getUsers(final long orgId, final CriteriaDetailsHolder criteriaDetailsHolder,
                                        final LimitConfig limitConfig, final Collection<Long> excludeCrmUserIds) {
        return userResolver.getUsers(orgId, criteriaDetailsHolder, limitConfig, excludeCrmUserIds);
    }
}