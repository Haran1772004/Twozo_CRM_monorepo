package com.tlc.crm.common.export.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.internal.export.code.DataExportErrorCodes;

/**
 * <p>
 * Holds the export providers
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public enum ExportProviderType {

    DETAIL("detail"),
    LIST("list");

    private final String type;

    ExportProviderType(final String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    /**
     * <p>
     * Gets the {@link ExportProviderType}
     * </p>
     *
     * @param type Represents the export type name
     * @return The {@link ExportProviderType}
     */
    public static ExportProviderType get(final String type) {
        return switch (type) {
            case "detail" -> ExportProviderType.DETAIL;
            case "list" -> ExportProviderType.LIST;
            default -> throw ErrorCode.get(DataExportErrorCodes.UNSUPPORTED_EXPORT_TYPE);
        };
    }
}
