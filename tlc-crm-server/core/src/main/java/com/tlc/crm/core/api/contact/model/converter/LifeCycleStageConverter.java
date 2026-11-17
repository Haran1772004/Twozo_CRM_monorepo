package com.tlc.crm.core.api.contact.model.converter;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.converter.OptionJsonConverter;

import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.field.contact.LifeCycleStageFields;
import com.tlc.crm.core.internal.contact.status.ContactErrorCodes;

import com.tlc.crm.entity.builder.EntityConverter;

import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.I18nService;
import com.tlc.i18n.internal.registry.I18nServiceRegistry;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * Provides different ways to convert the data from / into {@link LifeCycleStage}
 *
 * @author Selvakumar
 * @version 1.0
 */
public final class LifeCycleStageConverter extends EntityConverter<LifeCycleStage> implements OptionJsonConverter<LifeCycleStage> {

    private static final byte LOST_STATUS_INDEX_DESC = 1;
    private static final byte WON_STATUS_INDEX_DESC = 2;

    private final LifeCycleStatusConverter statusConverter;
    private final LifeCycleStageManager lifeCycleStageManager;
    private final I18nService i18nService;

    private static class Instance {
        private static final LifeCycleStageConverter INSTANCE = new LifeCycleStageConverter();
    }

    private LifeCycleStageConverter() {
        statusConverter = LifeCycleStatusConverter.getInstance();
        lifeCycleStageManager = LifeCycleStageManager.getInstance();
        i18nService = I18nServiceRegistry.getInstance().get();
    }

    public static LifeCycleStageConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Converts {@link JsonObject} into {@link LifeCycleStage}
     * </p>
     *
     * @param converterConfig Converter Configuration
     * @param jsonObject      Holds the lifeCycleStage attributes
     * @return {@link LifeCycleStage}
     */
    @Override
    public LifeCycleStage convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        if (converterConfig.isPartialData()) {
            return buildLifecycleStageByPartialData(converterConfig, jsonObject);
        } else {
            return buildLifecycleStage(converterConfig, jsonObject);
        }
    }

    /**
     * <p>
     * Converts {@link LifeCycleStage} into {@link JsonObject}
     * </p>
     *
     * @param lifeCycleStage A model which represents the stage and holds its attribute values
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convert(final LifeCycleStage lifeCycleStage) {
        final JsonObject stageJson = Json.object();
        final Collection<LifeCycleStatus> statuses = lifeCycleStage.getStatuses();
        final boolean isNotLastStage = !lifeCycleStage.isLastStage();
        final boolean isSystemStage = lifeCycleStage.isSystemStage();
        final boolean isActive = lifeCycleStage.isActive();

        stageJson.put(LifeCycleStageFields.ID.getName(), lifeCycleStage.id());
        stageJson.put(LifeCycleStageFields.NAME.getName(), lifeCycleStage.getName());
        stageJson.put(LifeCycleStageFields.IS_SYSTEM_STAGE.getName(), isSystemStage);
        stageJson.put(LifeCycleStageFields.IS_ACTIVE.getName(), isActive);

        if (isSystemStage && isNotLastStage) {
            stageJson.put(LifeCycleStageFields.IS_DEACTIVATABLE.getName(), true);
        }

        if (isActive) {
            stageJson.put(LifeCycleStageFields.IS_MOVABLE.getName(), isNotLastStage);
            stageJson.put(LifeCycleStageFields.IS_DELETABLE.getName(), !isSystemStage);
        }

        if (Objects.nonNull(statuses)) {
            final JsonArray statusesJson = Json.array();

            for (final LifeCycleStatus status : statuses) {
                final JsonObject statusJson = statusConverter.convert(status);
                final boolean isLabelNull = Objects.isNull(status.getLabel());

                statusJson.put(LifeCycleStageFields.IS_MOVABLE.getName(), isLabelNull);
                statusJson.put(LifeCycleStageFields.IS_DELETABLE.getName(), isNotLastStage || isLabelNull);
                statusesJson.put(statusJson);
            }
            stageJson.put(LifeCycleStageFields.STATUSES.getName(), statusesJson);
        }
        return stageJson;
    }

    /**
     * <p>
     * Converts {@link JsonObject} into {@link LifeCycleStage}
     * </p>
     *
     * @param converterConfig Converter Configuration
     * @param jsonObject      Holds the lifeCycleStage attributes
     * @return {@link LifeCycleStage}
     */
    private LifeCycleStage buildLifecycleStage(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final LifeCycleStage lifeCycleStage = new LifeCycleStage();
        final String idKey = LifeCycleStageFields.ID.getName();

        if (jsonObject.containsKey(idKey)) {
            lifeCycleStage.setId(jsonObject.getLong(idKey));
        }
        setJsonDataToModel(converterConfig, lifeCycleStage, jsonObject);
        return lifeCycleStage;
    }

    /**
     * Converts partial data into {@link LifeCycleStage}
     *
     * @param converterConfig Converter Configuration
     * @param jsonObject      Holds the lifeCycleStage attributes
     * @return {@link LifeCycleStage}
     */
    private LifeCycleStage buildLifecycleStageByPartialData(final ConverterConfig converterConfig,
                                                            final JsonObject jsonObject) {
        final LifeCycleStage lifeCycleStage = lifeCycleStageManager.get(converterConfig.orgId(),
                jsonObject.getLong(LifeCycleStageFields.ID.getName()));

        setJsonDataToModel(converterConfig, lifeCycleStage, jsonObject);
        return lifeCycleStage;
    }

    /**
     * Sets JSON data into {@link LifeCycleStage}
     *
     * @param converterConfig Converter Configuration
     * @param lifeCycleStage  A model to which the lifeCycleStage attributes will be set
     * @param jsonObject      Holds the field values of contact source
     */
    private void setJsonDataToModel(final ConverterConfig converterConfig, final LifeCycleStage lifeCycleStage,
                                    final JsonObject jsonObject) {
        final String nameKey = LifeCycleStageFields.NAME.getName();
        final String statusKey = LifeCycleStageFields.STATUSES.getName();

        if (jsonObject.containsKey(nameKey)) {
            lifeCycleStage.setName(jsonObject.getString(nameKey));
        }

        if (jsonObject.containsKey(statusKey)) {
            final JsonArray statusJson = jsonObject.getJsonArray(statusKey);
            final HashSet<LifeCycleStatus.Label> labels = new HashSet<>();
            final int size = statusJson.size();

            for (int index = 0; index < size; index++) {
                final LifeCycleStatus lifeCycleStatus = statusConverter.convert(converterConfig, statusJson.getJsonObject(index));
                final LifeCycleStatus.Label label = lifeCycleStatus.getLabel();

                if (Objects.isNull(label)) {
                    lifeCycleStatus.setOrder((short) (index + 1));
                } else if (index != size - (LifeCycleStatus.Label.CLOSED_WON == label ? WON_STATUS_INDEX_DESC : LOST_STATUS_INDEX_DESC)) {
                    throw ErrorCode.get(ContactErrorCodes.INVALID_LIFECYCLE_STATUS_ORDER, "i18n.lifecycle.stage.invalid.order");
                } else if (!labels.add(label)) {
                    final I18nResolver i18nResolver = i18nService.getResolver(converterConfig.locale());

                    throw ErrorCode.get(ContactErrorCodes.LABELED_STATUS_ALREADY_EXISTS, String.format(i18nResolver.get("i18n.lifecycle.labelled.status.already.exists"), label));
                }
                lifeCycleStage.addStatus(lifeCycleStatus);
            }
        }
        lifeCycleStage.setOrgId(converterConfig.orgId());
    }

    /**
     * Converts {@link LifeCycleStage} into {@link JsonObject}
     *
     * @param lifeCycleStage  The lifeCycleStage entity with the attributes of lifeCycleStage details.
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertModelToOptionJson(final LifeCycleStage lifeCycleStage) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(LifeCycleStageFields.VALUE.getName(), lifeCycleStage.id());
        jsonObject.put(LifeCycleStageFields.NAME.getName(), lifeCycleStage.getName());

        return jsonObject;
    }
}
