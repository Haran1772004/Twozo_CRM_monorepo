package com.tlc.crm.common.export.action;

import com.tlc.crm.common.export.req.DataExportRequest;

/**
 * Provides an abstract way to export the required data based on the request parameters
 *
 * @author Selvakumar
 * @version 1.0
 */
public interface DataExportAction {

    /**
     * Does the data export action
     *
     * @param dataExportRequest Holds the data export request parameters
     * @return If the data export is successful, it will return true otherwise false will be returned
     */
    boolean doExport(final DataExportRequest dataExportRequest);
}
