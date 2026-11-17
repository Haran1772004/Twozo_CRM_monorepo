package com.tlc.analytics.config.manager.table;

import com.tlc.analytics.config.manager.AnalyticsConfigManager;
import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.config.model.table.TableDetail;
import com.tlc.analytics.config.model.table.TableRelation;

/**
 * <p>
 * The TableConfigAPI interface provides access to various configuration managers responsible for handling
 * different aspects of table-related configurations in the analytics system.
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
public interface TableConfigManager
{
    /**
     * <p>
     * Gets the {@link TableDetail} manager instance
     * </p>
     *
     * @return The {@link TableDetail} manager instance
     */
    default AnalyticsConfigManager<TableDetail> getTableManager()
    {
        return TableManager.INSTANCE();
    }

    /**
     * <p>
     * Gets the {@link ColumnDetail} manager instance
     * </p>
     *
     * @return The {@link ColumnDetail} manager instance
     */
    default AnalyticsConfigManager<ColumnDetail> getColumnManager()
    {
        return ColumnManager.INSTANCE();
    }

    /**
     * <p>
     * Gets the {@link TableRelation} manager instance
     * </p>
     *
     * @return The {@link TableRelation} manager instance
     */
    default AnalyticsConfigManager<TableRelation> getLookupManager()
    {
        return LookupManager.INSTANCE();
    }
}
