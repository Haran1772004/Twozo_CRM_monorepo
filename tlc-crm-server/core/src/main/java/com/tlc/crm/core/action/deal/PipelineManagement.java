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
import com.tlc.crm.core.api.deal.manager.PipelineManager;
import com.tlc.crm.core.api.deal.manager.PipelineStageManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.Deal.State;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.api.deal.model.converter.PipelineConverter;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.internal.deal.status.DealErrorCodes;
import com.tlc.crm.core.internal.event.index.publisher.IndexDataPublisher;
import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.core.field.deal.PipelineFields;
import com.tlc.web.WebAction;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java .util.List;
import java.util.stream.Collectors;

/**
 * Manages the {@link Pipeline} related web actions
 *
 * @author Sundar
 * @version 1.0
 */
@WebAction(path = "/pipeline/mgmt")
public class PipelineManagement extends CrmConfigAction<Pipeline> {

    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 15;
    private static final short PIPELINE_STAGE_NAME_LIMIT = 250;
    private static final long ALL_PIPELINE_ID = -1L;

    private final ConverterConfigBuilder converterConfigBuilder;
    private final PipelineConverter pipelineConverter;
    private final PipelineManager pipelineManager;
    private final PipelineStageManager pipelineStageManager;
    private final IndexDataPublisher indexDataPublisher;

    public PipelineManagement() {
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        pipelineManager = PipelineManager.getInstance();
        pipelineConverter = PipelineConverter.getInstance();
        pipelineStageManager = PipelineStageManager.getInstance();
        indexDataPublisher = IndexDataPublisher.getInstance();
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
        final String type = requestJson.getString(DealFields.TYPE);
        final JsonObject data = requestJson.getJsonObject(DealFields.DATA);
        final Long orgId = request.orgId();
        final long userId = request.userId();

        switch (type) {
            case "deletePipeline" -> deletePipeline(orgId, data);
            case "list" -> response.put(PipelineFields.LIST.getName(), getPipelineList(orgId, userId, data, response));
            case "updateOrder" -> updatePipelinesOrder(orgId, data);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link AbstractConfigManager} of Pipeline
     */
    @Override
    public ConfigManager<Pipeline> getConfigManager() {
        return pipelineManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Pipeline convert(CrmRequest request, JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);

        return pipelineConverter.convert(converterConfig, jsonObject);
    }


    /**
     * {@inheritDoc}
     *
     * @param pipeline which contains the pipeline data
     * @return the {@link JsonObject} filled with data
     */
    @Override
    public JsonObject convert(final Pipeline pipeline) {
        return pipelineConverter.convert(pipeline);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Pipeline convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, jsonObject);

        return pipelineConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     represents the organization ID
     * @param userId    Represents the user id.
     * @param action    perform action like create, update or delete
     * @param pipelines which contains the {@link Collection} of pipelines
     */
    @Override
    protected final boolean isValidDetails(final Long orgId, final Long userId, final Action action, final Collection<Pipeline> pipelines) {

        if (Objects.nonNull(pipelines) && !pipelines.isEmpty()) {
            switch (action) {
                case CREATE -> {
                    final int pipelineSize = pipelines.size();

                    if (pipelineSize > MAX_LIMIT || (pipelineManager.getPipelineCount(orgId) + pipelineSize) > MAX_LIMIT) {
                        throw ErrorCode.get(DealErrorCodes.PIPELINE_LIMIT_REACHED, "i18n.pipeline.limit.reached");
                    }

                    for (final Pipeline pipeline : pipelines) {
                        validatePipeline(orgId, pipeline, action);
                        validatePipelineStages(orgId, pipeline, action);
                    }
                }
                case UPDATE -> {
                    if (pipelines.stream().filter(Pipeline::isDefault).count() > 1) {
                        throw ErrorCode.get(DealErrorCodes.DEFAULT_PIPELINE_EXISTS, "i18n.pipeline.default.exists");
                    }
                    final Long defaultPipelineId = pipelineManager.getDefaultPipeline(orgId).id();
                    final Map<Long, Long> defaultStageMap = pipelineStageManager.getDefaultStageIds(orgId, pipelines.stream().map(Pipeline::id).collect(Collectors.toSet()));

                    for (final Pipeline pipeline : pipelines) {
                        final Long pipelineId = pipeline.id();

                        if (!pipeline.isDefault() && Objects.equals(pipelineId, defaultPipelineId)) {
                            throw ErrorCode.get(DealErrorCodes.PIPELINE_MUST_BE_DEFAULT, "i18n.pipeline.must.default");
                        }

                        if (pipelineManager.isPipelineAlreadyExists(orgId, pipelineId, pipeline.getName())) {
                            throw ErrorCode.get(DealErrorCodes.PIPELINE_ALREADY_EXISTS, "i18n.pipeline.already.exists");
                        }

                        pipeline.getPipelineStages().stream().findFirst().filter(pipelineStage ->
                                !Objects.equals(pipelineStage.id(), defaultStageMap.get(pipelineId))).ifPresent(pipelineStage -> {
                            throw ErrorCode.get(DealErrorCodes.DEFAULT_PIPELINE_STAGE_SHOULD_BE_POSITIONED_FIRST, "i18n.pipeline.default.should.positioned.first");
                        });
                        validatePipelineStages(orgId, pipeline, action);
                    }
                }
            }
        }

        return true;
    }

    /**
     * <p>
     * Validates the pipeline stages.
     * </p>
     *
     * @param pipeline An entity that holds the pipeline attributes.
     */
    private void validatePipelineStages(final long orgId, final Pipeline pipeline, final Action action) {
        final String won = State.WON.getName();
        final String lost = State.LOST.getName();
        final Collection<PipelineStage> pipelineStages = pipeline.getPipelineStages();
        final Set<String> stages = pipelineStages.stream().map(stage ->
                stage.getName().trim().toLowerCase()).collect(Collectors.toSet());
        final boolean areDealStateStagesPresent = stages.containsAll(Arrays.asList(won, lost));
        final Long pipelineId = pipeline.id();

        if (!(areDealStateStagesPresent && stages.size() > 2)) {
            throw ErrorCode.get(DealErrorCodes.DEAL_STATE_STAGES_MISSING);
        }

        switch (action) {
            case CREATE -> validatePipelineStageName(orgId, pipelineId, true, pipelineStages);
            case UPDATE -> validatePipelineStageName(orgId, pipelineId, false, pipelineStages);
        }
    }

    /**
     * <p>
     * Validates the pipeline stage names
     * </p>
     *
     * @param orgId          Represents the org id
     * @param pipelineId     Represents the pipeline id
     * @param isCreateEvent  Represents whether its a create event or not
     * @param pipelineStages Represents the {@link Collection} of {@link PipelineStage}
     */
    private void validatePipelineStageName(final Long orgId, final Long pipelineId, final boolean isCreateEvent, final Collection<PipelineStage> pipelineStages) {
        final int pipelineStageSize = pipelineStages.size();
        final Collection<String> stageNames = new HashSet<>(pipelineStageSize);
        final Collection<String> newStageNames = new HashSet<>(pipelineStageSize);
        final ErrorCode duplicateStageErrorCode = ErrorCode.get(DealErrorCodes.PIPELINE_STAGE_ALREADY_EXISTS, "i18n.pipelineStage.creation.failed");

        for (final PipelineStage pipelineStage : pipelineStages) {
            final String pipelineStageName = pipelineStage.getName();

            validateStageNameLength(pipelineStageName);

            if (!stageNames.add(pipelineStageName.toLowerCase())) {
                throw duplicateStageErrorCode;
            }

            if (!isCreateEvent && Objects.isNull(pipelineStage.id())) {
                newStageNames.add(pipelineStageName);
            }
        }

        if (!newStageNames.isEmpty() && pipelineStageManager.anyPipelineStageExists(orgId, pipelineId, newStageNames)) {
            throw duplicateStageErrorCode;
        }
    }

    /**
     * <p>
     * Validates the length of the pipeline stage name
     * </p>
     *
     * @param pipelineStageName Represents the pipeline stage name
     */
    private void validateStageNameLength(final String pipelineStageName) {

        if (pipelineStageName.length() > PIPELINE_STAGE_NAME_LIMIT) {
            throw ErrorCode.get(DealErrorCodes.PIPELINE_STAGE_NAME_LIMIT_EXCEEDED, "i18n.pipelineStage.name.limit.exceeded");
        }
    }

    /**
     * <p>
     * validates the pipeline
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param pipeline An entity that holds the pipeline attributes
     */
    private void validatePipeline(final Long orgId, final Pipeline pipeline, final Action action) {
        final Collection<PipelineStage> pipelineStages = pipeline.getPipelineStages();
        final Long pipelineId = pipeline.id();

        if (pipelineManager.isPipelineAlreadyExists(orgId, pipelineId, pipeline.getName())) {
            throw ErrorCode.get(DealErrorCodes.PIPELINE_ALREADY_EXISTS, "i18n.pipeline.already.exists");
        }

        switch (action) {
            case CREATE -> {
                if (pipelineStages.size() > MAX_LIMIT) {
                    throw ErrorCode.get(DealErrorCodes.PIPELINE_STAGE_LIMIT_REACHED, "i18n.pipelineStage.limit.reached");
                }

                for (final PipelineStage pipelineStage : pipelineStages) {

                    if (pipelineStage.getName().isBlank()) {
                        throw ErrorCode.get(DealErrorCodes.INVALID_PIPELINE_STAGE_NAME, "i18n.pipelineStage.invalid.name");
                    }
                }
            }
            case UPDATE -> {
                final Long newPipelineStageCount = pipelineStages.stream().filter(pipelineStage -> Objects.isNull(pipelineStage.id())).count();

                if (pipelineStageManager.getPipelineStageCount(orgId, pipelineId) + newPipelineStageCount > MAX_LIMIT) {
                    throw ErrorCode.get(DealErrorCodes.PIPELINE_STAGE_LIMIT_REACHED);
                }
                final Map<State, PipelineStage> stages = pipelineStageManager.getPipelineStages(orgId, pipelineId);
                final PipelineStage openStage = stages.get(State.OPEN);
                final PipelineStage lostStage = stages.get(State.LOST);
                final PipelineStage wonStage = stages.get(State.WON);

                for (final PipelineStage pipelineStage : pipelineStages) {
                    final Long pipelineStageId = pipelineStage.id();

                    if (pipelineStage.getName().isBlank()) {
                        throw ErrorCode.get(DealErrorCodes.INVALID_PIPELINE_STAGE_NAME, "i18n.pipelineStage.invalid.name");
                    }
                    final boolean isWonStage = Objects.equals(pipelineStageId, wonStage.id());
                    final boolean isDefaultStage = pipelineStage.isDefault();

                    if (isWonStage || Objects.equals(pipelineStageId, lostStage.id())) {
                        final PipelineStage stage = isWonStage ? wonStage : lostStage;

                        if ((Objects.nonNull(pipelineStage.getName()) && !pipelineStage.getName().equalsIgnoreCase(stage.getName())) ||
                                !Objects.equals(pipelineStage.getProbability(), stage.getProbability()) ||
                                !Objects.equals(isDefaultStage, stage.isDefault())) {
                            throw ErrorCode.get(DealErrorCodes.WON_OR_LOST_STAGE_UPDATE_RESTRICTED);
                        }
                    } else if (Objects.equals(pipelineStageId, openStage.id()) && !Objects.equals(isDefaultStage, openStage.isDefault())) {
                        throw ErrorCode.get(DealErrorCodes.IS_DEFAULT_UPDATE_RESTRICTED);
                    }
                }
            }
        }
    }

    /**
     * Deletes the given pipeline and migrate the deals to another pipeline
     *
     * @param orgId      represents the organization id
     * @param jsonObject contains request data
     */
    private void deletePipeline(final Long orgId, final JsonObject jsonObject) {
        final long pipelineId = jsonObject.getLong(DealFields.PIPELINE_ID);

        if (!pipelineManager.exists(orgId, pipelineId)) {
            throw ErrorCode.get(DealErrorCodes.PIPELINE_NOT_FOUND, "i18n.pipeline.not.found");
        }

        final List<Deal> migratedDeals = pipelineManager.migratePipelineDeals(orgId, pipelineId);

        if (!migratedDeals.isEmpty()) {
            indexDataPublisher.publishIndexData(orgId, migratedDeals, Action.PARTIAL_UPDATE);
        }
    }

    /**
     * <p>
     * Gets the pipeline list with corresponding pipeline stages
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents the user id
     * @param data     An object that holds request data
     * @param response Represents the crm response
     * @return {@link JsonArray} of pipelines
     */
    private JsonArray getPipelineList(final long orgId, final long userId, final JsonObject data, final CrmResponse response) {
        final JsonArray jsonArray = Json.array();
        final int start = Math.max(data.optInt(PipelineFields.START.getName(), DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(PipelineFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
        final Long totalRecords = pipelineManager.getPipelineCount(orgId);
        final boolean hasMore = (start + limit) <= totalRecords;
        final boolean isAllPipelineEnabled = data.optBoolean(PipelineFields.IS_ALL_PIPELINE_ENABLED.getName());
        final JsonObject allPipelineJson = Json.object();
        boolean anyPipelineSelected = false;

        if (isAllPipelineEnabled) {
            allPipelineJson.put(PipelineFields.ID.getName(), ALL_PIPELINE_ID);
            allPipelineJson.put(PipelineFields.NAME.getName(), response.resolveI18nString("i18n.pipeline.list.all.pipeline"));
            allPipelineJson.put(PipelineFields.ORG_ID.getName(), orgId);
            allPipelineJson.put(PipelineFields.IS_DEFAULT.getName(), false);

            jsonArray.put(allPipelineJson);
        }

        if (totalRecords > 0) {
            final Collection<Pipeline> pipelines = pipelineManager.getPipelineList(orgId, userId, new DropdownLimit(start, limit), null, isAllPipelineEnabled);

            for (final Pipeline pipeline : pipelines) {
                if (!anyPipelineSelected && pipeline.isSelected()) {
                    anyPipelineSelected = true;
                }

                jsonArray.put(convert(pipeline));
            }
        }

        if (isAllPipelineEnabled && !anyPipelineSelected) {
            allPipelineJson.put(PipelineFields.IS_SELECTED.getName(), true);
        }
        response.put(PipelineFields.TOTAL_RECORDS.getName(), isAllPipelineEnabled ? totalRecords + 1L : totalRecords);
        response.put(PipelineFields.HAS_MORE.getName(), hasMore);

        return jsonArray;
    }

    /**
     * <p>
     * Changes the order of pipelines based on the provided JSON object
     * </p>
     *
     * @param orgId      Represents the organization id
     * @param jsonObject The JSON object containing the pipeline IDs representing the new order
     */
    private void updatePipelinesOrder(final Long orgId, final JsonObject jsonObject) {
        final Set<Long> pipelineIds = new LinkedHashSet<>();

        jsonObject.getJsonArray(PipelineFields.IDS.getName()).forEach(pipelineId -> pipelineIds.add((Long) pipelineId));

        if (!pipelineManager.exists(orgId, pipelineIds)) {
            throw ErrorCode.get(DealErrorCodes.PIPELINE_NOT_FOUND, "i18n.pipeline.not.found");
        }
        pipelineManager.updatePipelinesOrder(orgId, pipelineIds);
    }
}
