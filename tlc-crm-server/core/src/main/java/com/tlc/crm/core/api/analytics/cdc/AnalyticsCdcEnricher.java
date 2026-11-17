package com.tlc.crm.core.api.analytics.cdc;

import com.tlc.cdc.core.enrichment.CdcEnricher;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.core.api.analytics.general.keys.AnalyticsCdcKeys;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;

/**
 * Interface for enriching CRM module data with analytics information.
 *
 * <p>This interface defines the contract for analytics enrichers that transform CDC event data
 * from CRM modules into analytics-ready enriched records. Different implementations handle
 * enrichment for specific CRM modules such as Company, Contact, Deal, Product, Activity, and Territory.</p>
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
interface AnalyticsCdcEnricher extends CdcEnricher {

    /**
     * Retrieves the appropriate enricher instance for the specified CRM module.
     *
     * @param module The name of the CRM module (case-insensitive).
     * @return The AnalyticsCrmEnricher instance for the specified module.
     */
    static AnalyticsCdcEnricher get(final String module) {
        return switch (module) {
            case AnalyticsCdcKeys.COMPANY -> AnalyticsCrmCompanyEnricher.getInstance();
            case AnalyticsCdcKeys.CONTACT -> AnalyticsCrmContactEnricher.getInstance();
            case AnalyticsCdcKeys.DEAL -> AnalyticsCrmDealEnricher.getInstance();
            case AnalyticsCdcKeys.PRODUCT, AnalyticsCdcKeys.DEAL_LINE_ITEMS ->
                    AnalyticsCrmProductEnricher.getInstance();
            case AnalyticsCdcKeys.ACTIVITY -> AnalyticsCrmActivityEnricher.getInstance();
            case AnalyticsCdcKeys.TERRITORY -> AnalyticsCrmTerritoryEnricher.getInstance();
            case AnalyticsCdcKeys.USER -> AnalyticsCrmUserEnricher.getInstance();
            case AnalyticsCdcKeys.ENTITY_INSIGHT -> AnalyticsCrmEmailEnricher.getInstance();
            default -> throw ErrorCode.get(AnalyticsErrorCodes.INVALID_ACTION_TYPE);
        };
    }

}
