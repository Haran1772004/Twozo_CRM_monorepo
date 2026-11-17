package com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout;

import com.tlc.analytics.config.manager.AnalyticsConfigManager;
import com.tlc.analytics.config.model.view.ViewDetails;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.core.api.analytics.integ.general.analytics.AnalyticsServiceAPI;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewManagerAPI;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMChildViewDetails;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * <p>
 * A helper class that provides a way to delete child views from the dataContainer
 * </p>
 *
 * @author ryan
 * @version 1.0
 */
public class CRMDashboardViewRemover {
    private static final Logger LOGGER = LoggerFactory.getLogger(CRMDashboardViewRemover.class);

    /**
     * Deletes analytics view details from the data store for a specific ids.
     *
     * @param orgId          The ID of the organization whose analytics view details are to be deleted.
     * @param ids            A collection of primary key IDs representing the rows to be deleted.
     * @param dataContainer  The {@link DataContainer} to which the fetched rows will be appended and from which they will be deleted.
     * @throws RuntimeException If an error occurs during data fetching or deletion. Throws an {@link ErrorCode} with {@code AnalyticsErrorCodes.CREATE_ACTION_FAILED}.
     */
    public static void deleteUnMappedAnaViewDetails( final Long orgId, final Collection<Long> ids,
                                                     final DataContainer dataContainer,
                                                     final AnalyticsServiceAPI analyticsServiceAPI)
    {
        final AnalyticsConfigManager<ViewDetails> analyticsConfigManager = analyticsServiceAPI.getViewDetailsManager();
        final Table table = analyticsConfigManager.getTable();
        final WhereClause whereClause = new WhereClause( Criteria.in( table.getPKColumn(), ids ) );
        final SelectQuery selectQuery = SelectQuery.get( table );
        selectQuery.setWhereClause( whereClause );

        try
        {
            final DataContainer viewDetailsContainer = analyticsConfigManager.getOrgDataStore( orgId ).get( selectQuery );
            dataContainer.append( viewDetailsContainer );
            dataContainer.getRows( table, whereClause ).forEach( dataContainer::deleteRow );
            LOGGER.info( "CRM Analytics: Successfully loaded values in the dataContainer for orgId: {}", orgId );
        }
        catch ( Exception error )
        {
            LOGGER.error( "CRM Analytics: Failed to load dataContainer for orgId: {}. Reason: {}", orgId, error );
            throw ErrorCode.get( AnalyticsErrorCodes.CREATE_ACTION_FAILED, "i18n.analytics.create.action.failed" );
        }
    }

    /**
     * Deletes CRM child view details from the data store for a specific ids.
     *
     * @param orgId          The ID of the organization whose CRM child view details are to be deleted.
     * @param ids            A collection of primary key IDs representing the rows to be deleted.
     * @param dataContainer  The {@link DataContainer} to which the fetched rows will be appended and from which they will be deleted.
     * @throws RuntimeException If an error occurs during data fetching or deletion. Throws an {@link ErrorCode} with {@code AnalyticsErrorCodes.CREATE_ACTION_FAILED}.
     */
    public static void deleteUnMappedCrmDetails( final Long orgId, final Collection<Long> ids,
                                                 final DataContainer dataContainer,
                                                 final CRMViewManagerAPI crmViewManagerAPI)
    {
        final CRMViewConfigManager<CRMChildViewDetails> crmViewConfigManager = crmViewManagerAPI.getCRMChildViewManager();
        final Table table = crmViewConfigManager.getTable();
        final WhereClause whereClause = new WhereClause( Criteria.in( table.getPKColumn(), ids ) );
        final SelectQuery selectQuery = SelectQuery.get( table );
        selectQuery.setWhereClause( whereClause );

        try
        {
            final DataContainer crmChildDetailsDataContainer = crmViewConfigManager.getOrgDataStore( orgId ).get( selectQuery );
            dataContainer.append( crmChildDetailsDataContainer );
            dataContainer.getRows( table, whereClause ).forEach( dataContainer::deleteRow );
            LOGGER.info( "CRM Analytics: Successfully loaded values in the dataContainer for orgId: {}", orgId );
        }
        catch ( Exception error )
        {
            LOGGER.error( "CRM Analytics: Failed to load dataContainer for orgId: {}. Reason: {}", orgId, error );
            throw ErrorCode.get( AnalyticsErrorCodes.CREATE_ACTION_FAILED, "i18n.analytics.create.action.failed" );
        }
    }
}
