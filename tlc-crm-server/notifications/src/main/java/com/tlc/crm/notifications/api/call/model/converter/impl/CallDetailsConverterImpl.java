package com.tlc.crm.notifications.api.call.model.converter.impl;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;

import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.DealValue;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.entity.features.api.note.model.Note;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.models.type.Phone;

import com.tlc.crm.entity.models.type.converter.MonetaryConverter;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.crm.notifications.api.call.model.CallDetails;
import com.tlc.crm.notifications.api.call.model.CallForwardDetails;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.notification.model.call.CallRecord;
import com.tlc.notification.model.call.CallStatus;
import com.tlc.crm.notifications.api.call.model.CallSummary;
import com.tlc.crm.notifications.api.call.model.PhoneNumber;
import com.tlc.crm.notifications.api.call.model.converter.CallDetailsConverter;
import com.tlc.crm.notifications.fields.NotificationFields;

import com.tlc.notification.model.call.CallType;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Provides conversion methods for transforming call history and call details between
 * JSON format and Java objects.
 *
 * @author Sathish
 * @version 1.0
 */
public class CallDetailsConverterImpl implements CallDetailsConverter {

    private final UserService userService;
    private final MonetaryConverter monetaryConverter;
    private final FieldService fieldService;
    private final FieldTypeService fieldTypeService;
    private final ContactConverter contactConverter;

    private CallDetailsConverterImpl() {
        userService = UserService.getInstance();
        monetaryConverter = MonetaryConverter.getInstance();
        fieldService = FieldService.getInstance();
        fieldTypeService =  FieldTypeService.getInstance();
        contactConverter = ContactConverter.getInstance();
    }

    private static final class InstanceHolder {
        private static final CallDetailsConverterImpl CALL_HISTORY_MANAGER = new CallDetailsConverterImpl();
    }

    public static CallDetailsConverterImpl getInstance() {
        return InstanceHolder.CALL_HISTORY_MANAGER;
    }

    /**
     * {@inheritDoc}
     *
     * @param jsonObject The JSON object containing call details data.
     * @return A {@link CallDetails} object populated with the JSON data.
     */
    @Override
    public CallDetails convert(final JsonObject jsonObject) {
        final String numberIdKey = NotificationFields.NUMBER_ID.getName();
        final String contactKey = NotificationFields.CONTACT.getName();
        final long orgId = jsonObject.getLong(NotificationFields.ORG_ID.getName());
        final CallDetails callDetails = new CallDetails();

        if (jsonObject.containsKey(numberIdKey)) {
            final PhoneNumber phoneNumber = new PhoneNumber();

            phoneNumber.setId(jsonObject.getLong(numberIdKey));
            callDetails.setUserNumber(phoneNumber);
        }
        callDetails.setOrgId(orgId);
        callDetails.setParticipantUser(userService.getUser(orgId, jsonObject.getLong(NotificationFields.USER_ID.getName())));
        callDetails.setExternalId(jsonObject.getString(NotificationFields.CALL_SERVICE_ID.getName()));
        callDetails.setType(CallType.get(jsonObject.getString(NotificationFields.CALL_TYPE.getName())));
        callDetails.setStatus(CallStatus.get(jsonObject.getString(NotificationFields.CALL_STATUS.getName())));
        callDetails.setContactNumber(convertJsonIntoContactNumber(jsonObject.getJsonObject(NotificationFields.CONTACT_NUMBER.getName())));

        if (jsonObject.containsKey(contactKey)) {
            callDetails.setContact(convertJsonIntoContact(jsonObject.getJsonObject(contactKey)));
        }
        return callDetails;
    }

    /**
     * {@inheritDoc}
     *
     * @param callDetails The {@link CallDetails} object to be converted.
     * @return A {@link JsonObject} representing the call details.
     */
    @Override
    public JsonObject convert(final CallDetails callDetails) {
        final JsonObject jsonObject = Json.object();
        final Contact contact = callDetails.getContact();
        final Deal deal = callDetails.getDeal();
        final Collection<Note> notes = callDetails.getNotes();
        final Collection<CallRecord> callRecordings = callDetails.getCallRecordings();
        final CallForwardDetails forwardDetails = callDetails.getForwardDetails();
        final CallSummary summary = callDetails.getSummary();
        final CrmUser participantUser = callDetails.getParticipantUser();
        final CallStatus callStatus = callDetails.getStatus();

        jsonObject.put(NotificationFields.ID.getName(), callDetails.id());
        jsonObject.put(NotificationFields.TYPE.getName(), callDetails.getType().getId());
        jsonObject.put(NotificationFields.STATUS.getName(), Objects.equals(CallStatus.BUSY, callStatus) || Objects.equals(CallStatus.CANCELED, callStatus)
                ? CallStatus.NO_ANSWER.getId() : callStatus.getId());
        jsonObject.put(NotificationFields.IS_CALL_LOGGED.getName(), callDetails.isStoredAsCallLog());
        jsonObject.put(NotificationFields.IS__LOGGABLE.getName(), callDetails.isLoggable());
        final PhoneNumber userNumber = callDetails.getUserNumber();

        if (Objects.nonNull(summary)) {
            jsonObject.put(NotificationFields.SUMMARY.getName(), convertSummaryIntoJson(summary));
        }

        if (Objects.nonNull(userNumber)) {
            jsonObject.put(NotificationFields.NUMBER.getName(), convertPhoneNumberIntoJson(userNumber));
        }

        if (Objects.nonNull(contact)) {
            final DisplayPicture thumbnail = contact.getThumbnail();
            final JsonObject contactJson = convertContactIntoJson(contact);

            if (Objects.nonNull(thumbnail)) {
                contactJson.put(ContactFields.THUMBNAIL, contactConverter.convertThumbnailToJson(contact.id(), Contact.ENTITY_TYPE, thumbnail));
            }
            jsonObject.put(NotificationFields.CONTACT.getName(), contactJson);
        }

        if (Objects.nonNull(deal)) {
            jsonObject.put(NotificationFields.RELATED_DEAL.getName(), convertDealIntoJson(deal));
        }

        if (Objects.nonNull(notes) && !notes.isEmpty()) {
            jsonObject.put(NotificationFields.NOTES.getName(), convertNotesIntoJson(notes));
        }

        if (Objects.nonNull(callRecordings) && !callRecordings.isEmpty()) {
            jsonObject.put(NotificationFields.RECORDINGS.getName(), convertRecordingsIntoJson(callRecordings));
        }

        if (Objects.nonNull(participantUser)) {
            jsonObject.put(NotificationFields.USER.getName(), convertUserIntoJson(participantUser));
        }

        if (Objects.nonNull(forwardDetails)) {
            jsonObject.put(NotificationFields.IS_FORWARDED_CALL.getName(), true);
            jsonObject.put(NotificationFields.FORWARD_DETAILS.getName(), convertForwardDetailsIntoJson(forwardDetails));
        }
        final Phone contactNumber = callDetails.getContactNumber();

        if (Objects.nonNull(contactNumber)) {
            jsonObject.put(NotificationFields.CONTACT_NUMBER.getName(), convertPhoneIntoJson(contactNumber));
        }
        final Optional<FieldInfo> phoneFieldInfo = fieldService.getDefaultFieldByAliasName(Source.CONTACT, ContactFields.PHONES);

        phoneFieldInfo.ifPresent(fieldInfo -> {
            jsonObject.put(NotificationFields.PHONES_FIELD_ID.getName(), fieldInfo.id());
            final Map<StoreSubType, FieldSubType> phonesSubtypes = fieldTypeService.groupSubtypesByStoreSubType(StoreType.PHONES);
            final JsonObject phoneSubFieldJson = Json.object();

            phoneSubFieldJson.put(NotificationFields.PHONE_NUMBER_ID.getName(), phonesSubtypes.get(StoreSubType.PHONE_NUMBER).id());
            phoneSubFieldJson.put(NotificationFields.PHONE_TYPE_ID.getName(), phonesSubtypes.get(StoreSubType.PHONE_TYPE).id());
            phoneSubFieldJson.put(NotificationFields.COUNTRY_CODE_ID.getName(), phonesSubtypes.get(StoreSubType.COUNTRY_CODE).id());

            jsonObject.put(NotificationFields.PHONES_SUB_FIELD.getName(), phoneSubFieldJson);
        });

        return jsonObject;
    }

    /**
     * Converts a JSON object into a Phone object.
     *
     * @param jsonObject The JSON object containing phone details.
     * @return A Phone object with the extracted details.
     */
    private Phone convertJsonIntoContactNumber(final JsonObject jsonObject) {
        final Phone phone = new Phone();

        phone.setId(jsonObject.getLong(NotificationFields.ID.getName()));
        phone.setNumber(jsonObject.getString(NotificationFields.NUMBER.getName()));
        return phone;
    }

    /**
     * Converts a JSON object into a Contact object.
     *
     * @param jsonObject The JSON object containing contact details.
     * @return A Contact object with the extracted details.
     */
    private Contact convertJsonIntoContact(final JsonObject jsonObject) {
        final Contact contact = new Contact();

        contact.setId(jsonObject.getLong(NotificationFields.ID.getName()));
        contact.setFullName(jsonObject.getString(NotificationFields.NAME.getName()));
        return contact;
    }

    /**
     * Converts a collection of {@link CallForwardDetails} into a JSON array.
     *
     * @param forwardDetails The {@link CallForwardDetails} to convert.
     * @return A JSON object containing the call forwarding details.
     */
    private JsonObject convertForwardDetailsIntoJson(final CallForwardDetails forwardDetails) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(NotificationFields.FORWARDED_BY.getName(), convertUserIntoJson(forwardDetails.getForwardedBy()));
        jsonObject.put(NotificationFields.FORWARDED_TO.getName(), convertUserIntoJson(forwardDetails.getForwardedTo()));
        return jsonObject;
    }

    /**
     * Converts a CrmUser object into a JSON object.
     *
     * @param user The CrmUser object to convert.
     * @return A JSON object containing user details.
     */
    private JsonObject convertUserIntoJson(final CrmUser user) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(NotificationFields.ID.getName(), user.id());
        jsonObject.put(NotificationFields.NAME.getName(), user.getFullName());
        return jsonObject;
    }

    /**
     * Converts a collection of CallRecordingDetails into a JSON array.
     *
     * @param callRecordings The collection of call recordings to convert.
     * @return A JSON array containing call recording details.
     */
    private JsonArray convertRecordingsIntoJson(final Collection<CallRecord> callRecordings) {
        final JsonArray jsonArray = Json.array();
        final String idKey = NotificationFields.ID.getName();
        final String recordUrlKey = NotificationFields.RECORD_URL.getName();

        for (final CallRecord callRecording : callRecordings) {
            final JsonObject jsonObject = Json.object();

            jsonObject.put(idKey, callRecording.getId());
            jsonObject.put(recordUrlKey, callRecording.getUri());
            jsonArray.put(jsonObject);
        }
        return jsonArray;
    }

    /**
     * Converts a collection of Note objects into a JSON array.
     *
     * @param notes The collection of notes to convert.
     * @return A JSON array containing note details.
     */
    private JsonArray convertNotesIntoJson(final Collection<Note> notes) {
        final JsonArray jsonArray = Json.array();

        for (final Note note : notes) {
            final JsonObject jsonObject = Json.object();

            jsonObject.put(NotificationFields.ID.getName(), note.id());
            jsonObject.put(NotificationFields.NOTE.getName(), note.getContent());
            jsonObject.put(NotificationFields.CREATED_TIME.getName(), note.getCreatedTime().getUtcTimeStamp());
            jsonObject.put(NotificationFields.CREATED_BY.getName(), convertUserIntoJson(note.getCreatedBy()));
            jsonArray.put(jsonObject);
        }
        return jsonArray;
    }

    /**
     * Converts a {@link Contact} into a JSON object.
     *
     * @param contact The {@link Contact} object to convert.
     * @return A JSON object containing contact details.
     */
    private JsonObject convertContactIntoJson(final Contact contact) {
        final JsonObject jsonObject = Json.object();
        final Company company = contact.getCompany();

        jsonObject.put(NotificationFields.ID.getName(), contact.id());
        jsonObject.put(NotificationFields.NAME.getName(), contact.getFullName());

        if (Objects.nonNull(company)) {
            jsonObject.put(NotificationFields.COMPANY.getName(), company.getName());
        } //TODO Add profile pic
        return jsonObject;
    }

    /**
     * Converts a {@link Deal} into a JSON object.
     *
     * @param deal The {@link Deal} object to convert.
     * @return A JSON object containing deal details.
     */
    private JsonObject convertDealIntoJson(final Deal deal) {
        final JsonObject jsonObject = Json.object();
        final DealValue dealValue = deal.getDealValue();
        final PipelineStage pipelineStage = deal.getPipelineStage();
        final Date expectedCloseDate = deal.getExpectedCloseDate();

        jsonObject.put(NotificationFields.ID.getName(), deal.id());
        jsonObject.put(NotificationFields.TITLE.getName(), deal.getTitle());
        jsonObject.put(NotificationFields.DEAL_VALUE.getName(), monetaryConverter.convertPartialData(dealValue));
        jsonObject.put(NotificationFields.PIPELINE_STAGE.getName(), convertPipelineStageIntoJson(pipelineStage));

        if (Objects.nonNull(expectedCloseDate)) {
            jsonObject.put(NotificationFields.EXPECTED_CLOSE_DATE.getName(), expectedCloseDate.getTimeStamp());
        }
        return jsonObject;
    }

    /**
     * Converts a {@link PipelineStage} object into a JSON object.
     *
     * @param pipelineStage The {@link PipelineStage} object containing deal stage details.
     * @return A JSON object containing {@link PipelineStage} details.
     */
    private JsonObject convertPipelineStageIntoJson(final PipelineStage pipelineStage) {
        final JsonObject stageJson = Json.object();

        stageJson.put(NotificationFields.ID.getName(), pipelineStage.id());
        stageJson.put(NotificationFields.NAME.getName(), pipelineStage.getName());
        return stageJson;
    }

    /**
     * Converts a CallSummary object into a JSON object.
     *
     * @param summary The CallSummary object containing call details.
     * @return A JSON object containing call summary details.
     */
    private JsonObject convertSummaryIntoJson(final CallSummary summary) {
        final JsonObject jsonObject = Json.object();
        final Monetary cost = summary.getCost();

        jsonObject.put(NotificationFields.START_TIME.getName(), summary.getStartTime().getUtcTimeStamp());
        jsonObject.put(NotificationFields.END_TIME.getName(), summary.getEndTime().getUtcTimeStamp());
        jsonObject.put(NotificationFields.DURATION.getName(), summary.getDuration());
        jsonObject.put(NotificationFields.BILLING_DURATION.getName(), summary.getBillingDuration());

        if (Objects.nonNull(cost)) {
            jsonObject.put(NotificationFields.COST.getName(), Monetary.getValueWithSymbol(cost.getSymbol(), cost.getValue().doubleValue()));
        }
        return jsonObject;
    }

    /**
     * Converts a {@link Phone} into a JSON object.
     *
     * @param phone The {@link Phone} object to convert.
     * @return A JSON object containing {@link Phone} details.
     */
    private JsonObject convertPhoneIntoJson(final Phone phone) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(NotificationFields.ID.getName(), phone.id());
        jsonObject.put(NotificationFields.NUMBER.getName(), phone.getNumberWithCode());

        return jsonObject;
    }

    /**
     * Converts a {@link PhoneNumber} into a JSON object.
     *
     * @param phoneNumber The {@link PhoneNumber} object to convert.
     * @return A JSON object containing {@link PhoneNumber} details.
     */
    private JsonObject convertPhoneNumberIntoJson(final PhoneNumber phoneNumber) {
        final JsonObject jsonObject = Json.object();
        final String number = phoneNumber.getNumber();

        jsonObject.put(NotificationFields.ID.getName(), phoneNumber.id());
        jsonObject.put(NotificationFields.NUMBER.getName(), number);

        return jsonObject;
    }
}