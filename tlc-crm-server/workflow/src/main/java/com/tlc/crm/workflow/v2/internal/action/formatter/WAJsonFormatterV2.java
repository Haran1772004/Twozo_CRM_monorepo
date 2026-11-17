package com.tlc.crm.workflow.v2.internal.action.formatter;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.template.PlaceHolderValueInterpreter;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.models.type.Phone;

import com.tlc.crm.fields.models.StoreType;

import com.tlc.crm.messenger.fields.MessengerFields;

import com.tlc.crm.security.iam.LoggedInUserInfo;

import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.api.model.WorkflowField;
import com.tlc.crm.workflow.api.model.history.WorkflowActionExecutionFailureCause;
import com.tlc.crm.workflow.field.WorkflowAttributes;
import com.tlc.crm.workflow.internal.status.WorkflowErrorCodes;
import com.tlc.crm.workflow.internal.trigger.action.tracker.ActionFailureCauseTracker;
import com.tlc.crm.workflow.v2.field.WorkflowAttributesV2;
import com.tlc.crm.workflow.v2.internal.execution.NodeExecutionContext;

import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.I18nService;
import com.tlc.i18n.service.provider.I18nServiceProvider;

import com.tlc.social.common.model.ChatPlaceHolder;
import com.tlc.social.common.model.Platform;

import com.tlc.validator.TlcModel;

import java.lang.reflect.Field;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Copied from {@code WAJsonFormatter}.
 */
public final class WAJsonFormatterV2 extends AbstractJsonFormatterV2 {

    private static final String I18N_PREFIX = "i18n.";

    private final PlaceHolderValueInterpreter placeHolderValueInterpreter;
    private final UserService userService;
    private final I18nService i18nService;
    private final ContactManager contactManager;

    private WAJsonFormatterV2() {
        placeHolderValueInterpreter = PlaceHolderValueInterpreter.getInstance();
        userService = UserService.getInstance();
        i18nService = I18nServiceProvider.get();
        contactManager = ContactManager.getInstance();
    }

    /**
     * Holds the instance of {@link WAJsonFormatterV2}.
     */
    private static final class InstanceHolder {
        private static final WAJsonFormatterV2 WA_JSON_FORMATTER = new WAJsonFormatterV2();
    }

    /**
     * <p>
     * Gets the instance of {@link WAJsonFormatterV2}.
     * </p>
     *
     * @return the {@link WAJsonFormatterV2} instance.
     */
    public static WAJsonFormatterV2 getInstance() {
        return InstanceHolder.WA_JSON_FORMATTER;
    }

    /**
     * Formats the WhatsApp action field mapping JSON for V2 execution.
     *
     * @param orgId                     Represents the organization id.
     * @param workflowActionId          Represents the workflow action id.
     * @param fieldMappingJson          Represents the {@link JsonArray} which holds information about the data pertaining to actions.
     * @param nodeExecutionContext      Represents the {@link NodeExecutionContext}.
     * @param actionSource              Represents the {@code TriggerActionSource} for which the step options are retrieved.
     * @param actionFailureCauseTracker Represents the {@link ActionFailureCauseTracker} to be used for tracking the error cause.
     * @return The formatted {@link JsonObject}.
     */
    @Override
    public JsonObject format(final Long orgId, final Long workflowActionId, final JsonArray fieldMappingJson,
                             final NodeExecutionContext nodeExecutionContext, final TriggerActionSource actionSource,
                             final ActionFailureCauseTracker actionFailureCauseTracker) {
        final Pair<TriggerSource, TlcModel> triggerModel = nodeExecutionContext.triggerSourceModel();
        final int fieldMappingSize = fieldMappingJson.size();
        final Collection<Integer> phoneTypeIds = new HashSet<>();
        final JsonObject templateJson = Json.object();
        final TriggerSource triggerSource = triggerModel.getKey();
        final Map<Long, FieldInfo> fieldMap = fieldService.getFieldsBySource(orgId, Source.get(triggerSource.getName()).getName()).stream().collect(Collectors.toMap(FieldInfo::id, Function.identity()));
        final Set<String> phoneNumbers = new HashSet<>();
        final UserRegionalPreference regionalPreference = ((CrmUser) LoggedInUserInfo.getUser()).getPreferenceHolder().get(UserRegionalPreference.TYPE);
        final TimeZone timeZone = regionalPreference.getTimezoneInfo().getTimeZone();
        final I18nResolver i18nResolver = i18nService.getResolver(Locale.forLanguageTag(regionalPreference.getLanguage().getName()));
        final Entity entity = (Entity) triggerModel.getValue();
        final Map<String, Field> entityFields = placeHolderValueInterpreter.loadEntityFields(entity.getClass());

        templateJson.put(MessengerFields.PLATFORM, Platform.WHATSAPP.getId());

        for (int index = 0; index < fieldMappingSize; index++) {
            final JsonObject fieldMapping = fieldMappingJson.getJsonObject(index);
            final WorkflowField workflowField = workflowFieldService.get(fieldMapping.getLong(WorkflowAttributes.FIELD_ID));

            switch (workflowField.getAliasName()) {
                case WorkflowAttributes.RECIPIENTS -> {
                    final JsonArray valueArray = fieldMapping.getJsonArray(WorkflowAttributes.VALUES);
                    final int valueArraySize = valueArray.size();

                    for (int valueIndex = 0; valueIndex < valueArraySize; valueIndex++) {
                        final JsonObject valueObject = valueArray.getJsonObject(valueIndex);

                        phoneTypeIds.add(valueObject.getInt(WorkflowAttributes.VALUE));
                    }
                }
                case WorkflowAttributes.TEMPLATE -> {
                    final JsonObject valueJson = fieldMapping.getJsonArray(WorkflowAttributes.VALUES)
                            .getJsonObject(NumericalConstant.ZERO).getJsonObject(WorkflowAttributes.VALUE);

                    templateJson.put(WorkflowAttributes.TEMPLATE_ID, valueJson.getLong(WorkflowAttributes.TEMPLATE_ID));

                    if (valueJson.containsKey(MessengerFields.PARAM_DATA)) {
                        final JsonObject paramData = valueJson.getJsonObject(MessengerFields.PARAM_DATA);

                        if (paramData.containsKey(MessengerFields.BODY)) {
                            final JsonObject bodyParam = paramData.getJsonObject(MessengerFields.BODY);
                            final Collection<Long> iamUserIds = fetchUserIds(workflowActionId, bodyParam, fieldMap, entity, timeZone, entityFields, actionFailureCauseTracker);
                            final Map<Long, CrmUser> userMap = iamUserIds.isEmpty() ? Collections.emptyMap() : userService.getUsersByIamId(orgId, iamUserIds)
                                    .stream().collect(Collectors.toMap(CrmUser::getIamId, Function.identity()));

                            resolvePlaceHolderValue(workflowActionId, bodyParam, fieldMap, entity, timeZone, i18nResolver, userMap, entityFields, actionFailureCauseTracker);
                        }

                        if (paramData.containsKey(MessengerFields.HEADER)) {
                            final JsonObject headerParam = paramData.getJsonObject(MessengerFields.HEADER);

                            if (headerParam.optBoolean(MessengerFields.HAS_PLACEHOLDER)) {
                                headerParam.remove(MessengerFields.HAS_PLACEHOLDER);
                                final Collection<Long> userIds = fetchUserIds(workflowActionId, headerParam, fieldMap, entity, timeZone, entityFields, actionFailureCauseTracker);
                                final Map<Long, CrmUser> userMap = userIds.isEmpty() ? Collections.emptyMap() : userService.getUsers(orgId, userIds).stream().collect(Collectors.toMap(CrmUser::id, Function.identity()));

                                resolvePlaceHolderValue(workflowActionId, headerParam, fieldMap, entity, timeZone, i18nResolver, userMap, entityFields, actionFailureCauseTracker);
                            }
                        }

                        templateJson.put(MessengerFields.PARAM_DATA, paramData);
                    }
                }

                case WorkflowAttributesV2.SENDER_WHATSAPP_NO -> {
                    final JsonArray valueArray = fieldMapping.getJsonArray(WorkflowAttributes.VALUES);

                    templateJson.put(MessengerFields.BUSINESS_PHONE_NUMBER_ID, valueArray.getJsonObject(NumericalConstant.ZERO).getLong(WorkflowAttributes.VALUE));
                }
            }
        }
        final Contact contact;

        if (Objects.equals(triggerSource, TriggerSource.CONTACT)) {
            contact = (Contact) triggerModel.getValue();
        } else if (Objects.equals(triggerSource, TriggerSource.DEAL)) {
            contact = ((Deal) triggerModel.getValue()).getPrimaryContact();
        } else {
            contact = null;
        }

        if (Objects.nonNull(contact)) {
            final Collection<Phone> phones = contact.getPhones();
            final Collection<Phone> existingPhones = Objects.nonNull(phones) ? phones : contactManager.getPhones(orgId, List.of(contact.id()));

            for (final Phone phone : existingPhones) {
                final int phoneTypeId = phone.getType().getId();

                if (phoneTypeIds.contains(phoneTypeId)) {
                    phoneNumbers.add(phone.getNumber());
                    phoneTypeIds.remove(phoneTypeId);
                }
            }

            if (!phoneTypeIds.isEmpty()) {
                final Collection<FieldInfo> customPhoneFields = fieldService.getPersistentFieldsBySource(orgId, Contact.ENTITY_TYPE)
                        .stream().filter(fieldInfo -> Objects.equals(StoreType.PHONE, fieldInfo.storeType())).toList();

                for (final FieldInfo fieldInfo : customPhoneFields) {
                    final ValueHolder valueHolder = contact.getValueHolder();

                    if (Objects.nonNull(valueHolder)) {
                        final Phone customPhone = (Phone) valueHolder.get(fieldInfo.id());

                        if (Objects.nonNull(customPhone)) {
                            final int phoneTypeId = customPhone.getType().getId();

                            if (phoneTypeIds.contains(phoneTypeId)) {
                                phoneNumbers.add(customPhone.getNumber());
                                phoneTypeIds.remove(phoneTypeId);
                            }

                            if (phoneTypeIds.isEmpty()) {
                                break;
                            }
                        }
                    }
                }
            }
        }
        templateJson.put(MessengerFields.RECIPIENT_PHONE_NUMBERS, phoneNumbers);

        return templateJson;
    }

    /**
     * Resolves the placeholder values in the given {@link JsonObject} using the provided field mapping and entity details.
     *
     * @param workflowActionId Represents the workflow action id.
     * @param paramJson        Represents the {@link JsonObject} which contains the placeholder keys and values to be resolved.
     * @param fieldMap         Represents the mapping of field ids to their corresponding {@link FieldInfo}.
     * @param entity           Represents the {@link Entity} for which the placeholders are being resolved.
     * @param timeZone         Represents the {@link TimeZone} to be considered while resolving date/time placeholders.
     * @param i18nResolver     Represents the {@link I18nResolver} to be used for resolving i18n placeholders.
     * @param userMap          Represents the mapping of user ids to their corresponding {@link CrmUser} details, which can be used for resolving owner type placeholders.
     * @param entityFields     Represents the mapping of field names to their corresponding {@link Field} details for the entity, which can be used for resolving placeholders.
     * @param errorTracker     Represents the {@link ActionFailureCauseTracker} to be used for tracking the error cause.
     */
    private void resolvePlaceHolderValue(final Long workflowActionId, final JsonObject paramJson, final Map<Long, FieldInfo> fieldMap, final Entity entity,
                                         final TimeZone timeZone, final I18nResolver i18nResolver, final Map<Long, CrmUser> userMap, final Map<String, Field> entityFields, final ActionFailureCauseTracker errorTracker) {
        final Long orgId = entity.orgId();

        for (Map.Entry<String, Object> entry : paramJson.toMap().entrySet()) {
            final Object value = entry.getValue();
            final FieldInfo fieldInfo = fieldMap.get(NumberUtils.parseNumber(value).longValue());

            if (Objects.nonNull(fieldInfo)) {
                final ChatPlaceHolder placeHolder = new ChatPlaceHolder();

                placeHolder.setKey(value.toString());
                placeHolderValueInterpreter.resolve(placeHolder, fieldInfo, entity, timeZone, entityFields);
                final Object placeholderValue = placeHolder.getValue();

                if (Objects.isNull(placeholderValue)) {
                    errorTracker.track(workflowActionId, WorkflowActionExecutionFailureCause.FAILED_TO_RESOLVE_CONTACT_PLACEHOLDER);

                    throw ErrorCode.get(WorkflowErrorCodes.FAILED_TO_RESOLVE_WHATSAPP_PLACEHOLDER);
                }
                final Object resolvedValue;

                if (fieldInfo.storeType().isOwnerType()) {
                    final Long iamUserId = NumberUtils.parseNumber(placeholderValue).longValue();
                    final CrmUser crmUser = userMap.get(iamUserId);

                    resolvedValue = Objects.nonNull(crmUser) ? crmUser.getFullName() : userService.getUserByIamId(orgId, iamUserId).get().getFullName();
                } else if (placeholderValue instanceof final String valueInString && valueInString.startsWith(I18N_PREFIX)) {
                    resolvedValue = i18nResolver.get(valueInString);
                } else {
                    resolvedValue = placeholderValue;
                }
                paramJson.put(entry.getKey(), resolvedValue);
            }
        }
    }

    /**
     * <p>
     * Fetches the user ids from the given {@link JsonObject} using the provided field mapping and entity details.
     * </p>
     *
     * @param paramJson    Represents the {@link JsonObject} which contains the placeholder keys and values to be resolved for fetching user ids.
     * @param fieldMap     Represents the mapping of field ids to their corresponding {@link FieldInfo}.
     * @param entity       Represents the {@link Entity} for which the user ids are being fetched.
     * @param timeZone     Represents the {@link TimeZone} to be considered while resolving date/time placeholders if required for fetching user ids.
     * @param entityFields Represents the mapping of field names to their corresponding {@link Field} details for the entity, which can be used for resolving placeholders if required for fetching user ids.
     * @return The collection of user ids fetched based on the provided parameters.
     */
    private Collection<Long> fetchUserIds(final Long workflowActionId, final JsonObject paramJson, final Map<Long, FieldInfo> fieldMap,
                                          final Entity entity, final TimeZone timeZone, final Map<String, Field> entityFields, final ActionFailureCauseTracker errorTracker) {
        final Collection<Long> userIds = new HashSet<>();

        for (Map.Entry<String, Object> entry : paramJson.toMap().entrySet()) {
            final Object value = entry.getValue();
            final FieldInfo fieldInfo = fieldMap.get(NumberUtils.parseNumber(value).longValue());

            if (Objects.nonNull(fieldInfo) && fieldInfo.storeType().isOwnerType()) {
                final ChatPlaceHolder placeHolder = new ChatPlaceHolder();

                placeHolder.setKey(value.toString());
                placeHolderValueInterpreter.resolve(placeHolder, fieldInfo, entity, timeZone, entityFields);
                final Object placeholderValue = placeHolder.getValue();

                if (Objects.isNull(placeholderValue)) {
                    errorTracker.track(workflowActionId, WorkflowActionExecutionFailureCause.FAILED_TO_RESOLVE_CONTACT_PLACEHOLDER);

                    throw ErrorCode.get(WorkflowErrorCodes.FAILED_TO_RESOLVE_WHATSAPP_PLACEHOLDER);
                }
                userIds.add(NumberUtils.parseNumber(placeholderValue).longValue());
            }
        }

        return userIds;
    }
}
