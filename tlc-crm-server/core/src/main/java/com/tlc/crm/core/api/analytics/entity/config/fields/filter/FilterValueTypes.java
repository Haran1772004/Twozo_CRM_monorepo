package com.tlc.crm.core.api.analytics.entity.config.fields.filter;

import com.tlc.analytics.config.model.view.filter.OperatorType;
import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import lombok.Getter;

/**
 * <p>
 * Represents the filter value types
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@Getter
public enum FilterValueTypes
{
    DROPDOWN( 2, "i18n.crm.analytics.filter.value.type.dropdown" ),
    DATE_RANGE( 3, "i18n.crm.analytics.filter.value.type.date.range" ),
    TIME_PERIOD( 4, "i18n.crm.analytics.filter.value.type.time.period" ),
    NUMBER_RANGE(5, "i18n.crm.analytics.filter.value.type.number.range" ),
    MULTI_SELECT(6, "i18n.crm.analytics.filter.value.type.multi.select");

    private final int typeCode;
    private final String i18nKey;

    FilterValueTypes(int typeCode, String i18nKey)
    {
        this.typeCode = typeCode;
        this.i18nKey = i18nKey;
    }

    /**
     * Retrieves the {@link FilterValueTypes} enum corresponding to a given type code.
     *
     * @param typeCode The type code to look up.
     * @return The {@link FilterValueTypes} enum associated with the provided code.
     */
    public static FilterValueTypes getFilterValueTypes(final int typeCode )
    {
        return switch (typeCode) {
            case 2 -> DROPDOWN;
            case 3 -> DATE_RANGE;
            case 4 -> TIME_PERIOD;
            case 5 -> NUMBER_RANGE;
            case 6 -> MULTI_SELECT;
            default -> throw ErrorCode.get( AnalyticsErrorCodes.INVALID_FILTER_VALUE_TYPE,
                    "i18n.analytics.invalid.filter.value.types" );
        };
    }

    /**
     * Retrieves the {@link FilterValueTypes} enum corresponding to a given OperatorType.
     *
     * @param operatorTypeId The OperatorType code (Comparator) to look up.
     * @return An Optional containing the {@link FilterValueTypes} if mapped, otherwise empty.
     */
    public static FilterValueTypes getByOperatorType(final int operatorTypeId) {
        final OperatorType operatorType = OperatorType.getOperatorType(operatorTypeId);

        return (switch (operatorType) {
            case ON, DAY_IS, AFTER, BEFORE, SINCE -> DROPDOWN;
            case BETWEEN -> DATE_RANGE;
            case IN_THE_LAST, NOT_IN_THE_LAST -> TIME_PERIOD;
            case BETWEEN_NUMBER -> NUMBER_RANGE;
            case INCLUDES, DOES_NOT_INCLUDE -> MULTI_SELECT;
            default -> null;
        });
    }
}
