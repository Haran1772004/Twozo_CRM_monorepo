package com.tlc.crm.workflow.internal.trigger.action.converter;

import com.tlc.iam.common.models.User;
import com.tlc.validator.TlcModel;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.core.workflow.EntityTriggerManager;
import com.tlc.crm.core.workflow.internal.converter.ActivityWorkflowConverter;
import com.tlc.crm.core.workflow.internal.converter.CompanyWorkflowConverter;
import com.tlc.crm.core.workflow.internal.converter.ContactWorkflowConverter;
import com.tlc.crm.core.workflow.internal.converter.DealWorkflowConverter;
import com.tlc.crm.core.workflow.internal.trigger.EntityTrigger;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;

import com.tlc.crm.workflow.api.model.Workflow;
import com.tlc.crm.workflow.api.model.TriggerSource.TriggerEvent;
import com.tlc.crm.workflow.api.manager.WorkflowManager;
import com.tlc.crm.workflow.api.model.WorkflowAction;
import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.internal.trigger.action.ActionExecutionContext;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionDataParserFields;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionAttributes;
import com.tlc.crm.workflow.internal.trigger.action.navigate.ActionNavigator;
import com.tlc.crm.workflow.internal.trigger.action.ActionDataParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The parser which facilitates the conversion of data form
 * {@link ActionExecutionContext} to {@link JsonObject} and vice versa.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ActionDataParserImpl implements ActionDataParser {

    private final ContactManager contactManager;
    private final CompanyManager companyManager;
    private final DealManager dealManager;
    private final SourceActivityManager sourceActivityManager;
    private final WorkflowManager workflowManager;
    private final ContactWorkflowConverter contactWorkflowConverter;
    private final CompanyWorkflowConverter companyWorkflowConverter;
    private final DealWorkflowConverter dealWorkflowConverter;
    private final ActivityWorkflowConverter activityWorkflowConverter;
    private final EntityTriggerManager entityTriggerManager;

    private ActionDataParserImpl() {
        this.contactManager = ContactManager.getInstance();
        this.companyManager = CompanyManager.getInstance();
        this.dealManager = DealManager.getInstance();
        this.sourceActivityManager = SourceActivityManager.getInstance();
        this.workflowManager = WorkflowManager.getInstance();
        this.contactWorkflowConverter = ContactWorkflowConverter.getInstance();
        this.companyWorkflowConverter = CompanyWorkflowConverter.getInstance();
        this.dealWorkflowConverter = DealWorkflowConverter.getInstance();
        this.activityWorkflowConverter = ActivityWorkflowConverter.getInstance();
        this.entityTriggerManager = EntityTriggerManager.getInstance();
    }

    private static class Instance {
        private static final ActionDataParserImpl INSTANCE = new ActionDataParserImpl();
    }

    public static ActionDataParserImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                  Refers the organization id.
     * @param workflow               Refers the workflow.
     * @param actionExecutionContext Refers the {@link ActionExecutionContext}.
     * @return The converted json.
     */
    @Override
    public JsonObject wrapToJson(final Long orgId, final Workflow workflow, final ActionExecutionContext actionExecutionContext) {
        final JsonObject jsonObject = Json.object();
        final Pair<TriggerSource, TlcModel> triggerModel = actionExecutionContext.triggerModel();

        jsonObject.put(ActionDataParserFields.ORG_ID, orgId);
        jsonObject.put(ActionDataParserFields.WORKFLOW_ID, workflow.id());
        jsonObject.put(ActionDataParserFields.HISTORY_ID, actionExecutionContext.executionHistoryId());
        jsonObject.put(ActionDataParserFields.USERS, convertUsersToJson(actionExecutionContext.groupedUsers()));
        jsonObject.put(ActionDataParserFields.TRIGGER_MODEL, convertTriggerModelToJson(triggerModel));
        jsonObject.put(ActionDataParserFields.CREATED_ENTITIES, convertModelPairsToJson(actionExecutionContext.retrieveCreatedEntities()));
        jsonObject.put(ActionDataParserFields.WORKFLOW_ACTIONS, actionExecutionContext.navigator().getAll().stream().map(WorkflowAction::getId).toList());
        jsonObject.put(ActionDataParserFields.TRIGGER_EVENT_ID, workflow.getTriggerEvent().getId());
        jsonObject.put(ActionDataParserFields.ENTITY_STATE_ID, actionExecutionContext.entityStateIdMap().get(triggerModel.getValue().id()));

        return jsonObject;
    }

    /**
     * {@inheritDoc}
     *
     * @param jsonObject Refers the {@link JsonObject} which holds the action requisite data.
     * @return The {@link ActionExecutionContext}.
     */
    @Override
    public ActionExecutionContext unwrapJson(final JsonObject jsonObject) {
        final long orgId = jsonObject.getLong(ActionDataParserFields.ORG_ID);
        final long historyId = jsonObject.getLong(ActionDataParserFields.HISTORY_ID);
        final Map<Long, User> groupedUsers = convertToUsers(jsonObject.getJsonArray(ActionDataParserFields.USERS));
        final Pair<TriggerSource, TlcModel> triggerModel = convertTriggerModel(orgId, jsonObject);
        final List<Pair<String, TlcModel>> createdEntities = convertCreatedEntities(orgId, jsonObject.optJsonArray(ActionDataParserFields.CREATED_ENTITIES));
        final Collection<Long> workflowActionIds = new ArrayList<>();

        jsonObject.getJsonArray(ActionDataParserFields.WORKFLOW_ACTIONS).forEach(actionId -> workflowActionIds.add((Long) actionId));

        final List<WorkflowAction> workflowActions = workflowManager.getWorkflowActions(orgId, jsonObject.getLong(ActionDataParserFields.WORKFLOW_ID), workflowActionIds);

        return new ActionExecutionContext(triggerModel, new ActionNavigator(workflowActions), groupedUsers,
                historyId, createdEntities, jsonObject, Collections.emptyMap());
    }

    /**
     * Converts the users detail to {@link JsonArray}.
     *
     * @param users Refers the grouped users.
     * @return The {@link JsonArray} which holds the
     */
    private JsonArray convertUsersToJson(final Map<Long, User> users) {
        final JsonArray jsonArray = Json.array();

        for (final User user : users.values()) {
            final JsonObject userJson = Json.object();

            userJson.put(ActionDataParserFields.ID, user.id());
            userJson.put(ActionDataParserFields.FIRST_NAME, user.getFirstName());
            userJson.put(ActionDataParserFields.LAST_NAME, user.getLastName());
            userJson.put(ActionDataParserFields.EMAIL, user.getEmail());
            userJson.put(ActionDataParserFields.PHONE, user.getMobile());
            userJson.put(ActionDataParserFields.STATE, user.getUserState().id());

            jsonArray.put(userJson);
        }

        return jsonArray;
    }

    /**
     * Converts the trigger model to {@link JsonObject}.
     *
     * @param triggerModel Refers the model which is to be triggered to execute workflow.
     * @return The converted json.
     */
    private JsonObject convertTriggerModelToJson(final Pair<TriggerSource, TlcModel> triggerModel) {
        final JsonObject triggerModelJson = Json.object();
        final String triggerSource = triggerModel.getKey().getName();

        triggerModelJson.put(ActionDataParserFields.TRIGGER_SOURCE, triggerSource);
        triggerModelJson.put(ActionDataParserFields.ENTITY_ID, triggerModel.getValue().id());

        return triggerModelJson;
    }

    /**
     * {@inheritDoc}
     *
     * @param createdModelPair Refers the model which is created from the workflow execution.
     * @return The converted json.
     */
    @Override
    public JsonObject convertModelPairToJson(final Pair<String, TlcModel> createdModelPair) {
        final JsonObject jsonObject = Json.object();
        final String actionSource = createdModelPair.getKey();

        jsonObject.put(ActionDataParserFields.ACTION_SOURCE, actionSource);
        jsonObject.put(ActionDataParserFields.ENTITY_ID, createdModelPair.getValue().id());

        return jsonObject;
    }

    /**
     * Converts the models which are created from the workflow execution to {@link JsonArray}.
     *
     * @param createdEntities Refers the models which is created from the workflow execution.
     * @return The converted json.
     */
    private JsonArray convertModelPairsToJson(final List<Pair<String, TlcModel>> createdEntities) {
        final JsonArray jsonArray = Json.array();

        for (final Pair<String, TlcModel> createdModelPair : createdEntities) {
            jsonArray.put(convertModelPairToJson(createdModelPair));
        }

        return jsonArray;
    }

    /**
     * Converts the json array to user.
     *
     * @param jsonArray Refers the json array which holds the user details.
     * @return The users grouped by their IDs, with each ID serving as a key and its corresponding user as the value.
     */
    private Map<Long, User> convertToUsers(final JsonArray jsonArray) {
        final Map<Long, User> groupedUsers = new HashMap<>();

        for (int index = 0; index < jsonArray.size(); index++) {
            final JsonObject userJson = jsonArray.getJsonObject(index);
            final User user = new User();
            final Long id = userJson.getLong(ActionDataParserFields.ID);

            user.setId(id);
            user.setFirstName(userJson.optString(ActionDataParserFields.FIRST_NAME, ""));
            user.setLastName(userJson.optString(ActionDataParserFields.LAST_NAME, ""));
            user.setEmail(userJson.optString(ActionDataParserFields.EMAIL, ""));
            user.setMobile(userJson.optString(ActionDataParserFields.PHONE, ""));
            user.setUserState(User.State.get(userJson.getShort(ActionDataParserFields.STATE)));

            groupedUsers.put(id, user);
        }

        return groupedUsers;
    }

    /**
     * Converts the {@link JsonObject} into model.
     *
     * @param orgId      Refers the organization id.
     * @param jsonObject Refers the {@link JsonObject}.
     * @return The trigger model object.
     */
    private Pair<TriggerSource, TlcModel> convertTriggerModel(final Long orgId, final JsonObject jsonObject) {
        final JsonObject triggerModelJson = jsonObject.getJsonObject(ActionDataParserFields.TRIGGER_MODEL);
        final TriggerSource triggerSource = TriggerSource.getByName(triggerModelJson.getString(ActionDataParserFields.TRIGGER_SOURCE));
        final String source = triggerSource.getName();
        final TriggerEvent triggerEvent = TriggerEvent.get(jsonObject.getShort(ActionDataParserFields.TRIGGER_EVENT_ID));

        return Pair.of(triggerSource, Objects.equals(TriggerEvent.DELETED, triggerEvent)
                ? fetchDeletedModel(orgId, jsonObject.getLong(ActionDataParserFields.ENTITY_STATE_ID), Source.get(source))
                : fetchModel(orgId, triggerModelJson.getLong(ActionDataParserFields.ENTITY_ID), source));
    }

    /**
     * Converts the {@link JsonArray} into list of model pair.
     *
     * @param orgId     Refers the organization id.
     * @param jsonArray Refers the {@link JsonArray} which holds the created entity details.
     * @return The list of created entity.
     */
    private List<Pair<String, TlcModel>> convertCreatedEntities(final Long orgId, final JsonArray jsonArray) {
        if (Objects.nonNull(jsonArray)) {
            final List<Pair<String, TlcModel>> createdEntities = new LinkedList<>();

            for (int index = 0; index < jsonArray.size(); index++) {
                final JsonObject jsonObject = jsonArray.getJsonObject(index);
                final String actionSource = jsonObject.getString(ActionDataParserFields.ACTION_SOURCE);

                createdEntities.add(Pair.of(actionSource, switch (actionSource) {
                    case ActionAttributes.EMAIL, ActionAttributes.WHATSAPP -> null;
                    default -> fetchModel(orgId, jsonObject.getLong(ActionDataParserFields.ENTITY_ID), actionSource);
                }));
            }

            return createdEntities;
        }

        return List.of();
    }

    /**
     * Fetches the {@link TlcModel} for the given entity id and source.
     *
     * @param orgId    Refers the organization id.
     * @param entityId Refers the entity id.
     * @param source   Refers the source of the model.
     * @return {@link TlcModel}.
     */
    private TlcModel fetchModel(final Long orgId, final Long entityId, final String source) {
        return switch (source.toLowerCase()) {
            case "company" -> {
                final Company company = companyManager.get(orgId, entityId);

                companyManager.loadSummaryDetails(orgId, company);
                yield company;
            }
            case "contact" -> {
                final Contact contact = contactManager.get(orgId, entityId);

                contactManager.loadSummaryDetails(orgId, contact);
                yield contact;
            }
            case "deal" -> {
                final Deal deal = dealManager.get(orgId, entityId);

                dealManager.loadSummaryDetails(orgId, deal);
                yield deal;
            }
            case "activity" -> sourceActivityManager.get(orgId, entityId);
            default -> null;
        };
    }

    /**
     * <p>
     * Fetches the deleted {@link TlcModel}
     * </p>
     *
     * @param orgId           Represents the organization id
     * @param entityTriggerId Represents the entity trigger id
     * @param source          Represents the source
     * @return the {@link TlcModel}
     */
    private TlcModel fetchDeletedModel(final Long orgId, final Long entityTriggerId, final Source source) {
        final Optional<EntityTrigger> optionalEntityTrigger = entityTriggerManager.get(orgId, List.of(entityTriggerId), source).stream().findFirst();

        if (optionalEntityTrigger.isPresent()) {
            final JsonObject priorStateJson = optionalEntityTrigger.get().priorState();

            return switch (source) {
                case CONTACT -> contactWorkflowConverter.convert(priorStateJson);
                case COMPANY -> companyWorkflowConverter.convert(priorStateJson);
                case DEAL -> dealWorkflowConverter.convert(priorStateJson);
                case ACTIVITY -> activityWorkflowConverter.convert(priorStateJson);
                default -> null;
            };
        }

        return null;
    }
}
