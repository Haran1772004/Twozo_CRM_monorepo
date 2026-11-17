package com.tlc.crm.core.api.analytics.converter.data;

import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.config.model.table.AnalyticsDataType;
import com.tlc.analytics.general.utils.ValueNormalizer;
import com.tlc.analytics.service.column.ColumnDetailsService;

import com.tlc.crm.common.util.DateUtils;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.sql.resource.MDODEAL;

import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.features.api.activity.model.Availability;
import com.tlc.crm.entity.features.api.activity.model.Priority;
import com.tlc.crm.entity.features.api.activity.model.Status;
import com.tlc.crm.entity.sql.resource.OMACACTIVITY;

import com.tlc.crm.fields.sql.resource.OMETADDRESS;

import com.tlc.i18n.I18nResolver;

import com.tlc.l10n.model.country.Country;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.l10n.temporal.model.DateTime;

import lombok.NonNull;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Objects;

/**
 * Resolving raw data values into user-friendly display values based on type (e.g., Date, Enum) and column metadata.
 *
 * @author KarthickV
 * @version 1.0
 */
public class DisplayValueResolver
{
    private final ColumnDetailsService columnDetailsService;
    private final BaseCurrencyService baseCurrencyService;

    private DisplayValueResolver()
    {
        this.columnDetailsService = ColumnDetailsService.getInstance();
        this.baseCurrencyService = BaseCurrencyService.getInstance();
    }

    private static class Instance
    {
        private static final DisplayValueResolver INSTANCE = new DisplayValueResolver();
    }

    /**
     * Gets the {@link DisplayValueResolver} instance
     *
     * @return {@link DisplayValueResolver}
     */
    public static DisplayValueResolver INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * Resolves the display value for the given input based on its type and column mapping.
     *
     * <p>If the value is a DateTime or Date, its timestamp is returned. For other types,
     * the value is converted based on the enum mapping and then localized using the i18nResolver.</p>
     *
     * @param orgId        the organization ID
     * @param value        the raw value to process
     * @param columnId     Represents the column ID
     * @param i18nResolver the resolver used for internationalized strings
     * @return a processed, display value
     */
    public Object resolveDisplayValue( final @NonNull Long orgId, final @NonNull Object value,
                                       final Long columnId,
                                       final @NonNull I18nResolver i18nResolver )
    {
        if ( value instanceof final DateTime dateTime )
        {
            return dateTime.getTimeStamp();
        }
        else if ( value instanceof final Date date )
        {
            return date.getTimeStamp();
        } else {
            final Object convertedValue = Objects.isNull(columnId) ? value : convertEnumFieldValue(orgId, columnId, value);

            if (convertedValue instanceof Number number) {
                return number;
            }

            if (convertedValue instanceof Collection<?> || convertedValue instanceof Object[]
                    || (convertedValue instanceof String convertedValueString && convertedValueString.startsWith("[")
                    && convertedValueString.endsWith("]"))) {
                return ValueNormalizer.normalizeToCollection(convertedValue).stream()
                        .map(item -> item instanceof Number ? item : i18nResolver.get(item.toString())).toList();
            }

            return i18nResolver.get(convertedValue.toString());
        }
    }

    /**
     * Resolves a value for export based on the column's data type.
     *
     * @param orgId                  the organization ID
     * @param columnId               the ID of the column used to determine data type
     * @param value                  the raw value to convert
     * @param i18nResolver           resolver for localized display values
     * @return the export-ready formatted value
     */
    public Object resolveExportFileValue(final @NonNull Long orgId,
                                         final @NonNull Long columnId,
                                         final @NonNull Object value,
                                         final @NonNull I18nResolver i18nResolver) {

        final ColumnDetail columnDetail = columnDetailsService.get(columnId);
        final AnalyticsDataType analyticsDataType = columnDetail != null ? columnDetail.getDataType() : null;

        return Objects.equals(AnalyticsDataType.DATE, analyticsDataType)
                ? DateUtils.getFormattedDateString(
                value instanceof Date date
                ? date.getTimeStamp()
                : value instanceof DateTime dateTime
                  ? dateTime.getTimeStamp()
                  : ((Number) value).longValue()
        )
                : resolveDisplayValue(orgId, value, columnId, i18nResolver);
    }

    /**
     * Converts a raw enum-related value to its corresponding display name using the column configuration and domain mappings.
     *
     * @param orgId    the organization ID
     * @param columnId Represents the column ID
     * @param value    the raw enum value
     * @return a display value if a mapping exists; otherwise, the original value
     */
    private Object convertEnumFieldValue( final Long orgId, final Long columnId, final Object value )
    {
        final ColumnDetail columnDetail = columnDetailsService.get( columnId );

        if ( Objects.isNull(columnDetail)  || value instanceof String )
        {
            return value;
        }

        return switch (columnDetail.getColumnName())
                {
                    case OMETADDRESS.COUNTRY -> Country.getCountryById( (int) value )
                                                       .getName();
                    case MDODEAL.STATE -> Deal.State.getStateById( (int) value )
                                                    .getDisplayName();
                    case OMACACTIVITY.STATUS -> Status.get( (int) value )
                                                      .getDisplayName();
                    case OMACACTIVITY.AVAILABILITY -> Availability.get( (int) value )
                                                                  .getDisplayName();
                    case OMACACTIVITY.PRIORITY -> Priority.get( (int) value )
                                                          .getDisplayName();
                    default -> applyCurrencyPrecisionIfNeeded( orgId, value );
                };
    }

    /**
     * Applies currency precision to the given numeric value based on the organization's settings.
     *
     * @param orgId the ID of the organization whose currency precision settings should be used
     * @param value the value to which precision should be applied, if numeric
     * @return the value with precision applied if it's numeric; otherwise, the original value
     */
    private Object applyCurrencyPrecisionIfNeeded( final Long orgId, final Object value )
    {
        if (value instanceof Float || value instanceof Double ) {
            return baseCurrencyService.applyPrecision(orgId, BigDecimal.valueOf(((Number) value).doubleValue()));
        } else if (value instanceof BigDecimal bigDecimal) {
            return baseCurrencyService.applyPrecision(orgId, bigDecimal);
        } else {
            return value;
        }
    }
}
