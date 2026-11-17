package com.tlc.crm.plan.and.billing.util;

import com.tlc.commons.util.StringUtil;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.l10n.temporal.TemporalServiceProvider;

import java.util.Optional;
import java.util.StringJoiner;

/**
 * Utility class for generating and extracting transaction IDs.
 * <p>
 * The transaction ID is constructed using organization ID, user ID, and current UTC datetime,
 * joined by underscores.
 * <p>
 * Provides methods for parsing organization ID from a transaction ID string.
 *
 * @author Sathish
 * @version 1.0
 */
public final class TransactionIdUtil {

    /**
     * Generates a transaction ID by concatenating orgId, userId, and current UTC datetime,
     * separated by underscores.
     *
     * @param orgId  the organization ID
     * @param userId the user ID
     * @return the generated transaction ID string
     */
    public static String generateTransactionId(final Long orgId, final Long userId) {
        final StringJoiner transactionIdBuilder = new StringJoiner(StringUtil.Delimiter.UNDER_SCORE.getDelimiter());

        transactionIdBuilder.add(orgId.toString());
        transactionIdBuilder.add(userId.toString());
        transactionIdBuilder.add(TemporalServiceProvider.get().getUtcDateTime().toString());
        return transactionIdBuilder.toString();
    }

    /**
     * Attempts to extract the organization ID from the transaction ID string.
     *
     * @param transactionId the transaction ID string
     * @return an OptionalLong containing the parsed organization ID if valid, or empty if parsing fails
     */
    public static Optional<Long> extractOrgId(final String transactionId) {
        final String orgIdString = transactionId.split(StringUtil.Delimiter.UNDER_SCORE.getDelimiter())[0];

        try {
            return Optional.of(NumberUtils.parseNumber(orgIdString).longValue());
        } catch (final Exception exception) {
            return Optional.empty();
        }
    }
}
