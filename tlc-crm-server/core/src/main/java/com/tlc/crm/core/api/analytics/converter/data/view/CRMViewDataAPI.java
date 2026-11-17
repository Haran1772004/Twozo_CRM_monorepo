package com.tlc.crm.core.api.analytics.converter.data.view;

import com.tlc.crm.core.api.analytics.converter.data.CRMReportsData;

/**
 * The {@link CRMViewDataAPI} interface defines the contract for handling CRM view data processing.
 *
 * @author Jamesh
 * @version 1.0
 */
public interface CRMViewDataAPI
{
    /**
     * <p>
     * Provides an instance of CRMReportsData for chart data retrieval
     * </p>
     *
     * @return an instance of CRMChartData, implementing CRMReportsData for chart data access
     */
    default CRMReportsData getChartDataAPI()
    {
        return CRMChartData.INSTANCE();
    }

    /**
     * <p>
     * Provides an instance of CRMReportsData for bubble chart data retrieval
     * </p>
     *
     * @return an instance of CRMChartData, implementing CRMReportsData for bubble chart data access
     */
    default CRMReportsData getBubbleChartDataAPI()
    {
        return BubbleChartData.INSTANCE();
    }


    /**
     * <p>
     * Provides an instance of CRMReportsData for HeatMap data retrieval
     * </p>
     *
     * @return an instance of CRMHeatMapData, implementing CRMReportsData for Heat map data access
     */
    default CRMReportsData getHeapMapAPI()
    {
        return CRMHeatMapData.INSTANCE();
    }

    /**
     * <p>
     * Provides an instance of CRMReportsData for KPI data retrieval
     * </p>
     *
     * @return an instance of CRMKPIData, implementing CRMReportsData for KPI data access
     */
    default CRMReportsData getKPIDataAPI()
    {
        return CRMKPIData.INSTANCE();
    }
}
