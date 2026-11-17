package com.tlc.crm.core.api.deal.model.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.converter.OptionJsonConverter;
import com.tlc.crm.core.api.deal.manager.PipelineManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.field.deal.PipelineFields;
import com.tlc.crm.core.field.deal.PipelineStageFields;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.common.converter.ConverterConfig;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Provides different ways to convert the data from / into {@link Pipeline}
 *
 * @author Selvakumar
 * @version 1.0
 */
public class PipelineConverter extends EntityConverter<Pipeline> implements OptionJsonConverter<Pipeline> {

    private final PipelineManager pipelineManager;
    private final PipelineStageConverter pipelineStageConverter;

    private static class Instance {
        private static final PipelineConverter INSTANCE = new PipelineConverter();
    }

    private PipelineConverter() {
        pipelineManager = PipelineManager.getInstance();
        pipelineStageConverter = PipelineStageConverter.getInstance();
    }

    public static PipelineConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Converts the Json data into the {@link Pipeline}
     *
     * @param converterConfig Converter Configuration
     * @param jsonObject      which contains the data
     * @return the {@link Pipeline} filled with the data
     */
    @Override
    public Pipeline convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        if (converterConfig.isPartialData()) {
            return buildPipelineByPartialData(converterConfig, jsonObject);
        } else {
            return buildPipeline(converterConfig, jsonObject);
        }
    }

    /**
     * Converts the Json data into the {@link Pipeline}
     *
     * @param converterConfig Converter Configuration
     * @param jsonObject      which contains the data
     * @return the {@link Pipeline} filled with the data
     */
    private Pipeline buildPipeline(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Pipeline pipeline = new Pipeline();
        final String idKey = PipelineFields.ID.getName();

        if (jsonObject.containsKey(idKey)) {
            pipeline.setId(jsonObject.getLong(idKey));
        }

        setJsonToPipeline(converterConfig, pipeline, jsonObject);
        pipeline.setOrgId(converterConfig.orgId());
        return pipeline;
    }

    /**
     * Constructs {@link Pipeline} by using partial data
     *
     * @param converterConfig Converter Configuration
     * @param jsonObject      which contains the pipeline data
     * @return the {@link Pipeline} filled with partial data
     */
    private Pipeline buildPipelineByPartialData(final ConverterConfig converterConfig,
                                                final JsonObject jsonObject) {
        final Pipeline pipeline = pipelineManager.get(converterConfig.orgId(),
                jsonObject.getLong(PipelineFields.ID.getName()));

        setJsonToPipeline(converterConfig, pipeline, jsonObject);
        return pipeline;
    }

    /**
     * Sets the json into the {@link Pipeline}
     *
     * @param converterConfig Converter Configuration
     * @param pipeline        the pipeline to be filled with the data
     * @param jsonObject      which contains the data to be converted
     */
    private void setJsonToPipeline(final ConverterConfig converterConfig, final Pipeline pipeline,
                                   final JsonObject jsonObject) {
        final Long orgId = converterConfig.orgId();
        final String nameKey = PipelineFields.NAME.getName();
        final String rottenKey = PipelineFields.ROTTEN_DAYS.getName();
        final String defaultKey = PipelineFields.IS_DEFAULT.getName();
        final short defaultRottenDays = 30;

        pipeline.setOrgId(orgId);
        pipeline.setName(jsonObject.containsKey(nameKey) ? jsonObject.getString(nameKey) : pipeline.getName());
        pipeline.setDefault(jsonObject.containsKey(defaultKey) ? jsonObject.getBoolean(defaultKey)
                : pipeline.isDefault());

        if (jsonObject.containsKey(rottenKey)) {
            pipeline.setRottenDays(jsonObject.getShort(rottenKey));
        } else {
            final Short existingRottenDays = pipeline.getRottenDays();

            pipeline.setRottenDays(Objects.isNull(existingRottenDays) ? defaultRottenDays : existingRottenDays);
        }

        setPipelineStages(converterConfig, pipeline, jsonObject);
    }

    /**
     * Sets the pipeline stages into the pipeline
     *
     * @param converterConfig Converter Configuration
     * @param pipeline        which needs the pipeline stages
     * @param jsonObject      which contains the pipeline stage data
     */
    private void setPipelineStages(final ConverterConfig converterConfig, final Pipeline pipeline,
                                   final JsonObject jsonObject) {
        final String stagesKey = PipelineFields.STAGES.getName();

        if (jsonObject.containsKey(stagesKey)) {
            final JsonArray stages = jsonObject.getJsonArray(stagesKey);
            final Long pipelineId = pipeline.id();
            final int stageSize = stages.size();
            short nextOrder = 1;
            boolean isDefault = true;

            for (int index = 0; index < stageSize; index++) {
                final JsonObject stageJson = stages.getJsonObject(index);

                if (Objects.nonNull(pipelineId)) {
                    stageJson.put(PipelineStageFields.PIPELINE_ID.getName(), pipelineId);
                }
                final PipelineStage pipelineStage = pipelineStageConverter.convert(converterConfig, stageJson);

                if (!converterConfig.isPartialData()) {
                    final String stageName = pipelineStage.getName();

                    if (!Deal.State.LOST.equalsIgnoreCase(stageName) && !Deal.State.WON.equalsIgnoreCase(stageName) && isDefault) {
                        pipelineStage.setDefault(true);
                        isDefault = false;
                    }

                    if (pipelineStage.isDefault()) {
                        pipelineStage.setOrder(nextOrder++);
                    } else {
                        pipelineStage.setOrder(switch (pipelineStage.getName().toLowerCase()) {
                            case "won" -> (short) (stageSize - 1);
                            case "lost" -> (short) stageSize;
                            default -> nextOrder++;
                        });
                    }
                }
                pipeline.addPipelineStages(pipelineStage);
            }
        }
    }

    /**
     * Converts {@link Pipeline} into {@link JsonObject}
     *
     * @param pipeline which contains the pipeline data
     * @return the {@link JsonObject} filled with data
     */
    @Override
    public JsonObject convert(Pipeline pipeline) {
        final JsonObject pipelineJson = Json.object();
        final JsonArray pipelineStagesJson = Json.array();
        final Collection<PipelineStage> pipelineStages = pipeline.getPipelineStages();

        if (Objects.nonNull(pipeline.id())) {
            pipelineJson.put(PipelineFields.ID.getName(), pipeline.id());
        }

        if (Objects.nonNull(pipelineStages)) {
            final List<PipelineStage> stages = pipelineStages.stream().sorted(Comparator.comparing(PipelineStage::getOrder)).toList();

            for (int index = 0; index < stages.size(); index++) {
                final PipelineStage pipelineStage = stages.get(index);
                final JsonObject stageJson = pipelineStageConverter.convert(pipelineStage);

                if (index == 0) {
                    stageJson.put(PipelineFields.IS_EDITABLE.getName(), true);
                    stageJson.put(PipelineFields.IS_DRAGGABLE.getName(), false);
                }
                pipelineStagesJson.put(stageJson);
            }
        }

        if (pipeline.isSelected()) {
            pipelineJson.put(PipelineFields.IS_SELECTED.getName(), true);
        }

        pipelineJson.put(PipelineFields.NAME.getName(), pipeline.getName());
        pipelineJson.put(PipelineFields.ORG_ID.getName(), pipeline.orgId());
        pipelineJson.put(PipelineFields.IS_DEFAULT.getName(), pipeline.isDefault());
        pipelineJson.put(PipelineFields.ROTTEN_DAYS.getName(), pipeline.getRottenDays());
        pipelineJson.put(PipelineFields.STAGES.getName(), pipelineStagesJson);

        return pipelineJson;
    }

    /**
     * <p>
     * Converts partial {@link Pipeline} into {@link JsonObject}
     * </p>
     *
     * @param pipeline which contains the pipeline data
     * @return the {@link JsonObject} filled with data
     */
    @Override
    public JsonObject convertPartialData(final Pipeline pipeline) {
        final JsonObject pipelineJson = Json.object();

        if (Objects.nonNull(pipeline.id())) {
            pipelineJson.put(PipelineFields.ID.getName(), pipeline.id());
        }
        pipelineJson.put(PipelineFields.NAME.getName(), pipeline.getName());
        pipelineJson.put(PipelineFields.ORG_ID.getName(), pipeline.orgId());

        return pipelineJson;
    }

    /**
     * Converts {@link Pipeline} into {@link JsonObject}
     *
     * @param pipeline The pipeline entity with the attributes of pipeline details.
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertModelToOptionJson(final Pipeline pipeline) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(PipelineFields.VALUE.getName(), pipeline.id());
        jsonObject.put(PipelineFields.NAME.getName(), pipeline.getName());

        return jsonObject;
    }
}
