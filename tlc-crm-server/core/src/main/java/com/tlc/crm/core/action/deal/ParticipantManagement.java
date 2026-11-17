package com.tlc.crm.core.action.deal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmConfigAction;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.manager.ParticipantManager;
import com.tlc.crm.core.api.deal.model.Participant;
import com.tlc.crm.core.api.deal.model.converter.ParticipantConverter;
import com.tlc.crm.core.field.contact.CompanyFields;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.field.deal.ParticipantFields;
import com.tlc.crm.core.internal.deal.status.DealErrorCodes;
import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages the {@link Participant} related web actions
 *
 * @author Sundar
 * @version 1.0
 */
@WebAction(path = "/deal/participant/mgmt")
public class ParticipantManagement extends CrmConfigAction<Participant> {

    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 50;

    private final ConverterConfigBuilder converterConfigBuilder;
    private final ContactManager contactManager;
    private final DealManager dealManager;
    private final ParticipantConverter participantConverter;
    private final ParticipantManager participantManager;
    private final FieldService fieldService;

    public ParticipantManagement() {
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        contactManager = ContactManager.getInstance();
        dealManager = DealManager.getInstance();
        participantConverter = ParticipantConverter.getInstance();
        participantManager = ParticipantManager.getInstance();
        fieldService = FieldService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  the received request
     * @param response the response to be sent
     */
    @Override
    public void handle(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString("type");
        final JsonObject data = requestJson.getJsonObject("data");
        final Long orgId = request.orgId();

        switch (type) {
            case "getParticipantsByDeal" -> getParticipantsByDeal(orgId, data, response);
            case "removeParticipant" -> removeParticipant(orgId, data);
            case "addRelatedContacts" -> addRelatedContactsToDeal(orgId, data, response, false);
            case "getParticipantFieldIds" -> getParticipantFieldIds(orgId, response);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     * @throws Exception if an error occurs during the request processing
     */
    @Override
    protected void handleMobileRequest(final CrmRequest crmRequest, final CrmResponse crmResponse) throws Exception {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final String type = requestJson.getString(CompanyFields.TYPE);
        final JsonObject data = requestJson.getJsonObject(CompanyFields.DATA);
        final long orgId = crmRequest.orgId();

        switch (type) {
            case "addRelatedContacts" -> addRelatedContactsToDeal(orgId, data, crmResponse, true);
            default -> authorizedProcess(crmRequest, crmResponse);
        }
    }

    /**
     * <p>
     * Adds the related contacts to a given deal for the specified organization
     * </p>
     *
     * @param orgId                      Represents the organization id
     * @param jsonObject                 Represents the {@link JsonObject} containing deal and participant ids
     * @param crmResponse                Represents the {@link CrmResponse}
     * @param removeExistingParticipants Represents a flag to determine whether to remove existing participants or not
     */
    private void addRelatedContactsToDeal(final long orgId, final JsonObject jsonObject, final CrmResponse crmResponse, final boolean removeExistingParticipants) {
        final Long dealId = jsonObject.getLong(ParticipantFields.DEAL_ID.getName());

        if (dealManager.exists(orgId, dealId)) {
            final JsonArray jsonArray = jsonObject.getJsonArray(ParticipantFields.CONTACT_IDS.getName());
            final Collection<Long> contactIds = new HashSet<>();
            final int size = jsonArray.size();

            for (int index = 0; index < size; index++) {
                contactIds.add(jsonArray.getLong(index));
            }

            if (!contactIds.isEmpty() && !contactManager.exists(orgId, contactIds)) {
                throw ErrorCode.get(DealErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found");
            }

            participantManager.updateParticipants(orgId, dealId, contactIds, removeExistingParticipants);
            crmResponse.put(CompanyFields.MESSAGE, crmResponse.resolveI18nString("i18n.contact.linked.successfully"));
        } else {
            throw ErrorCode.get(DealErrorCodes.DEAL_NOT_FOUND, "i18n.deal.not.found");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link AbstractConfigManager} of participant
     */
    @Override
    public ConfigManager<Participant> getConfigManager() {
        return participantManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Participant convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);

        return participantConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param participant which contains the data
     * @return the {@link JsonObject} filled with the data
     */
    @Override
    public JsonObject convert(final Participant participant) {
        return participantConverter.convert(participant);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Participant convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, jsonObject);

        return participantConverter.convert(converterConfig, jsonObject);
    }

    /**
     * Gets the participants by  given deal id
     *
     * @param orgId    Represents the organisation id
     * @param data     Which contains the request data
     * @param response Which represents the response data
     */
    private void getParticipantsByDeal(final Long orgId, final JsonObject data, final CrmResponse response) {
        final JsonArray participantsArray = Json.array();
        final Long id = data.getLong(ParticipantFields.ID.getName());
        final int begin = data.optInt(ParticipantFields.START.getName(), DEFAULT_START_VALUE);
        final int count = data.optInt(ParticipantFields.LIMIT.getName(), DEFAULT_LIMIT);
        final int start = begin > 0 ? begin : DEFAULT_START_VALUE;
        final int limit = count > 0 ? count : DEFAULT_LIMIT;
        final Long totalRecords = participantManager.getDealParticipantCount(orgId, id);
        final Collection<Participant> participants = participantManager.getParticipantsByDeal(orgId, id, start, limit);

        participants.forEach(participant -> participantsArray.put(participantConverter.convertDealParticipant(participant)));

        response.put(ParticipantFields.TOTAL_RECORDS.getName(), totalRecords);
        response.put(ParticipantFields.HAS_MORE.getName(), (start + limit) <= totalRecords);
        response.put(ParticipantFields.LIST.getName(), participantsArray);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        represents the organization ID
     * @param userId       Represents the user id.
     * @param action       perform action like create, update or delete
     * @param participants which contains the {@link Collection} of participants
     */
    @Override
    protected final boolean isValidDetails(final Long orgId, final Long userId, final Action action, final Collection<Participant> participants) {
        if (Objects.nonNull(participants) && !participants.isEmpty()) {
            switch (action) {
                case CREATE, UPDATE -> {
                    final Map<Long, List<Participant>> dealParticipants = participants.stream()
                            .collect(Collectors.groupingBy(Participant::getDealId));
                    final Set<Long> contactIds = participants.stream().map(Participant::getContactId)
                            .collect(Collectors.toSet());

                    if (!dealManager.exists(orgId, dealParticipants.keySet())) {
                        throw ErrorCode.get(DealErrorCodes.DEAL_NOT_FOUND, "i18n.deal.not.found");
                    }

                    if (!contactManager.exists(orgId, contactIds)) {
                        throw ErrorCode.get(DealErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found");
                    }

                    if (participantManager.isAlreadyMapped(orgId, dealParticipants)) {
                        throw ErrorCode.get(DealErrorCodes.CONTACT_ALREADY_LINKED, "i18n.contact.linked.already");
                    }
                }
            }
        }
        return true;
    }

    /**
     * <p>
     * Removes the participant from the deal
     * </p>
     *
     * @param orgId Represents the organisation Id
     * @param data  Represents the data json
     */
    private void removeParticipant(final Long orgId, final JsonObject data) {
        final long contactId = data.getLong(ParticipantFields.CONTACT_ID.getName());
        final long dealId = data.getLong(ParticipantFields.DEAL_ID.getName());
        final Collection<Participant> participants = new HashSet<>();
        final Collection<Long> participantIds = participantManager.getParticipantIds(orgId, dealId, List.of(contactId));

        participantIds.forEach(id -> {
            final Participant participant = new Participant();

            participant.setId(id);
            participants.add(participant);
        });
        participantManager.delete(orgId, participants);
    }

    /**
     * <p>
     * Gets the participant field ids
     * </p>
     *
     * @param orgId       Represents the org id
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void getParticipantFieldIds(final long orgId, final CrmResponse crmResponse) {
        final String firstNameKey = ContactFields.FIRST_NAME;
        final String lastNameKey = ContactFields.LAST_NAME;
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(orgId, Contact.ENTITY_TYPE, Set.of(firstNameKey, lastNameKey));
        final JsonObject fieldJson = Json.object();
        final FieldInfo firstNameField = fields.get(firstNameKey);
        final FieldInfo lastNameField = fields.get(lastNameKey);

        if (Objects.nonNull(firstNameField)) {
            fieldJson.put(ParticipantFields.FIRST_NAME_FIELD_ID.getName(), firstNameField.id());
        }

        if (Objects.nonNull(lastNameField)) {
            fieldJson.put(ParticipantFields.LAST_NAME_FIELD_ID.getName(), lastNameField.id());
        }
        crmResponse.put(ParticipantFields.RELATED_FIELD_IDS.getName(), fieldJson);
    }
}