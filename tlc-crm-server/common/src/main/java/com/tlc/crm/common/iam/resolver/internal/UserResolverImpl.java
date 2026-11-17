package com.tlc.crm.common.iam.resolver.internal;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.api.manager.CrmUserManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.cache.UserCache;
import com.tlc.crm.common.iam.field.CrmUserFields;
import com.tlc.crm.common.iam.resolver.UserResolver;
import com.tlc.crm.common.models.CriteriaDetails;
import com.tlc.crm.common.models.CriteriaDetailsHolder;
import com.tlc.crm.common.models.LimitConfig;
import com.tlc.iam.client.dropdown.SearchFilter;
import com.tlc.sql.common.api.dml.Operator;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.function.Function;

/**
 * <p>
 * A resolver that resolves the {@link CrmUser} related details
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class UserResolverImpl implements UserResolver {

    private final CrmUserManager crmUserManager;
    private final UserCache userCache;

    public UserResolverImpl() {
        this.crmUserManager = CrmUserManager.getInstance();
        this.userCache = UserCache.getInstance();
    }

    private static class Instance {
        private static final UserResolverImpl INSTANCE = new UserResolverImpl();
    }

    public static UserResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents an organization id
     * @param userIds Contains the collection of user ids
     * @return The collection of {@link CrmUser} details
     */
    @Override
    public Collection<CrmUser> getUsers(final Long orgId, final Collection<Long> userIds) {
        final Collection<CrmUser> existingCrmUsers = userCache.getUsers(orgId, userIds);

        if (userIds.size() != existingCrmUsers.size()) {
            final Collection<CrmUser> crmUsers = crmUserManager.get(orgId, userIds);

            for (final CrmUser crmUser : crmUsers) {
                userCache.put(crmUser);
            }

            return crmUsers;
        }

        return existingCrmUsers;
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
        final CrmUser crmUser = userCache.getUser(orgId, userId);

        if (Objects.isNull(crmUser)) {
            final CrmUser fetchedUser = crmUserManager.get(orgId, userId);

            userCache.put(fetchedUser);

            return fetchedUser;
        }

        return crmUser;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param iamId Represents the iam id
     * @return The crm user id
     */
    @Override
    public Long getUserId(final Long orgId, final Long iamId) {
        final Long crmUserId = userCache.getUserId(iamId);

        if (Objects.isNull(crmUserId)) {
            final CrmUser crmUser = crmUserManager.getUsersByIamId(orgId, List.of(iamId)).stream().findFirst().get();

            userCache.putByIamId(crmUser);

            return crmUser.id();
        }

        return crmUserId;
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
        final Collection<CrmUser> exitingCrmUsers = userCache.getUsersByIamId(iamUserIds);

        if (iamUserIds.size() != exitingCrmUsers.size()) {
            final Collection<CrmUser> crmUsers = crmUserManager.getUsersByIamId(orgId, iamUserIds);

            for (final CrmUser crmUser : crmUsers) {
                userCache.putByIamId(crmUser);
            }

            return crmUsers;
        }

        return exitingCrmUsers;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization ID
     * @return
     */
    @Override
    public Long getAdminUserId(final long orgId) {
        return crmUserManager.getAdminUserId(orgId);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id.
     * @return The admin {@code CrmUser}.
     */
    @Override
    public CrmUser getAdminUser(long orgId) {
        return getUser(orgId, crmUserManager.getAdminUserId(orgId));
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
        return crmUserManager.getUsersCount(orgId, criteriaDetailsHolder, excludeCrmUserIds);
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
        final List<Long> iamUserIds = crmUserManager.getIamUserIds(orgId, criteriaDetailsHolder, excludeCrmUserIds)
                .stream().toList();
        final int start = Math.max(limitConfig.start(), NumericalConstant.ONE);
        final int limit = limitConfig.limit();
        final int fromIndex = Math.min(start - NumericalConstant.ONE, iamUserIds.size());
        final int toIndex = Math.min(fromIndex + limit, iamUserIds.size());

        if (fromIndex >= toIndex) {
            return List.of();
        }

        final List<Long> pageIamIds = iamUserIds.subList(fromIndex, toIndex);
        final Map<Long, CrmUser> crmUserByIamId = crmUserManager.getUserListByIamId(orgId, pageIamIds).stream()
                .collect(Collectors.toMap(CrmUser::getIamId, Function.identity(), (first, second) -> first));
        final List<CrmUser> pageUsers = new ArrayList<>(pageIamIds.size());

        for (final Long iamUserId : pageIamIds) {
            final CrmUser crmUser = crmUserByIamId.get(iamUserId);

            if (Objects.nonNull(crmUser)) {
                pageUsers.add(crmUser);
            }
        }

        return pageUsers;
    }
}