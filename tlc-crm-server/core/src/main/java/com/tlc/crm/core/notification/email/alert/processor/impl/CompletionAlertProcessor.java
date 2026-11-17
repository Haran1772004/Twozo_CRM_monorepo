package com.tlc.crm.core.notification.email.alert.processor.impl;

import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.core.notification.email.alert.handler.EmailAlertHandler;
import com.tlc.crm.core.notification.email.alert.processor.EmailAlertProcessor;
import com.tlc.crm.core.notification.email.alert.util.MessageTruncator;
import com.tlc.crm.entity.features.api.note.model.Note;

import java.util.Collection;
import java.util.Objects;


/**
 * <p>
 * Processor which process email alerts for completion action.
 * </p>
 *
 * @author Mohammed Asif J.
 * @version 1.0
 */
public class CompletionAlertProcessor implements EmailAlertProcessor {

    private final EmailAlertHandler emailAlertHandler;
    private final SourceActivityManager sourceActivityManager;
    private final MessageTruncator messageTruncator;

    private CompletionAlertProcessor() {
        this.emailAlertHandler = EmailAlertHandler.getInstance();
        this.sourceActivityManager = SourceActivityManager.getInstance();
        this.messageTruncator = MessageTruncator.getInstance();
    }

    /**
     * <p>
     * Holder class for {@link CompletionAlertProcessor} instance
     * </p>
     */
    private static class InstanceHolder {
        private static final CompletionAlertProcessor INSTANCE = new CompletionAlertProcessor();
    }

    /**
     * <p>
     * Gets the instance of {@link CompletionAlertProcessor}
     * </p>
     *
     * @return The {@link CompletionAlertProcessor} instance
     */
    public static CompletionAlertProcessor getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Processes the email alert for completion action
     * </p>
     *
     * @param orgId    Represents the organization ID
     * @param userId   Represents the user ID
     * @param template Represents the template of the email alert
     * @param ids      Represents the collection of model IDs which triggered the email alert
     */
    @Override
    public void process(final long orgId, final long userId,
                        final EmailNotificationTemplate template, final Collection<Long> ids) {
        final Collection<ActivityHolder> holders = sourceActivityManager.get(orgId, ids);

        holders.forEach(activityHolder -> {
            final Note note = activityHolder.getActivity().getNote();

            if (Objects.nonNull(note)) {
                messageTruncator.truncate(note);
            }
        });

        if (!holders.isEmpty()) {
            emailAlertHandler.sendEmailAlerts(orgId, userId,
                    holders, null, Action.CREATE, template.getName());
        }
    }
}