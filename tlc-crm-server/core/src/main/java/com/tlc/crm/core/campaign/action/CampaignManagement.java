package com.tlc.crm.core.campaign.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmEventRequest;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.converter.CrmConverterData;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.fields.ActionFields;

import com.tlc.crm.core.campaign.converter.CampaignConverter;
import com.tlc.crm.core.campaign.converter.CampaignFilterPreferenceConverter;
import com.tlc.crm.core.campaign.fields.CampaignFields;
import com.tlc.crm.core.campaign.manager.CampaignFolderManager;
import com.tlc.crm.core.campaign.manager.CampaignManager;
import com.tlc.crm.core.campaign.model.Campaign;
import com.tlc.crm.core.campaign.model.CampaignCategory;
import com.tlc.crm.core.campaign.service.CampaignFilterPreferenceService;
import com.tlc.crm.entity.action.AsyncBulkConfigAction;
import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.campaign.status.CampaignErrorCodes;
import com.tlc.crm.security.permission.annotation.authorizer.MarketingCampaignAuthorizer;
import com.tlc.web.WebAction;

import java.util.List;

/**
 * <p>
 * Manages campaign related web actions.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
@MarketingCampaignAuthorizer
@WebAction(path = "/campaign/mgmt")
public class CampaignManagement extends AsyncBulkConfigAction<Campaign> {

    private static final String CONFIG_ACTION_NAME = "campaign";

    private final CampaignManager campaignManager;
    private final CampaignConverter campaignConverter;
    private final CampaignFilterPreferenceConverter campaignFilterPreferenceConverter;
    private final ConverterConfigBuilder converterConfigBuilder;
    private final CampaignFilterPreferenceService campaignFilterPreferenceService;
    private final CampaignFolderManager campaignFolderManager;

    /**
     * <p>
     * Constructs the {@link CampaignManagement}.
     * </p>
     */
    public CampaignManagement() {
        campaignManager = CampaignManager.getInstance();
        campaignConverter = CampaignConverter.getInstance();
        campaignFilterPreferenceConverter = CampaignFilterPreferenceConverter.getInstance();
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        campaignFilterPreferenceService = CampaignFilterPreferenceService.getInstance();
        campaignFolderManager = CampaignFolderManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request     Represents the {@link CrmRequest}.
     * @param crmResponse Represents the {@link CrmResponse}.
     */
    @Override
    protected void secureProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString("type");
        final JsonObject data = requestJson.getJsonObject("data");
        final Long orgId = request.orgId();
        final Long userId = request.userId();
        final EventData eventData = ((CrmEventRequest) request).getEventData();

        switch (type) {
            case "getFilter" -> loadFilterPreference(orgId, data, request, crmResponse);
            case "rename" -> rename(data, request, crmResponse);
            case "archive" -> archive(orgId, data, crmResponse);
            case "unarchive" -> unarchive(orgId, data, crmResponse);
            case "addToFolder" -> addToFolder(orgId, userId, data, crmResponse);
            case "removeFromFolder" -> removeFromFolder(orgId, userId, data, crmResponse);
            case "stopSending" -> stopSending(orgId, userId, data, crmResponse);
            case "pause" -> pause(orgId, userId, data, crmResponse);
            case "reschedule" -> reschedule(orgId, userId, data, crmResponse);
            case "delete" -> delete(orgId, userId, data, crmResponse, eventData);
            case "bAddToFolder", "bArchive", "bUnarchive", "bDelete", "bRemoveFromFolder" ->
                    bUpdate(request, crmResponse, ((CrmEventRequest) request).getEventData(), false);
            default -> handle(request, crmResponse);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Represents the {@link CrmRequest}.
     * @param response Represents the {@link CrmResponse}.
     */
    @Override
    public void handle(final CrmRequest request, final CrmResponse response) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getConfigActionName() {
        return CONFIG_ACTION_NAME;
    }

    /**
     * <p>
     * Loads the campaign filter preference for the current user and category.
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param data     Represents the request data.
     * @param request  Represents the {@link CrmRequest}.
     * @param response Represents the {@link CrmResponse}.
     */
    private void loadFilterPreference(final Long orgId, final JsonObject data, final CrmRequest request, final CrmResponse response) {
        final CampaignCategory category = CampaignCategory.get(data.getShort(CampaignFields.CATEGORY));

        response.put(CampaignFields.FILTER, campaignFilterPreferenceConverter.convert(converterConfigBuilder.build(request, false),
                campaignFilterPreferenceService.get(orgId, request.userId(), category)));
    }

    /**
     * <p>
     * Renames the given campaign.
     * </p>
     *
     * @param data     Represents the request data.
     * @param request  Represents the {@link CrmRequest}.
     * @param response Represents the {@link CrmResponse}.
     */
    private void rename(final JsonObject data, final CrmRequest request, final CrmResponse response) {
        final Long orgId = request.orgId();
        final Long campaignId = data.getLong(CampaignFields.ID);
        final String campaignName = data.getString(CampaignFields.NAME);
        final CampaignCategory category = CampaignCategory.get(data.getShort(CampaignFields.CATEGORY));

        if (campaignManager.existsByName(orgId, category, campaignId, campaignName)) {
            throw ErrorCode.get(CampaignErrorCodes.CAMPAIGN_NAME_ALREADY_EXISTS, "i18n.campaign.name.already.exists");
        }
        campaignManager.rename(orgId, request.userId(), campaignId, campaignName);
        response.put(ActionFields.MESSAGE, response.resolveI18nString("i18n.campaign.renamed.successfully"));
    }

    /**
     * <p>
     * Archives the given campaign.
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param data     Represents the request data.
     * @param response Represents the {@link CrmResponse}.
     */
    private void archive(final Long orgId, final JsonObject data, final CrmResponse response) {
        final Long campaignId = data.getLong(CampaignFields.ID);

        if (!campaignManager.exists(orgId, campaignId)) {
            throw ErrorCode.get(CampaignErrorCodes.CAMPAIGN_NOT_FOUND, "i18n.campaign.not.found");
        }

        campaignManager.updateArchiveState(orgId, campaignId, true);
        response.put(ActionFields.MESSAGE, response.resolveI18nString("i18n.campaign.archived.successfully"));

    }

    /**
     * <p>
     * Unarchives the given campaign.
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param data     Represents the request data.
     * @param response Represents the {@link CrmResponse}.
     */
    private void unarchive(final Long orgId, final JsonObject data, final CrmResponse response) {
        final Long campaignId = data.getLong(CampaignFields.ID);

        if (!campaignManager.exists(orgId, campaignId)) {
            throw ErrorCode.get(CampaignErrorCodes.CAMPAIGN_NOT_FOUND, "i18n.campaign.not.found");
        }

        campaignManager.updateArchiveState(orgId, campaignId, false);
        response.put(ActionFields.MESSAGE, response.resolveI18nString("i18n.campaign.unarchived.successfully"));
    }

    /**
     * <p>
     * Adds the campaign to a folder.
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param userId   Represents the user id.
     * @param data     Represents the request data.
     * @param response Represents the {@link CrmResponse}.
     */
    private void addToFolder(final Long orgId, final Long userId, final JsonObject data, final CrmResponse response) {
        final Long campaignId = data.getLong(CampaignFields.ID);
        final CampaignCategory category = CampaignCategory.get(data.getShort(CampaignFields.CATEGORY));
        final Long folderId = data.getLong(CampaignFields.FOLDER_ID);

        if (!campaignManager.exists(orgId, campaignId)) {
            throw ErrorCode.get(CampaignErrorCodes.CAMPAIGN_NOT_FOUND, "i18n.campaign.not.found");
        }

        if (!campaignFolderManager.exists(orgId, category, folderId)) {
            throw ErrorCode.get(CampaignErrorCodes.CAMPAIGN_FOLDER_NOT_FOUND, "i18n.campaign.folder.not.found");
        }

        campaignManager.addToFolder(orgId, userId, campaignId, folderId);
        response.put(ActionFields.MESSAGE, response.resolveI18nString("i18n.campaign.added.to.folder.successfully"));
    }

    /**
     * <p>
     * Removes the campaign from its folder.
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param userId   Represents the user id.
     * @param data     Represents the request data.
     * @param response Represents the {@link CrmResponse}.
     */
    private void removeFromFolder(final Long orgId, final Long userId, final JsonObject data, final CrmResponse response) {
        final Long campaignId = data.getLong(CampaignFields.ID);

        if (!campaignManager.exists(orgId, campaignId)) {
            throw ErrorCode.get(CampaignErrorCodes.CAMPAIGN_NOT_FOUND, "i18n.campaign.not.found");
        }

        campaignManager.removeFromFolder(orgId, userId, campaignId);
        response.put(ActionFields.MESSAGE, response.resolveI18nString("i18n.campaign.removed.from.folder.successfully"));
    }

    /**
     * <p>
     * Stops sending for the given campaign.
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param userId   Represents the user id.
     * @param data     Represents the request data.
     * @param response Represents the {@link CrmResponse}.
     */
    private void stopSending(final Long orgId, final Long userId, final JsonObject data, final CrmResponse response) {
        final Long campaignId = data.getLong(CampaignFields.ID);

        campaignManager.getStatus(orgId, campaignId).ifPresentOrElse(campaignStatus -> {

            if (campaignStatus.isInProgress()) {
                campaignManager.stopSending(orgId, userId, campaignId);
            } else {
                throw ErrorCode.get(CampaignErrorCodes.FAILED_TO_STOP_CAMPAIGN, "i18n.campaign.invalid.status");
            }
        }, () -> {
            throw ErrorCode.get(CampaignErrorCodes.CAMPAIGN_NOT_FOUND, "i18n.campaign.not.found");
        });

        response.put(ActionFields.MESSAGE, response.resolveI18nString("i18n.campaign.stopped.successfully"));
    }

    /**
     * <p>
     * Pauses a scheduled campaign.
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param userId   Represents the user id.
     * @param data     Represents the request data.
     * @param response Represents the {@link CrmResponse}.
     */
    private void pause(final Long orgId, final Long userId, final JsonObject data, final CrmResponse response) {
        final Long campaignId = data.getLong(CampaignFields.ID);

        campaignManager.getStatus(orgId, campaignId).ifPresentOrElse(campaignStatus -> {

            if (campaignStatus.isScheduled()) {
                campaignManager.pause(orgId, userId, campaignId);
            } else {
                throw ErrorCode.get(CampaignErrorCodes.FAILED_TO_PAUSE_CAMPAIGN, "i18n.campaign.invalid.status");
            }
        }, () -> {
            throw ErrorCode.get(CampaignErrorCodes.CAMPAIGN_NOT_FOUND, "i18n.campaign.not.found");
        });

        response.put(ActionFields.MESSAGE, response.resolveI18nString("i18n.campaign.paused.successfully"));
    }

    /**
     * <p>
     * Reschedules the given campaign.
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param userId   Represents the user id.
     * @param data     Represents the request data.
     * @param response Represents the {@link CrmResponse}.
     */
    private void reschedule(final Long orgId, final Long userId, final JsonObject data, final CrmResponse response) {
        final Long campaignId = data.getLong(CampaignFields.ID);
        final long scheduledTime = data.getLong(CampaignFields.SCHEDULED_TIME);
        final int timeZoneId = data.getInt(CampaignFields.TIME_ZONE_ID);

        campaignManager.getStatus(orgId, campaignId).ifPresentOrElse(campaignStatus -> {

            if (campaignStatus.isScheduled()) {
                campaignManager.reschedule(orgId, userId, campaignId, scheduledTime, timeZoneId);
            } else {
                throw ErrorCode.get(CampaignErrorCodes.FAILED_TO_RESCHEDULE_CAMPAIGN, "i18n.campaign.invalid.status");
            }
        }, () -> {
            throw ErrorCode.get(CampaignErrorCodes.CAMPAIGN_NOT_FOUND, "i18n.campaign.not.found");
        });

        response.put(ActionFields.MESSAGE, response.resolveI18nString("i18n.campaign.rescheduled.successfully"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigManager<Campaign> getConfigManager() {
        return campaignManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param request    Represents the {@link CrmRequest}.
     * @param jsonObject Represents the {@link JsonObject}.
     * @return {@link Campaign}
     */ 
    @Override
    public Campaign convert(final CrmRequest request, final JsonObject jsonObject) {
        return campaignConverter.convertToModel(new CrmConverterData<>(request.getUser(), request.locale(), jsonObject));
    }

    /**
     * {@inheritDoc}
     *
     * @param request    Represents the {@link CrmRequest}.
     * @param jsonObject Represents the {@link JsonObject}.
     * @return {@link Campaign}
     */
    @Override
    public Campaign convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        return campaignConverter.convertToModel(new CrmConverterData<>(request.getUser(), request.locale(),
                true, true, jsonObject));
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Represents the {@link CrmRequest}.
     * @param campaign Represents the {@link Campaign}.
     * @return {@link JsonObject}
     */
    @Override
    protected JsonObject convert(final CrmRequest request, final Campaign campaign) {
        return campaignConverter.convertToJson(new CrmConverterData<>(request.getUser(), request.locale(), campaign));
    }

    /**
     * <p>
     * Deletes the campaign
     * </p>
     *
     * @param orgId     Represents the organization id
     * @param userId    Represents the user id
     * @param data      Represents the request data
     * @param response  Represents the {@link CrmResponse}
     * @param eventData Represents the {@link EventData}
     */
    public void delete(final Long orgId, final Long userId, final JsonObject data, final CrmResponse response, final EventData eventData) {
        final Long campaignId = data.getLong(CampaignFields.ID);

        if (!campaignManager.exists(orgId, campaignId)) {
            throw ErrorCode.get(CampaignErrorCodes.CAMPAIGN_NOT_FOUND, "i18n.campaign.not.found");
        }

        campaignManager.getStatus(orgId, campaignId).ifPresentOrElse(campaignStatus -> {

            if (campaignStatus.isDeletable()) {
                final Campaign campaign = new Campaign();

                campaign.setId(campaignId);
                campaign.setOrgId(orgId);
                delete(response, orgId, userId, List.of(campaign), eventData);
            } else {
                throw ErrorCode.get(CampaignErrorCodes.CAMPAIGN_DELETION_FAILED, "i18n.campaign.deletion.failed");
            }
        }, () -> {
            throw ErrorCode.get(CampaignErrorCodes.CAMPAIGN_NOT_FOUND, "i18n.campaign.not.found");
        });
    }
}