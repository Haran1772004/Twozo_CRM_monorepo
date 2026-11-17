package com.tlc.crm.core.features.internal.dataimport.handler.impl;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.features.internal.dataimport.handler.UpdatableFieldsExtractor;
import com.tlc.crm.core.features.field.dataimport.ImportFields;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * The Implementation of {@link UpdatableFieldsExtractor}
 * </p>
 *
 * @author Muthu kumar V
 * @version 2.0
 */
public final class UpdatableFieldsExtractorImpl implements UpdatableFieldsExtractor {

    private final FieldService fieldService;
    private final FieldTypeService fieldTypeService;

    private static final class Instance {
        private static final UpdatableFieldsExtractorImpl UPDATE_HISTORY_CONTAINER = new UpdatableFieldsExtractorImpl();
    }

    private UpdatableFieldsExtractorImpl() {
        fieldTypeService = FieldTypeService.getInstance();
        fieldService = FieldService.getInstance();
    }

    /**
     * Gets the {@link UpdatableFieldsExtractorImpl} instance
     *
     * @return {@link UpdatableFieldsExtractorImpl}
     */
    public static UpdatableFieldsExtractorImpl getInstance() {
        return Instance.UPDATE_HISTORY_CONTAINER;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId             Represents the organization ID
     * @param source            Represents the {@link Source}
     * @param previousStateJson Represents the previous state json
     * @param currentStateJson  Represents the current state json
     * @return The extracted Collection of updated field keys
     */
    public Collection<String> extractUpdatedFieldKeys(final Long orgId, final Source source, final JsonObject previousStateJson, final JsonObject currentStateJson) {
        final Collection<String> updatedFields = new HashSet<>();
        final String idKey = ImportFields.ID.getName();
        final Collection<String> fieldKeys = currentStateJson.keySet().stream().filter(key -> !key.equals(idKey)).collect(Collectors.toSet());
        final Map<String, FieldInfo> fields = getSourceFields(orgId, source);

        for (final String fieldName : fieldKeys) {

            if (previousStateJson.containsKey(fieldName)) {
                final Object previousValue = previousStateJson.get(fieldName);
                final Object currentValue = currentStateJson.get(fieldName);

                if (Objects.nonNull(previousValue) && Objects.nonNull(currentValue)) {
                    final StoreType storeType = fields.get(fieldName).storeType();

                    if (isModelFieldValueUpdated(storeType, previousValue, currentValue)) {
                        updatedFields.add(fieldName);
                    }
                }
            } else {
                updatedFields.add(fieldName);
            }
        }
        return updatedFields;
    }

    /**
     * <p>
     * Gets the source fields with the given source.
     * </p>
     *
     * @param orgId  Represents the organization ID
     * @param source Represents the {@link Source}
     * @return The map containing keys and their {@link FieldInfo}
     */
    private Map<String, FieldInfo> getSourceFields(final Long orgId, final Source source) {
        final Collection<FieldInfo> sourceFieldInfos = fieldService.getFieldsBySource(orgId, source.getName());
        final Function<FieldInfo, String> fieldExtractor = Source.isPrimarySource(source) ? FieldInfo::idAsString : FieldInfo::aliasName;

        return sourceFieldInfos.stream().collect(Collectors.toMap(fieldExtractor, Function.identity()));
    }

    /**
     * <p>
     * Checks whether the model field value is updated or not
     * </p>
     *
     * @param storeType     Represents the store type of the field
     * @param previousValue Represents the previous value of the field
     * @param currentValue  Represents the current value of the field
     * @return True if the field value is updated, false otherwise
     */
    private boolean isModelFieldValueUpdated(final StoreType storeType, final Object previousValue, final Object currentValue) {
        return switch (storeType) {
            case SINGLE_STRING, TEXT, NOTE, CONTACT, COMPANY, CONTACT_ONBOARD, COMPANY_ONBOARD, SINGLE_NUMBER, DROPDOWN,
                 OWNER, TERRITORY, TIME, DATE_TIME, DATE -> isValueUpdated(previousValue, currentValue);
            case EMAILS -> areValuesUpdated(fieldTypeService.getFieldSubTypeByStoreType(storeType, StoreSubType.EMAIL_VALUE).idAsString(),
                            previousValue, currentValue);
            case PHONES -> areValuesUpdated(fieldTypeService.getFieldSubTypeByStoreType(storeType, StoreSubType.PHONE_NUMBER).idAsString(),
                            previousValue, currentValue);
            case TAGS -> areValuesUpdated(ImportFields.ID.getName(), previousValue, currentValue);
            case OWNERS, MULTI_SELECT, CONTACTS_ONBOARD -> areValuesUpdated(null, previousValue, currentValue);
            case ADDRESS -> areAddressValuesUpdated(previousValue, currentValue);
            case EMAIL -> isValueUpdated(fieldTypeService.getFieldSubTypeByStoreType(storeType, StoreSubType.EMAIL_VALUE).idAsString(),
                            previousValue, currentValue);
            case PHONE -> isValueUpdated(fieldTypeService.getFieldSubTypeByStoreType(storeType, StoreSubType.PHONE_NUMBER).idAsString(),
                            previousValue, currentValue);
            case DATE_RANGE, TIME_RANGE, DATE_TIME_RANGE -> isRangeValueUpdated(fieldTypeService.groupStoreSubTypeIds(storeType),
                            previousValue, currentValue);
            case MONETARY -> isMonetaryValueUpdated(previousValue, currentValue);
            default -> false;
        };
    }

    /**
     * <p>
     * Checks whether the value is updated or not
     * </p>
     *
     * @param previousValue Represents the previous value of the field
     * @param currentValue  Represents the current value of the field
     * @return True if the field value is updated, false otherwise
     */
    private boolean isValueUpdated(final Object previousValue, final Object currentValue) {
        return !Objects.equals(previousValue,currentValue);
    }

    /**
     * <p>
     * Checks whether the key values are updated or not
     * </p>
     *
     * @param previousValueArray Represents the array of previous values
     * @param currentValueArray  Represents the array of current values
     * @return True if the field values is updated, false otherwise
     */
    private boolean areValuesUpdated(final String key, final Object previousValueArray, final Object currentValueArray) {

        if (!(previousValueArray instanceof Collection<?> previousValuesCollection && currentValueArray instanceof Collection<?> currentValuesCollection)) {
            return false;
        }
        final int previousValuesSize = previousValuesCollection.size();

        if (previousValuesSize != currentValuesCollection.size()) {
            return true;
        }

        if (Objects.isNull(key)) {
            return !previousValuesCollection.equals(currentValuesCollection);
        } else {

            if (previousValuesCollection instanceof final List<?> previousValuesList && currentValuesCollection instanceof final List<?> currentValuesList) {

                for (int index = 0; index < previousValuesSize; index++) {

                    if (previousValuesList.get(index) instanceof final Map<?, ?> previousMap && currentValuesList.get(index) instanceof final Map<?, ?> currentMap) {

                       if (!currentMap.containsValue(previousMap.get(key))) {
                           return true;
                       }
                    }
                }
            }
            return false;
        }
    }

    /**
     * <p>
     * Checks whether the key value of is updated or not
     * </p>
     *
     * @param previousJson Represents the previous value json
     * @param currentJson  Represents the current value json
     * @return True if the field values is updated, false otherwise
     */
    private boolean isValueUpdated(final String subTypeIdString, final Object previousJson, final Object currentJson) {
        if (previousJson instanceof final Map<?, ?> previousValue && currentJson instanceof final Map<?, ?> currentValue &&
                currentValue.containsKey(subTypeIdString)) {
            return isValueUpdated(previousValue.get(subTypeIdString), currentValue.get(subTypeIdString));
        }
        return false;
    }

    /**
     * <p>
     * Checks whether the range field values are updated or not
     * </p>
     *
     * @param previousRangeJson Represents the json that contains the previous range values
     * @param currentRangeJson  Represents the json that contains the current range values
     * @return True if the field values is updated, false otherwise
     */
    private boolean isRangeValueUpdated(final Map<StoreSubType, String> subTypeIds, final Object previousRangeJson,
                                        final Object currentRangeJson) {
        if (previousRangeJson instanceof final Map<?, ?> previousRange && currentRangeJson instanceof final Map<?, ?> currentRange) {
            final String fromIdKey = subTypeIds.get(StoreSubType.FROM);
            final String toIdKey = subTypeIds.get(StoreSubType.TO);

            return isValueUpdated(previousRange.get(fromIdKey), currentRange.get(fromIdKey)) &&
                    isValueUpdated(previousRange.get(toIdKey), currentRange.get(toIdKey));
        }
        return false;
    }

    /**
     * <p>
     * Checks whether the address values are updated or not
     * </p>
     *
     * @param previousAddressJson Represents the json that contains the previous address values
     * @param currentAddressJson  Represents the json that contains the current address values
     * @return True if the field values is updated, false otherwise
     */
    private boolean areAddressValuesUpdated(final Object previousAddressJson, final Object currentAddressJson) {
        final Map<StoreSubType, String> addressSubFields = fieldTypeService.groupStoreSubTypeIds(StoreType.ADDRESS);

        if (previousAddressJson instanceof final Map<?,?> previousAddress && currentAddressJson instanceof final Map<?,?> currentAddress) {
            final String addressLine1 = addressSubFields.get(StoreSubType.ADDRESS_LINE1);
            final String addressLine2 = addressSubFields.get(StoreSubType.ADDRESS_LINE2);
            final String city = addressSubFields.get(StoreSubType.CITY);
            final String state = addressSubFields.get(StoreSubType.STATE);
            final String zipcode = addressSubFields.get(StoreSubType.ZIPCODE);
            final String country = addressSubFields.get(StoreSubType.COUNTRY);

            return isValueUpdated(previousAddress.get(addressLine1), currentAddress.get(addressLine1)) ||
                    (isValueUpdated(previousAddress.get(addressLine2), currentAddress.get(addressLine2))) ||
                    (isValueUpdated(previousAddress.get(city), currentAddress.get(city))) ||
                    (isValueUpdated(previousAddress.get(state), currentAddress.get(state))) ||
                    (isValueUpdated(previousAddress.get(zipcode), currentAddress.get(zipcode))) ||
                    (isValueUpdated(previousAddress.get(country), currentAddress.get(country)));
        }
        return false;
    }

    /**
     * <p>
     * Checks whether the monetary field value is updated or not
     * </p>
     *
     * @param previousMonetaryJson Represents the json that contains the previous monetary values
     * @param currentMonetaryJson  Represents the json that contains the current monetary values
     * @return True if the field values is updated, false otherwise
     */
    private boolean isMonetaryValueUpdated(final Object previousMonetaryJson, final Object currentMonetaryJson) {
        if (previousMonetaryJson instanceof final Map<?, ?> previousMonetary && currentMonetaryJson instanceof final Map<?, ?> currentMonetary) {
            final Map<StoreSubType, String> monetarySubTypeIds = fieldTypeService.groupStoreSubTypeIds(StoreType.MONETARY);
            final String currencyIdKey = monetarySubTypeIds.get(StoreSubType.CURRENCY);
            final String valueIdString = monetarySubTypeIds.get(StoreSubType.VALUE);

            return isValueUpdated(previousMonetary.get(currencyIdKey), currentMonetary.get(currencyIdKey)) &&
                    isValueUpdated(previousMonetary.get(valueIdString), currentMonetary.get(valueIdString));
        }
        return false;
    }
}