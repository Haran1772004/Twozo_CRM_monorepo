package com.tlc.crm.common.email.alert.models;

import com.tlc.crm.common.models.Action;
import com.tlc.notification.model.email.template.TemplateType;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Which contains the different types of {@link EmailNotificationTemplate}
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public enum EmailNotificationTemplate implements TemplateType {

    USER_INVITATION("userInvitation", List.of(Action.CREATE)),
    ACTIVITY_GUEST_INVITATION("activityGuestInvitation", List.of(Action.CREATE, Action.UPDATE)),
    ORGANIZATION_EMAIL("organizationEmail", List.of(Action.CREATE)),
    ACTIVITY_ASSIGNED("activityAssigned", List.of(Action.CREATE, Action.UPDATE)),
    ACTIVITY_CREATED("activityCreated", List.of(Action.CREATE)),
    ACTIVITY_MODIFIED("activityModified", List.of(Action.UPDATE)),
    ACTIVITY_COMPLETED("activityCompleted", List.of(Action.CREATE, Action.UPDATE)),
    CONTACT_ASSIGNED("contactAssigned", List.of(Action.CREATE, Action.UPDATE)),
    COMPANY_ASSIGNED("companyAssigned", List.of(Action.CREATE, Action.UPDATE)),
    DEAL_ASSIGNED("dealAssigned", List.of(Action.CREATE, Action.UPDATE)),
    PRODUCT_ASSIGNED("productAssigned", List.of(Action.CREATE, Action.UPDATE)),
    DEAL_TRANSFERRED("dealTransferred", List.of(Action.UPDATE)),
    CONTACT_TRANSFERRED("contactTransferred", List.of(Action.UPDATE)),
    COMPANY_TRANSFERRED("companyTransferred", List.of(Action.UPDATE)),
    PRODUCT_TRANSFERRED("productTransferred", List.of(Action.UPDATE)),
    FORGOT_PASSWORD("forgotPassword", List.of(Action.CREATE)),
    DATA_EXPORT("dataExport", List.of(Action.CREATE)),
    SIGNUP_EMAIL_OTP_VERIFICATION("signUpEmailOtpVerification", List.of(Action.CREATE)),
    ANALYTICS_EXPORT("analyticsExport", List.of(Action.CREATE)),
    TEAM_INBOX_VERIFICATION_EMAIL("teamInboxVerification", List.of(Action.CREATE)),
    USER_DEACTIVATION("userDeactivation", List.of(Action.CREATE)),
    WEB_FORM_EMAIL("webFormEmail", List.of(Action.CREATE)),
    DNS_RECORD_SET_UP_EMAIL("domainDnsRecordsSetUp", List.of(Action.CREATE)),
    CONTACT_NOTE_MENTION("contactNoteMention", List.of(Action.CREATE, Action.UPDATE)),
    DEAL_NOTE_MENTION("dealNoteMention", List.of(Action.CREATE, Action.UPDATE)),
    COMPANY_NOTE_MENTION("companyNoteMention", List.of(Action.CREATE, Action.UPDATE)),
    ACTIVITY_NOTE_MENTION("activityNoteMention", List.of(Action.CREATE, Action.UPDATE)),
    CONTACT_NOTE_COMMENT_MENTION("contactNoteCommentMention", List.of(Action.CREATE, Action.UPDATE)),
    DEAL_NOTE_COMMENT_MENTION("dealNoteCommentMention", List.of(Action.CREATE, Action.UPDATE)),
    COMPANY_NOTE_COMMENT_MENTION("companyNoteCommentMention", List.of(Action.CREATE, Action.UPDATE)),
    ;

    private static final Map<String, EmailNotificationTemplate> TEMPLATE_MAP;

    static {
        TEMPLATE_MAP = new HashMap<>();

        for (final EmailNotificationTemplate template : values()) {
            TEMPLATE_MAP.put(template.name, template);
        }
    }

    private final String name;
    private final Collection<Action> types;

    EmailNotificationTemplate(final String name, final Collection<Action> types) {
        this.name = name;
        this.types = types;
    }

    /**
     * {@inheritDoc}
     *
     * @return The template type name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * <p>
     * Retrieves a template by its name String.
     * </p>
     *
     * @param name The template name (e.g., "userInvitation")
     * @return The corresponding {@link EmailNotificationTemplate}, or null if not found
     */
    public static EmailNotificationTemplate getByName(final String name) {
        return TEMPLATE_MAP.get(name);
    }

    /**
     * <p>
     * Gets a collection of {@link Action}
     * </p>
     *
     * @return The collection  of  {@link Action}
     */
    public Collection<Action> getTypes() {
        return types;
    }

    /**
     * <p>
     * Checks if the current template  correspond to the transferred template or not
     * </p>
     *
     * @returnb true if the template is a transferred template  false otherwise
     */
    public boolean isTransferredTemplate() {
        return switch (this) {
            case COMPANY_TRANSFERRED, CONTACT_TRANSFERRED, DEAL_TRANSFERRED, PRODUCT_TRANSFERRED -> true;
            default -> false;
        };
    }

    /**
     * <p>
     * Checks if the current template represents an assigned template
     * </p>
     *
     * @return true if the template is an assigned template, false otherwise
     */
    public boolean isAssignedTemplate() {
        return switch (this) {
            case COMPANY_ASSIGNED, CONTACT_ASSIGNED, DEAL_ASSIGNED, ACTIVITY_ASSIGNED, PRODUCT_ASSIGNED -> true;
            default -> false;
        };
    }

    /**
     * <p>
     * Checks if the current template is associated with an email alert template
     * </p>
     *
     * @return true if the current object is linked to an email alert template,
     * false otherwise.
     */
    public boolean isEmailAlertTemplate() {
        return switch (this) {
            case COMPANY_ASSIGNED, CONTACT_ASSIGNED, DEAL_ASSIGNED, ACTIVITY_ASSIGNED, ACTIVITY_CREATED, ACTIVITY_MODIFIED,
                 CONTACT_NOTE_MENTION, DEAL_NOTE_MENTION, COMPANY_NOTE_MENTION,
                 CONTACT_NOTE_COMMENT_MENTION, DEAL_NOTE_COMMENT_MENTION, COMPANY_NOTE_COMMENT_MENTION,
                 ACTIVITY_NOTE_MENTION -> true;
            default -> false;
        };
    }

    /**
     * <p>
     * Checks if the current template is a note mention template
     * </p>
     *
     * @return true if the current template is a note mention template, false otherwise
     */
    public boolean isNoteMentionTemplate() {
        return switch (this) {
            case CONTACT_NOTE_MENTION, DEAL_NOTE_MENTION,
                 COMPANY_NOTE_MENTION, ACTIVITY_NOTE_MENTION -> true;
            default -> false;
        };
    }

    /**
     * <p>
     * Checks if the current template is a note comment mention template
     * </p>
     *
     * @return true if the current template is a note comment mention template, false otherwise
     */
    public boolean isNoteCommentMentionTemplate() {
        return switch (this) {
            case CONTACT_NOTE_COMMENT_MENTION, DEAL_NOTE_COMMENT_MENTION,
                 COMPANY_NOTE_COMMENT_MENTION -> true;
            default -> false;
        };
    }

    /**
     * <p>
     * Checks if the current template is an activity private note mention template
     * </p>
     *
     * @return true if the current template is an activity private note mention template, false otherwise
     */
    public boolean isPrivateNoteMentionTemplate() {
        return ACTIVITY_NOTE_MENTION == this;
    }
}
