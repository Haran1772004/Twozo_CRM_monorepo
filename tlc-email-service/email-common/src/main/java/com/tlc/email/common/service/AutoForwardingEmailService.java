package com.tlc.email.common.service;

import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.StringUtil;
import com.tlc.email.common.fields.EmailFields;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * Provides utility methods for identifying and verifying auto-forwarding emails
 * received from supported email providers such as Gmail and Outlook.
 * </p>
 *
 * @author Mohamed Yasar K
 * version 1.0
 */
public final class AutoForwardingEmailService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

    private static final String DELIMITER = ",";
    private static final String BLANK = "";

    private static final String[] KEYWORDS = {
            "receive mail from",
            "forwarding to"
    };

    private final String[] gmailVerifiedSenders;
    private final String[] outlookVerifiedSenders;

    private static class InstanceHolder {
        private static final AutoForwardingEmailService INSTANCE = new AutoForwardingEmailService();
    }

    private AutoForwardingEmailService() {
        final Configurations configurations = ConfHolder.getInstance()
                .getProperties(EmailFields.EMAIL)
                .getNestedConfigs(EmailFields.ADD_ON_SERVICES)
                .getNestedConfigs(EmailFields.TEAM_INBOX)
                .getNestedConfigs(EmailFields.VERIFICATION)
                .getNestedConfigs(EmailFields.VERIFIED_FROM_ADDRESSES);

        this.gmailVerifiedSenders = StringUtil.splitAndTrim(configurations.getConfig(EmailFields.GMAIL), StringUtil.Delimiter.COMMA);
        this.outlookVerifiedSenders = StringUtil.splitAndTrim(configurations.getConfig(EmailFields.OUTLOOK), StringUtil.Delimiter.COMMA);
    }

    public static AutoForwardingEmailService getInstance() {
        return AutoForwardingEmailService.InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Extracts the forwarding email address from Gmail or Outlook verification subjects.
     * </p>
     *
     * @param subject The subject line of the email.
     * @return The extracted forwarding email address, or an empty string if none is found.
     */
    public String extractForwardingAddress(final String subject) {
        if (Objects.isNull(subject) || subject.isBlank()) {
            return BLANK;
        }
        final String value = subject.trim().toLowerCase();

        for (final String keyword : KEYWORDS) {
            final int index = value.indexOf(keyword);

            if (index >= 0) {
                final String email = findEmail(subject.substring(index));

                if (!email.isEmpty()) {
                    return email;
                }
            }
        }

        return findEmail(subject);
    }

    /**
     * <p>
     * Checks whether the given sender address belongs to a valid
     * auto-forwarding source such as Gmail or Outlook.
     * </p>
     *
     * @param from The "From" address of the email.
     * @return {@code true} if the address is a recognized forwarding system sender {@code false} otherwise.
     */
    public boolean isValidForwardingSource(final String from) {
        if (Objects.isNull(from) || from.isBlank()) {
            return false;
        }
        final String value = from.trim().toLowerCase();

        return isValidForwardingSource(value, gmailVerifiedSenders) || isValidForwardingSource(value, outlookVerifiedSenders);
    }

    /**
     * <p>
     * Checks whether the given value exists within the specified list.
     * </p>
     *
     * @param value           The string value to check.
     * @param verifiedSenders Represent the verified senders.
     * @return {@code true} if the value matches any item in the list; otherwise {@code false}.
     */
    private boolean isValidForwardingSource(final String value, final String[] verifiedSenders) {
        for (final String senderAddress : verifiedSenders) {

            if (value.equals(senderAddress)) {
                return true;
            }
        }

        return false;
    }

    /**
     * <p>
     * Finds and returns the first valid email address within the given text.
     * </p>
     *
     * @param text The text content to search for an email address.
     * @return The matched email address, or an empty string if no match is found.
     */
    private String findEmail(final String text) {
        final Matcher matcher = EMAIL_PATTERN.matcher(text);

        return matcher.find() ? matcher.group() : BLANK;
    }
}