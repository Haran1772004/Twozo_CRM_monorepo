package com.tlc.analytics.core.api.query;

import com.tlc.analytics.core.api.query.converter.ReportQueryConverterAPI;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * It responsible for converting report queries.
 *
 * @author jamesh
 * @version 1.0
 * @see ReportQueryConverterAPI
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class ReportsQueryConverter implements ReportQueryConverterAPI
{
    private static class Instance
    {
        private static final ReportsQueryConverter INSTANCE = new ReportsQueryConverter();
    }

    public static ReportsQueryConverter INSTANCE()
    {
        return Instance.INSTANCE;
    }
}
