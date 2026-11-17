package com.tlc.web.model.link;

import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.interval.DateInterval;

/**
 * <p>
 * Represents a {@link TemporaryLink} that expires after a certain period and define behavior for links
 * that are valid only for a limited time before becoming inaccessible.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see HttpLink
 */
public class TemporaryLink extends HttpLink {

    private static final byte ONE_DAY_IN_HOURS = 24;

    private long expireTime;

    public void setExpireTime(final long expireTime) {
        this.expireTime = expireTime;
    }

    public long getExpireTime() {
        return 0 == expireTime ? TemporalServiceProvider.get().plus(ONE_DAY_IN_HOURS, DateInterval.HOURS) : expireTime;
    }
}
