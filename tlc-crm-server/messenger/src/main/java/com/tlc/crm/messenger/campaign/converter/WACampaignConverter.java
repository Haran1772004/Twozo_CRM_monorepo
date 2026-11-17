package com.tlc.crm.messenger.campaign.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.converter.CrmConverterData;
import com.tlc.crm.common.converter.CrmModelConverter;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.core.campaign.converter.CampaignConverter;
import com.tlc.crm.core.campaign.fields.CampaignFields;
import com.tlc.crm.core.campaign.model.Campaign;
import com.tlc.crm.core.campaign.model.CampaignFolder;
import com.tlc.crm.core.campaign.model.CampaignStatus;
import com.tlc.crm.messenger.api.channel.model.CrmWAPhoneNumber;
import com.tlc.crm.messenger.campaign.model.WACampaign;
import com.tlc.crm.messenger.campaign.manager.WACampaignManager;
import com.tlc.crm.messenger.fields.MessengerFields;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.social.common.whatsapp.model.template.WATemplate;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Converts {@link WACampaign} from and into JSON.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WACampaignConverter extends CrmModelConverter<WACampaign, JsonObject> {

    private final UserService userService;
    private final WACampaignManager waCampaignManager;
    private final CampaignConverter campaignConverter;

    private static final class InstanceHolder {
        private static final WACampaignConverter INSTANCE = new WACampaignConverter();
    }

    private WACampaignConverter() {
        userService = UserService.getInstance();
        waCampaignManager = WACampaignManager.getInstance();
        campaignConverter = CampaignConverter.getInstance();
    }

    /**
     * <p>
     * Gets the instance of {@link WACampaignConverter}.
     * </p>
     *
     * @return {@link WACampaignConverter}.
     */
    public static WACampaignConverter getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     * 
     * @param converterData Represents the {@link CrmConverterData<JsonObject>}
     * @return the campaign
     */
    @Override
    public WACampaign convertToModel(final CrmConverterData<JsonObject> converterData) {
        final Long orgId = converterData.orgId();
        final JsonObject data = converterData.dataToBeConverted();
        final boolean containsId = data.containsKey(CampaignFields.ID);
        final WACampaign waCampaign = converterData.isPartialData() && containsId ? waCampaignManager.get(orgId, data.getLong(CampaignFields.ID)) : new WACampaign();

        if (containsId) {
            waCampaign.setId(data.getLong(CampaignFields.ID));
        }
        campaignConverter.loadCampaignDetails(orgId, converterData.ownerId(), waCampaign, data);

        if (data.containsKey(CampaignFields.BUSINESS_PHONE_NUMBER)) {
            final JsonObject phoneNumberJson = data.getJsonObject(CampaignFields.BUSINESS_PHONE_NUMBER);
            final CrmWAPhoneNumber waPhoneNumber = new CrmWAPhoneNumber();

            waPhoneNumber.setCrmPhoneNumberId(phoneNumberJson.getLong(CampaignFields.ID));
            waPhoneNumber.setOrgId(orgId);
            waCampaign.setBusinessPhoneNumber(waPhoneNumber);
        }

        if (data.containsKey(CampaignFields.TEMPLATE)) {
            final JsonObject templateJson = data.getJsonObject(CampaignFields.TEMPLATE);
            final WATemplate waTemplate = new WATemplate();
            final JsonObject templateConfig = Json.object();

            waTemplate.setId(templateJson.getLong(CampaignFields.ID));
            waCampaign.setTemplate(waTemplate);

            if (templateJson.containsKey(MessengerFields.PARAM_DATA)) {
                templateConfig.put(MessengerFields.PARAM_DATA, templateJson.getJsonObject(MessengerFields.PARAM_DATA));
            }

            if (templateJson.containsKey(CampaignFields.SKIP_IF_PLACEHOLDER_EMPTY)) {
                templateConfig.put(CampaignFields.SKIP_IF_PLACEHOLDER_EMPTY, templateJson.getBoolean(CampaignFields.SKIP_IF_PLACEHOLDER_EMPTY));
            }

            if (templateJson.containsKey(CampaignFields.REQUIRED_PLACEHOLDERS)) {
                templateConfig.put(CampaignFields.REQUIRED_PLACEHOLDERS, templateJson.getJsonArray(CampaignFields.REQUIRED_PLACEHOLDERS));
            }

            if (templateConfig.size() > 0) {
                waCampaign.setTemplateConfigJson(templateConfig);
            }
        }

        return waCampaign;
    }

    /**
     * {@inheritDoc}
     * 
     * @param converterData Represents the {@link CrmConverterData<WACampaign>}
     * @return the json object
     */
    @Override
    public JsonObject convertToJson(final CrmConverterData<WACampaign> converterData) {
        final WACampaign waCampaign = converterData.dataToBeConverted();
        final JsonObject templateConfig = waCampaign.getTemplateConfigJson();
        final JsonObject campaignJson = campaignConverter.convertToJson(new CrmConverterData<>(converterData.getUser(),
                converterData.locale(), waCampaign));
        final WATemplate template = waCampaign.getTemplate();
        final JsonObject templateJson = Json.object();

        templateJson.put(CampaignFields.ID, template.id());

        if (Objects.nonNull(templateConfig)) {
            if (templateConfig.containsKey(MessengerFields.PARAM_DATA)) {
                templateJson.put(MessengerFields.PARAM_DATA, templateConfig.getJsonObject(MessengerFields.PARAM_DATA));
            }

            if (templateConfig.containsKey(CampaignFields.SKIP_IF_PLACEHOLDER_EMPTY)) {
                templateJson.put(CampaignFields.SKIP_IF_PLACEHOLDER_EMPTY, templateConfig.getBoolean(CampaignFields.SKIP_IF_PLACEHOLDER_EMPTY));
            }

            if (templateConfig.containsKey(CampaignFields.REQUIRED_PLACEHOLDERS)) {
                templateJson.put(CampaignFields.REQUIRED_PLACEHOLDERS, templateConfig.getJsonArray(CampaignFields.REQUIRED_PLACEHOLDERS));
            }
        }
        campaignJson.put(MessengerFields.TEMPLATE, templateJson);

        return campaignJson;
    }

    /**
     * <p>
     * Converts campaigns into list row JSON (same shape as the former WA campaign list transformer).
     * </p>
     *
     * @param orgId     Represents the organization id.
     * @param campaigns Represents the campaigns to convert.
     * @return {@link JsonArray} of list rows.
     */
    public JsonArray convertListToJson(final Long orgId, final Collection<Campaign> campaigns) {
        if (Objects.isNull(campaigns) || campaigns.isEmpty()) {
            return Json.array();
        }
        final Collection<Long> startedCampaignIds = campaigns.stream().filter(campaign -> campaign.getStatus()
                .isStarted()).map(Campaign::id).toList();
        final boolean containsStartedCampaigns = !startedCampaignIds.isEmpty();
        final Map<Long, Integer> sentCounts = containsStartedCampaigns ? waCampaignManager.getSentCounts(orgId, startedCampaignIds)
                : Collections.emptyMap();
        final Map<Long, Integer> deliveredCounts = containsStartedCampaigns ? waCampaignManager.getDeliveredCounts(orgId, startedCampaignIds)
                : Collections.emptyMap();
        final Map<Long, Integer> readCounts = containsStartedCampaigns ? waCampaignManager.getReadCounts(orgId, startedCampaignIds)
                : Collections.emptyMap();
        final JsonArray listArray = Json.array();

        for (final Campaign campaign : campaigns) {
            final Long campaignId = campaign.id();
            final int totalRecipientCount = campaign.getTotalRecipientCount();
            final CampaignStatus status = campaign.getStatus();
            final JsonObject campaignJson = Json.object();
            final CampaignFolder folder = campaign.getFolder();
            final Long updatedById = campaign.getUpdatedById();
            final JsonObject nameJson = Json.object();

            campaignJson.put(CampaignFields.ID, campaignId);
            campaignJson.put(CampaignFields.STATUS, status.getId());
            nameJson.put(CampaignFields.VALUE, campaign.getName());

            if (status.isInProgress()) {
                nameJson.put(CampaignFields.SENT, buildListMetric(sentCounts.getOrDefault(campaignId, 0), totalRecipientCount));
            }
            campaignJson.put(CampaignFields.NAME, nameJson);

            if (status.isStarted()) {
                campaignJson.put(CampaignFields.SENT, buildListMetric(sentCounts.getOrDefault(campaignId, 0), totalRecipientCount));
                campaignJson.put(CampaignFields.DELIVERED, buildListMetric(deliveredCounts.getOrDefault(campaignId, 0), totalRecipientCount));
                campaignJson.put(CampaignFields.READ, buildListMetric(readCounts.getOrDefault(campaignId, 0), totalRecipientCount));
            }

            if (Objects.nonNull(updatedById)) {
                final CrmUser user = userService.getUser(orgId, updatedById);

                if (Objects.nonNull(user)) {
                    final JsonObject userJson = Json.object();

                    userJson.put(CampaignFields.ID, updatedById);
                    userJson.put(CampaignFields.NAME, user.getFullName());
                    final DateTime updatedTime = campaign.getUpdatedTime();

                    if (Objects.nonNull(updatedTime)) {
                        userJson.put(CampaignFields.UPDATED_AT, updatedTime.getUtcTimeStamp());
                    }
                    campaignJson.put(CampaignFields.LAST_UPDATED_BY_AND_ON, userJson);
                }
            }

            if (Objects.nonNull(folder)) {
                final JsonObject folderJson = Json.object();

                folderJson.put(CampaignFields.ID, folder.id());
                folderJson.put(CampaignFields.NAME, folder.getName());
                campaignJson.put(CampaignFields.FOLDER, folderJson);
            }
            listArray.put(campaignJson);
        }

        return listArray;
    }

    /**
     * <p>
     * Builds the campaign metric
     * </p>
     *
     * @param count Represents the count
     * @param total Represents the total recipient
     * @return the metric json
     */
    private JsonObject buildListMetric(final int count, final int total) {
        return Json.object()
                .put(CampaignFields.COUNT, count)
                .put(CampaignFields.TOTAL, total)
                .put(CampaignFields.PERCENTAGE, total == 0 ? 0 : (count * NumericalConstant.ONE_HUNDRED) / total);
    }
}