package com.tlc.crm.email.core.handler;

import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.core.notification.email.api.manager.CoreMailManager;
import com.tlc.crm.core.notification.email.api.manager.addon.bcc.SmartBccManager;
import com.tlc.crm.core.notification.email.api.model.EntityThread;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.notification.email.api.model.email.SmartBcc;
import com.tlc.crm.email.core.OutboundEmailLinkerService;
import com.tlc.crm.email.notification.email.sync.api.EmailSyncManager;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.Thread;
import com.tlc.email.common.model.ThreadHolder;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.type.Email;
import com.tlc.iam.client.UserService;
import com.tlc.iam.common.models.User;
import com.tlc.web.client.event.EventCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Handles Smart Bcc email data loading actions by fetching and storing emails
 * into the system and linking them with CRM entities.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class SmartBccDataLoadActionHandler extends AbstractEmailDataLoadActionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmartBccDataLoadActionHandler.class);

    private final OutboundEmailLinkerService outboundEmailLinkerService;
    private final EmailSyncManager emailSyncManager;
    private final UserService iamUserService;
    private final OrganizationService organizationService;
    private final com.tlc.crm.common.iam.service.UserService userService;
    private final CoreMailManager mailManager;
    private final SmartBccManager smartBccManager;

    private SmartBccDataLoadActionHandler() {
        this.outboundEmailLinkerService = OutboundEmailLinkerService.getInstance();
        this.emailSyncManager = EmailSyncManager.getInstance();
        this.iamUserService = UserService.getInstance();
        this.organizationService = OrganizationService.getInstance();
        this.userService = com.tlc.crm.common.iam.service.UserService.getInstance();
        this.mailManager = CoreMailManager.getInstance();
        this.smartBccManager = SmartBccManager.getInstance();
    }

    /**
     * <p>
     * Holder class for the singleton instance of {@link SmartBccDataLoadActionHandler}.
     * </p>
     */
    private static class Instance {
        private static final EmailAddOnServiceDataLoadActionHandler INSTANCE = new SmartBccDataLoadActionHandler();
    }

    /**
     * <p>
     * Returns the singleton instance of {@link EmailAddOnServiceDataLoadActionHandler}.
     * </p>
     *
     * @return the singleton {@link SmartBccDataLoadActionHandler} instance
     */
    public static EmailAddOnServiceDataLoadActionHandler getInstance() {
        return SmartBccDataLoadActionHandler.Instance.INSTANCE;
    }


    /**
     * <p>
     * Returns a new {@link SmartBcc} email with the message category.
     * </p>
     *
     */
    @Override
    public void handle(final ThreadHolder threadHolder) {
        fetchAndStoreEmails(threadHolder);
    }

    /**
     * <p>
     * Returns a new {@link SmartBcc} email with the message category
     * </p>
     *
     * @return a new instance of {@link SmartBcc}
     */
    protected EntityEmail getEmail() {
        final EntityEmail entityEmail = new SmartBcc();

        entityEmail.setMessageCategory(Email.MessageCategory.OUTBOUND);

        return entityEmail;
    }

    /**
     * <p>
     * Fetches and stores the emails from the thread
     * </p>
     *
     * @param threadHolder Represents the {@link ThreadHolder}
     */
    private void fetchAndStoreEmails(final ThreadHolder threadHolder) {
        final Collection<com.tlc.email.common.model.Thread> threads = threadHolder.getThreads();
        final Collection<String> fromAddresses = threads.stream().flatMap(thread -> thread.getEmails().stream())
                .map(email -> email.getSender().getEmailAddress()).filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
        final EventCallback<Collection<User>> callback = new EventCallback<>() {

            /**
             * {@inheritDoc}
             *
             * @param users Response from the consumer
             */
            @Override
            public void onSuccess(final Collection<User> users) {
                final Map<String, User> iamUsers = users.stream().collect(Collectors.toMap(User::getEmail, Function.identity()));

                for (final Thread thread : threads) {
                    thread.getEmails().stream().findFirst().ifPresent(email -> {
                        final EmailAddress sender = email.getSender();
                        final String fromAddress = sender.getEmailAddress();
                        final User user = iamUsers.get(fromAddress);

                        if (Objects.nonNull(user)) {
                            final Long orgId = organizationService.getOrgId(user.orgId());
                            final Long crmUserId = userService.getUserId(orgId, user.id());

                            secureProcess(orgId, crmUserId, () -> {
                                final EmailMetaData metaData = email.getMetaData();

                                if (mailManager.messageExists(orgId, metaData.id())) {
                                    sender.setId(emailSyncManager.getOrInitEmailId(orgId, fromAddress));
                                    final Collection<String> addresses = outboundEmailLinkerService.getEmailRecipients(email);
                                    final String currentMailBox = metaData.getCurrentMailboxId();

                                    if (smartBccManager.hasValidSmartBccAddress(orgId, List.of(currentMailBox))) {
                                        final EntityThread smartBccThread = composeThread(orgId, crmUserId, thread.id(), email);

                                        addresses.remove(smartBccManager.fetchSmartBccAddress(orgId));

                                        if (!addresses.isEmpty()) {
                                            preProcess(smartBccThread);
                                            outboundEmailLinkerService.createThread(smartBccThread);
                                        }
                                    }
                                }
                            });
                        }
                    });
                }
                LOGGER.debug("Smart Bcc: Email retrieved successfully");
            }

            /**
             * {@inheritDoc}
             *
             * @param cause Cause of the failure
             */
            @Override
            public void onFailure(final Throwable cause) {
                LOGGER.debug("Smart Bcc: Smart bcc email retrieved failed, Reason : {}", cause.getMessage());
            }
        };
        iamUserService.fetchUsersByEmails(fromAddresses, callback);
    }
}