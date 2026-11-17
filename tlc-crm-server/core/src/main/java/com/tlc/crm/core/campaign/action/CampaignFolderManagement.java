package com.tlc.crm.core.campaign.action;

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
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.campaign.fields.CampaignFields;
import com.tlc.crm.core.campaign.converter.CampaignFilterPreferenceConverter;
import com.tlc.crm.core.campaign.converter.CampaignFolderConverter;
import com.tlc.crm.core.campaign.manager.CampaignFolderManager;
import com.tlc.crm.core.campaign.manager.CampaignManager;
import com.tlc.crm.core.campaign.model.CampaignFilterGroup;
import com.tlc.crm.core.campaign.model.CampaignFilterUserPreference;
import com.tlc.crm.core.campaign.model.CampaignFolder;
import com.tlc.crm.core.campaign.model.CampaignCategory;
import com.tlc.crm.core.campaign.service.CampaignFilterPreferenceService;

import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.campaign.status.CampaignErrorCodes;

import com.tlc.crm.plan.and.billing.api.manager.OrgPlanManager;
import com.tlc.crm.plan.and.billing.api.models.OrgSubscriptionPlan;
import com.tlc.plans.and.billings.models.PlanType;

import com.tlc.web.WebAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages campaign folder CRUD actions.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
@WebAction(path = "/campaign/folder/mgmt")
public class CampaignFolderManagement extends CrmConfigAction<CampaignFolder> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CampaignFolderManagement.class);

    private final CampaignFolderManager campaignFolderManager;
    private final CampaignManager campaignManager;
    private final CampaignFolderConverter campaignFolderConverter;
    private final CampaignFilterPreferenceConverter campaignFilterPreferenceConverter;
    private final ConverterConfigBuilder converterConfigBuilder;
    private final CampaignFilterPreferenceService campaignFilterPreferenceService;
    private final OrgPlanManager orgPlanManager;

    /**
     * <p>
     * Constructs the {@link CampaignFolderManagement}.
     * </p>
     */
    public CampaignFolderManagement() {
        campaignFolderManager = CampaignFolderManager.getInstance();
        campaignManager = CampaignManager.getInstance();
        campaignFolderConverter = CampaignFolderConverter.getInstance();
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        campaignFilterPreferenceConverter = CampaignFilterPreferenceConverter.getInstance();
        campaignFilterPreferenceService = CampaignFilterPreferenceService.getInstance();
        orgPlanManager = OrgPlanManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Represents the {@link CrmRequest}.
     * @param response Represents the {@link CrmResponse}.
     */
    @Override
    public void handle(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(CampaignFields.TYPE);
        final JsonObject data = requestJson.getJsonObject(CampaignFields.DATA);

        switch (type) {
            case "list" -> list(request, response);
            case "rename" -> rename(data, request, response);
        }
    }

    /**
     * <p>
     * Lists the campaign folders.
     * </p>
     *
     * @param request  Represents the {@link CrmRequest}.
     * @param response Represents the {@link CrmResponse}.
     */
    private void list(final CrmRequest request, final CrmResponse response) {
        final Long orgId = request.orgId();
        final JsonObject data = request.getRequestJson().getJsonObject(CampaignFields.DATA);
        final JsonObject criteriaJson = data.optJsonObject(CampaignFields.CRITERIA);
        final CampaignFilterUserPreference campaignFilterPreference = campaignFilterPreferenceConverter.convert(converterConfigBuilder.build(request,
                false), criteriaJson);

        campaignFilterPreference.setSearchText(data.optString(CampaignFields.SEARCH_TEXT, null));
        campaignFilterPreference.setCategory(CampaignCategory.get(data.getShort(CampaignFields.CATEGORY)));
        campaignFilterPreference.setSortJson(data.optJsonObject(ListActionFields.SORT));
        campaignFilterPreference.setStart(data.optInt(ListActionFields.START, 0));
        campaignFilterPreference.setLimit(data.optInt(ListActionFields.LIMIT, 0));

        campaignFilterPreferenceService.upsert(campaignFilterPreference);
        final long totalRecords = campaignFolderManager.getTotalCount(orgId, campaignFilterPreference);
        final Collection<CampaignFolder> folders = totalRecords == 0 ? Collections.emptyList() : campaignFolderManager.get(orgId, campaignFilterPreference);
        final JsonArray folderArray = Json.array();
        final Collection<Long> folderIds = folders.stream().map(CampaignFolder::id).collect(Collectors.toUnmodifiableSet());
        final Map<Long, Long> campaignCounts = folderIds.isEmpty() ? Collections.emptyMap() : campaignManager.getCampaignCounts(orgId, folderIds);

        folders.forEach(folder -> {
            folder.setCampaignCount(campaignCounts.getOrDefault(folder.id(), 0L));
            final JsonObject folderJson = campaignFolderConverter.convertToJson(new CrmConverterData<>(request.getUser(), request.locale(), folder));
            final JsonObject criteria = Json.object();

            criteria.put(CampaignFields.GROUP, Json.object().put(CampaignFields.ID, folder.isStarred()
                    ? CampaignFilterGroup.STARRED_FOLDERS.getId() : CampaignFilterGroup.FOLDERS.getId()));
            criteria.put(CampaignFields.SUBGROUP, Json.object().put(CampaignFields.ID, folder.id()));
            folderJson.put(CampaignFields.CRITERIA, criteria);

            folderArray.put(folderJson);
        });
        response.put(CampaignFields.LIST, folderArray);
        response.put(CampaignFields.TOTAL_RECORDS, totalRecords);
        response.put(CampaignFields.HAS_MORE, campaignFilterPreference.getStart() + folders.size() <= totalRecords);
    }

    /**
     * <p>
     * Renames the given folder.
     * </p>
     *
     * @param data     Represents the request data.
     * @param request  Represents the {@link CrmRequest}.
     * @param response Represents the {@link CrmResponse}.
     */
    private void rename(final JsonObject data, final CrmRequest request, final CrmResponse response) {
        final Long orgId = request.orgId();
        final Long folderId = data.getLong(CampaignFields.ID);
        final String folderName = data.getString(CampaignFields.NAME);
        final CampaignCategory category = CampaignCategory.get(data.getShort(CampaignFields.CATEGORY));

        if (campaignFolderManager.existsByName(orgId, category, folderId, folderName)) {
            throw ErrorCode.get(CampaignErrorCodes.CAMPAIGN_FOLDER_NAME_ALREADY_EXISTS, "i18n.campaign.folder.name.already.exists");
        }
        campaignFolderManager.rename(orgId, request.userId(), folderId, folderName);
        response.put(ActionFields.MESSAGE, response.resolveI18nString("i18n.campaign.folder.renamed.successfully"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigManager<CampaignFolder> getConfigManager() {
        return campaignFolderManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param request    Crm request
     * @param jsonObject Holds folder details
     * @return {@link CampaignFolder}
     */
    @Override
    public CampaignFolder convert(final CrmRequest request, final JsonObject jsonObject) {
        return campaignFolderConverter.convertToModel(new CrmConverterData<>(request.getUser(), request.locale(), jsonObject));
    }

    /**
     * {@inheritDoc}
     *
     * @param request    Represents the {@link CrmRequest}.
     * @param jsonObject Represents the {@link JsonObject}.
     * @return {@link CampaignFolder}
     */
    @Override
    public CampaignFolder convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        return campaignFolderConverter.convertToModel(new CrmConverterData<>(request.getUser(), request.locale(),
                true, true, jsonObject));
    }

    /**
     * {@inheritDoc}
     *
     * @param request Represents the {@link CrmRequest}.
     * @param folder  Represents the {@link CampaignFolder}.
     * @return {@link JsonObject}
     */
    @Override
    protected JsonObject convert(final CrmRequest request, final CampaignFolder folder) {
        return campaignFolderConverter.convertToJson(new CrmConverterData<>(request.getUser(), request.locale(), folder));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents the organization id.
     * @param userId  Represents the user id.
     * @param action  Represents the action.
     * @param folders Represents the {@link Collection} of {@link CampaignFolder}.
     * @return true if the entity is valid, false otherwise
     */
    @Override
    protected boolean isValidDetails(final Long orgId, final Long userId, final Action action, final Collection<CampaignFolder> folders) {
        if (action.isCreateOrUpdate() && folders != null && !folders.isEmpty()) {
            final CampaignFolder folder = folders.stream().findFirst().get();
            final CampaignCategory category = folder.getCategory();
            final String folderName = folder.getName();

            if (action.isCreate()) {
                final CampaignFilterUserPreference preference = new CampaignFilterUserPreference();

                preference.setCategory(category);

                if (!((campaignFolderManager.getTotalCount(orgId, preference) + folders.size()) <= getAllowedLimit(orgId))) {
                    LOGGER.error("Campaign: Folder creation failed due to exceeding the allowed limit for organization. Org Id: {}", orgId);
                    return false;
                }

                if (campaignFolderManager.existsByName(orgId, category, null, folderName)) {
                    throw ErrorCode.get(CampaignErrorCodes.CAMPAIGN_FOLDER_NAME_ALREADY_EXISTS, "i18n.campaign.folder.name.already.exists");
                }
            }

            if (action.isUpdate() && campaignFolderManager.existsByName(orgId, category, folder.id(), folderName)) {
                throw ErrorCode.get(CampaignErrorCodes.CAMPAIGN_FOLDER_NAME_ALREADY_EXISTS, "i18n.campaign.folder.name.already.exists");
            }
        }

        return true;
    }

    /**
     * <p>
     * Gets the allowed folder limit based on the organization's current subscription plan.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @return Allowed folder limit.
     */
    private int getAllowedLimit(final Long orgId) {
        final Optional<OrgSubscriptionPlan> optionalOrgSubscriptionPlan = orgPlanManager.getCurrentPlans(orgId, PlanType.ORDINARY)
                .stream().findFirst();

        return optionalOrgSubscriptionPlan.isPresent() ?
                getLimit(optionalOrgSubscriptionPlan.get().getPlan().getOrder()) : NumericalConstant.ONE;
    }

    /**
     * <p>
     * Determines the campaign folder limit based on the subscription plan order.
     * </p>
     *
     * @param order Represents the subscription plan order.
     * @return The campaign folder limit corresponding to the given plan order.
     */
    private short getLimit(final Short order) {
        return switch (order) {
            case 1 -> NumericalConstant.TWENTY;
            case 2 -> NumericalConstant.FIFTY;
            case 3 -> NumericalConstant.ONE_HUNDRED;
            default -> NumericalConstant.TEN;
        };
    }
}