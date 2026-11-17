package com.tlc.crm.core.api.contact.model.converter;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.converter.OptionJsonConverter;
import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStatusManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.field.contact.LifeCycleStageFields;
import com.tlc.crm.core.field.contact.LifeCycleStatusFields;
import com.tlc.crm.core.internal.contact.status.ContactErrorCodes;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.I18nService;
import com.tlc.i18n.internal.registry.I18nServiceRegistry;

import java.util.Objects;

/**
 * Provides different ways to convert the data from / into {@link LifeCycleStatus}.
 *
 * @author Sathish
 * @version 1.0
 */
public final class LifeCycleStatusConverter extends EntityConverter<LifeCycleStatus> implements OptionJsonConverter<LifeCycleStatus> {

    private static final byte WON_STATUS_INDEX = 9;
    private static final byte LOST_STATUS_INDEX = 10;

    private final LifeCycleStatusManager lifeCycleStatusManager;
    private final LifeCycleStageManager lifeCycleStageManager;
    private final I18nService i18nService;

    private static class Instance {
        private static final LifeCycleStatusConverter INSTANCE = new LifeCycleStatusConverter();
    }

    private LifeCycleStatusConverter() {
        lifeCycleStatusManager = LifeCycleStatusManager.getInstance();
        lifeCycleStageManager = LifeCycleStageManager.getInstance();
        i18nService = I18nServiceRegistry.getInstance().get();
    }

    public static LifeCycleStatusConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Converts {@link JsonObject} into {@link LifeCycleStatus}.
     * </p>
     *
     * @param converterConfig Converter Configuration.
     * @param jsonObject      Holds the lifeCycleStatus attributes.
     * @return {@link LifeCycleStatus}.
     */
    @Override
    public LifeCycleStatus convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        if (converterConfig.isPartialData()) {
            return buildStatusByPartialData(converterConfig, jsonObject);
        } else {
            return buildLifecycleStatus(converterConfig, jsonObject);
        }
    }

    /**
     * <p>
     * Converts {@link LifeCycleStatus} into {@link JsonObject}.
     * </p>
     *
     * @param lifeCycleStatus A model which represents the status and holds its attribute values.
     * @return {@link JsonObject}.
     */
    @Override
    public JsonObject convert(final LifeCycleStatus lifeCycleStatus) {
        final JsonObject jsonObject = Json.object();
        final LifeCycleStatus.Label label = lifeCycleStatus.getLabel();

        jsonObject.put(LifeCycleStatusFields.ID.getName(), lifeCycleStatus.id());
        jsonObject.put(LifeCycleStatusFields.NAME.getName(), lifeCycleStatus.getName());
        jsonObject.put(LifeCycleStatusFields.IS_SYSTEM_STATUS.getName(), lifeCycleStatus.isSystemStatus());

        if (Objects.nonNull(label)) {
            jsonObject.put(LifeCycleStatusFields.LABEL.getName(), label.getId());
        }
        return jsonObject;
    }

    /**
     * Converts {@link LifeCycleStatus} into {@link JsonObject}
     *
     * @param lifeCycleStatus The lifeCycleStatus entity with the attributes of lifeCycleStatus details.
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertModelToOptionJson(final LifeCycleStatus lifeCycleStatus) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(LifeCycleStageFields.VALUE.getName(), lifeCycleStatus.id());
        jsonObject.put(LifeCycleStageFields.NAME.getName(), lifeCycleStatus.getName());

        return jsonObject;
    }

    /**
     * <p>
     * Converts {@link JsonObject} into {@link LifeCycleStatus}.
     * </p>
     *
     * @param converterConfig Converter Configuration.
     * @param jsonObject      Holds the lifeCycleStatus attributes.
     * @return {@link LifeCycleStatus}.
     */
    private LifeCycleStatus buildLifecycleStatus(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final LifeCycleStatus lifeCycleStatus = new LifeCycleStatus();
        final String idKey = LifeCycleStatusFields.ID.getName();

        if (jsonObject.containsKey(idKey)) {
            lifeCycleStatus.setId(jsonObject.getLong(idKey));
        }
        setJsonDataToModel(converterConfig, lifeCycleStatus, jsonObject);
        return lifeCycleStatus;
    }

    /**
     * Converts partial data into {@link LifeCycleStatus}.
     *
     * @param converterConfig Converter Configuration.
     * @param jsonObject      Holds the lifeCycleStatus attributes.
     * @return {@link LifeCycleStatus}.
     */
    private LifeCycleStatus buildStatusByPartialData(final ConverterConfig converterConfig,
                                                     final JsonObject jsonObject) {
        final LifeCycleStatus lifeCycleStatus = lifeCycleStatusManager.get(converterConfig.orgId(),
                jsonObject.getLong(LifeCycleStatusFields.ID.getName()));

        setJsonDataToModel(converterConfig, lifeCycleStatus, jsonObject);
        return lifeCycleStatus;
    }

    /**
     * Sets JSON data into {@link LifeCycleStatus}.
     *
     * @param converterConfig Converter Configuration.
     * @param lifeCycleStatus A model to which the lifeCycleStatus attributes will be set.
     * @param jsonObject      Holds the field values of contact source.
     */
    private void setJsonDataToModel(final ConverterConfig converterConfig, final LifeCycleStatus lifeCycleStatus,
                                    final JsonObject jsonObject) {
        final String nameKey = LifeCycleStatusFields.NAME.getName();
        final String labelKey = LifeCycleStatusFields.LABEL.getName();
        final String lifeCycleStageIdKey = LifeCycleStatusFields.LIFE_CYCLE_STAGE_ID.getName();
        final boolean isStageIdContains = jsonObject.containsKey(lifeCycleStageIdKey);

        if (jsonObject.containsKey(nameKey)) {
            lifeCycleStatus.setName(jsonObject.getString(nameKey));
        }

        if (isStageIdContains) {
            lifeCycleStatus.setStageId(jsonObject.getLong(lifeCycleStageIdKey));
        }

        if (jsonObject.containsKey(labelKey)) {
            final LifeCycleStatus.Label label = LifeCycleStatus.Label.get(jsonObject.getShort(labelKey));

            if (isStageIdContains) {
                final Long stageId = lifeCycleStatus.getStageId();
                final I18nResolver i18nResolver = i18nService.getResolver(converterConfig.locale());

                if (lifeCycleStageManager.isLabeledStatusExists(converterConfig.orgId(), stageId, label)) {
                    throw ErrorCode.get(ContactErrorCodes.LABELED_STATUS_ALREADY_EXISTS, String.format(i18nResolver.get("i18n.lifecycle.labelled.status.already.exists"), label));
                }
            }
            lifeCycleStatus.setLabel(label);
            lifeCycleStatus.setOrder((short) (LifeCycleStatus.Label.CLOSED_WON == label ? WON_STATUS_INDEX : LOST_STATUS_INDEX));
        }
        lifeCycleStatus.setOrgId(converterConfig.orgId());
    }
}
