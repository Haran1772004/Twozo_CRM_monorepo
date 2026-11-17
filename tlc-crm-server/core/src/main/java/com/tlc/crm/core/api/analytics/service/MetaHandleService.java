package com.tlc.crm.core.api.analytics.service;

import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.config.model.table.TableDetail;
import com.tlc.crm.core.api.analytics.service.impl.MetaHandleServiceImpl;

import java.util.Collection;

/**
 * Service contract for accessing cached analytics metadata tables and columns.
 * Author: karthick V
 * Version: 1.0
 */
public interface MetaHandleService {

    /**
     * Returns singleton metadata service implementation.
     *
     * @return The {@link MetaHandleService} instance.
     */
    static MetaHandleService getInstance() {
        return MetaHandleServiceImpl.getInstance();
    }

    /**
     * Returns table details for a module identifier.
     *
     * @param moduleId Represents module id.
     * @return Collection of {@link TableDetail}.
     */
    Collection<TableDetail> getTableDetails(final Integer moduleId);

    /**
     * Returns column details for a table identifier.
     *
     * @param tableId Represents table id.
     * @return Collection of {@link ColumnDetail}.
     */
    Collection<ColumnDetail> getColumnDetails(final Long tableId);
}