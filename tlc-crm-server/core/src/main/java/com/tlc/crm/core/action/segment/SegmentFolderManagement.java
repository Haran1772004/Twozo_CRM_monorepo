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
import com.tlc.crm.common.converter.CrmConverterData;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.internal.status.SegmentErrorCodes;
import com.tlc.crm.core.segment.manager.SegmentFolderManager;
import com.tlc.crm.core.segment.models.SegmentFilterUserPreference;
import com.tlc.crm.core.segment.models.SegmentFolder;
import com.tlc.crm.core.segment.converter.SegmentFilterPreferenceConverter;
import com.tlc.crm.core.segment.converter.SegmentFolderConverter;
import com.tlc.crm.core.segment.fields.SegmentFields;
import com.tlc.crm.core.segment.fields.SegmentFolderFields;
import com.tlc.crm.core.segment.service.SegmentFilterPreferenceService;
import com.tlc.crm.plan.and.billing.api.manager.OrgPlanManager;
import com.tlc.crm.plan.and.billing.api.models.OrgSubscriptionPlan;
import com.tlc.crm.security.permission.annotation.authorizer.MarketingCampaignAuthorizer;
import com.tlc.plans.and.billings.models.PlanType;
import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Manages segment folder CRUD actions.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@MarketingCampaignAuthorizer
@WebAction(path = "/segment/folder/mgmt")
public class SegmentFolderManagement extends CrmConfigAction<SegmentFolder> {

    private final SegmentFolderManager segmentFolderManager;
    private final SegmentFolderConverter segmentFolderConverter;
    private final SegmentFilterPreferenceConverter segmentFilterPreferenceConverter;
    private final ConverterConfigBuilder converterConfigBuilder;
    private final SegmentFilterPreferenceService segmentFilterPreferenceService;
    private final OrgPlanManager orgPlanManager;

    public SegmentFolderManagement() {
        segmentFolderManager = SegmentFolderManager.getInstance();
        segmentFolderConverter = SegmentFolderConverter.getInstance();
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        segmentFilterPreferenceConverter = SegmentFilterPreferenceConverter.getInstance();
        segmentFilterPreferenceService = SegmentFilterPreferenceService.getInstance();
        this.orgPlanManager = OrgPlanManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Represents the {@link CrmRequest}.
     * @param response Represents the {@link CrmResponse}.
     */
    @Override
    public void handle(final CrmRequest request, final CrmResponse response) {
        final String type = request.getRequestJson().getString(SegmentFolderFields.TYPE.getName());

        switch (type) {
            case "list" -> list(request, response);
            default -> throw ErrorCode.get(SegmentErrorCodes.UNSUPPORTED_ACTION, "i18n.segment.unsupported.action");
        }
    }

    /**
     * <p>
     * Lists the segment folders.
     * </p>
     *
     * @param request  Represents the {@link CrmRequest}.
     * @param response Represents the {@link CrmResponse}.
     */
    private void list(final CrmRequest request, final CrmResponse response) {
        final Long orgId = request.orgId();
        final JsonObject data = request.getRequestJson().getJsonObject(SegmentFields.DATA.getName());
        final JsonObject criteriaJson = data.optJsonObject(SegmentFields.CRITERIA.getName());
        final SegmentFilterUserPreference segmentFilterPreference = segmentFilterPreferenceConverter.convert(
                converterConfigBuilder.build(request, false),
                criteriaJson
        );

        segmentFilterPreferenceConverter.populateDefaultPreferences(segmentFilterPreference, criteriaJson);
        final SegmentFilterUserPreference updatedFilterPreference = segmentFilterPreferenceService.upsert(
                orgId, request.userId(), segmentFilterPreference
        );
        final long totalRecords = segmentFolderManager.getTotalCount(orgId, updatedFilterPreference);
        final Collection<SegmentFolder> folders = totalRecords == 0 ? Collections.emptyList() :
                segmentFolderManager.get(orgId, updatedFilterPreference);
        final JsonArray folderArray = Json.array();
        final CrmUser crmUser = request.getUser();
        final Locale locale = request.locale();

        folders.forEach(folder -> folderArray.put(segmentFolderConverter.convertToJson(
                new CrmConverterData<>(crmUser, locale, folder)
        )));
        response.put(SegmentFields.LIST.getName(), Json.object()
                .put(SegmentFolderFields.SEGMENT_FOLDERS.getName(), folderArray));
        final JsonObject sortJson = updatedFilterPreference.getSortJson();

        if (Objects.nonNull(sortJson) && !Json.isEmpty(sortJson)) {
            response.put(SegmentFields.SORT_CONFIG.getName(), sortJson);
        }
        final JsonObject limitJson = updatedFilterPreference.getLimitJson();

        if (Objects.nonNull(limitJson) && !Json.isEmpty(limitJson)) {
            response.put(ListActionFields.LIMIT, limitJson);
        }
        response.put(SegmentFields.TOTAL_RECORDS.getName(), totalRecords);
        response.put(SegmentFields.HAS_MORE.getName(), updatedFilterPreference.getStart() + folders.size() <= totalRecords);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link SegmentFolderManager}.
     */
    @Override
    public ConfigManager<SegmentFolder> getConfigManager() {
        return segmentFolderManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param request    Crm request.
     * @param jsonObject Holds folder details.
     * @return {@link SegmentFolder}.
     */
    @Override
    public SegmentFolder convert(final CrmRequest request, final JsonObject jsonObject) {
        return segmentFolderConverter.convertToModel(new CrmConverterData<>(request.getUser(), request.locale(), jsonObject));
    }

    /**
     * {@inheritDoc}
     *
     * @param request    Represents the {@link CrmRequest}.
     * @param jsonObject Represents folder partial details.
     * @return {@link SegmentFolder}.
     */
    @Override
    public SegmentFolder convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        return segmentFolderConverter.convertToModel(new CrmConverterData<>(
                request.getUser(), request.locale(), true, true, jsonObject
        ));
    }

    /**
     * {@inheritDoc}
     *
     * @param request Represents the {@link CrmRequest}.
     * @param folder  Represents the {@link SegmentFolder}.
     * @return {@link JsonObject}.
     */
    @Override
    protected JsonObject convert(final CrmRequest request, final SegmentFolder folder) {
        return segmentFolderConverter.convertToJson(new CrmConverterData<>(request.getUser(), request.locale(), folder));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Organization id.
     * @param userId   Represents the user id.
     * @param action   Perform action like create, update or delete.
     * @param folders  Collection of folders.
     * @param response Represents the {@link CrmResponse}.
     * @return {@code true} if folder details are valid.
     */
    @Override
    protected boolean isValidDetails(final Long orgId, final Long userId, final Action action, final Collection<SegmentFolder> folders,
                                     final CrmResponse response) {
        if (action.isCreateOrUpdate() && folders != null && !folders.isEmpty()) {

            if (action.isCreate()) {
                final int allowedLimit = getAllowedLimit(orgId);

                if ((segmentFolderManager.getTotalCount(orgId, null) + folders.size()) > allowedLimit) {
                    final String message = response.resolveI18nString("i18n.segment.folder.limit.reached");

                    throw ErrorCode.get(SegmentErrorCodes.SEGMENT_FOLDER_LIMIT_REACHED, String.format(message, allowedLimit));
                }
            }
            final Collection<String> names = new HashSet<>();
            final Collection<Long> ids = new HashSet<>();

            for (final SegmentFolder folder : folders) {
                if (Objects.isNull(folder)) {
                    continue;
                }
                final String name = folder.getName();

                if (Objects.nonNull(name)) {
                    if (!names.add(name)) {
                        throw ErrorCode.get(SegmentErrorCodes.SEGMENT_FOLDER_NAME_ALREADY_EXISTS, "i18n.segment.folder.name.already.exists");
                    }
                }
                final Long id = folder.id();

                if (Objects.nonNull(id)) {
                    ids.add(id);
                }
            }

            if (!names.isEmpty()) {
                if (action.isCreate() && segmentFolderManager.existsByName(orgId, names)) {
                    throw ErrorCode.get(SegmentErrorCodes.SEGMENT_FOLDER_NAME_ALREADY_EXISTS, "i18n.segment.folder.name.already.exists");
                }

                if (action.isUpdate() && segmentFolderManager.existsByName(orgId, ids, names)) {
                    throw ErrorCode.get(SegmentErrorCodes.SEGMENT_FOLDER_NAME_ALREADY_EXISTS, "i18n.segment.folder.name.already.exists");
                }
            }
        }

        return true;
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
                getLimit(optionalOrgSubscriptionPlan.get().getPlan().getOrder()) : NumericalConstant.ONE;
    }

    /**
     * <p>
     * Determines the segment folder limit based on the subscription plan order.
     * </p>
     *
     * @param order Represents the subscription plan order
     * @return the segment folder limit corresponding to the given plan order
     */
    private short getLimit(final Short order) {
        return switch (order) {
            case 1 -> NumericalConstant.TEN;
            case 2 -> NumericalConstant.TWENTY;
            case 3 -> NumericalConstant.FIFTY;
            default -> NumericalConstant.FIVE;
        };
    }
}