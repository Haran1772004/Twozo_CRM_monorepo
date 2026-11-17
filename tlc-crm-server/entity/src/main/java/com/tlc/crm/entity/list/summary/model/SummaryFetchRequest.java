package com.tlc.crm.entity.list.summary.model;

import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.sql.common.api.dml.SelectQuery;

/**
 * <p>
 * Holds the details for fetching the list summary.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public record SummaryFetchRequest(long orgId, long totalCount, I18nResolutionAdapter i18NResolutionAdapter, SelectQuery selectQuery) {
}