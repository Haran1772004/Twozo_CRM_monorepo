package com.tlc.crm.zapier.action.web;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.type.Option;

import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;

import com.tlc.crm.zapier.api.models.DropdownInfo;
import com.tlc.crm.zapier.api.models.ZapAction;
import com.tlc.crm.zapier.api.models.ZapActionField;
import com.tlc.crm.zapier.api.models.ZapActionRequestContext;
import com.tlc.crm.zapier.fields.ZapAttributes;
import com.tlc.crm.zapier.internal.action.dispatcher.DealZapActionDispatcher;
import com.tlc.crm.zapier.internal.action.dispatcher.FollowerZapActionDispatcher;
import com.tlc.crm.zapier.internal.action.dispatcher.NoteZapActionDispatcher;
import com.tlc.crm.zapier.internal.action.dispatcher.SourceZapActionDispatcher;
import com.tlc.crm.zapier.internal.action.dispatcher.TagZapActionDispatcher;
import com.tlc.crm.zapier.internal.service.ZapierActionFieldService;
import com.tlc.crm.zapier.action.web.secure.SecureZapierAction;
import com.tlc.crm.zapier.internal.status.ZapierErrorCodes;

import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles the management-specific Zapier actions within the Zapier integration module.
 * This class serves as a base for any custom logic tied to managing Zap actions
 * beyond basic execution, such as validation, context mapping, or access control.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@WebAction(path = "/zap/action/mgmt")
public class ZapActionManagement extends SecureZapierAction {

    private final ZapierActionFieldService actionFieldService;
    private final FollowerZapActionDispatcher followerZapActionDispatcher;
    private final TagZapActionDispatcher tagZapActionDispatcher;
    private final NoteZapActionDispatcher noteZapActionDispatcher;
    private final DealZapActionDispatcher dealZapActionDispatcher;
    private final FieldTypeService fieldTypeService;

    public ZapActionManagement() {
        this.actionFieldService = ZapierActionFieldService.getInstance();
        this.followerZapActionDispatcher = FollowerZapActionDispatcher.getInstance();
        this.tagZapActionDispatcher = TagZapActionDispatcher.getInstance();
        this.noteZapActionDispatcher = NoteZapActionDispatcher.getInstance();
        this.dealZapActionDispatcher = DealZapActionDispatcher.getInstance();
        this.fieldTypeService = FieldTypeService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The request containing necessary data for processing.
     * @param response The response object where the processed result will be stored.
     */
    @Override
    protected final void zapProcess(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(ZapAttributes.TYPE);

        switch (type) {
            case "fields" -> processFields(request, requestJson, response);
            case "create", "update" -> upsertEntity(request, requestJson, response);
            case "addProductToDeal" -> addProductToDeal(new ZapActionRequestContext(request, response));
            case "mapTag" -> mapTag(new ZapActionRequestContext(request, response));
            case "mapFollower" -> mapFollower(new ZapActionRequestContext(request, response));
            case "mapNote" -> mapNote(new ZapActionRequestContext(request, response));
        }
    }

    /**
     * Creates or updates an entity based on the incoming request data.
     *
     * @param request  The CRM request containing organizational and user context.
     * @param data     The JSON object holding the entity data to be processed.
     * @param response The response object used to send back results or errors.
     */
    private void upsertEntity(final CrmRequest request, final JsonObject data, final CrmResponse response) {
        final Optional<Source> optionalSource = ZapAction.getSource(ZapAction.get(data.getShort(ZapAttributes.EVENT)));

        optionalSource.ifPresentOrElse(source -> SourceZapActionDispatcher.getInstance(source).dispatch(new ZapActionRequestContext(request, response)),
                () -> {
                    response.setErrorCode(ErrorCode.get(ZapierErrorCodes.INVALID_ZAP_ACTION));
                    response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.action.invalid"));
                    response.send();
                });
    }

    /**
     * Handles the dispatching of the add product to deal action.
     * Delegates the request to the appropriate DealZapActionDispatcher.
     *
     * @param zapActionRequestContext The context containing request and response data.
     */
    private void addProductToDeal(final ZapActionRequestContext zapActionRequestContext) {
        dealZapActionDispatcher.dispatch(zapActionRequestContext);
    }

    /**
     * Handles the dispatching of the follower mapping action.
     * Delegates the request to the FollowerZapActionDispatcher.
     *
     * @param actionRequestContext The context containing request and response data.
     */
    private void mapFollower(final ZapActionRequestContext actionRequestContext) {
        followerZapActionDispatcher.dispatch(actionRequestContext);
    }

    /**
     * Handles the dispatching of the note mapping action.
     * Delegates the request to the NoteZapActionDispatcher.
     *
     * @param actionRequestContext The context containing request and response data.
     */
    private void mapNote(final ZapActionRequestContext actionRequestContext) {
        noteZapActionDispatcher.dispatch(actionRequestContext);
    }

    /**
     * Handles the dispatching of the tag mapping action.
     * Delegates the request to the TagZapActionDispatcher.
     *
     * @param actionRequestContext The context containing request and response data.
     */
    private void mapTag(final ZapActionRequestContext actionRequestContext) {
        tagZapActionDispatcher.dispatch(actionRequestContext);
    }

    /**
     * Processes and enriches the given request's field-related data.
     *
     * @param request  The incoming CRM request containing context and metadata.
     * @param data     The JSON payload containing field data to process.
     * @param response The CRM response object to be populated with results or errors.
     */
    private void processFields(final CrmRequest request, final JsonObject data, final CrmResponse response) {
        final Collection<ZapActionField> fields = actionFieldService.getFields(request.orgId(), request.i18nResolver(), ZapAction.get(data.getShort(ZapAttributes.ID)));
        final JsonArray array = Json.array();

        for (final ZapActionField zapActionField : fields) {
            final DropdownInfo dropdownInfo = zapActionField.getDropdownInfo();
            final JsonObject jsonObject = Json.object();

            jsonObject.put(ZapAttributes.KEY, zapActionField.getAliasName());
            jsonObject.put(ZapAttributes.LABEL, getLabel(zapActionField, response));
            jsonObject.put(ZapAttributes.REQUIRED, zapActionField.getConfig().optBoolean(ZapAttributes.REQUIRED));
            jsonObject.put(ZapAttributes.TYPE, getType(zapActionField.getFieldType().storeType()));

            if (Objects.nonNull(dropdownInfo)) {
                final Collection<Option> choices = dropdownInfo.getChoices();

                if (!choices.isEmpty()) {
                    final JsonArray choiceArray = Json.array();

                    for (final Option choice : choices) {
                        final JsonObject choiceJson = Json.object();

                        choiceJson.put(ZapAttributes.VALUE, choice.id());
                        choiceJson.put(ZapAttributes.LABEL, response.resolveI18nString(choice.getOption()));
                        choiceArray.put(choiceJson);
                    }
                    jsonObject.put(ZapAttributes.CHOICES, choiceArray);
                }
                jsonObject.put(ZapAttributes.DYNAMIC, dropdownInfo.getName());
                jsonObject.put(ZapAttributes.LIST, dropdownInfo.isMultiSelect() ? true : null);
            }
            array.put(jsonObject);
        }

        response.put(ZapAttributes.LIST, array);
        response.send();
    }

    /**
     * Retrieves the label for a given Zapier action field.
     *
     * @param zapActionField The Zapier action field for which the label is required.
     * @param response       The CRM response containing metadata or field value mappings.
     * @return The resolved label as a {@link String}.
     */
    private String getLabel(final ZapActionField zapActionField, final CrmResponse response) {
        final StoreType storeType = zapActionField.getFieldType().storeType();

        if (storeType.isRangeType()) {
            final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(storeType);
            final String aliasName = zapActionField.getAliasName();

            for (final Map.Entry<StoreSubType, FieldSubType> subtype : fieldSubTypes.entrySet()) {
                final String subtypeName = subtype.getValue().name();

                if (aliasName.contains(subtypeName)) {
                    return String.join(" - ", response.resolveI18nString(zapActionField.getName()), subtypeName);
                }
            }
        }

        return response.resolveI18nPlaceholder(zapActionField.getName());
    }

    /**
     * Maps the given {@link StoreType} to its corresponding type string.
     *
     * @param storeType The store type to map.
     * @return The string representing the type, or a default/fallback if unknown.
     */
    private String getType(final StoreType storeType) {
        return switch (storeType) {
            case TEXT -> ZapAttributes.TEXT;
            case SINGLE_NUMBER, PHONES, DEAL_VALUE, MONETARY, PRICE_TAGS -> ZapAttributes.NUMBER;
            case TIME, DATE, DATE_TIME, DATE_RANGE, TIME_RANGE, DATE_TIME_RANGE -> ZapAttributes.DATETIME;
            default -> ZapAttributes.STRING;
        };
    }

}
