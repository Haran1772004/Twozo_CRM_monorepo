package com.tlc.crm.core.features.internal.dataimport;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.StringUtil;
import com.tlc.crm.core.features.internal.dataimport.model.ImportFieldWrapper;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.marketinglist.manager.MarketingListManager;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.common.iam.api.manager.TerritoryManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.entity.features.api.tag.manager.TagManager;
import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.crm.entity.features.field.tag.TagFields;
import com.tlc.crm.entity.models.type.DropdownType;
import com.tlc.crm.entity.service.TagService;

import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.I18nService;
import com.tlc.i18n.service.provider.I18nServiceProvider;

import com.tlc.l10n.model.country.Country;
import com.tlc.l10n.model.currency.Currency;

import com.tlc.crm.core.internal.formatter.ValueFormatter;
import com.tlc.crm.core.features.internal.dataimport.status.ImportErrorCodes;
import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.service.OptionService;
import com.tlc.crm.fields.service.FieldTypeService;

import com.tlc.crm.common.util.NumberUtils;
import com.tlc.commons.util.TypeChecker;
import com.tlc.l10n.temporal.TemporalPatternType;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.TimezoneInfo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The Formatter class is designed to format values that are linked to predefined field types in a specific manner
 *
 * @author sundar
 * @version 1.0
 */
public final class ImportValueFormatter implements ValueFormatter<ImportFieldWrapper, Object, Object> {

    private static final String COMMA_SEPARATOR = ",";
    private static final String HYPHEN_SEPARATOR = "-";
    private static final String EMPTY_STRING = "";

    /**
     * Pattern to match monetary values with optional currency codes.
     * Supported formats:
     * 1. "INR 200"      - Currency code followed by a monetary value.
     * 2. "200 INR"      - Monetary value followed by a currency code.
     * 3. "200"          - Monetary value without any currency code (optional for some cases).
     * Note:
     * - The currency code is a three-letter uppercase code (e.g., INR, USD).
     * - The monetary value supports integers and decimals (e.g., 200 or 200.50).
     * - Whitespace between currency code and value is optional but allowed.
     */
    private static final Pattern MONETARY_PATTERN = Pattern.compile("([A-Z]{3}|[^\\d\\s])?\\s*(\\d+(?:,\\d{1,3})*(?:\\.\\d+)?)\\s*([A-Z]{3}|[^\\d\\s])?");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}:\\d{1,2})(:\\d{1,2})?(\\s(?:AM|am|pm|PM))?");
    private static final String NON_ALPHABETIC_CHARACTERS_REGEX = "[^a-zA-Z]+";

    private final FieldTypeService fieldTypeService;
    private final OptionService optionService;
    private final TagService tagService;
    private final BaseCurrencyService baseCurrencyService;
    private final I18nService i18nService;
    private final TerritoryManager territoryManager;
    private final TagManager tagManager;
    private final MarketingListManager marketingListManager;

    private ImportValueFormatter() {
        this.fieldTypeService = FieldTypeService.getInstance();
        this.optionService = OptionService.getInstance();
        this.baseCurrencyService = BaseCurrencyService.getInstance();
        this.tagService = TagService.getInstance();
        this.i18nService = I18nServiceProvider.get();
        this.territoryManager = TerritoryManager.getInstance();
        this.tagManager = TagManager.getInstance();
        this.marketingListManager = MarketingListManager.getInstance();
    }

    private static final class Instance {
        private static final ValueFormatter<ImportFieldWrapper, Object, Object> VALUE_FORMATTER = new ImportValueFormatter();
    }

    /**
     * Gets the instance of {@link ValueFormatter}
     *
     * @return {@link ValueFormatter} instance
     */
    public static ValueFormatter<ImportFieldWrapper, Object, Object> getInstance() {
        return Instance.VALUE_FORMATTER;
    }

    /**
     * Transforms the value according to appropriate {@link StoreType}
     *
     * @param importFieldWrapper Represents the value format request
     * @param value              Represents the value to be formatted
     * @return the formatted value
     */
    @Override
    public Object format(final long orgId, final ImportFieldWrapper importFieldWrapper, final Object value) {
        if (Objects.isNull(value)) {
            return null;
        }

        if (value instanceof final String stringValue && stringValue.isBlank()) {
            return null;
        }


        if (importFieldWrapper.fieldInfo().storeType().isArrayType()) {
            final JsonArray jsonArray = Json.array();

            if (value instanceof final String stringValues) {
                if (!StringUtil.isValidString(stringValues)) {
                    return null;
                }
                final String[] values = stringValues.split(COMMA_SEPARATOR);

                for (final String stringValue : values) {
                    jsonArray.put(formatValueByStoreType(orgId, importFieldWrapper, stringValue.trim()));
                }
            } else if (value instanceof JsonArray) {
                return formatValueByStoreType(orgId, importFieldWrapper, value);
            } else {
                final Object formattedValue = formatValueByStoreType(orgId, importFieldWrapper, value);

                if (formattedValue instanceof final JsonArray array) {
                    return array;
                } else {
                    jsonArray.put(formattedValue);
                }
            }
            return jsonArray;
        }
        return formatValueByStoreType(orgId, importFieldWrapper, value);
    }

    /**
     * Checks whether the given string contains the time with meridian or not
     *
     * @param timeString Contains the value to be checked
     * @return true if the String contains meridian
     */
    private boolean hasMeridian(final String timeString) {
        return TIME_PATTERN.matcher(timeString).results().anyMatch(matchResult -> Objects.nonNull(matchResult.group(3)));
    }

    /**
     * Gets the UTC time from given date
     *
     * @param date Represents the date to be converted
     * @return the UTC time of given date
     */
    private Long getTime(final Date date) {
        return TemporalServiceProvider.get(date.getTime()).getUtcDateTime();
    }

    /**
     * Gets the UTC time from date time String of specified store type
     *
     * @param storeType      Represents the store type
     * @param dateTimeString To be converted to UTC time
     * @param timezoneInfo   Represents the timezone info of the user
     * @return the UTC time
     */
    private Long getTime(final StoreType storeType, final String dateTimeString, final TimezoneInfo timezoneInfo) {
        return switch (storeType) {
            case DATE_RANGE -> getTime(dateTimeString, TemporalPatternType.DATE, timezoneInfo);
            case DATE_TIME_RANGE -> getTime(dateTimeString, TIME_PATTERN.matcher(dateTimeString).find()
                    ? TemporalPatternType.REGULAR_DATE_TIME : TemporalPatternType.DATE, timezoneInfo);
            case TIME_RANGE -> getTime(dateTimeString, hasMeridian(dateTimeString) ? TemporalPatternType.MERIDIAN_TIME : TemporalPatternType.INTERNATIONAL_TIME, timezoneInfo);
            default ->
                    throw ErrorCode.get(ImportErrorCodes.INVALID_DATE_OR_TIME_TYPE, "i18n.import.format.invalid.date.time");
        };
    }

    /**
     * Gets the UTC time of given date time String
     *
     * @param dateTimeString      Refers the date time value in String format
     * @param temporalPatternType Represents the date/time pattern type of the dateTimeString to be converted
     * @param timezoneInfo        Represents the timezone info of the user
     * @return the UTC time of given date time String
     */
    private Long getTime(final String dateTimeString, final TemporalPatternType temporalPatternType, final TimezoneInfo timezoneInfo) {
        try {
            return Objects.isNull(temporalPatternType) ? null : TemporalServiceProvider.get(dateTimeString, Objects.isNull(timezoneInfo) ? TimezoneInfo.UTC : timezoneInfo, temporalPatternType).getUtcDateTime();
        } catch (Exception exception) {
            throw ErrorCode.get(ImportErrorCodes.INVALID_DATE_OR_TIME_TYPE, "i18n.import.format.invalid.date.time");
        }
    }

    /**
     * Builds the range JSON from given value
     *
     * @param value        To be converted
     * @param subTypeIds   Which contains the {@link  StoreSubType} mapped with the subtype ID in String format
     * @param storeType    Represents the store type
     * @param timezoneInfo Represents the timezone info of the user
     * @return {@link JsonObject} that contains the date, time or date time range data
     */
    private JsonObject buildRangeJson(final Object value, final Map<StoreSubType, String> subTypeIds,
                                      final StoreType storeType, final TimezoneInfo timezoneInfo) {
        final String fromIdKey = subTypeIds.get(StoreSubType.FROM);
        final String toIdKey = subTypeIds.get(StoreSubType.TO);
        final JsonObject rangeJson = Json.object();

        if (value instanceof final String rangeString) {
            final String[] rangeArray = rangeString.split(HYPHEN_SEPARATOR);
            final int length = rangeArray.length;

            if (2 == length) {
                final String fromStringValue = rangeArray[0].trim();
                final String toStringValue = rangeArray[1].trim();

                rangeJson.put(fromIdKey, getTime(storeType, fromStringValue, timezoneInfo));
                rangeJson.put(toIdKey, getTime(storeType, toStringValue, timezoneInfo));
            } else {
                throw ErrorCode.get(ImportErrorCodes.INVALID_DATE_OR_TIME_TYPE, "i18n.import.format.invalid.date.time");
            }
        } else if (value instanceof final Date date) {
            final long time = getTime(date);
            final long currentTime = Instant.now().toEpochMilli();

            rangeJson.put(fromIdKey, Math.min(time, currentTime));
            rangeJson.put(toIdKey, Math.max(time, currentTime));
        }
        return rangeJson;
    }

    /**
     * Loads the price tag value into the price tag {@link JsonObject}
     *
     * @param priceTagJson       To be loaded with the price values
     * @param priceTagSubTypeIds Which contains the collection subfield IDs grouped by {@link StoreSubType}
     * @param currency           Represents the currency
     * @param price              Represents the price tag amount
     */
    private void loadPriceTagValue(final JsonObject priceTagJson, final Map<StoreSubType, String> priceTagSubTypeIds,
                                   final Currency currency, final Double price) {
        final String valueIdString = priceTagSubTypeIds.get(StoreSubType.UNIT_PRICE);

        if (Objects.nonNull(currency)) {
            priceTagJson.put(priceTagSubTypeIds.get(StoreSubType.CURRENCY), currency.getId());
        }
        priceTagJson.put(valueIdString, Objects.isNull(price) ? 0.0 : price);
    }

    /**
     * Formats the given value by the appropriate {@link StoreType} contained within the {@link FieldInfo}
     *
     * @param orgId              Represents the org id
     * @param importFieldWrapper Represents the value format request
     * @param value              Represents the value to be formatted
     * @return the formatted value
     */
    private Object formatValueByStoreType(final long orgId, final ImportFieldWrapper importFieldWrapper, final Object value) {
        final FieldInfo importField = importFieldWrapper.fieldInfo();
        final StoreType storeType = importField.storeType();

        return switch (storeType) {
            case SINGLE_STRING, TEXT -> Objects.isNull(value) ? null : String.valueOf(value).trim();
            case DROPDOWN -> fetchOption(orgId, value, importField);
            case TAGS -> fetchTagIds(orgId, value, importField);
            case MARKETING_LIST -> fetchMarketingListIds(orgId, value, importField);
            case MULTI_SELECT -> fetchMultiSelectOptionIds(orgId, value, importField);
            case DATE, TIME -> {
                final TimezoneInfo timezoneInfo = importFieldWrapper.timezoneInfo();

                if (value instanceof final Date date) {
                    yield getTime(date);
                } else if (value instanceof final String dateString) {
                    final String dateValue = dateString.trim();

                    if (StoreType.TIME == storeType) {
                        yield getTime(dateValue, hasMeridian(dateValue) ? TemporalPatternType.MERIDIAN_TIME : TemporalPatternType.INTERNATIONAL_TIME, timezoneInfo);
                    }
                    yield getTime(dateValue, TemporalPatternType.DATE, timezoneInfo);
                }
                yield value;
            }
            case DATE_TIME -> {
                final TimezoneInfo timezoneInfo = importFieldWrapper.timezoneInfo();

                if (value instanceof final Date date) {
                    yield getTime(date);
                } else if (value instanceof final String dateTimeString) {
                    final Matcher matcher = TIME_PATTERN.matcher(dateTimeString);

                    if (matcher.matches()) {
                        final String meridian = matcher.group(3);

                        yield getTime(dateTimeString, Objects.isNull(meridian) ? TemporalPatternType.INTERNATIONAL_TIME : TemporalPatternType.MERIDIAN_TIME, timezoneInfo);
                    } else if (matcher.find()) {
                        yield getTime(dateTimeString, TemporalPatternType.REGULAR_DATE_TIME, timezoneInfo);
                    } else {
                        yield getTime(dateTimeString, TemporalPatternType.DATE, timezoneInfo);
                    }
                }
                yield value;
            }
            case PHONE, PHONES -> buildPhoneNumberJson(storeType, value);
            case EMAIL, EMAILS -> buildEmailJson(storeType, value);
            case PRICE_TAGS -> buildPriceTagsJson(orgId, value);
            case OWNER, OWNERS -> Objects.isNull(value) ? null : value.toString();
            case ADDRESS -> buildAddressJson(value);
            case SINGLE_NUMBER -> TypeChecker.isDecimalNumber(value) ? Math.round(Double.parseDouble((String) value)) : NumberUtils.parseNumber(value);
            case CONTACT, CONTACT_ONBOARD, CONTACTS_ONBOARD -> {
                try {
                    yield NumberUtils.parseNumber(value);
                } catch (Exception exception) {
                    throw ErrorCode.get(ImportErrorCodes.INVALID_CONTACT_FIELD_TYPE_VALUE, "i18n.import.contact.field.type.value.invalid");
                }
            }
            case COMPANY, COMPANY_ONBOARD -> {
                try {
                    yield NumberUtils.parseNumber(value);
                } catch (Exception exception) {
                    throw ErrorCode.get(ImportErrorCodes.INVALID_COMPANY_FIELD_TYPE_VALUE, "i18n.import.company.field.type.value.invalid");
                }
            }
            case DEAL_VALUE, MONETARY -> buildMonetaryJson(orgId, value);
            case DATE_RANGE, DATE_TIME_RANGE, TIME_RANGE ->
                    buildRangeJson(value, fieldTypeService.groupStoreSubTypeIds(storeType), storeType, importFieldWrapper.timezoneInfo());
            case TERRITORY -> fetchTerritoryId(orgId, value);
            default -> value;
        };
    }

    /**
     * Fetches the territory ID for a given value
     *
     * @param orgId Represents the organization ID
     * @param value Represents the value to fetch from
     * @return the territory ID
     */
    private Long fetchTerritoryId(final long orgId, final Object value) {
        if (Objects.isNull(value)) {
            return null;
        }

        if (TypeChecker.isNonDecimalNumber(value)) {
            return NumberUtils.parseNumber(value).longValue();
        } else {
            final String territoryName = String.valueOf(value).trim();

            if (territoryManager.checkTerritoriesAreExists(orgId, Set.of(territoryName))) {
                final Optional<Territory> optionalTerritory = territoryManager.getTerritoryByName(orgId, territoryName);

                return optionalTerritory.map(Territory::id).orElseGet(() -> null);
            }
        }
        return null;
    }

    /**
     * Fetches the appropriate option for the provided value
     *
     * @param value The value to retrieve the option for
     * @param field Represents the {@link FieldInfo}
     * @return The corresponding option ID if the value is valid, otherwise returns the default option ID
     * @fixme Static options should be handled by fetching them using the appropriate I18NResolver based on the current user's preferred Locale settings
     */
    private Object fetchOption(final long orgId, final Object value, final FieldInfo field) {
        if (!(value instanceof final String optionValue)) {
            return null;
        }
        final String optionName = optionValue.trim();

        if (Objects.isNull(field) || !StoreType.isSelectorType(field.storeType())) {
            return null;
        }
        final DropdownType dropdownType = field.presetConfig().dropdownType();

        if (Objects.isNull(dropdownType)) {
            return null;
        }

        if (!Source.isPrimarySource(Source.get(field.fieldSource().getName()))) {
            return optionName;
        }

        final I18nResolver resolver = i18nService.getResolver(Locale.US);
        final Function<Predicate<Option>, Long> optionIdResolver = (criteria -> {
            final Collection<Option> options = optionService.get(orgId, field);
            final Optional<Option> optionalOption = options.stream().filter(criteria).findFirst();

            if (optionalOption.isEmpty()) {
                final Optional<Option> optionalDefaultOption = options.stream().filter(Option::isDefault).findFirst();

                return optionalDefaultOption.map(Option::id).orElse(null);
            }
            return optionalOption.get().id();
        });

        return switch (dropdownType) {
            case STATIC, DYNAMIC, HYBRID ->
                    optionIdResolver.apply(option -> optionName.equalsIgnoreCase(resolver.get(option.getOption())));
            case HIDDEN_OPTION -> optionName;
            default -> null;
        };
    }

    /**
     * Fetches the options IDs of the multi select field
     *
     * @param value The value to retrieve the options for
     * @param field Represents the {@link FieldInfo}
     * @return The corresponding option IDs if the value is valid, otherwise returns the default option ID(s)
     */
    private Collection<Long> fetchMultiSelectOptionIds(final long orgId, final Object value, final FieldInfo field) {
        final I18nResolver resolver = i18nService.getResolver(Locale.US);

        if (value instanceof final String multipleOptions) {

            if (Objects.nonNull(field)) {
                final Collection<String> optionNames = Arrays.stream(multipleOptions.trim().split(COMMA_SEPARATOR))
                        .map(optionName -> (optionName.trim().toLowerCase())).toList();
                final Collection<Option> options = optionService.get(orgId, field).stream()
                        .filter(option -> optionNames.contains(resolver.get(option.getOption()).toLowerCase())).toList();

                return options.isEmpty() ? optionService.getDefaultOptions(orgId, field).stream().map(Option::id).toList()
                        : options.stream().map(Option::id).toList();
            }
        }
        return Collections.emptyList();
    }

    /**
     * Fetches tag Ids based on the provided tag names. If a tag name is not found, then returns the tag names to create
     *
     * @param value The tag name(s) to retrieve or create
     * @param field Represents the {@link FieldInfo}
     * @return {@link JsonArray} of tag Ids or tag names if no tag is found
     */
    private JsonArray fetchTagIds(final long orgId, final Object value, final FieldInfo field) {

        if (value instanceof final String tagValues) {

            if (Objects.nonNull(field)) {
                final Collection<String> tagNames = Arrays.stream(tagValues.trim().split(COMMA_SEPARATOR))
                        .map(tagValue -> tagValue.toLowerCase().trim()).collect(Collectors.toSet());
                final JsonArray tags = Json.array();
                final String tagValueKey = TagFields.VALUE.getName();
                final Consumer<Collection<Tag>> tagJsonLoader = tagCollection -> {

                    for (final Tag tag : tagCollection) {
                        final JsonObject tagIdJson = Json.object();

                        tagIdJson.put(tagValueKey, tag.id());
                        tagNames.remove(tag.getName().toLowerCase());
                        tags.put(tagIdJson);
                    }
                };
                final Collection<Tag> tagsByNames = tagService.getTagsByNames(orgId, tagNames);

                if (!tagsByNames.isEmpty()) {
                    tagJsonLoader.accept(tagsByNames);
                }

                if (!tagNames.isEmpty()) {
                    final Collection<Tag> nonExistingTags = new HashSet<>();
                    final CrmUser user = LoggedInUserInfo.getUser();
                    final Collection<Tag> tagsToBeMapped = new ArrayList<>();

                    for (final String tagName : tagNames) {

                        if (tagManager.isTagNameExists(orgId, tagName)) {
                            tagManager.get(orgId, tagName).ifPresent(tagsToBeMapped::add);
                        } else {
                            final Tag tag = new Tag();

                            tag.setName(tagName);
                            tag.setOrgId(orgId);
                            tag.setCreatedBy(user);

                            tagsToBeMapped.add(tag);
                            nonExistingTags.add(tag);
                        }
                    }
                    tagManager.create(orgId, nonExistingTags);
                    tagJsonLoader.accept(tagsToBeMapped);
                }
                return tags;
            }
        }
        return null;
    }

    /**
     * Fetches marketing list Ids based on the provided marketing list names
     *
     * @param orgId       The organization ID
     * @param value       The marketing list name(s) to retrieve
     * @param importField Represents the {@link FieldInfo}
     * @return the collection of marketing list Ids, or null if no marketing lists are found
     */
    private Collection<Long> fetchMarketingListIds(final long orgId, final Object value, final FieldInfo importField) {
        if (!(value instanceof final String marketingListValues) || Objects.isNull(importField)) {
            return null;
        }
        final Collection<String> marketingListNames = Arrays.stream(marketingListValues.trim().split(COMMA_SEPARATOR))
                .map(name -> name.toLowerCase().trim()).filter(name -> !name.isEmpty()).collect(Collectors.toSet());

        if (marketingListNames.isEmpty()) {
            return null;
        }
        final Collection<Long> marketingListIds = marketingListManager.getMarketingListIds(orgId, marketingListNames);

        return marketingListIds.isEmpty() ? null : marketingListIds;
    }

    /**
     * Builds the phone number {@link JsonObject} from given value
     *
     * @param storeType  Represents the store type
     * @param phoneValue To be converted
     * @return {@link JsonObject} that contains the phone number data
     */
    private JsonObject buildPhoneNumberJson(final StoreType storeType, final Object phoneValue) {
        final Map<StoreSubType, String> phoneSubTypeIds = fieldTypeService.groupStoreSubTypeIds(storeType);
        final JsonObject phoneJson = Json.object();

        phoneJson.put(phoneSubTypeIds.get(StoreSubType.PHONE_TYPE), Phone.Type.WORK.getId());
        phoneJson.put(phoneSubTypeIds.get(StoreSubType.PHONE_NUMBER), String.valueOf(phoneValue).trim());

        return phoneJson;
    }

    /**
     * Builds the email {@link JsonObject} from given value
     *
     * @param storeType  Represents the store type
     * @param emailValue To be converted
     * @return {@link JsonObject} that contains the email data
     */
    private JsonObject buildEmailJson(final StoreType storeType, final Object emailValue) {
        final Map<StoreSubType, String> emailSubTypeIds = fieldTypeService.groupStoreSubTypeIds(storeType);
        final JsonObject emailJson = Json.object();

        emailJson.put(emailSubTypeIds.get(StoreSubType.EMAIL_TYPE), Email.Type.WORK.getId());
        emailJson.put(emailSubTypeIds.get(StoreSubType.EMAIL_VALUE), String.valueOf(emailValue).trim());

        return emailJson;
    }

    /**
     * Builds the price tag {@link JsonObject} using provided value
     *
     * @param orgId Represents the organization ID
     * @param value To be formatted into price tag JSON
     * @return {@link JsonObject} that contains the price tag data
     */
    private Object buildPriceTagsJson(final Long orgId, final Object value) {

        if (Objects.isNull(value)) {
            return null;
        }
        final Map<StoreSubType, String> groupedStoreSubTypeIds = fieldTypeService.groupStoreSubTypeIds(StoreType.PRICE_TAGS);
        final JsonArray priceTagsArray = Json.array();

        if (value instanceof final JsonObject priceTagJsonValue) {
            final String taxIdString = groupedStoreSubTypeIds.get(StoreSubType.TAX);
            final String discountIdString = groupedStoreSubTypeIds.get(StoreSubType.DISCOUNT);
            final String unitPriceIdString = groupedStoreSubTypeIds.get(StoreSubType.UNIT_PRICE);
            final Object taxValue = priceTagJsonValue.opt(taxIdString);
            final Object discountValue = priceTagJsonValue.opt(discountIdString);
            final Object unitPrice = priceTagJsonValue.opt(unitPriceIdString);

            priceTagJsonValue.put(taxIdString, Objects.isNull(taxValue) ? 0.0 : NumberUtils.parseDouble(taxValue));
            priceTagJsonValue.put(discountIdString, Objects.isNull(discountValue) ? 0.0 : NumberUtils.parseDouble(discountValue));

            if (unitPrice instanceof final String priceValue) {
                loadPriceTagJson(orgId, priceValue, priceTagJsonValue, groupedStoreSubTypeIds);
            } else if (unitPrice instanceof final Double priceValue) {
                loadPriceTagValue(priceTagJsonValue, groupedStoreSubTypeIds, baseCurrencyService.getBaseCurrency(orgId).getDefaultCurrency(), priceValue);
            }
            priceTagsArray.put(priceTagJsonValue);
            return priceTagsArray;
        } else if (value instanceof final String priceValue) {
            final JsonObject priceTagJson = Json.object();

            loadPriceTagJson(orgId, priceValue, priceTagJson, groupedStoreSubTypeIds);

            if (priceTagJson.size() > 0) {
                return priceTagJson;
            }
        }
        return null;
    }

    /**
     * Loads the price tag JSON with the price value
     *
     * @param orgId                  Represents the organization ID
     * @param priceValue             Value of the price to be formatted and loaded
     * @param priceTagJson           Represents the price tag JSON to be loaded with the price value
     * @param groupedStoreSubTypeIds Which contains the price tag field sub type IDs
     */
    private void loadPriceTagJson(final long orgId, final String priceValue, final JsonObject priceTagJson,
                                  final Map<StoreSubType, String> groupedStoreSubTypeIds) {
        final Matcher matcher = MONETARY_PATTERN.matcher(priceValue.trim());

        if (matcher.find()) {
            final String currencyCode = Objects.isNull(matcher.group(1)) ? matcher.group(3) : matcher.group(1);
            final String priceAsString = matcher.group(2);
            final Currency currency = Objects.isNull(currencyCode) ? baseCurrencyService.getBaseCurrency(orgId).getDefaultCurrency()
                    : Currency.valueOf(currencyCode);

            loadPriceTagValue(priceTagJson, groupedStoreSubTypeIds, currency, Objects.isNull(priceAsString) ? null : Double.valueOf(priceAsString.replace(COMMA_SEPARATOR, EMPTY_STRING)));
        }
    }

    /**
     * Builds the address {@link JsonObject} from the given value
     *
     * @param addressValue To construct the JSON representation of an address
     * @return {@link JsonObject} that contains the address values
     */
    private JsonObject buildAddressJson(final Object addressValue) {

        if (addressValue instanceof final JsonObject addressJson) {
            final StoreType address = StoreType.ADDRESS;
            final String latitudeSubTypeIdString = fieldTypeService.getFieldSubTypeByStoreType(address, StoreSubType.LATITUDE).idAsString();
            final String longitudeSubTypeIdString = fieldTypeService.getFieldSubTypeByStoreType(address, StoreSubType.LONGITUDE).idAsString();
            final String countrySubTypeIdString = fieldTypeService.getFieldSubTypeByStoreType(address, StoreSubType.COUNTRY).idAsString();

            if (addressJson.containsKey(latitudeSubTypeIdString)) {
                final Object latitude = addressJson.get(latitudeSubTypeIdString);

                addressJson.put(latitudeSubTypeIdString, latitude instanceof final Number latitudeValue
                        ? latitudeValue.doubleValue() : NumberUtils.parseDouble(latitude));
            }

            if (addressJson.containsKey(longitudeSubTypeIdString)) {
                final Object longitude = addressJson.get(longitudeSubTypeIdString);

                addressJson.put(longitudeSubTypeIdString, longitude instanceof final Number longitudeValue
                        ? longitudeValue.doubleValue() : NumberUtils.parseDouble(longitude));
            }

            if (addressJson.containsKey(countrySubTypeIdString)) {
                final Object value = addressJson.get(countrySubTypeIdString);

                if (value instanceof final String countryValue) {
                    final String countryName = countryValue.replaceAll(NON_ALPHABETIC_CHARACTERS_REGEX, EMPTY_STRING).trim();
                    final Optional<Country> country = Country.getCountryByName(countryName);

                    if (country.isPresent()) {
                        addressJson.put(countrySubTypeIdString, country.get().getId());
                    } else {
                        throw ErrorCode.get(ImportErrorCodes.INVALID_COUNTRY, "i18n.l10n.country.invalid");
                    }
                }
            }
            return addressJson;
        }
        return null;
    }

    /**
     * Builds the monetary {@link JsonObject} from given value
     *
     * @param orgId Represents the organization ID
     * @param value To be converted
     * @return {@link JsonObject} that contains the monetary data
     */
    private JsonObject buildMonetaryJson(final Long orgId, final Object value) {
        final Currency baseCurrency = baseCurrencyService.getBaseCurrency(orgId).getDefaultCurrency();

        if (value instanceof final String priceValue) {
            final String priceString = priceValue.trim();
            final Matcher matcher = MONETARY_PATTERN.matcher(priceString);

            if (matcher.find()) {
                final String currencyCode = Objects.isNull(matcher.group(1)) ? matcher.group(3) : matcher.group(1);
                final String price = matcher.group(2);
                final Double priceInDouble = Objects.isNull(price) ? null : Double.valueOf(price.replace(COMMA_SEPARATOR, EMPTY_STRING));

                if (Objects.nonNull(currencyCode)) {
                    final Optional<Currency> optionalCurrency = Currency.getCurrencyByCode(currencyCode);

                    if (optionalCurrency.isPresent() && baseCurrencyService.isActive(orgId, optionalCurrency.get())) {
                        return buildMonetaryJson(optionalCurrency.get(), priceInDouble);
                    } else {
                        return buildMonetaryJson(baseCurrency, priceInDouble);
                    }
                } else {
                    return buildMonetaryJson(baseCurrency, priceInDouble);
                }
            } else {
                return buildMonetaryJson(baseCurrency, NumberUtils.parseDouble(priceString));
            }
        } else if (value instanceof final Double priceValue) {
            return buildMonetaryJson(baseCurrency, priceValue);
        }
        return null;
    }

    /**
     * Builds the {@link JsonObject} of monetary by using the currency code and price
     *
     * @param currency Represents the currency
     * @param price    Represents the amount
     * @return {@link JsonObject} that contains the monetary values
     */
    private JsonObject buildMonetaryJson(final Currency currency, final Double price) {
        final Map<StoreSubType, String> monetarySubTypeIds = fieldTypeService.groupStoreSubTypeIds(StoreType.MONETARY);
        final String valueIdString = monetarySubTypeIds.get(StoreSubType.VALUE);
        final JsonObject monetaryJson = Json.object();

        if (Objects.nonNull(currency)) {
            monetaryJson.put(monetarySubTypeIds.get(StoreSubType.CURRENCY), currency.getId());
        }
        monetaryJson.put(valueIdString, Objects.isNull(price) ? 0.0 : price);

        return monetaryJson;
    }
}