package com.tlc.crm.core.features.action.call.model.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.converter.ConverterConfig;

import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.core.features.action.call.manager.CallLogManager;
import com.tlc.crm.core.features.action.call.model.CallLog;
import com.tlc.crm.core.features.action.call.model.Outcome;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.DealValue;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.field.call.CallLogFields;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.models.type.converter.MonetaryConverter;
import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.service.provider.I18nServiceProvider;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.notification.model.call.CallType;

import java.time.Instant;
import java.util.Objects;

/**
 * Class for converting CallLog objects to and from JSON representations.
 * Acts as a bridge between internal CallLog models and their external JSON formats.
 *
 * @author Sathish
 * @version 1.0
 */
public class CallLogConverter extends EntityConverter<CallLog> {

    private final CallLogManager callLogManager;
    private final MonetaryConverter monetaryConverter;
    private final UserService userService;

    private CallLogConverter() {
        callLogManager = CallLogManager.getInstance();
        monetaryConverter = MonetaryConverter.getInstance();
        userService = UserService.getInstance();
    }

    private static final class InstanceHolder {
        private static final CallLogConverter CALL_LOG_CONVERTER = new CallLogConverter();
    }

    public static CallLogConverter getInstance() {
        return InstanceHolder.CALL_LOG_CONVERTER;
    }

    /**
     * Converts a {@link JsonObject} into a {@link CallLog} object using the provided configuration.
     *
     * @param converterConfig Configuration that may include context or mapping strategies.
     * @param jsonObject      JSON object containing call log data.
     * @return {@link CallLog} instance created from JSON input.
     */
    @Override
    public CallLog convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        if (converterConfig.isPartialData()) {
            return constructCallLogByPartialData(converterConfig, jsonObject);
        } else {
            return constructCallLog(converterConfig, jsonObject);
        }
    }

    /**
     * Converts a {@link CallLog} object into its JSON representation.
     *
     * @param converterConfig Configuration that may include context or mapping strategies.
     * @param callLog         The call log object to convert.
     * @return {@link JsonObject} representing the call log.
     */
    @Override
    public JsonObject convert(final ConverterConfig converterConfig, final CallLog callLog) {
        final Outcome outcome = callLog.getOutcome();

        if (Objects.nonNull(outcome)) {
            final I18nResolver i18nResolver = I18nServiceProvider.get().getResolver(converterConfig.locale());

            outcome.setName(i18nResolver.get(outcome.getName()));
        }
        return convert(callLog);
    }

    /**
     * Converts a {@link CallLog} object into its JSON representation.
     *
     * @param callLog The call log object to convert.
     * @return {@link JsonObject} representing the call log.
     */
    @Override
    public JsonObject convert(final CallLog callLog) {
        final JsonObject jsonObject = Json.object();
        final Outcome outcome = callLog.getOutcome();
        final Deal relatedDeal = callLog.getRelatedDeal();
        final DateTime startTime = callLog.getStartTime();
        final Contact participant = callLog.getParticipant();

        jsonObject.put(CallLogFields.ID.getName(), callLog.id());
        jsonObject.put(CallLogFields.AGENDA.getName(), callLog.getAgenda());
        jsonObject.put(CallLogFields.CALL_TYPE.getName(), callLog.getCallType().getId());
        jsonObject.put(CallLogFields.LOGGED_TIME.getName(), callLog.getLoggedTime().getUtcTimeStamp());
        jsonObject.put(CallLogFields.CREATED_BY.getName(), callLog.getCreatedBy().getFullName());

        if (Objects.nonNull(participant)) {
            jsonObject.put(CallLogFields.PARTICIPANT.getName(), convertContactIntoJson(participant));
        }

        if (Objects.nonNull(startTime)) {
            jsonObject.put(CallLogFields.START_TIME.getName(), startTime.getUtcTimeStamp());
        }

        if (Objects.nonNull(outcome)) {
            jsonObject.put(CallLogFields.OUTCOME.getName(), convertOutcomeIntoJson(outcome));
        }

        if (Objects.nonNull(relatedDeal)) {
            jsonObject.put(CallLogFields.RELATED_DEAL.getName(), convertDealIntoJson(relatedDeal));
        }
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

        jsonObject.put(CallLogFields.ID.getName(), deal.id());
        jsonObject.put(CallLogFields.TITLE.getName(), deal.getTitle());
        jsonObject.put(CallLogFields.DEAL_VALUE.getName(), convertDealValueIntoJson(dealValue));
        jsonObject.put(CallLogFields.IS_ARCHIVED.getName(), deal.isArchived());
        jsonObject.put(CallLogFields.PIPELINE_STAGE.getName(), convertPipelineStageIntoJson(pipelineStage));

        if (Objects.nonNull(expectedCloseDate)) {
            jsonObject.put(CallLogFields.EXPECTED_CLOSE_DATE.getName(), expectedCloseDate.getTimeStamp());
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

        stageJson.put(CallLogFields.ID.getName(), pipelineStage.id());
        stageJson.put(CallLogFields.NAME.getName(), pipelineStage.getName());
        return stageJson;
    }

    /**
     * Converts a {@link DealValue} object into a JSON object.
     *
     * @param dealValue The {@link DealValue} object containing deal value details.
     * @return A JSON object containing {@link DealValue} details.
     */
    private JsonObject convertDealValueIntoJson(final DealValue dealValue) {
        return monetaryConverter.convert(new ConverterConfig(false, false), dealValue);
    }

    /**
     * Converts an {@link Outcome} object into its JSON representation.
     *
     * @param outcome the Outcome object to convert
     * @return JsonObject representing the outcome with ID and name
     */
    private JsonObject convertOutcomeIntoJson(final Outcome outcome) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(CallLogFields.ID.getName(), outcome.getId());
        jsonObject.put(CallLogFields.NAME.getName(), outcome.getName());
        return jsonObject;
    }

    /**
     * Converts a {@link Contact} object into its JSON representation.
     *
     * @param participant the {@link Contact} object to convert.
     * @return {@link JsonObject} representing the contact with ID, full name and company.
     */
    private JsonObject convertContactIntoJson(final Contact participant) {
        final JsonObject jsonObject = Json.object();
        final Company company = participant.getCompany();
        final Phone phone = participant.getPrimaryPhone();

        jsonObject.put(CallLogFields.ID.getName(), participant.id());
        jsonObject.put(CallLogFields.NAME.getName(), participant.getFullName()); // TODO: Add profile picture

        if (Objects.nonNull(phone)) {
            jsonObject.put(CallLogFields.CONTACT_NUMBER.getName(), convertPhoneIntoJson(phone));
        }

        if (Objects.nonNull(company)) {
            jsonObject.put(CallLogFields.COMPANY.getName(), convertCompanyToJson(company));
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

        jsonObject.put(CallLogFields.ID.getName(), phone.id());
        jsonObject.put(CallLogFields.NUMBER.getName(), phone.getNumberWithCode());

        return jsonObject;
    }

    /**
     * Converts a {@link Company} object into its JSON representation.
     *
     * @param company the {@link Company} object to convert.
     * @return {@link JsonObject} representing the contact with ID and name.
     */
    private JsonObject convertCompanyToJson(final Company company) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(CallLogFields.ID.getName(), company.id());
        jsonObject.put(CallLogFields.NAME.getName(), company.getName());
        return jsonObject;
    }

    /**
     * Constructs a partial {@link CallLog} object by loading from DB using ID from input JSON.
     * Used when a pre-existing CallLog is being updated.
     *
     * @param converterConfig Configuration containing organization and user context
     * @param jsonObject JSON object with minimal data including CallLog ID
     * @return CallLog object populated with existing data and updated from JSON
     */
    private CallLog constructCallLogByPartialData(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final CallLog callLog = callLogManager.get(converterConfig.orgId(), jsonObject.getLong(CallLogFields.ID.getName()));

        setJsonDataToModel(callLog, jsonObject);
        return callLog;
    }

    /**
     * Constructs a new {@link CallLog} object or sets values into an existing one.
     * If ID is present, assumes update; otherwise, creates a new CallLog with metadata.
     *
     * @param converterConfig Configuration containing organization and user context
     * @param jsonObject JSON object with call log data
     * @return A new or updated CallLog instance
     */
    private CallLog constructCallLog(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final CallLog callLog = new CallLog();
        final long orgId = converterConfig.orgId();
        final String idKey = CallLogFields.ID.getName();

        if (jsonObject.containsKey(idKey)) {
            callLog.setId(jsonObject.getLong(idKey));
        } else {
            callLog.setCreatedBy(userService.getUser(orgId, converterConfig.ownerId()));
            callLog.setLoggedTime(new DateTime(Instant.now().toEpochMilli()));
        }
        callLog.setOrgId(orgId);
        setJsonDataToModel(callLog, jsonObject);
        return callLog;
    }

    /**
     * Maps fields from the input {@link JsonObject} into the given {@link CallLog} object.
     * Fields handled include agenda, call type, start time, participant, outcome, and related deal.
     *
     * @param callLog the CallLog object to populate
     * @param jsonObject the JSON object with values for the CallLog fields
     */
    private void setJsonDataToModel(final CallLog callLog, final JsonObject jsonObject) {
        final String agendaKey = CallLogFields.AGENDA.getName();
        final String callTypeKey = CallLogFields.CALL_TYPE.getName();
        final String startTimeKey = CallLogFields.START_TIME.getName();
        final String participantIdKey = CallLogFields.PARTICIPANT_ID.getName();
        final String outcomeIdKey = CallLogFields.OUTCOME_ID.getName();
        final String relatedDealIdKey = CallLogFields.RELATED_DEAL_ID.getName();

        if (jsonObject.containsKey(agendaKey)) {
            callLog.setAgenda(jsonObject.getString(agendaKey));
        }

        if (jsonObject.containsKey(callTypeKey)) {
            callLog.setCallType(CallType.get(jsonObject.getShort(callTypeKey)));
        }

        if (jsonObject.containsKey(startTimeKey)) {
            callLog.setStartTime(new DateTime(jsonObject.getLong(startTimeKey)));
        }

        if (jsonObject.containsKey(participantIdKey)) {
            final Contact contact = new Contact();

            contact.setId(jsonObject.getLong(participantIdKey));
            callLog.setParticipant(contact);
        }

        if (jsonObject.containsKey(outcomeIdKey)) {
            final Outcome outcome = new Outcome();

            outcome.setId(jsonObject.getLong(outcomeIdKey));
            callLog.setOutcome(outcome);
        }

        if (jsonObject.containsKey(relatedDealIdKey)) {
            final Deal deal = new Deal();

            deal.setId(jsonObject.getLong(relatedDealIdKey));
            callLog.setRelatedDeal(deal);
        }
    }
}
