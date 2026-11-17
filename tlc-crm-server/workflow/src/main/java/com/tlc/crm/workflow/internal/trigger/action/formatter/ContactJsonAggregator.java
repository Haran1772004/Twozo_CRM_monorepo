package com.tlc.crm.workflow.internal.trigger.action.formatter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.models.type.converter.EmailConverter;
import com.tlc.crm.entity.models.type.converter.PhoneConverter;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionAttributes;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Aggregates the data of a contact with the existing data
 *
 * @author Maharaja S
 * @version 1.0
 */
public final class ContactJsonAggregator extends EntityJsonAggregator {

    private final FieldTypeService fieldTypeService;
    private final ContactManager contactManager;
    private final EmailConverter emailConverter;
    private final PhoneConverter phoneConverter;

    private ContactJsonAggregator() {
        this.contactManager = ContactManager.getInstance();
        this.emailConverter = EmailConverter.getInstance();
        this.phoneConverter = PhoneConverter.getInstance();
        this.fieldTypeService = FieldTypeService.getInstance();
    }

    private static final class InstanceHolder {
        private static final ContactJsonAggregator INSTANCE = new ContactJsonAggregator();
    }

    /**
     * Gets the instance of {@link ContactJsonAggregator}.
     *
     * @return {@link ContactJsonAggregator} instance.
     */
    public static ContactJsonAggregator getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents the organization id
     * @param contactJson Represents the json data of a contact
     */
    @Override
    public void merge(final long orgId, final JsonObject contactJson) {
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(orgId, ActionAttributes.CONTACT);
        final String emailsFieldKey = fields.get(ActionAttributes.EMAILS).idAsString();
        final String phonesFieldKey = fields.get(ActionAttributes.PHONES).idAsString();
        final String tagsFieldKey = fields.get(ActionAttributes.TAGS).idAsString();

        if (contactJson.containsKey(ActionAttributes.ID)) {
            final Long contactId = contactJson.getLong(ActionAttributes.ID);

            if (contactJson.containsKey(tagsFieldKey)) {
                final Collection<Long> mappedTagIds = sourceTagManager.getMappedTagIds(orgId, ActionAttributes.CONTACT, sourceTagManager.getContactTagTable(), List.of(contactId));

                if (!mappedTagIds.isEmpty()) {
                    mergeTags(contactJson, mappedTagIds, tagsFieldKey);
                }
            }
            mergeContactData(contactJson, emailsFieldKey, ()-> contactManager.getEmails(orgId, List.of(contactId)),
                    StoreType.EMAILS, StoreSubType.EMAIL_VALUE, Email::getValue, emailConverter::convertEmail);

            mergeContactData(contactJson, phonesFieldKey, ()-> contactManager.getPhones(orgId, List.of(contactId)),
                    StoreType.PHONES, StoreSubType.PHONE_NUMBER, Phone::getNumber, phoneConverter::convertPhone);
        }
    }

    /**
     * <p>
     * Merges the data of the contact with the existing data for the provided field key
     * </p>
     *
     * @param contactJson            Represents the json data of a contact
     * @param fieldKey               Represents the key of the field
     * @param existingValuesSupplier Represents the supplier to get the collection of existing values
     * @param storeType              Represents the {@link StoreType}
     * @param storeSubType           Represents the {@link StoreSubType}
     * @param valueExtractor         Represents the function to extract the string values
     * @param converter              Represents the function to convert the model into json
     * @param <T>                    Represents the type of the model
     */
    private <T> void mergeContactData(final JsonObject contactJson, final String fieldKey, final Supplier<Collection<T>> existingValuesSupplier,
                                      final StoreType storeType, final StoreSubType storeSubType, final Function<T, String> valueExtractor,
                                      final Function<T, JsonObject> converter) {
        if (contactJson.containsKey(fieldKey)) {
            final Collection<T> existingValues = existingValuesSupplier.get();

            if (!existingValues.isEmpty()) {
                final Collection<String> existingStringValues = existingValues.stream().map(valueExtractor).collect(Collectors.toSet());
                final String valueKey = fieldTypeService.groupSubtypesByStoreSubType(storeType).get(storeSubType).idAsString();
                final JsonArray jsonArray = contactJson.getJsonArray(fieldKey);
                final JsonArray mergedArray = Json.array();
                final int arraySize = jsonArray.size();

                for (final T value : existingValues) {
                    mergedArray.put(converter.apply(value));
                }

                for (int index = 0; index < arraySize; index++) {
                    final JsonObject jsonData = jsonArray.getJsonObject(index);

                    if (Objects.nonNull(jsonData) && jsonData.containsKey(valueKey) && !existingStringValues.contains(jsonData.getString(valueKey))) {
                        mergedArray.put(jsonData);
                    }
                }

                if (mergedArray.size() > 0) {
                    contactJson.put(fieldKey, mergedArray);
                }
            }
        }
    }
}