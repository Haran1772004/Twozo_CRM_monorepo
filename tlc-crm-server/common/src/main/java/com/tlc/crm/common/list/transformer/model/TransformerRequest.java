package com.tlc.crm.common.list.transformer.model;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.l10n.temporal.model.TimezoneInfo;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * A model which represents the {@link TransformerRequest}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public record TransformerRequest(long orgId, Collection<Map<String, Object>> records, TimezoneInfo timezoneInfo,
                                 CrmRequest.ClientType clientType, ListResponseStructureType listResponseStructureType) {

    public TransformerRequest(final long orgId, final Collection<Map<String, Object>> records, final TimezoneInfo timezoneInfo) {
        this(orgId, records, timezoneInfo, CrmRequest.ClientType.WEB, ListResponseStructureType.COMMON);
    }

    public TransformerRequest(final long orgId, final Collection<Map<String, Object>> records, final TimezoneInfo timezoneInfo, final CrmRequest.ClientType clientType) {
        this(orgId, records, timezoneInfo, clientType, ListResponseStructureType.COMMON);
    }
}
