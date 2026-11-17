package com.tlc.analytics.config.action;

import com.tlc.crm.common.action.CrmResponse;

import com.tlc.l10n.temporal.model.TimezoneInfo;

import lombok.NonNull;

/**
 * <p>
 * A record that holds the analytics request
 * </p>
 *
 * @param orgId        Represents an organisation id
 * @param userId       Represents the user id
 * @param timezoneInfo Represents the {@link TimezoneInfo} details
 * @author pandiarajkumarG
 * @version 1.0
 */
public record AnalyticsRequest( @NonNull Long orgId,
                                @NonNull Long userId,
                                @NonNull Long roleId,
                                @NonNull TimezoneInfo timezoneInfo,
                                CrmResponse crmResponse )

{
    public AnalyticsRequest ( final @NonNull Long orgId,
                              final @NonNull Long userId,
                              final @NonNull Long roleId,
                              final @NonNull TimezoneInfo timeZoneInfo )
    {
        this( orgId, userId, roleId, timeZoneInfo, null);
    }
}
