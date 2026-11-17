package com.tlc.crm.core.action.segment;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmConfigAction;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.segment.action.ActionType;
import com.tlc.crm.core.segment.converter.SegmentConverter;
import com.tlc.crm.core.segment.converter.SegmentFilterPreferenceConverter;
import com.tlc.crm.core.segment.fields.SegmentFields;
import com.tlc.crm.core.segment.manager.SegmentManager;
import com.tlc.crm.core.segment.models.Segment;
import com.tlc.crm.core.segment.models.SegmentCondition;
import com.tlc.crm.core.segment.models.SegmentConditionCriteria;
import com.tlc.crm.core.segment.models.SegmentConditionGroup;
import com.tlc.crm.core.segment.models.SegmentFilterUserPreference;
import com.tlc.crm.core.segment.service.SegmentFilterPreferenceService;
import com.tlc.crm.core.segment.service.SegmentRecomputeService;
import com.tlc.crm.core.segment.service.SegmentService;
import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.internal.status.SegmentErrorCodes;
import com.tlc.crm.plan.and.billing.api.manager.OrgPlanManager;
import com.tlc.crm.plan.and.billing.api.models.OrgSubscriptionPlan;
import com.tlc.crm.security.permission.annotation.authorizer.MarketingCampaignAuthorizer;
import com.tlc.plans.and.billings.models.PlanType;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.type.Group;
import com.tlc.web.WebAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;

import com.tlc.crm.entity.filter.api.manager.FilterManager;

/**
 * <p>
 * Provides the segment management related web actions such as listing segments,
 * loading the filter preference, and triggering a segment recalculation refresh.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@MarketingCampaignAuthorizer
@WebAction(path = "/segment/mgmt")
public class SegmentManagement extends CrmConfigAction<Segment> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SegmentManagement.class);

    private final SegmentService segmentService;
    private final SegmentConverter segmentConverter;
    private final ConverterConfigBuilder converterConfigBuilder;
    private final SegmentFilterPreferenceConverter segmentFilterPreferenceConverter;
    private final SegmentFilterPreferenceService segmentFilterPreferenceService;
    private final SegmentRecomputeService segmentRecomputeService;
    private final OrgPlanManager orgPlanManager;
    private final FilterManager filterManager;

    public SegmentManagement() {
        segmentService = SegmentService.getInstance();
        segmentConverter = SegmentConverter.getInstance();
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        segmentFilterPreferenceConverter = SegmentFilterPreferenceConverter.getInstance();
        segmentFilterPreferenceService = SegmentFilterPreferenceService.getInstance();
        segmentRecomputeService = SegmentRecomputeService.getInstance();
        this.orgPlanManager = OrgPlanManager.getInstance();
        this.filterManager = FilterManager.getInstance();
    }

    /**
     * <p>
     * Routes the incoming request to the appropriate handler based on the request type.
     * </p>
     *
     * @param request  Represents the CRM request.
     * @param response Represents the CRM response.
     */
    @Override
    public void handle(final CrmRequest request, final CrmResponse response) {
        final String type = request.getRequestJson().getString(ActionFields.TYPE);

        switch (type) {
            case "list" -> list(request, response);
            case "getFilter" -> loadFilterPreference(request, response);
            case "refresh" -> refresh(request, response);
            case "checkStatus" -> this.checkStatus(request, response);
            default -> throw ErrorCode.get(SegmentErrorCodes.UNSUPPORTED_ACTION, "i18n.segment.unsupported.action");
        }
    }

    /**
     * <p>
     * Handles bulk partial updates for segments (archive/unarchive, add-to-folder).
     * </p>
     *
     * @param crmRequest  Holds the {@link CrmRequest}
     * @param crmResponse Holds the {@link CrmResponse}
     * @param eventData   Represents the {@link EventData}
     * @param isPartial   A flag indicating whether the update is partial or full
     */
    @Override
    protected void bUpdate(final CrmRequest crmRequest, final CrmResponse crmResponse,
                           final EventData eventData, final boolean isPartial) {
        if (!isPartial) {
            super.bUpdate(crmRequest, crmResponse, eventData, false);
            return;
        }
        final long orgId = crmRequest.orgId();
        final JsonObject data = crmRequest.getRequestJson().getJsonObject(ActionFields.DATA);
        final Collection<Long> segmentIds = new ArrayList<>();

        data.getJsonArray(SegmentFields.IDS.getName())
                .forEach(id -> segmentIds.add((Long) id));

        switch (ActionType.get(data.getShort(SegmentFields.ACTION_TYPE_ID.getName()))) {
            case STATUS_CHANGE -> segmentService.updateStatus(orgId, segmentIds,
                    data.getShort(SegmentFields.STATUS_ID.getName()));
            case ADD_TO_FOLDER -> segmentService.addToFolder(orgId, segmentIds,
                    data.containsKey(SegmentFields.FOLDER_ID.getName()) ? data.getLong(SegmentFields.FOLDER_ID.getName()) : null);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Organization id.
     * @param userId   Represents the user id.
     * @param action   Perform action like create, update or delete.
     * @param segments Collection of segments.
     * @param response Represents the {@link CrmResponse}.
     * @return {@code true} if folder details are valid.
     */
    @Override
    protected boolean isValidDetails(final Long orgId, final Long userId,
                                     final Action action, final Collection<Segment> segments, final CrmResponse response) {
        if (action.isCreateOrUpdate()) {

            if (action.isCreate()) {
                final int allowedLimit = getAllowedLimit(orgId);

                if ((segmentService.getTotalCount(orgId, null) + segments.size()) > allowedLimit) {
                    final String message = response.resolveI18nString("i18n.segment.limit.reached");

                    throw ErrorCode.get(SegmentErrorCodes.SEGMENT_LIMIT_REACHED, String.format(message, allowedLimit));
                }
            }
            final ErrorCode nameExistsErrorCode = ErrorCode.get(
                    SegmentErrorCodes.SEGMENT_NAME_ALREADY_EXISTS, "i18n.segment.name.already.exists"
            );
            final Collection<String> names = new HashSet<>();
            final Collection<Long> ids = new HashSet<>();

            for (final Segment segment : segments) {
                final String name = segment.getName();
                final Long id = segment.id();

                if (Objects.nonNull(name)) {
                    if (!names.add(name)) {
                        throw nameExistsErrorCode;
                    }
                }

                if (Objects.nonNull(id)) {
                    ids.add(id);
                }
            }

            if (!names.isEmpty()) {
                if (action.isCreate() && segmentService.existsByName(orgId, names)) {
                    throw nameExistsErrorCode;
                } else if (action.isUpdate() && segmentService.existsByName(orgId, ids, names)) {
                    throw nameExistsErrorCode;
                }
            }
        }

        return true;
    }

    /**
     * <p>
     * Triggers a segment recalculation refresh for the segment id resolved from the request.
     * </p>
     *
     * @param request  Represents the CRM request.
     * @param response Represents the CRM response.
     */
    private void refresh(final CrmRequest request, final CrmResponse response) {
        final JsonObject data = request.getRequestJson().getJsonObject(SegmentFields.DATA.getName());

        Long segmentId = null;

        if (data.containsKey(SegmentFields.ID.getName())) {
            segmentId = data.getLong(SegmentFields.ID.getName());
        }

        if (Objects.isNull(segmentId)) {
            throw ErrorCode.get(SegmentErrorCodes.SEGMENT_NOT_FOUND, "i18n.segment.not.found");
        }
        this.segmentRecomputeService.publish(request.orgId(), segmentId);
        response.put(SegmentFields.MESSAGE.getName(), "i18n.segment.refreshed.successfully");
    }

    /**
     * <p>
     * Gets the config manager used to perform the segment CRUD actions.
     * </p>
     *
     * @return The {@link SegmentManager} instance.
     */
    @Override
    public ConfigManager<Segment> getConfigManager() {
        return SegmentManager.getInstance();
    }

    /**
     * <p>
     * Converts the given request JSON into a {@link Segment} for a full, non-partial update.
     * </p>
     *
     * @param request    Represents the CRM request.
     * @param jsonObject Represents the request JSON to convert.
     * @return The converted {@link Segment}.
     */
    @Override
    public Segment convert(final CrmRequest request, final JsonObject jsonObject) {
        return segmentConverter.convert(converterConfigBuilder.build(request, false, jsonObject), jsonObject);
    }

    /**
     * <p>
     * Converts the given request JSON into a {@link Segment} for a partial update.
     * </p>
     *
     * @param request    Represents the CRM request.
     * @param jsonObject Represents the request JSON to convert.
     * @return The converted {@link Segment}.
     */
    @Override
    public Segment convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        return segmentConverter.convert(converterConfigBuilder.build(request, true, jsonObject), jsonObject);
    }

    /**
     * <p>
     * Converts the given {@link Segment} into its response JSON representation.
     * </p>
     *
     * @param request Represents the CRM request.
     * @param segment Represents the segment to convert.
     * @return The converted {@link JsonObject}.
     */
    @Override
    protected JsonObject convert(final CrmRequest request, final Segment segment) {
        return segmentConverter.convert(converterConfigBuilder.build(request, false), segment);
    }

    /**
     * <p>
     * Converts the given {@link Segment} into its JSON representation.
     * </p>
     *
     * @param segment Represents the segment to convert.
     * @return The converted {@link JsonObject}.
     */
    @Override
    protected JsonObject convert(final Segment segment) {
        return segmentConverter.convert(segment);
    }

    /**
     * <p>
     * Loads the segment filter preference for the current user and adds it to the response.
     * </p>
     *
     * @param request  Represents the CRM request.
     * @param response Represents the CRM response.
     */
    private void loadFilterPreference(final CrmRequest request, final CrmResponse response) {
        response.put(
                SegmentFields.FILTER.getName(), segmentFilterPreferenceConverter.convert(
                        converterConfigBuilder.build(request, false),
                        segmentFilterPreferenceService.get(request.orgId(), request.userId())
                )
        );
    }

    /**
     * <p>
     * Lists the segments matching the request criteria, upserts the resolved filter preference,
     * and adds the segment list along with pagination details to the response.
     * </p>
     *
     * @param request  Represents the CRM request.
     * @param response Represents the CRM response.
     */
    private void list(final CrmRequest request, final CrmResponse response) {
        final Long orgId = request.orgId();
        final JsonObject data = request.getRequestJson().getJsonObject(SegmentFields.DATA.getName());
        final JsonObject criteriaJson = data.getJsonObject(SegmentFields.CRITERIA.getName());
        final SegmentFilterUserPreference segmentFilterPreference = segmentFilterPreferenceConverter.convert(
                converterConfigBuilder.build(request, false), criteriaJson);

        segmentFilterPreferenceConverter.populateDefaultPreferences(segmentFilterPreference, criteriaJson);
        final SegmentFilterUserPreference updatedFilterPreference = segmentFilterPreferenceService.upsert(
                orgId, request.userId(), segmentFilterPreference);
        final long totalRecords = segmentService.getTotalCount(orgId, updatedFilterPreference);
        final Collection<Segment> segments = totalRecords == 0 ? Collections.emptyList() :
                segmentService.get(orgId, updatedFilterPreference);
        final JsonObject jsonObject = Json.object();
        final JsonArray segmentArray = Json.array();

        segments.forEach(segment -> segmentArray.put(segmentConverter.convert(
                converterConfigBuilder.build(request, true), segment))
        );
        jsonObject.put(SegmentFields.SEGMENT.getName(), segmentArray);

        response.put(SegmentFields.LIST.getName(), jsonObject);
        final JsonObject sortJson = updatedFilterPreference.getSortJson();

        if (Objects.nonNull(sortJson) && !Json.isEmpty(sortJson)) {
            response.put(SegmentFields.SORT_CONFIG.getName(), sortJson);
        }
        final JsonObject limitJson = updatedFilterPreference.getLimitJson();

        if (Objects.nonNull(limitJson) && !Json.isEmpty(limitJson)) {
            response.put(ListActionFields.LIMIT, limitJson);
        }
        response.put(SegmentFields.TOTAL_RECORDS.getName(), totalRecords);
        response.put(SegmentFields.HAS_MORE.getName(), updatedFilterPreference.getStart() + segments.size() <= totalRecords);
    }

    /**
     * <p>
     * Returns the current computation state of the segment resolved from the request.
     * </p>
     *
     * @param request  Represents the CRM request.
     * @param response Represents the CRM response.
     */
    private void checkStatus(final CrmRequest request, final CrmResponse response) {
        final JsonObject data = request.getRequestJson().getJsonObject(SegmentFields.DATA.getName());
        final String idKey = SegmentFields.ID.getName();
        final Long segmentId = data.containsKey(idKey) ? data.getLong(idKey) : null;

        if (Objects.isNull(segmentId)) {
            throw ErrorCode.get(SegmentErrorCodes.INVALID_SEGMENT_ID, "i18n.segment.invalid.id");
        }
        final Segment.ComputationState computationState =
                this.segmentService.getComputationState(request.orgId(), segmentId);

        if (Objects.isNull(computationState)) {
            throw ErrorCode.get(SegmentErrorCodes.SEGMENT_NOT_FOUND, "i18n.segment.not.found");
        }
        response.put(SegmentFields.COMPUTATION_STATE.getName(), this.resolveState(computationState).name());
    }

    /**
     * <p>
     * Resolves the computation state to a simplified state for response purposes.
     * </p>
     *
     * @param state The current {@link Segment.ComputationState}.
     * @return The resolved {@link Segment.ComputationState} for response.
     */
    private Segment.ComputationState resolveState(final Segment.ComputationState state) {
        return switch (state) {
            case UNCOMPUTED, COMPUTING -> Segment.ComputationState.COMPUTING;
            case COMPUTED, FAILED -> Segment.ComputationState.COMPUTED;
        };
    }

    /**
     * <p>
     * gets the limit data based on the organization's current subscription plan
     * </p>
     *
     * @param orgId Represents the organization id
     */
    private int getAllowedLimit(final Long orgId) {
        final Optional<OrgSubscriptionPlan> optionalOrgSubscriptionPlan =
                orgPlanManager.getCurrentPlans(orgId, PlanType.ORDINARY).stream().findFirst();

        return optionalOrgSubscriptionPlan.isPresent() ?
                getLimit(optionalOrgSubscriptionPlan.get().getPlan().getOrder()) : NumericalConstant.TEN;
    }

    /**
     * <p>
     * Determines the segment limit based on the subscription plan order.
     * </p>
     *
     * @param order Represents the subscription plan order
     * @return the segment limit corresponding to the given plan order
     */
    private short getLimit(final Short order) {
        return switch (order) {
            case 1 -> NumericalConstant.FIFTY;
            case 2 -> NumericalConstant.ONE_HUNDRED;
            case 3 -> NumericalConstant.FIVE_HUNDRED;
            default -> NumericalConstant.TEN;
        };
    }

    /**
     * <p>
     * Override delete to also clean up any FilterCriteria rows that were created
     * for the deleted segments and are not referenced by other segments or filters.
     * </p>
     */
    @Override
    public void delete(final CrmResponse crmResponse, final Long orgId, final Long userId,
                       final Collection<Segment> segments, final EventData eventData) {
        final Collection<ErrorDescriptor> errors = validate(segments, Group.Delete.class);
        final Action action = Action.DELETE;

        if (errors.isEmpty() && isValidDetails(orgId, userId, action, segments)) {

            try {
                getConfigManager().bulkDelete(orgId, segments);
                deleteCriteria(orgId, segments);
            } catch (ErrorCode errorCode) {
                crmResponse.setErrorCode(errorCode);
                crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode)));
            }
        } else {
            final ErrorCode errorCode = ErrorCode.get(CommonErrorCodes.DELETE_ACTION_FAILED, "i18n.common.delete.failed");

            crmResponse.setErrorCode(errorCode);
            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode,
                    ErrorKeys.DELETE_ACTION_DATA_VALIDATION_FAILED), errors));
        }
    }

    /**
     * <p>
     * Collects the ids of any {@link FilterCriteria} referenced by the given segments'
     * condition groups and delegates to the filter manager to delete those that are
     * no longer referenced by other segments or filters.
     * </p>
     *
     * @param orgId    the id of the organization the segments belong to
     * @param segments the segments whose referenced filter criteria are candidates for deletion
     */
    private void deleteCriteria(final Long orgId, final Collection<Segment> segments) {
        final Collection<Long> candidateCriteriaIds = new HashSet<>();

        segments.forEach(segment -> {
            final Collection<SegmentConditionGroup> segmentConditionGroups = segment.getSegmentConditionGroups();

            if (Objects.nonNull(segmentConditionGroups)) {

                segmentConditionGroups.forEach(group -> {
                    final Collection<SegmentCondition> segmentConditions = group.getConditions();

                    if (Objects.isNull(segmentConditions)) {
                        return;
                    }
                    segmentConditions.forEach(condition -> {
                        final SegmentConditionCriteria segmentConditionCriteria = condition.getConditionCriteria();
                        final FilterCriteria filterCriteria;
                        final Long filterCriteriaId;

                        if (Objects.nonNull(segmentConditionCriteria)
                                && Objects.nonNull(filterCriteria = segmentConditionCriteria.getFilterCriteria())
                                && Objects.nonNull(filterCriteriaId = filterCriteria.getId())) {
                            candidateCriteriaIds.add(filterCriteriaId);
                        }
                    });
                });
            }
        });

        filterManager.deleteCriteria(orgId, candidateCriteriaIds);
    }
}