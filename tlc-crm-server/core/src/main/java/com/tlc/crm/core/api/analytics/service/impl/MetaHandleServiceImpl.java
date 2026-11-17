package com.tlc.crm.core.api.analytics.service.impl;

import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.config.model.table.TableDetail;
import com.tlc.analytics.service.column.ColumnDetailsService;
import com.tlc.analytics.service.table.TableDetailService;
import com.tlc.crm.core.api.analytics.service.MetaHandleService;

import java.util.Collection;

/**
 * Implementation of {@link MetaHandleService} that resolves analytics metadata
 * via the remote cache-backed {@link TableDetailService} and {@link ColumnDetailsService}.
 *
 * @author Karthick V
 * @version 2.0
 */
public class MetaHandleServiceImpl implements MetaHandleService {

    private final TableDetailService tableDetailService;
    private final ColumnDetailsService columnDetailsService;

    /**
     * <p>
     * Private constructor initializing service dependencies.
     * </p>
     */
    private MetaHandleServiceImpl() {
        this.tableDetailService = TableDetailService.getInstance();
        this.columnDetailsService = ColumnDetailsService.getInstance();
    }

    private static class Instance {
        private static final MetaHandleServiceImpl INSTANCE = new MetaHandleServiceImpl();
    }

    /**
     * <p>
     * Retrieves the singleton instance of {@link MetaHandleServiceImpl}.
     * </p>
     *
     * @return {@link MetaHandleServiceImpl}
     */
    public static MetaHandleServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<TableDetail> getTableDetails(final Integer moduleId) {
        return tableDetailService.getBySourceId(moduleId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ColumnDetail> getColumnDetails(final Long tableId) {
        return columnDetailsService.getByTableId(tableId);
    }
}