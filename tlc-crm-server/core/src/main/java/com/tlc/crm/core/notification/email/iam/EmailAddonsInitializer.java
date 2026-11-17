package com.tlc.crm.core.notification.email.iam;

import com.tlc.crm.common.iam.OrgInitiator;
import com.tlc.crm.common.iam.OrgInitiatorAction;
import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.notification.email.api.manager.addon.bcc.SmartBccManager;
import com.tlc.crm.core.notification.email.api.manager.addon.team.TeamInboxManager;
import com.tlc.email.common.model.ServiceType;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.service.EmailAddOnService;
import com.tlc.email.common.service.SmartBccService;
import com.tlc.email.common.service.TeamInboxService;
import com.tlc.iam.client.UserService;
import com.tlc.iam.common.models.User;
import com.tlc.sql.common.api.Row;
import com.tlc.web.client.event.EventCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * Unified initializer for email add-ons (Smart BCC and Team Inbox) during organization initialization.
 * This class consolidates the initialization logic for both add-ons into a single initializer.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@OrgInitiator(name = "emailAddons")
public final class EmailAddonsInitializer implements OrgInitiatorAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailAddonsInitializer.class);

    private final EmailAddOnService smartBccService;
    private final SmartBccManager smartBccManager;
    private final EmailAddOnService teamInboxService;
    private final TeamInboxManager teamInboxManager;
    private final UserService iamUserService;

    /**
     * <p>
     * Default constructor initializing both Smart BCC and Team Inbox services and managers.
     * </p>
     */
    public EmailAddonsInitializer() {
        this.smartBccService = SmartBccService.getInstance();
        this.smartBccManager = SmartBccManager.getInstance();
        this.teamInboxService = TeamInboxService.getInstance();
        this.teamInboxManager = TeamInboxManager.getInstance();
        this.iamUserService = UserService.getInstance();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Executes the initialization process for both Smart BCC and Team Inbox add-ons.
     * Optimized to use a single user fetch call and shared container for both addons.
     * </p>
     */
    @Override
    public void execute(final CrmOrg crmOrg, final CrmUser crmUser) {
        final Long orgId = crmOrg.id();
        final Long userId = crmUser.id();

        LOGGER.info("Org Init Action: Email addons initialization started. Org ID: {}, User ID: {}", orgId, userId);

        smartBccManager.loadParentAddressesIntoContainer(List.of(smartBccService.getAccount()));
        teamInboxManager.loadParentAddressesIntoContainer(List.of(teamInboxService.getAccount()));

        final EventCallback<Collection<User>> userFetchCallBack = new EventCallback<>() {

            @Override
            public void onSuccess(final Collection<User> users) {
                initializeSmartBcc(orgId, userId, users);
                initializeTeamInbox(orgId, userId, users);

                LOGGER.info("Org Init Action: Email addons initialization completed. Org ID: {}, User ID: {}",
                        orgId, userId);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOGGER.error("Org Init Action: Email addons user fetch failed. Org ID: {}, Reason: {}",
                        orgId, throwable.getMessage());
            }
        };

        iamUserService.fetchUsers(crmOrg.getIamOrgId(), List.of(crmUser.getIamId()), userFetchCallBack);
    }

    /**
     * <p>
     * Initializes the Smart BCC add-on with the fetched users.
     * </p>
     *
     * @param orgId  Organization ID
     * @param userId User ID
     * @param users  Collection of IAM users
     */
    private void initializeSmartBcc(final Long orgId, final Long userId, final Collection<User> users) {
        final String serviceName = ServiceType.SMART_BCC.getName();

        try {
            final Row parentAddress = smartBccManager.getParentAddress();

            smartBccManager.loadEmailAddress(
                    orgId, userId, users,
                    new EmailAddress(smartBccService.generateSharedMailBoxAddress()),
                    parentAddress.getPKValue()
            );
        } catch (final Throwable throwable) {
            LOGGER.error("Org Init Action: {} address creation failed. Org ID: {}, Reason: {}",
                    serviceName, orgId, throwable.getMessage(), throwable);
        }
    }

    /**
     * <p>
     * Initializes the Team Inbox add-on with the fetched users.
     * </p>
     *
     * @param orgId  Organization ID
     * @param userId User ID
     * @param users  Collection of IAM users
     */
    private void initializeTeamInbox(final Long orgId, final Long userId, final Collection<User> users) {
        final String serviceName = ServiceType.TEAM_INBOX.getName();

        try {
            final Row parentAddress = teamInboxManager.getParentAddress();

            teamInboxManager.loadEmailAddress(
                    orgId, userId, users,
                    new EmailAddress(teamInboxService.generateSharedMailBoxAddress()),
                    parentAddress.getPKValue()
            );
        } catch (final Throwable throwable) {
            LOGGER.error("Org Init Action: {} address creation failed. Org ID: {}, Reason: {}",
                    serviceName, orgId, throwable.getMessage(), throwable);
        }
    }
}