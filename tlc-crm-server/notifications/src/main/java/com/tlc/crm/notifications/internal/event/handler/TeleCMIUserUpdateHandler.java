package com.tlc.crm.notifications.internal.event.handler;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.notifications.marketplace.api.manager.TeleCMIActionManager;
import com.tlc.crm.notifications.marketplace.api.model.CrmTeleCMIUser;
import com.tlc.l10n.model.number.PhoneNumberUtil;
import com.tlc.notification.service.telephony.telecmi.TeleCMIService;
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
 * TeleCMI-specific implementation of {@link TelephoneUserUpdateHandler} that synchronizes
 * user changes (deactivation, phone/name/email updates) with the TeleCMI provider.
 *
 * @author Kishore J
 * @version 1.0
 */
public class TeleCMIUserUpdateHandler implements TelephoneUserUpdateHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TeleCMIUserUpdateHandler.class);

    private final TeleCMIService teleCMIService;
    private final TeleCMIActionManager teleCMIActionManager;

    private TeleCMIUserUpdateHandler() {
        this.teleCMIService = TeleCMIService.getInstance();
        this.teleCMIActionManager = TeleCMIActionManager.getInstance();
    }

    private static class Instance {
        private static final TeleCMIUserUpdateHandler TELE_CMI_USER_UPDATE_HANDLER = new TeleCMIUserUpdateHandler();
    }

    /**
     * Returns the singleton instance of {@link TeleCMIUserUpdateHandler}.
     *
     * @return the singleton instance
     */
    public static TeleCMIUserUpdateHandler getInstance() {
        return Instance.TELE_CMI_USER_UPDATE_HANDLER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleDeactivatedUsers(final long orgId, final Set<Long> deactivatedUserIds) {
        final Collection<CrmTeleCMIUser> crmTeleCMIUsers = teleCMIActionManager.getUsers(orgId, deactivatedUserIds);
        Long marketplaceId = null;

        for (final CrmTeleCMIUser crmTeleCMIUser : crmTeleCMIUsers) {

            if (Objects.isNull(marketplaceId)) {
                marketplaceId = crmTeleCMIUser.getMarketplaceAppId();
            }

            if (Objects.equals(crmTeleCMIUser.getSyncStatus(), CrmTeleCMIUser.SyncStatus.CONNECTED)) {
                teleCMIService.removeUser(crmTeleCMIUser);
            }
        }

        if (Objects.nonNull(marketplaceId)) {
            teleCMIActionManager.removeUser(orgId, marketplaceId, deactivatedUserIds);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleUserUpdate(final long orgId, final Collection<CrmUser> crmUsers, final Collection<CrmUser> previousUsers) {
        final Collection<CrmTeleCMIUser> teleCMIUsers = teleCMIActionManager.getUsers(orgId, crmUsers.stream().map(CrmUser::id)
                .collect(Collectors.toSet()));
        final Map<Long, CrmTeleCMIUser> teleCMIUserMap = teleCMIUsers.stream().collect(Collectors.toMap(crmTeleCMIUser ->
                crmTeleCMIUser.getCrmUser().id(), Function.identity()));
        final Set<Long> userIdsToRemove = new HashSet<>(crmUsers.size());
        Long marketplaceAppId = null;

        for (final CrmUser crmUser : crmUsers) {
            final Long userId = crmUser.id();

            if (teleCMIUserMap.containsKey(userId)) {
                final CrmTeleCMIUser crmTeleCMIUser = teleCMIUserMap.get(userId);
                final String mobile = crmUser.getMobile();

                if (Objects.isNull(marketplaceAppId)) {
                    marketplaceAppId = crmTeleCMIUser.getMarketplaceAppId();
                }

                try {
                    final boolean isConnected = crmTeleCMIUser.getSyncStatus().isConnected();

                    if (!PhoneNumberUtil.isValid(mobile)) {
                        userIdsToRemove.add(userId);
                        teleCMIUserMap.remove(userId);

                        if (isConnected) {
                            teleCMIService.removeUser(crmTeleCMIUser);
                        }
                        continue;
                    }
                    final String firstName = crmUser.getFirstName();

                    crmTeleCMIUser.setPhoneNumber(PhoneNumberUtil.parse(mobile).getNumberWithCountryCode());
                    crmTeleCMIUser.setFirstName(firstName);
                    crmTeleCMIUser.setLastName(Objects.requireNonNullElse(crmUser.getLastName(), firstName));
                    crmTeleCMIUser.setEmail(crmUser.getEmail());

                    if (isConnected) {
                        teleCMIService.updateUser(crmTeleCMIUser);
                    }
                } catch (final Exception exception) {
                    LoggerUtil.logError(LOGGER, exception, "TeleCMI user update: Error occurred during user update. OrgId: {}, UserId: {} Cause: {}",
                            orgId, userId, exception.getMessage());
                }
            }
        }

        if (Objects.nonNull(marketplaceAppId)) {

            if (!userIdsToRemove.isEmpty()) {
                teleCMIActionManager.removeUser(orgId, marketplaceAppId, userIdsToRemove);
            }
            teleCMIActionManager.updateUser(orgId, marketplaceAppId, teleCMIUserMap.values());
        }
    }
}
