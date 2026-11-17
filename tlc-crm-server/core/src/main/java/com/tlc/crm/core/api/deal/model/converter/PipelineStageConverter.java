package com.tlc.crm.core.api.deal.model.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.TypeChecker;
import com.tlc.crm.common.converter.OptionJsonConverter;
import com.tlc.crm.core.api.deal.manager.PipelineStageManager;
import com.tlc.crm.core.api.deal.model.Deal.State;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.field.deal.PipelineFields;
import com.tlc.crm.core.field.deal.PipelineStageFields;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.common.converter.ConverterConfig;

import java.util.Objects;

/**
 * Provides different ways to convert the data from / into {@link PipelineStage}
 *
 * @author Selvakumar
 * @version 1.0
 */
public class PipelineStageConverter extends EntityConverter<PipelineStage> implements OptionJsonConverter<PipelineStage> {

    private static final double ZERO = 0D;

    private final PipelineStageManager pipelineStageManager;

    private static class Instance {
        private static final PipelineStageConverter INSTANCE = new PipelineStageConverter();
    }

    private PipelineStageConverter() {
        pipelineStageManager = PipelineStageManager.getInstance();
    }

    public static PipelineStageConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Converts {@link JsonObject} into {@link PipelineStage}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the pipeline stage data
     * @return the {@link PipelineStage}
     */
    @Override
    public PipelineStage convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        if (converterConfig.isPartialData()) {
            return buildPipelineStageByPartialData(converterConfig, jsonObject);
        } else {
            return buildPipelineStage(converterConfig, jsonObject);
        }
    }

    /**
     * Converts {@link JsonObject} into {@link PipelineStage}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the pipeline stage data
     * @return the {@link PipelineStage}
     */
    private PipelineStage buildPipelineStage(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final PipelineStage pipelineStage = new PipelineStage();

        if (jsonObject.containsKey("id")) {
            pipelineStage.setId(jsonObject.getLong("id"));
        }

        setDataToPipelineStage(converterConfig, pipelineStage, jsonObject);

        return pipelineStage;
    }

    /**
     * Converts the partial data present in  {@link JsonObject} into {@link PipelineStage}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the pipeline stage data
     * @return the {@link PipelineStage}
     */
    private PipelineStage buildPipelineStageByPartialData(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final PipelineStage pipelineStage = pipelineStageManager.get(converterConfig.orgId(),
                jsonObject.getLong(PipelineStageFields.ID.getName()));

        setDataToPipelineStage(converterConfig, pipelineStage, jsonObject);
        return pipelineStage;
    }

    /**
     * <p>
     * Maps json data into {@link PipelineStage}
     * </p>
     *
     * @param converterConfig converter configuration
     * @param pipelineStage   which contains the pipeline stage data
     * @param jsonObject      which contains the pipeline stage fields data
     */
    private void setDataToPipelineStage(final ConverterConfig converterConfig, final PipelineStage pipelineStage,
                                        final JsonObject jsonObject) {
        final String pipelineIdKey = PipelineStageFields.PIPELINE_ID.getName();
        final String nameKey = PipelineStageFields.NAME.getName();
        final String probabilityKey = PipelineStageFields.PROBABILITY.getName();
        final String pipelineStageName = pipelineStage.getName();

        if (jsonObject.containsKey(pipelineIdKey)) {
            pipelineStage.setPipelineId(jsonObject.getLong(pipelineIdKey));
        }

        pipelineStage.setOrgId(converterConfig.orgId());
        pipelineStage.setName(jsonObject.containsKey(nameKey) ? jsonObject.getString(nameKey)
                : pipelineStageName);

        if (jsonObject.containsKey(probabilityKey)) {
            final Object probability = jsonObject.get(probabilityKey);

            if (probability instanceof final Number number) {
                pipelineStage.setProbability(number.doubleValue());
            } else if (probability instanceof final String value) {
                if (value.isEmpty() || !TypeChecker.isNumber(value) || TypeChecker.isNegativeNumber(value)) {
                    pipelineStage.setProbability(ZERO);
                } else {
                    pipelineStage.setProbability(Double.parseDouble(value));
                }
            }
        } else {
            final Double existingProbability = pipelineStage.getProbability();

            if (Objects.nonNull(pipelineStageName) && State.LOST.getName().equals(pipelineStageName)) {
                pipelineStage.setProbability(Objects.isNull(existingProbability) ? 0 : existingProbability);
            } else {
                pipelineStage.setProbability(Objects.isNull(existingProbability) ? 100.0 : existingProbability);
            }
        }
    }

    /**
     * Converts the pipeline stage data to json object
     *
     * @param pipelineStage which contains the pipeline stage data
     * @return the {@link JsonObject} filled with pipeline stage data
     */
    @Override
    public JsonObject convert(final PipelineStage pipelineStage) {
        final JsonObject pipelineStageJson = Json.object();
        final String pipelineStageName = pipelineStage.getName();

        pipelineStageJson.put(PipelineStageFields.ID.getName(), pipelineStage.id());
        pipelineStageJson.put(PipelineStageFields.NAME.getName(), pipelineStage.getName());
        pipelineStageJson.put(PipelineStageFields.PROBABILITY.getName(), pipelineStage.getProbability());
        pipelineStageJson.put(PipelineStageFields.ORG_ID.getName(), pipelineStage.orgId());
        pipelineStageJson.put(PipelineStageFields.IS_DEFAULT.getName(), pipelineStage.isDefault());
        pipelineStageJson.put(PipelineStageFields.PIPELINE_ID.getName(), pipelineStage.getPipelineId());

        if (Objects.nonNull(pipelineStageName)) {
            final boolean canDragAndEdit = !(State.WON.equalsIgnoreCase(pipelineStageName) || State.LOST.equalsIgnoreCase(pipelineStageName));

            pipelineStageJson.put(PipelineFields.IS_DRAGGABLE.getName(), canDragAndEdit);
            pipelineStageJson.put(PipelineFields.IS_EDITABLE.getName(), canDragAndEdit);
        }

        return pipelineStageJson;
    }

    /**
     * <p>
     * Converts partial pipeline stage data to json object
     * </p>
     *
     * @param pipelineStage which contains the pipeline stage data
     * @return the {@link JsonObject} filled with pipeline stage data
     */
    @Override
    public JsonObject convertPartialData(final PipelineStage pipelineStage) {
        final JsonObject pipelineStageJson = Json.object();

        pipelineStageJson.put(PipelineStageFields.ID.getName(), pipelineStage.id());
        pipelineStageJson.put(PipelineStageFields.NAME.getName(), pipelineStage.getName());
        pipelineStageJson.put(PipelineStageFields.ORG_ID.getName(), pipelineStage.orgId());

        return pipelineStageJson;
    }

    /**
     * Converts {@link PipelineStage} into {@link JsonObject}
     *
     * @param pipelineStage The pipeline stage entity with the attributes of pipeline stage details.
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertModelToOptionJson(final PipelineStage pipelineStage) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(PipelineStageFields.VALUE.getName(), pipelineStage.id());
        jsonObject.put(PipelineStageFields.NAME.getName(), pipelineStage.getName());

        return jsonObject;
    }

}
