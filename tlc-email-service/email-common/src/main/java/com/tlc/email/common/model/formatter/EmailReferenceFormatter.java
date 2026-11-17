package com.tlc.email.common.model.formatter;

import java.util.Arrays;
import java.util.Collection;

/**
 * <p>
 * Implements the formatting action for email
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class EmailReferenceFormatter {

    private EmailReferenceFormatter() {
    }

    /**
     * <p>
     * Formats a {@link Collection} of references and appends the provided message ID.
     * </p>
     *
     * @param references A collection of reference strings (previous message IDs or email references).
     * @param messageId  The unique identifier of the current email message.
     * @return A formatted string combining all references and the message ID.
     * @see String#format(String, Object...)
     */
    public static String formatReferences(final Collection<String> references, final String messageId) {
        return String.format("%s %s", String.join(" ", references), messageId);
    }

    /**
     * <p>
     * Formats the references of inbound email message for retrieving
     * </p>
     *
     * @param reference Represents the reference
     * @return Formatted reference
     */
    public static String formatIpReferences(final String reference) {
        return reference.replaceAll("@([^@>]+)>", "@[$1]>");
    }

    /**
     * <p>
     * Splits a string of space-separated references into a {@link Collection} of individual references.
     * </p>
     *
     * @param references A string containing multiple references, separated by spaces.
     * @return A collection of reference parsed from the input.
     * @see Arrays#asList(Object...)
     */
    public static Collection<String> splitReferences(final String references) {
        return Arrays.asList(references.split(" "));
    }

    /**
     * <p>
     * Replaces the asterisks from the email address
     * </p>
     *
     * @param blockedAddress Represents the blocked email address
     * @return domain of email address
     */
    public static String removeAsterisks(final String blockedAddress) {
        return blockedAddress.replace("*", "");
    }
}