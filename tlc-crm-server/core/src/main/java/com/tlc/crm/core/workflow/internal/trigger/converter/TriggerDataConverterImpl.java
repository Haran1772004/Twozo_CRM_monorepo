package com.tlc.crm.core.workflow.internal.trigger.converter;

import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.validator.TlcModel;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;

import com.tlc.crm.common.models.Action;
import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.core.workflow.model.TriggerInfo;
import com.tlc.crm.core.workflow.TriggerDataConverter;
import com.tlc.crm.core.workflow.handler.TriggerModelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The processor is tasked with encoding data into Json
 * and decoding them back, serving the purpose for workflow triggering.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class TriggerDataConverterImpl implements TriggerDataConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TriggerDataConverterImpl.class);
    private static final String ENTITY_IDS = "entityIds";
    private static final String ENTITY_TRIGGER_IDS = "entityTriggerIds";
    private static final String ORG_ID = "orgId";
    private static final String ORIGIN = "origin";
    private static final String ACTION = "action";
    private static final String NEED_NESTED_TRIGGER = "needNestedTrigger";
    private static final String NESTED_TRIGGER_INFO = "nestedTriggerInfo";

    private final TriggerModelHandler triggerModelHandler;

    private TriggerDataConverterImpl() {
        this.triggerModelHandler = TriggerModelHandler.getInstance();
    }

    private static class Instance {
        private static final TriggerDataConverterImpl INSTANCE = new TriggerDataConverterImpl();
    }

    /**
     * Gets the instance of {@link TriggerDataConverterImpl}.
     *
     * @return {@link TriggerDataConverterImpl} instance.
     */
    public static TriggerDataConverterImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Refers the organization id.
     * @param models Refers the models to be encapsulated.
     * @return The wrapped json object.
     */
    @Override
    public JsonObject convertToJson(final Long orgId, final Collection<? extends TlcModel> models, final TriggerInfo triggerInfo) {
        final JsonObject jsonObject = Json.object();
        final Source triggerSource = triggerInfo.source();
        final Collection<Long> entityIds = new HashSet<>(models.size());
        final JsonObject nestedTriggerInfoJson = Json.object();
        final boolean needNestedTrigger = triggerInfo.needNestedTrigger();

        jsonObject.put(ORG_ID, orgId);
        jsonObject.put(ENTITY_TRIGGER_IDS, triggerInfo.entityTriggerIds());
        jsonObject.put(ACTION, triggerInfo.action().name());
        jsonObject.put(ORIGIN, triggerInfo.modelSource().name());
        jsonObject.put(NEED_NESTED_TRIGGER, needNestedTrigger);

        for (final TlcModel model : models) {
            final Long modelId = model.id();

            entityIds.add(modelId);

            if (needNestedTrigger) {
                nestedTriggerInfoJson.put(String.valueOf(modelId), getNestedSourceTriggerStatus(triggerSource, model));
            }
        }

        if (needNestedTrigger) {
            jsonObject.put(NESTED_TRIGGER_INFO, nestedTriggerInfoJson);
        }
        jsonObject.put(ENTITY_IDS, entityIds);

        return jsonObject;
    }

    /**
     * <p>
     * Gets the nested source trigger status in json
     * </p>
     *
     * @param triggerSource Represents the source of the trigger event.
     * @param tlcModel      Represents the model of the trigger event.
     * @return the nested source trigger status in json.
     */
    private JsonObject getNestedSourceTriggerStatus(final Source triggerSource, final TlcModel tlcModel) {
        final JsonObject jsonObject = Json.object();

        switch (triggerSource) {
            case CONTACT -> {
                final Company company = ((Contact) tlcModel).getCompany();

                jsonObject.put(Source.COMPANY.getName(), Objects.nonNull(company) && company.isNewEntity());
            }
            case DEAL -> {
                final Deal deal = (Deal) tlcModel;
                final Contact primaryContact = deal.getPrimaryContact();
                final Company company = deal.getCompany();

                jsonObject.put(Source.CONTACT.getName(), Objects.nonNull(primaryContact) && primaryContact.isNewEntity());
                jsonObject.put(Source.COMPANY.getName(), Objects.nonNull(company) && company.isNewEntity());
            }
        }

        return jsonObject;
    }

    /**
     * {@inheritDoc}
     *
     * @param jsonObject Refers the json object to be unwrapped.
     * @param source     Refers the source of the model.
     * @param action     Refers the action event.
     * @return The collection of models.
     */
    @Override
    public Collection<Pair<TlcModel, TlcModel>> extractModelPairs(final JsonObject jsonObject, final Source source, final Action action, final Map<Long, Long> entityStateIdMap) {
        final Collection<Long> entityIds = new HashSet<>();
        final Long orgId = jsonObject.getLong(ORG_ID);
        final Collection<Long> entityStateIds = new HashSet<>();

        jsonObject.getJsonArray(ENTITY_IDS).forEach(entityId -> entityIds.add((Long) entityId));
        Optional.ofNullable(jsonObject.optJsonArray(ENTITY_TRIGGER_IDS))
                .ifPresent(jsonArray -> jsonArray.forEach(entityTriggerId -> entityStateIds.add((Long) entityTriggerId)));
        LOGGER.info("Workflow Execution: Retrieving models for entity Ids: {}, OrgId: {}, Source: {}", entityIds, orgId, source);

        if (Objects.equals(action, Action.DELETE)) {
            final Map<Long, TlcModel> deletedModels = triggerModelHandler.fetchOldModels(orgId, entityStateIds, source, entityStateIdMap);

            return getModelPairs(deletedModels.values(), Collections.emptyMap());
        } else {
            final Collection<? extends TlcModel> models = triggerModelHandler.fetchModels(orgId, entityIds, source);

            if (Objects.nonNull(models) && !models.isEmpty()) {
                return getModelPairs(models, Objects.equals(Action.UPDATE, action)
                        ? triggerModelHandler.fetchOldModels(orgId, entityStateIds, source, entityStateIdMap)
                        : Collections.emptyMap());
            }
        }

        return Collections.emptyList();
    }

    /**
     * <p>
     * Gets the model pairs from the provided collection of models and previous models
     * </p>
     *
     * @param models         Represents the collection of models
     * @param previousModels Represents the collection of previous models
     * @return the model pairs
     */
    private Collection<Pair<TlcModel, TlcModel>> getModelPairs(final Collection<? extends TlcModel> models, final Map<Long, TlcModel> previousModels) {
        final Collection<Pair<TlcModel, TlcModel>> modelPairs = new ArrayList<>();

        for (final TlcModel model : models) {
            modelPairs.add(Pair.of(model, previousModels.get(model.id())));
        }

        return modelPairs;
    }
}