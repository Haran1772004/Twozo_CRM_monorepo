package com.tlc.crm.workflow.v2.internal.action.formatter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;

import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.core.features.api.tag.SourceTagManager;

import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;

import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.cache.service.WorkflowFieldService;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionAttributes;
import com.tlc.crm.workflow.internal.trigger.action.formatter.StepOptionRetriever;
import com.tlc.crm.workflow.internal.trigger.action.tracker.ActionFailureCauseTracker;
import com.tlc.crm.workflow.v2.field.WorkflowAttributesV2;
import com.tlc.crm.workflow.v2.internal.action.StepOptionInfoV2;
import com.tlc.crm.workflow.v2.internal.execution.NodeExecutionContext;

import com.tlc.iam.common.models.User;

import com.tlc.validator.TlcModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Abstract class for JSON formatter V2.
 *
 * @author Thalaimalai Pandiyan T
 * @version 2.0
 */
abstract class AbstractJsonFormatterV2 {

    protected final FieldService fieldService;
    protected final FieldTypeService fieldTypeService;
    protected final WorkflowFieldService workflowFieldService;
    protected final BaseCurrencyService baseCurrencyService;
    protected final SourceTagManager sourceTagManager;

    private final StepOptionRetriever stepOptionRetriever;

    protected AbstractJsonFormatterV2() {
        this.fieldService = FieldService.getInstance();
        this.fieldTypeService = FieldTypeService.getInstance();
        this.workflowFieldService = WorkflowFieldService.getInstance();
        this.stepOptionRetriever = StepOptionRetriever.getInstance();
        this.baseCurrencyService = BaseCurrencyService.getInstance();
        this.sourceTagManager = SourceTagManager.getInstance();
    }

    /**
     * Loads the persistent field details into the {@link JsonObject}.
     *
     * @param orgId      Refers the organization id.
     * @param fieldInfo  Refers the {@link FieldInfo}.
     * @param valueArray Refers the {@link JsonArray} which holds the field mapping data.
     * @param jsonObject Refers the {@link JsonObject} to which the field data is loaded.
     */
    protected final void loadPersistentFieldValue(final Long orgId, final FieldInfo fieldInfo,
                                                  final JsonArray valueArray, final JsonObject jsonObject) {
        final StoreType storeType = fieldInfo.storeType();

        switch (fieldInfo.storeType()) {
            case SINGLE_NUMBER, DATE, TIME, CONTACT, COMPANY, OWNER, DROPDOWN -> {
                jsonObject.put(fieldInfo.idAsString(), NumberUtils.parseNumber(valueArray.getJsonObject(0).get(ActionAttributes.VALUE)));
            }
            case SINGLE_STRING, TEXT ->
                    jsonObject.put(fieldInfo.idAsString(), valueArray.getJsonObject(0).get(ActionAttributes.VALUE));
            case EMAIL -> {
                final JsonObject subFieldJson = Json.object();
                final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(storeType);

                subFieldJson.put(fieldSubTypes.get(StoreSubType.EMAIL_VALUE).idAsString(), valueArray.getJsonObject(0).get(ActionAttributes.VALUE));
                subFieldJson.put(fieldSubTypes.get(StoreSubType.EMAIL_TYPE).idAsString(), Email.Type.WORK.getId());

                jsonObject.put(fieldInfo.idAsString(), subFieldJson);
            }
            case PHONE -> {
                final JsonObject subFieldJson = Json.object();
                final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(storeType);

                subFieldJson.put(fieldSubTypes.get(StoreSubType.PHONE_NUMBER).idAsString(), valueArray.getJsonObject(0).get(ActionAttributes.VALUE));
                subFieldJson.put(fieldSubTypes.get(StoreSubType.PHONE_TYPE).idAsString(), Phone.Type.WORK.getId());

                jsonObject.put(fieldInfo.idAsString(), subFieldJson);
            }
            case MONETARY -> {
                final JsonObject subFieldJson = Json.object();
                final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(storeType);

                subFieldJson.put(fieldSubTypes.get(StoreSubType.CURRENCY).idAsString(), baseCurrencyService.getDefaultCurrency(orgId).getId());
                subFieldJson.put(fieldSubTypes.get(StoreSubType.VALUE).idAsString(), valueArray.getJsonObject(0).get(ActionAttributes.VALUE));

                jsonObject.put(fieldInfo.idAsString(), subFieldJson);
            }
            case ADDRESS -> {
                final JsonObject subFieldJson = Json.object();
                final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(storeType);

                subFieldJson.put(fieldSubTypes.get(StoreSubType.ADDRESS_LINE1).idAsString(), valueArray.getJsonObject(0).get(ActionAttributes.VALUE));
                jsonObject.put(fieldInfo.idAsString(), subFieldJson);
            }
            case MULTI_SELECT -> {
                final Collection<Long> optionIds = new ArrayList<>();

                valueArray.forEach(json -> optionIds.add(NumberUtils.parseNumber(((Map<String, ?>) json).get(ActionAttributes.VALUE)).longValue()));
                jsonObject.put(fieldInfo.idAsString(), optionIds);
            }
        }
    }

    /**
     * Gets the step option value from the {@link JsonObject} provided.
     *
     * @param valueJson            Refers the {@link JsonObject} from which the intricate details of step options are fetched.
     * @param source               Refers the {@link TriggerActionSource} for which the step option are retrieved.
     * @param nodeExecutionContext Refers the {@link NodeExecutionContext} object.
     * @return The step option value.
     */
    protected final Object getStepOptionValue(final JsonObject valueJson, final TriggerActionSource source, final NodeExecutionContext nodeExecutionContext) {
        final JsonObject dependsOnJson = valueJson.getJsonObject(ActionAttributes.DEPENDS_ON);
        final Long stepOptionId = valueJson.getLong(ActionAttributes.VALUE);
        final Map<Long, User> groupedUsers = nodeExecutionContext.groupedUsers();

        if (ActionAttributes.TRIGGER.equalsIgnoreCase(dependsOnJson.getString(ActionAttributes.TYPE))) {
            final Pair<TriggerSource, TlcModel> triggerModel = nodeExecutionContext.triggerSourceModel();

            return stepOptionRetriever.getValue(new StepOptionInfoV2(triggerModel.getValue(), stepOptionId, groupedUsers, Source.get(triggerModel.getKey().getName())), source);
        } else {
            final short nodeIdKey = dependsOnJson.getShort(WorkflowAttributesV2.NODE);
            final Pair<String, TlcModel> actionModel = nodeExecutionContext.actionModelPair(nodeExecutionContext.nodeKey(nodeIdKey));

            return stepOptionRetriever.getValue(new StepOptionInfoV2(actionModel.getValue(), stepOptionId, groupedUsers, Source.get(actionModel.getKey())), source);
        }
    }

    /**
     * Converts the fields mapping json into entity {@link JsonObject}.
     *
     * @param orgId                Refers the organization id.
     * @param workflowActionId     Refers the workflow action id
     * @param fieldMappingJson     Refers the {@link JsonArray} which holds information about the data pertaining to actions.
     * @param nodeExecutionContext Refers the {@link NodeExecutionContext}.
     * @param actionSource         Refers the {@code TriggerActionSource} for which the step options are retrieved.
     * @return The formatted {@link JsonObject}.
     */
    protected JsonObject format(final Long orgId, final Long workflowActionId, final JsonArray fieldMappingJson,
                                final NodeExecutionContext nodeExecutionContext, final TriggerActionSource actionSource) {
        return format(orgId, workflowActionId, fieldMappingJson, nodeExecutionContext, actionSource, ActionFailureCauseTracker.create());
    }

    /**
     * Converts the fields mapping json into entity {@link JsonObject}.
     *
     * @param orgId                     Refers the organization id.
     * @param workflowActionId          Refers the workflow action id
     * @param fieldMappingJson          Refers the {@link JsonArray} which holds information about the data pertaining to actions.
     * @param nodeExecutionContext      Refers the {@link NodeExecutionContext}.
     * @param actionSource              Refers the {@code TriggerActionSource} for which the step options are retrieved.
     * @param actionFailureCauseTracker Refers the {@code ActionFailureCauseTracker}.
     * @return The formatted {@link JsonObject}.
     */
    protected JsonObject format(final Long orgId, final Long workflowActionId, final JsonArray fieldMappingJson,
                                final NodeExecutionContext nodeExecutionContext, final TriggerActionSource actionSource,
                                final ActionFailureCauseTracker actionFailureCauseTracker) {
        return Json.object();
    }
}
