package com.tlc.crm.entity.export.formatter;

import com.tlc.crm.entity.export.internal.formatter.DataExportFormatterImpl;
import com.tlc.crm.entity.export.internal.model.DataExportFormatRequest;

/**
 * <p>
 * Formatter for formatting data during the export process
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface DataExportFormatter {

    /**
     * <p>
     * Gets the {@link DataExportFormatter} instance
     * </p>
     *
     * @return The {@link DataExportFormatter} instance
     */
    static DataExportFormatter getInstance() {
        return DataExportFormatterImpl.getInstance();
    }

    /**
     * <p>
     * Formats the data based on {@link DataExportFormatRequest}
     * </p>
     *
     * @param dataExportFormatRequest Represents the {@link DataExportFormatRequest}
     */
    void format(final DataExportFormatRequest dataExportFormatRequest);
}
