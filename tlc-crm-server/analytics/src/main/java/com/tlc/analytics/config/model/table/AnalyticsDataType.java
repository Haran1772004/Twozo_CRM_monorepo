package com.tlc.analytics.config.model.table;

import com.tlc.analytics.config.internal.ConfigErrorCodes;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.fields.models.StoreType;

import lombok.Getter;

import java.util.Objects;

/**
 * {@link AnalyticsDataType} is an enumeration representing different types of data that can be associated with columns.
 * It includes numerical, string, and date data types, each identified by a unique type code.
 * Use this enum to define and manage the data types of columns.
 *
 * @author Aswath
 * @version 1.0
 */
@Getter
public enum AnalyticsDataType {
    NUMERICAL(1),
    STRING(2),
    DATE(3);

    private final int typeCode;

    /**
     * Constructor for {@link AnalyticsDataType} enum.
     *
     * @param typeCode The unique code associated with the data type.
     */
    AnalyticsDataType(int typeCode) {
        this.typeCode = typeCode;
    }

    /**
     * Resolves an analytics data type code to its corresponding base {@link AnalyticsDataType}.
     *
     * @param dataTypeCode The ClickHouse data type code.
     * @return The base AnalyticsDataType (NUMERICAL, STRING, or DATE).
     */
    public static AnalyticsDataType resolveFromAnalyticsDataType(final int dataTypeCode) {
        return resolveFromAnalyticsDataType(dataTypeCode, null);
    }

    /**
     * Resolves an analytics data type code to its corresponding base {@link AnalyticsDataType},
     * with special handling for JSON fields based on the provided {@link StoreType}.
     *
     * @param dataTypeCode The ClickHouse data type code.
     * @param storeType    The source store type for JSON field resolution (may be null).
     * @return The base AnalyticsDataType (NUMERICAL, STRING, or DATE).
     */
    public static AnalyticsDataType resolveFromAnalyticsDataType(final int dataTypeCode, final StoreType storeType) {

        if (Objects.nonNull(storeType)) {
            final AnalyticsDataType resolved = resolveJsonFieldDataType(storeType);

            return Objects.nonNull(resolved) ? resolved : resolveBaseDataType(dataTypeCode);
        } else {
            return resolveBaseDataType(dataTypeCode);
        }
    }

    /**
     * Resolves the base {@link AnalyticsDataType} from a ClickHouse data type string.
     *
     * @param dataTypeCode The ClickHouse data type string (lowercase).
     * @return The base AnalyticsDataType (NUMERICAL, STRING, or DATE).
     */
    private static AnalyticsDataType resolveBaseDataType(final int dataTypeCode) {
        return switch (dataTypeCode) {
            case 1 -> NUMERICAL;
            case 2 -> STRING;
            case 3 -> DATE;
            default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_DATA_TYPE);
        };
    }

    /**
     * Resolves the data type for JSON-backed fields based on CRM store type.
     *
     * @param storeType The field store type.
     * @return The resolved base data type.
     */
    private static AnalyticsDataType resolveJsonFieldDataType(final StoreType storeType) {
        return switch (storeType) {
            case SINGLE_NUMBER, OWNER, COMPANY, CONTACT, MONETARY -> NUMERICAL;
            case SINGLE_STRING, EMAIL, PHONE, DROPDOWN, MULTI_SELECT -> STRING;
            case DATE -> DATE;
            default -> null;
        };
    }
}
