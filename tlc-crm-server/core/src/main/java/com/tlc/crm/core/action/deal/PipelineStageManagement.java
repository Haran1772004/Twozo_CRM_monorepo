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
import com.tlc.crm.common.models.SortType;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.manager.PipelineManager;
import com.tlc.crm.core.api.deal.manager.PipelineStageManager;
import com.tlc.crm.core.api.deal.model.DealKanbanSortFields;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.api.deal.model.converter.PipelineStageConverter;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.field.deal.PipelineStageFields;
import com.tlc.crm.core.internal.deal.status.DealErrorCodes;
import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.core.api.deal.model.Deal.State;
import com.tlc.crm.common.list.sort.models.SortCriteria;
import com.tlc.web.WebAction;

import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages the {@link PipelineStage} related web actions
 *
 * @author Sundar
 * @version 1.0
 */
@WebAction(path = "/pipeline/stage/mgmt")
public class PipelineStageManagement extends CrmConfigAction<PipelineStage> {

    private static final int MAX_PIPELINE_STAGE = 15;
    private final ConverterConfigBuilder converterConfigBuilder;
    private final PipelineStageConverter pipelineStageConverter;
    private final PipelineStageManager pipelineStageManager;
    private final PipelineManager pipelineManager;
    private final DealManager dealManager;

    public PipelineStageManagement() {
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        pipelineStageConverter = PipelineStageConverter.getInstance();
        pipelineStageManager = PipelineStageManager.getInstance();
        pipelineManager = PipelineManager.getInstance();
        dealManager = DealManager.getInstance();
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
        final String type = requestJson.getString("type");
        final JsonObject data = requestJson.getJsonObject("data");
        final Long orgId = request.orgId();

        switch (type) {
            case "getStagesByPipeline" -> {
                final long id = data.getLong(PipelineStageFields.ID.getName());

                if (!pipelineManager.exists(orgId, id)) {
                    throw ErrorCode.get(DealErrorCodes.PIPELINE_NOT_FOUND, "i18n.pipeline.not.found");
                }
                response.put("data", getStagesByPipeline(orgId, request.userId(), id));
            }
            case "deletePipelineStage" -> deletePipelineStage(orgId, data);
            case "updateOrder" -> updatePipelineStagesOrder(orgId, data);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link AbstractConfigManager} of pipeline stage
     */
    @Override
    public ConfigManager<PipelineStage> getConfigManager() {
        return pipelineStageManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PipelineStage convert(CrmRequest request, JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);

        return pipelineStageConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param pipelineStage which contains the pipeline stage data
     * @return the {@link JsonObject} filled with pipeline stage data
     */
    @Override
    public JsonObject convert(PipelineStage pipelineStage) {
        return pipelineStageConverter.convert(pipelineStage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PipelineStage convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, jsonObject);

        return pipelineStageConverter.convert(converterConfig, jsonObject);
    }

    /**
     * Gets the pipeline stages by pipeline
     *
     * @param orgId      organisation ID
     * @param userId     Represents the user id
     * @param pipelineId pipeline ID
     * @return the {@link JsonArray} of pipeline stages
     */
    private JsonArray getStagesByPipeline(final long orgId, final long userId, final long pipelineId) {
        final JsonArray pipelineStagesArray = Json.array();
        final Collection<PipelineStage> pipelineStages = pipelineStageManager.getPipelineStagesByPipelineId(orgId, pipelineId);
        pipelineManager.loadUserPreferredPipeline(orgId, userId, pipelineId);

        if (Objects.nonNull(pipelineStages)) {
            final Collection<Long> pipelineStageIdCollection = pipelineStages.stream().map(PipelineStage::id).collect(Collectors.toSet());
            final Map<Long, Optional<SortCriteria>> stageCriteriaMap = pipelineStageManager.getPipelineStageCriteriaList(orgId, userId, pipelineStageIdCollection);
            final String fieldKey = DealFields.FIELD;
            final String typeKey = DealFields.TYPE;
            final String sortKey = PipelineStageFields.SORT.getName();

            for (final PipelineStage stage : pipelineStages) {
                final Long stageId = stage.id();
                final JsonObject stageJson = pipelineStageConverter.convert(stage);

                if (stageCriteriaMap.containsKey(stageId)) {
                    final Optional<SortCriteria> optionalSortCriteria = stageCriteriaMap.get(stageId);

                    optionalSortCriteria.ifPresent(sortCriteria -> {
                        final JsonObject sortCriteriaJson = Json.object();

                        sortCriteriaJson.put(fieldKey, DealKanbanSortFields.getById(sortCriteria.getFieldId().intValue()).getName());
                        sortCriteriaJson.put(typeKey, SortType.getById(sortCriteria.getSortType().getId()).getOrderTypeName());
                        stageJson.put(sortKey, sortCriteriaJson);
                    });
                }
                pipelineStagesArray.put(stageJson);
            }
        }

        return pipelineStagesArray;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId          represents the organization ID
     * @param userId         Represents the user id.
     * @param action         perform action like create, update or delete
     * @param pipelineStages which contains the {@link Collection} of pipeline stages
     */
    @Override
    protected final boolean isValidDetails(final Long orgId, final Long userId, final Action action, final Collection<PipelineStage> pipelineStages) {

        if (Objects.nonNull(pipelineStages) && !pipelineStages.isEmpty()) {
            final Set<Long> pipelineIds = pipelineStages.stream().map(PipelineStage::getPipelineId).collect(Collectors.toSet());

            switch (action) {
                case CREATE -> {
                    final Map<Long, Collection<Long>> pipelineStagesIds = pipelineStageManager.getPipelineStagesIds(orgId, pipelineIds);

                    if (!pipelineManager.exists(orgId, pipelineIds)) {
                        throw ErrorCode.get(DealErrorCodes.PIPELINE_NOT_FOUND, "i18n.pipeline.not.found");
                    }

                    for (final PipelineStage pipelineStage : pipelineStages) {
                        final Long pipelineId = pipelineStage.getPipelineId();

                        if (pipelineStagesIds.get(pipelineId).size() >= MAX_PIPELINE_STAGE) {
                            throw ErrorCode.get(DealErrorCodes.PIPELINE_STAGE_LIMIT_REACHED);
                        }

                        if (pipelineStage.isDefault()) {
                            throw ErrorCode.get(DealErrorCodes.DEFAULT_PIPELINE_STAGE_EXISTS);
                        }

                        if (pipelineStageManager.isStageAlreadyExists(orgId, pipelineId, pipelineStage.id(), pipelineStage.getName())) {
                            throw ErrorCode.get(DealErrorCodes.PIPELINE_STAGE_ALREADY_EXISTS, "i18n.pipelineStage.creation.failed");
                        }
                    }
                }
                case UPDATE -> {
                    if (!pipelineManager.exists(orgId, pipelineIds)) {
                        throw ErrorCode.get(DealErrorCodes.PIPELINE_NOT_FOUND, "i18n.pipeline.not.found");
                    }
                    final Map<Long, Long> defaultStageMap = pipelineStageManager.getDefaultStageIds(orgId, pipelineIds);
                    final Map<Long, Map<State, PipelineStage>> stages = pipelineStageManager.getPipelineStages(orgId, pipelineIds);

                    for (final PipelineStage pipelineStage : pipelineStages) {
                        final Long pipelineStageId = pipelineStage.id();
                        final Long pipelineId = pipelineStage.getPipelineId();

                        if (pipelineStageManager.isStageAlreadyExists(orgId, pipelineId, pipelineStageId, pipelineStage.getName())) {
                            throw ErrorCode.get(DealErrorCodes.PIPELINE_STAGE_ALREADY_EXISTS, "i18n.pipelineStage.updation.failed");
                        }

                        if (pipelineStage.isDefault() && !Objects.equals(pipelineStageId, defaultStageMap.get(pipelineId))) {
                            throw ErrorCode.get(DealErrorCodes.DEFAULT_PIPELINE_STAGE_EXISTS);
                        }

                        final Map<State, PipelineStage> stageMap = stages.get(pipelineId);
                        final PipelineStage openStage = stageMap.get(State.OPEN);
                        final PipelineStage lostStage = stageMap.get(State.LOST);
                        final PipelineStage wonStage = stageMap.get(State.WON);
                        final boolean isWonStage = Objects.equals(pipelineStageId, wonStage.id());
                        final boolean isDefaultStage = pipelineStage.isDefault();

                        if (isWonStage || Objects.equals(pipelineStageId, lostStage.id())) {
                            final PipelineStage stage = isWonStage ? wonStage : lostStage;

                            if ((Objects.nonNull(pipelineStage.getName()) && !pipelineStage.getName().equalsIgnoreCase(stage.getName())) ||
                                    !Objects.equals(pipelineStage.getProbability(), stage.getProbability())
                                    || !Objects.equals(isDefaultStage, stage.isDefault())) {
                                throw ErrorCode.get(DealErrorCodes.WON_OR_LOST_STAGE_UPDATE_RESTRICTED);
                            }
                        } else if (Objects.equals(pipelineStageId, openStage.id()) && !Objects.equals(isDefaultStage, openStage.isDefault())) {
                            throw ErrorCode.get(DealErrorCodes.IS_DEFAULT_UPDATE_RESTRICTED);
                        }
                    }
                }
                case DELETE -> {
                    for (final PipelineStage pipelineStage : pipelineStages) {
                        if (dealManager.getDealsCountByPipelineAndStage(orgId, pipelineStage.getPipelineId(), pipelineStage.id()) > 0) {
                            throw ErrorCode.get(DealErrorCodes.PIPELINE_STAGE_DELETION_RESTRICTED, "i18n.pipelineStage.deletion.restricted");
                        }
                    }
                }
            }
        }

        return true;
    }

    /**
     * Deletes the given pipeline stage and migrate the deals to another stage of respective pipeline
     *
     * @param orgId      represents the organization id
     * @param jsonObject contains request data
     */
    private void deletePipelineStage(final Long orgId, final JsonObject jsonObject) {
        final Long pipelineId = jsonObject.getLong(DealFields.PIPELINE_ID);
        final Long pipelineStageId = jsonObject.getLong(DealFields.PIPELINE_STAGE_ID);

        pipelineStageManager.migratePipelineStageDeals(orgId, pipelineStageId, pipelineId);
    }

    /**
     * <p>
     * Changes the order of pipeline stages for the specified organization based on the provided JSON object
     * </p>
     *
     * @param orgId      Represents the organization id
     * @param jsonObject The JSON object containing the pipeline stage IDs representing the new order
     */
    private void updatePipelineStagesOrder(final Long orgId, final JsonObject jsonObject) {
        final Long pipelineId = jsonObject.getLong(PipelineStageFields.PIPELINE_ID.getName());
        final Set<Long> pipelineStageIds = new LinkedHashSet<>();

        jsonObject.getJsonArray(PipelineStageFields.IDS.getName()).forEach(pipelineStageId -> pipelineStageIds.add((Long) pipelineStageId));

        if (!pipelineManager.exists(orgId, pipelineId)) {
            throw ErrorCode.get(DealErrorCodes.PIPELINE_NOT_FOUND, "i18n.pipeline.not.found");
        }

        if (!pipelineStageManager.exists(orgId, pipelineId, pipelineStageIds)) {
            throw ErrorCode.get(DealErrorCodes.PIPELINE_STAGE_NOT_FOUND, "i18n.pipelineStage.not.found");
        }

        if (pipelineStageManager.isDraggableStages(orgId, pipelineStageIds)) {
            pipelineStageManager.updatePipelineStagesOrder(orgId, pipelineStageIds);
        } else {
            throw ErrorCode.get(DealErrorCodes.NON_DRAGGABLE_STAGES_CANNOT_BE_REORDERED);
        }
    }
}