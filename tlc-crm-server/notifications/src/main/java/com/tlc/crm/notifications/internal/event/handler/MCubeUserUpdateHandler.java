package com.tlc.crm.notifications.internal.event.handler;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.RoleService;
import com.tlc.crm.notifications.marketplace.api.manager.MCubeActionManager;
import com.tlc.crm.notifications.marketplace.api.model.CrmMCubeUser;
import com.tlc.l10n.model.number.PhoneNumberUtil;
import com.tlc.notification.service.telephony.mcube.MCubeService;
import com.tlc.validator.TlcModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * MCube-specific implementation of {@link TelephoneUserUpdateHandler} that synchronizes
 * user changes (deactivation, phone number updates) with the MCube provider.
 *
 * @author Sathish
 * @version 1.0
 */
public class MCubeUserUpdateHandler implements TelephoneUserUpdateHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MCubeUserUpdateHandler.class);

    private final MCubeService mCubeService;
    private final MCubeActionManager mCubeActionManager;
    private final RoleService roleService;

    private MCubeUserUpdateHandler() {
        this.mCubeService = MCubeService.getInstance();
        this.mCubeActionManager = MCubeActionManager.getInstance();
        roleService =  RoleService.getInstance();
    }

    private static class Instance {
        private static final MCubeUserUpdateHandler M_CUBE_USER_UPDATE_HANDLER = new MCubeUserUpdateHandler();
    }

    /**
     * Returns the singleton instance of {@link MCubeUserUpdateHandler}.
     *
     * @return the singleton instance
     */
    public static MCubeUserUpdateHandler getInstance() {
        return Instance.M_CUBE_USER_UPDATE_HANDLER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleDeactivatedUsers(final long orgId, final Set<Long> deactivatedUserIds) {
        final Collection<CrmMCubeUser> crmMCubeUsers = mCubeActionManager.getUsers(orgId, deactivatedUserIds);
        Long marketplaceId = null;

        for (final CrmMCubeUser crmMCubeUser : crmMCubeUsers) {

            if (Objects.isNull(marketplaceId)) {
                marketplaceId = crmMCubeUser.getMarketplaceAppId();
            }

            if (Objects.equals(crmMCubeUser.getSyncStatus(), CrmMCubeUser.SyncStatus.CONNECTED)) {
                mCubeService.removeUser(crmMCubeUser);
            }
        }

        if (Objects.nonNull(marketplaceId)) {
            mCubeActionManager.removeUser(orgId, marketplaceId, deactivatedUserIds);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleUserUpdate(final long orgId, final Collection<CrmUser> crmUsers, final Collection<CrmUser> previousUsers) {
        final Collection<CrmMCubeUser> mCubeUsers = mCubeActionManager.getUsers(orgId, crmUsers.stream().map(CrmUser::id)
                .collect(Collectors.toSet()));
        final Map<Long, CrmMCubeUser> mCubeUserMap = mCubeUsers.stream().collect(Collectors.toMap(crmMCubeUser ->
                crmMCubeUser.getCrmUser().id(), Function.identity()));
        final Map<Long, CrmUser> previousUserMap = previousUsers.stream().collect(Collectors.toMap(TlcModel::id, Function.identity()));
        final Set<Long> userIdsToRemove = new HashSet<>(crmUsers.size());
        Long marketplaceAppId = null;

        for (final CrmUser crmUser : crmUsers) {
            final Long userId = crmUser.id();

            if (mCubeUserMap.containsKey(userId)) {
                final CrmMCubeUser crmMCubeUser = mCubeUserMap.get(userId);
                final String mobile = crmUser.getMobile();

                if (Objects.isNull(marketplaceAppId)) {
                    marketplaceAppId = crmMCubeUser.getMarketplaceAppId();
                }

                try {
                    final boolean isConnected = crmMCubeUser.getSyncStatus().isConnected();

                    if (!PhoneNumberUtil.isValid(mobile)) {
                        userIdsToRemove.add(userId);
                        mCubeUserMap.remove(userId);

                        if (isConnected) {
                            mCubeService.removeUser(crmMCubeUser);
                        }
                        continue;
                    }

                    if (isConnected) {
                        final CrmUser previousCrmUser = previousUserMap.get(userId);

                        if (Objects.nonNull(previousCrmUser)) {
                            final String previousMobile = previousCrmUser.getMobile();

                            if (Objects.nonNull(previousMobile)) {
                                final CrmRole crmRole = roleService.getRoleByUser(orgId, userId);

                                crmMCubeUser.setRoleId(crmRole.getConfig().isAdmin() ? 1 : 2);
                                crmMCubeUser.setPreviousPhoneNumber(PhoneNumberUtil.parse(previousMobile).getNumber());
                                crmMCubeUser.setPhoneNumber(PhoneNumberUtil.parse(mobile).getNumber());
                                mCubeService.updateUser(crmMCubeUser);
                            }
                        }
                    }
                } catch (final Exception exception) {
                    LoggerUtil.logError(LOGGER, exception, "MCube user update: Error occurred during user update. OrgId: {}, UserId: {} Cause: {}",
                            orgId, userId, exception.getMessage());
                }
            }
        }

        if (Objects.nonNull(marketplaceAppId)) {

            if (!userIdsToRemove.isEmpty()) {
                mCubeActionManager.removeUser(orgId, marketplaceAppId, userIdsToRemove);
            }
            mCubeActionManager.updateUser(orgId, marketplaceAppId, mCubeUserMap.values());
        }
    }
}
