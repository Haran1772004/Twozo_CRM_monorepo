package com.tlc.crm.core.action.transformer;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.attachment.model.profilePicture.ProfilePicture;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.crm.common.list.transformer.formats.Formats;
import com.tlc.crm.common.list.transformer.model.TransformerRequest;
import com.tlc.crm.common.list.transformer.model.TransformerResponse;

import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.features.api.displayPicture.DisplayPictureManager;
import com.tlc.crm.core.field.contact.ContactFields;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.fields.transformer.TransformerFields;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;

import com.tlc.l10n.model.country.Country;
import com.tlc.l10n.model.number.PhoneNumberUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * It transforms the contact entities into required forms.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Transformer(name = "contact", type = Transformer.Type.ENTITY)
public class ContactTransformer extends AbstractModelTransformer {

    private static final String PHONE_NUMBER_PREFIX = "+";
    private final DisplayPictureManager displayPictureManager;
    private final ContactConverter contactConverter;

    public ContactTransformer() {
        displayPictureManager = DisplayPictureManager.getInstance();
        contactConverter = ContactConverter.getInstance();
    }

    /**
     * <p>
     * Groups the email and phone values to transforms the contact entities.
     * </p>
     *
     * @param transformerRequest Represents the {@link TransformerRequest}
     * @return {@link TransformerResponse}
     */
    @Override
    public TransformerResponse transform(final TransformerRequest transformerRequest) {
        final long orgId = transformerRequest.orgId();
        final Collection<Map<String, Object>> records = transformerRequest.records();
        final TransformerResponse transformerResponse = new TransformerResponse();
        final Map<Long, Map<String, Object>> contacts = new LinkedHashMap<>();
        final Collection<Long> userIds = new HashSet<>();
        final String emailValueKey = StoreSubType.EMAIL_VALUE.getAliasName();
        final String emailTypeKey = StoreSubType.EMAIL_TYPE.getAliasName();
        final String phoneNumberKey = StoreSubType.PHONE_NUMBER.getAliasName();
        final String phoneTypeKey = StoreSubType.PHONE_TYPE.getAliasName();
        final String countryCodeKey = StoreSubType.COUNTRY_CODE.getAliasName();
        final String nameKey = StoreSubType.NAME.getAliasName();
        final String emails = TransformerFields.EMAILS.getName();
        final String phones = TransformerFields.PHONES.getName();
        final String tags = TransformerFields.TAGS.getName();
        final String marketingList = TransformerFields.MARKETING_LIST.getName();
        final String salesOwner = TransformerFields.SALES_OWNER.getName();
        final String createdBy = TransformerFields.CREATED_BY.getName();
        final String updatedBy = TransformerFields.UPDATED_BY.getName();
        final String userId = TransformerFields.USER_ID.getName();
        final String followersKey = TransformerFields.FOLLOWERS.getName();
        final String idKey = TransformerFields.ID.getName();
        final String contactIdKey = TransformerFields.CONTACT_ID.getName();
        final Collection<FieldInfo> contactPersistentFields = FieldService.getSecureInstance().getPersistentFieldsBySource(orgId, TransformerFields.CONTACT.getName());
        final Collection<FieldInfo> ownerFields = Objects.isNull(contactPersistentFields) ? Collections.emptySet() : contactPersistentFields.stream()
                .filter(fieldInfo -> Objects.equals(StoreType.OWNER, fieldInfo.storeType())).collect(Collectors.toUnmodifiableSet());
        final Map<Long, Map<String, Boolean>> permissionAccess = getPermissionAccess(orgId, records);

        for (final Map<String, Object> contactRow : records) {
            final Map<String, Object> users = new HashMap<>();
            final Long pkValue = (Long) contactRow.get(idKey);

            if (permissionAccess.containsKey(pkValue)) {
                contactRow.putAll(permissionAccess.get(pkValue));
            }
            groupUserIdsFields(followersKey, contactRow, contacts);
            groupFields(emails, contactRow, contacts, emailValueKey, emailTypeKey);
            groupFields(phones, contactRow, contacts, phoneNumberKey, phoneTypeKey, countryCodeKey);
            groupFields(tags, contactRow, contacts, nameKey);
            groupFields(marketingList, contactRow, contacts, nameKey);
            collectUserIds(followersKey, contactRow, userIds, users);
            collectUserIds(salesOwner, contactRow, userIds, users);
            collectUserIds(createdBy, contactRow, userIds, users);
            collectUserIds(updatedBy, contactRow, userIds, users);
            mapLinkProperties(pkValue, contactRow, contactIdKey);

            for (final FieldInfo customOwnerField : ownerFields) {
                final String idAsString = customOwnerField.idAsString();

                if (Objects.nonNull(contactRow.get(idAsString))) {
                    collectUserIds(idAsString, contactRow, userIds, users);
                }
            }

            if (!userIds.isEmpty()) {
                contactRow.put(userId, users);
            }
        }
        final Collection<Map<String, Object>> contactRows = contacts.values();

        for (final Map<String, Object> contactRow : contactRows) {
            addPrimaryKey(emails, contactRow, ContactFields.PRIMARY_EMAIL);
            addPrimaryKey(phones, contactRow, ContactFields.PRIMARY_PHONE);
        }
        transformerResponse.setUserIds(userIds);
        transformerResponse.setResults(contactRows);
        return transformerResponse;
    }

    /**
     * <p>
     * Adds primary key for the given field
     * </p>
     *
     * @param field      Represents the field name
     * @param contactRow Represents the contact row
     * @param key        Represents the primary key
     */
    private void addPrimaryKey(final String field, final Map<String, Object> contactRow, final String key) {

        if (contactRow.containsKey(field)) {
            final Object value = contactRow.get(field);

            if (value instanceof Collection) {
                final Collection<Map<String, Object>> fields = (Collection<Map<String, Object>>) contactRow.get(field);
                final Long fieldId = (Long) contactRow.get(String.format(Formats.COLUMN_FORMAT.get(), field, key));
                final String idKey = TransformerFields.ID.getName();
                final String isPrimaryKey = TransformerFields.IS_PRIMARY.getName();

                fields.forEach(fieldValue -> {
                    final Long id = (Long) fieldValue.get(idKey);

                    fieldValue.put(isPrimaryKey, Objects.equals(id, fieldId));
                });
            }
        }
    }

    /**
     * <p>
     * Groups the given sub fields by field.
     * </p>
     *
     * @param field      Name of the field to be transformed. (eg., email, phone)
     * @param contactRow Retrieved contact entities.
     * @param contacts   Holds the details of the transformed entities.
     * @param subFields  Name of the subfields to be grouped. (eg., EMAIL_TYPE, EMAIL_VALUE, PHONE_TYPE, PHONE_NUMBER, COUNTRY_CODE)
     */
    private void groupFields(final String field, final Map<String, Object> contactRow,
                             final Map<Long, Map<String, Object>> contacts, final String... subFields) {
        final Long pkValue = (Long) contactRow.get(TransformerFields.ID.getName());
        final Long fieldId = (Long) contactRow.get(field);

        if (contacts.containsKey(pkValue)) {
            final Map<String, Object> contact = contacts.get(pkValue);

            if (contact.containsKey(field)) {
                final Object value = contact.get(field);

                if (value instanceof Collection) {
                    final Collection<Map<String, Object>> oldFields = (Collection<Map<String, Object>>) contact.get(field);
                    final Collection<Map<String, Object>> fields = new ArrayList<>(oldFields);
                    final Collection<Object> fieldIds = oldFields.stream().map(oldEmail -> oldEmail.get(TransformerFields.ID.getName())).collect(Collectors.toSet());

                    if (!fieldIds.contains(fieldId)) {
                        final Map<String, Object> fieldEntries = groupFields(field, subFields, contactRow);

                        if (Objects.nonNull(fieldEntries)) {
                            fields.add(fieldEntries);
                            contact.put(field, fields);
                        }
                    }
                } else {
                    final Map<String, Object> fieldEntries = groupFields(field, subFields, contactRow);

                    contact.put(field, Objects.isNull(fieldEntries) ? null : List.of(fieldEntries));
                }
            }
        } else {
            if (contactRow.containsKey(field) && Objects.nonNull(contactRow.get(field))) {
                final Map<String, Object> fieldEntries = groupFields(field, subFields, contactRow);

                contactRow.put(field, Objects.isNull(fieldEntries) ? null : List.of(fieldEntries));
            }
            contacts.put(pkValue, contactRow);
        }
    }

    /**
     * <p>
     * Groups the given sub fields by field.
     * </p>
     *
     * @param field      Name of the field to be transformed. (eg., email, phone)
     * @param subFields  Name of the subfields to be grouped. (eg., EMAIL_TYPE, EMAIL_VALUE, PHONE_TYPE, PHONE_NUMBER, COUNTRY_CODE)
     * @param contactRow Retrieved contact entities.
     * @return Grouped fields.
     */
    private Map<String, Object> groupFields(final String field, final String[] subFields,
                                            final Map<String, Object> contactRow) {
        final Optional<Object> optValue = Optional.ofNullable(contactRow.get(field));

        return optValue.map(fieldId -> {
            final Map<String, Object> fields = new HashMap<>();
            final String columnFormat = Formats.COLUMN_FORMAT.get();
            final String countryCodeKey = StoreSubType.COUNTRY_CODE.getName();

            for (final String subField : subFields) {
                final Object value = contactRow.remove(String.format(columnFormat, field, subField));

                if (Objects.nonNull(value)) {
                    switch (StoreSubType.get(subField)) {
                        case COUNTRY_CODE -> fields.put(subField, Country.getCountryById((int) value).getCode());
                        case PHONE_TYPE -> fields.put(subField, Phone.Type.valueOf((int) value).getDisplayName());
                        case EMAIL_TYPE -> fields.put(subField, Email.Type.valueOf((int) value).getDisplayName());
                        case PHONE_NUMBER -> {
                            try {
                                final String phoneNumber = value.toString();

                                if (phoneNumber.startsWith(PHONE_NUMBER_PREFIX)) {
                                    fields.put(subField, phoneNumber);
                                    continue;
                                }

                                final Object countryCode = contactRow.get(String.format(columnFormat, field, countryCodeKey));

                                if (Objects.nonNull(countryCode) || fields.containsKey(countryCodeKey)) {
                                    fields.put(subField, PhoneNumberUtil.parse(phoneNumber).getNumber());
                                    continue;
                                }
                                fields.put(subField, phoneNumber);
                            } catch (final ErrorCode errorCode) {
                                fields.put(subField, value);
                            }
                        }
                        default -> fields.put(subField, value);
                    }
                }
            }

            final Object number = fields.get(StoreSubType.PHONE_NUMBER.getAliasName());

            if (number instanceof final String text && text.startsWith(PHONE_NUMBER_PREFIX)) {
                fields.remove(StoreSubType.COUNTRY_CODE.getAliasName());
            }

            fields.put(TransformerFields.ID.getName(), fieldId);

            return fields;
        }).orElseGet(() -> null);
    }

    /**
     * <p>
     * Loads the profile pictures into contacts
     * </p>
     *
     * @param contacts               Holds the details of the contacts
     * @param contactProfilePictures Holds the details of the profile pictures
     */
    private void loadProfilePicturesIntoContacts(final Collection<Map<String, Object>> contacts,
                                                 final Map<Long, DisplayPicture> contactProfilePictures) {
        final String thumbnailKey = TransformerFields.THUMBNAIL.getName();
        final String contactKey = TransformerFields.CONTACT.getName();
        final String idKey = TransformerFields.ID.getName();

        for (final Map<String, Object> contact : contacts) {
            final Long contactId = (Long) contact.get(idKey);
            final DisplayPicture displayPicture = contactProfilePictures.get(contactId);

            if (Objects.nonNull(displayPicture)) {
                final JsonObject thumbnailJson = contactConverter.convertThumbnailToJson(contactId, contactKey, displayPicture);

                contact.put(thumbnailKey, thumbnailJson.toMap());
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param transformerRequest Represents the {@link TransformerRequest}
     * @return {@link TransformerResponse}
     */
    @Override
    public TransformerResponse transformDataForMobile(final TransformerRequest transformerRequest) {
        final TransformerResponse transformerResponse = transform(transformerRequest);
        final Collection<Map<String, Object>> results = transformerResponse.getResults();
        final String idKey = TransformerFields.ID.getName();
        final Collection<Long> contactIds = results.stream().map(contactRow -> (Long) contactRow.get(idKey)).collect(Collectors.toSet());

        if (!contactIds.isEmpty()) {
            final Map<Long, DisplayPicture> contactProfilePictures = displayPictureManager.
                    getDisplayPicture(transformerRequest.orgId(), contactIds, Source.CONTACT, ProfilePicture.Quality.THUMBNAIL);

            loadProfilePicturesIntoContacts(results, contactProfilePictures);
        }

        return transformerResponse;
    }
}