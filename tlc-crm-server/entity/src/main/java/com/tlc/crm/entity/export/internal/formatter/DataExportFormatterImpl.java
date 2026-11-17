package com.tlc.crm.entity.export.internal.formatter;

import com.tlc.commons.parser.html.HtmlParser;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.holder.UserPreferenceHolder;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.entity.export.formatter.DataExportFormatter;
import com.tlc.crm.entity.export.internal.model.DataExportFormatRequest;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.l10n.internal.temporal.TemporalPattern;
import com.tlc.l10n.model.country.Country;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.TimezoneInfo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * <p>
 * Implements the {@link DataExportFormatter} provides methods for formatting data during the export process
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public final class DataExportFormatterImpl implements DataExportFormatter {

    private static final String HYPHEN = " - ";
    private static final String BLANK_SPACE = " ";
    private static final String COMMA = ", ";
    private static final String LEFT_PARENTHESIS = "(";
    private static final String RIGHT_PARENTHESIS = ")";

    private final HtmlParser htmlParser;

    private DataExportFormatterImpl() {
        htmlParser = HtmlParser.getInstance();
    }

    private static class Instance {
        private static final DataExportFormatterImpl INSTANCE = new DataExportFormatterImpl();
    }

    /**
     * Gets the instance of {@link DataExportFormatterImpl}.
     *
     * @return The {@link DataExportFormatterImpl}
     */
    public static DataExportFormatterImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Formats the data based on {@link StoreType}.
     * </p>
     *
     * @param dataExportFormatRequest Represents the {@link DataExportFormatRequest}
     */
    @Override
    public void format(final DataExportFormatRequest dataExportFormatRequest) {
        final StoreType storeType = dataExportFormatRequest.storeType();
        final Map<String, Object> rowMap = dataExportFormatRequest.rowMap();
        final Object value = dataExportFormatRequest.value();
        final String fieldName = dataExportFormatRequest.fieldName();

        switch (storeType) {
            case SINGLE_STRING, TEXT -> formatAndMapStringField(fieldName, value, rowMap);
            case NOTE -> formatAndMapNoteField(fieldName, value, rowMap);
            case SINGLE_NUMBER -> rowMap.put(fieldName, value instanceof final Number number ? number.toString() : value.toString());
            case OWNER -> {
                if (value instanceof final Map<?, ?> dataMap) {
                    rowMap.put(fieldName, dataMap.get(DataExportFormatFields.NAME));
                } else {
                    formatAndMapMultiValueFields(fieldName, value, storeType, rowMap);
                }
            }
            case DEAL_VALUE, MONETARY -> formatAndMapMonetaryFields(fieldName, value, rowMap);
            case EMAIL -> formatField(fieldName, value, rowMap, StoreSubType.EMAIL_VALUE.getAliasName());
            case PHONE -> formatField(fieldName, value, rowMap, StoreSubType.PHONE_NUMBER.getAliasName());
            case DROPDOWN -> formatField(fieldName, value, rowMap, DataExportFormatFields.OPTION);
            case ADDRESS -> formatAndMapAddressField(fieldName, value, rowMap);
            case MULTI_SELECT, EMAILS, PHONES, CONTACTS_ONBOARD, TAGS, OWNERS, MARKETING_LIST ->
                    formatAndMapMultiValueFields(fieldName, value, storeType, rowMap);
            case CONTACT, CONTACT_ONBOARD -> {
                if (value instanceof final Map<?, ?> dataMap) {
                    rowMap.put(fieldName, String.format(DataExportFormatPatterns.CONCATENATION_PATTERN, Objects.toString(dataMap.get(DataExportFormatFields.FIRST_NAME), ""),
                            BLANK_SPACE, Objects.toString(dataMap.get(DataExportFormatFields.LAST_NAME), "")));
                }
            }
            case DEAL ->
                    formatAndMapMultiValueField(value, fieldName, DataExportFormatFields.DEAL_LIST, DataExportFormatFields.TITLE, rowMap);
            case PRODUCT ->
                    formatAndMapMultiValueField(value, fieldName, DataExportFormatFields.ITEMS, DataExportFormatFields.NAME, rowMap);
            case COMPANY, COMPANY_ONBOARD, TERRITORY ->
                    formatField(fieldName, value, rowMap, DataExportFormatFields.NAME);
            case DATE -> rowMap.put(fieldName, formatTimestampByUserPreference((Long) value));
            case TIME, DATE_TIME -> formatAndPopulateDateTimeField(fieldName, value, storeType, rowMap);
            case DATE_RANGE, DATE_TIME_RANGE, TIME_RANGE -> formatAndMapRangeField(fieldName, value, storeType, rowMap);
            default -> rowMap.put(fieldName, String.valueOf(value));
        }
    }

    /**
     * <p>
     * Formats and maps a specific field
     * </p>
     *
     * @param fieldName Represents the field name
     * @param value     Represents the value to be processed
     * @param rowMap    The {@link Map} where processed values are stored
     * @param dataKey   Represents the data field name
     */
    private void formatField(final String fieldName, final Object value,
                             final Map<String, Object> rowMap, final String dataKey) {
        if (value instanceof final Map<?, ?> dataMap) {
            rowMap.put(fieldName, dataMap.get(dataKey));
        }
    }

    /**
     * <p>
     * Format string fields by extracting the string value from the given value
     * </p>
     *
     * @param fieldName Represents the field name
     * @param value     Represents the value to be processed
     * @param rowMap    The {@link Map} where processed values are stored
     */
    private void formatAndMapStringField(final String fieldName, final Object value, final Map<String, Object> rowMap) {
        if (value instanceof final String name) {
            rowMap.put(fieldName, name);
        } else if (value instanceof final Map<?, ?> dataMap) {
            rowMap.put(fieldName, Objects.toString(dataMap.get(DataExportFormatFields.NAME), null));
        }
    }

    /**
     * <p>
     * Format the note field by extracting the string value from the given value
     * </p>
     *
     * @param fieldName Represents the field name
     * @param value     Represents the value to be processed
     * @param rowMap    The {@link Map} where processed values are stored
     */
    private void formatAndMapNoteField(final String fieldName, final Object value, final Map<String, Object> rowMap) {
        final Function<String, String> valueParser = (content) -> {
            if (Objects.isNull(content)) {
                return null;
            }

            if (htmlParser.isHtml(content)) {
                return htmlParser.convertHtmlToText(content);
            } else {
                return content;
            }
        };

        if (value instanceof final String note) {
            rowMap.put(fieldName, valueParser.apply(note));
        } else if (value instanceof final Map<?, ?> dataMap) {
            rowMap.put(fieldName, valueParser.apply(Objects.toString(dataMap.get(DataExportFormatFields.NAME), null)));
        }
    }

    /**
     * <p>
     * Formats and maps an address field by concatenating its components
     * </p>
     *
     * @param fieldName Represents the field name
     * @param value     Represents the value to be processed
     * @param rowMap    The {@link Map} where processed values are stored
     */
    private void formatAndMapAddressField(final String fieldName, final Object value, final Map<String, Object> rowMap) {
        final StringBuilder addressBuilder = new StringBuilder();
        final Consumer<Object> appendAddressPart = part -> {

            if (Objects.nonNull(part)) {

                if (!addressBuilder.isEmpty()) {
                    addressBuilder.append(COMMA);
                }
                addressBuilder.append(part);
            }
        };
        if (value instanceof final Map<?, ?> dataMap) {
            appendAddressPart.accept(dataMap.get(StoreSubType.ADDRESS_LINE1.getAliasName()));
            appendAddressPart.accept(dataMap.get(StoreSubType.ADDRESS_LINE2.getAliasName()));
            appendAddressPart.accept(dataMap.get(StoreSubType.CITY.getAliasName()));
            appendAddressPart.accept(dataMap.get(StoreSubType.STATE.getAliasName()));
            appendAddressPart.accept(getCountryName(dataMap.get(StoreSubType.COUNTRY.getAliasName())));
            appendAddressPart.accept(dataMap.get(StoreSubType.ZIPCODE.getAliasName()));
        }
        rowMap.put(fieldName, addressBuilder.toString());
    }

    /**
     * <p>
     * Formats country name in  export file
     * </p>
     *
     * @param countryValue Represents the country field value
     * @return The display name for export file
     */
    private String getCountryName(final Object countryValue) {
        if (Objects.isNull(countryValue)) {
            return null;
        }

        if (countryValue instanceof final Map<?, ?> countryMap) {
            final Object countryName = countryMap.get(FieldKeys.NAME.getName());

            if (Objects.nonNull(countryName)) {
                return String.valueOf(countryName);
            }

            return getCountryName(countryMap.get(FieldKeys.VALUE.getName()));
        }

        if (countryValue instanceof final Number countryId) {
            final Country country = Country.getCountryById(countryId.intValue());

            return Objects.nonNull(country) ? country.getName() : String.valueOf(countryValue);
        }

        try {
            final Country country = Country.getCountryById(Integer.parseInt(countryValue.toString()));

            return Objects.nonNull(country) ? country.getName() : countryValue.toString();
        } catch (final NumberFormatException exception) {
            return String.valueOf(countryValue);
        }
    }

    /**
     * <p>
     * Formats and maps monetary fields by combining currency symbol and amount.
     * </p>
     *
     * @param fieldName Represents the field name
     * @param value     Represents the value to be processed
     * @param rowMap    The {@link Map} where processed values are stored
     */
    private void formatAndMapMonetaryFields(final String fieldName, final Object value, final Map<String, Object> rowMap) {

        if (value instanceof Map) {
            final Map<String, Object> dataMap = (Map<String, Object>) value;
            final String currency = Objects.toString(dataMap.getOrDefault(DataExportFormatFields.SYMBOL, BLANK_SPACE), "");
            final String amount = Objects.toString(dataMap.get(StoreSubType.VALUE.getAliasName()), "");

            rowMap.put(fieldName, String.format(DataExportFormatPatterns.CONCATENATION_PATTERN, currency, BLANK_SPACE, amount));
        }
    }

    /**
     * <p>
     * Formats and maps multi-value fields based on the specified store type
     * </p>
     *
     * @param fieldName Represents the field name
     * @param value     Refers the collection of values to group.
     * @param storeType Refers the field {@link StoreType}
     * @param rowMap    The {@link Map} where processed values are stored
     */
    private void formatAndMapMultiValueFields(final String fieldName, final Object value, final StoreType storeType,
                                              final Map<String, Object> rowMap) {

        if (value instanceof String) {
            rowMap.put(fieldName, value.toString());
        } else if (value instanceof final Collection<?> values) {
            final StringBuilder valueBuilder = new StringBuilder();
            final Iterator<?> iterator = values.iterator();

            while (iterator.hasNext()) {
                final Object currentValue = iterator.next();

                if (currentValue instanceof final Map<?, ?> valueMap) {
                    final Map<String, Object> dataMap = (Map<String, Object>) valueMap;

                    switch (storeType) {
                        case EMAILS -> valueBuilder.append(dataMap.get(StoreSubType.EMAIL_VALUE.getAliasName()));
                        case PHONES -> valueBuilder.append(dataMap.get(StoreSubType.PHONE_NUMBER.getAliasName()));
                        case MULTI_SELECT -> valueBuilder.append(dataMap.get(DataExportFormatFields.OPTION));
                        case CONTACTS_ONBOARD ->
                                valueBuilder.append(dataMap.get(DataExportFormatFields.FIRST_NAME)).append(BLANK_SPACE)
                                        .append(dataMap.getOrDefault(DataExportFormatFields.LAST_NAME, ""));
                        case OWNER, OWNERS, TAGS, MARKETING_LIST -> valueBuilder.append(dataMap.get(DataExportFormatFields.NAME));
                    }
                }

                if (iterator.hasNext()) {
                    valueBuilder.append(COMMA);
                }
            }
            rowMap.put(fieldName, valueBuilder.toString());
        } else {
            rowMap.put(fieldName, value);
        }
    }

    /**
     * <p>
     * Formats and maps a range field based on the specified store type
     * </p>
     *
     * @param fieldName Represents the field name
     * @param value     Represents the range value
     * @param storeType Represents the {@link StoreType}
     * @param rowMap    The {@link Map} where processed values are stored
     */
    private void formatAndMapRangeField(final String fieldName, final Object value, final StoreType storeType, final Map<String, Object> rowMap) {
        final StringBuilder valueBuilder = new StringBuilder();

        switch (storeType) {
            case DATE_RANGE ->
                    processRange(value, getDateFormat(DataExportFormatPatterns.DATE_FORMAT_PATTERN), valueBuilder);
            case DATE_TIME_RANGE ->
                    processRange(value, getDateFormat(DataExportFormatPatterns.DATE_TIME_FORMAT_PATTERN), valueBuilder);
            case TIME_RANGE ->
                    processRange(value, getDateFormat(DataExportFormatPatterns.TIME_FORMAT_PATTERN), valueBuilder);
        }
        rowMap.put(fieldName, valueBuilder.toString());
    }

    /**
     * <p>
     * Processes the range value by converting date or time values into the specified format
     * </p>
     *
     * @param value        Represents the range value
     * @param format       The date/time format to be applied
     * @param valueBuilder The builder where the formatted range will be appended
     */
    private void processRange(final Object value, final DateFormat format, final StringBuilder valueBuilder) {
        if (value instanceof final Map<?, ?> dataMap) {
            valueBuilder.append(format.format(new Date(convertUtcToLocalTime((Long) dataMap.get(StoreSubType.FROM.getAliasName())))))
                    .append(HYPHEN)
                    .append(format.format(new Date(convertUtcToLocalTime((Long) dataMap.get(StoreSubType.TO.getAliasName())))));
        }
    }

    /**
     * <p>
     * Formats and maps date and time fields based on the specified store type
     * </p>
     *
     * @param fieldName Represents the field name
     * @param value     Represents the value to be processed
     * @param storeType Represents the {@link StoreType }
     * @param rowMap    The {@link Map} where processed values are stored
     */
    private void formatAndPopulateDateTimeField(final String fieldName, final Object value, final StoreType storeType, final Map<String, Object> rowMap) {
        if (Objects.nonNull(value)) {
            final String pattern = switch (storeType) {
                case TIME -> DataExportFormatPatterns.TIME_FORMAT_PATTERN;
                case DATE_TIME -> DataExportFormatPatterns.DATE_TIME_FORMAT_PATTERN;
                default -> null;
            };
            rowMap.put(fieldName, Objects.isNull(pattern) ? null : getDateFormat(pattern).format(convertUtcToLocalTime((Long) value)));
        } else {
            rowMap.put(fieldName, null);
        }
    }

    /**
     * Parses the pattern provided and generates the corresponding date format.
     *
     * @param pattern Refers the pattern.
     * @return {@link DateFormat} of given patten.
     */
    private DateFormat getDateFormat(final String pattern) {
        return new SimpleDateFormat(pattern);
    }

    /**
     * <p>
     * Formats and populates the values into row
     * </p>
     *
     * @param value         Represents field value
     * @param fieldName     Represents the field name
     * @param collectionKey Represents the collection key
     * @param dataKey       Represents the data field name
     * @param rowMap        The {@link Map} where processed values are stored
     */
    private void formatAndMapMultiValueField(final Object value, final String fieldName, final String collectionKey,
                                             final String dataKey, final Map<String, Object> rowMap) {
        if (value instanceof String) {
            rowMap.put(fieldName, value.toString());
        } else if (value instanceof final Map<?, ?> map) {
            final Object collection = map.get(collectionKey);

            if (collection instanceof final Collection<?> values && !values.isEmpty()) {
                final int size = values.size();
                final StringBuilder valueBuilder = new StringBuilder().append(size).append(BLANK_SPACE)
                        .append(fieldName).append(LEFT_PARENTHESIS);
                final Iterator<?> iterator = values.iterator();

                while (iterator.hasNext()) {
                    final Object currentValue = iterator.next();

                    if (currentValue instanceof final Map<?, ?> dataMap) {
                        final Object name = dataMap.get(dataKey);

                        if (Objects.nonNull(name)) {
                            valueBuilder.append(name);

                            if (iterator.hasNext()) {
                                valueBuilder.append(COMMA);
                            }
                        }
                    }
                }
                valueBuilder.append(RIGHT_PARENTHESIS);
                rowMap.put(fieldName, valueBuilder.toString());
            }
        }
    }

    /**
     * <p>
     * Converts a given UTC timestamp to the local time based on the logged-in user's timezone
     * </p>
     *
     * @param utcTime The UTC timestamp (in milliseconds) to be converted.
     * @return The corresponding local time (in milliseconds) adjusted for the user's timezone
     */
    private Long convertUtcToLocalTime(final long utcTime) {
        final CrmUser crmUser = LoggedInUserInfo.getUser();

        final UserPreferenceHolder preferenceHolder = crmUser.getPreferenceHolder();
        final UserRegionalPreference userRegionalPreference = preferenceHolder.get(UserRegionalPreference.TYPE);
        final TimezoneInfo timezoneInfo = userRegionalPreference.getTimezoneInfo();

        return TemporalServiceProvider.get().convertUtcToLocalTime(utcTime, timezoneInfo);
    }

    /**
     * <p>
     * Converts a given timestamp (in milliseconds) to a formatted date string based on the user regional preference
     * </p>
     *
     * @param timeStamp Represents the timestamp in milliseconds to be converted
     * @return A formatted date string according to the user's preferences
     */
    private String formatTimestampByUserPreference(final long timeStamp) {
        final CrmUser crmUser = LoggedInUserInfo.getUser();
        final UserRegionalPreference userRegionalPreference = crmUser.getPreferenceHolder().get(UserRegionalPreference.TYPE);
        final TimezoneInfo timezoneInfo = userRegionalPreference.getTimezoneInfo();
        final ZonedDateTime zonedDateTime = Instant.ofEpochMilli(timeStamp).atZone(timezoneInfo.getTimeZone().toZoneId());

        return switch (userRegionalPreference.getDateFormat()) {
            case RELATIVE_DATE -> TemporalServiceProvider.get(timeStamp).convertTimeStampToRelativeTime(timezoneInfo);
            case MONTH_DATE_YEAR ->  DateTimeFormatter.ofPattern(TemporalPattern.DatePattern.MONTH_DAY_YEAR.get()).format(zonedDateTime);
            case DAY_MONTH_YEAR -> DateTimeFormatter.ofPattern(TemporalPattern.DatePattern.DATE_1.get()).format(zonedDateTime);
        };
    }
}