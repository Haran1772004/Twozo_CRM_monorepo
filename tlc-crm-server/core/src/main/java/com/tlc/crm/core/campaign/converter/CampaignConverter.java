package com.tlc.crm.core.campaign.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.converter.CrmConverterData;
import com.tlc.crm.common.converter.CrmModelConverter;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.core.campaign.fields.CampaignFields;
import com.tlc.crm.core.campaign.manager.CampaignManager;
import com.tlc.crm.core.campaign.model.Campaign;
import com.tlc.crm.core.campaign.model.CampaignFolder;
import com.tlc.crm.entity.campaign.model.CampaignRecipient;
import com.tlc.crm.core.campaign.model.CampaignStatus;
import com.tlc.crm.core.segment.models.Segment;
import com.tlc.crm.entity.marketinglist.model.MarketingList;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.model.TimezoneInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Converts {@link Campaign} from and into JSON.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class CampaignConverter extends CrmModelConverter<Campaign, JsonObject> {

    private final UserService userService;
    private final CampaignManager campaignManager;

    private static final class InstanceHolder {
        private static final CampaignConverter INSTANCE = new CampaignConverter();
    }

    private CampaignConverter() {
        userService = UserService.getInstance();
        campaignManager = CampaignManager.getInstance();
    }

    /**
     * <p>
     * Gets the instance of {@link CampaignConverter}.
     * </p>
     *
     * @return {@link CampaignConverter}.
     */
    public static CampaignConverter getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterData Represents the {@link CrmConverterData}.
     * @return the converted campaign model.
     */
    @Override
    public Campaign convertToModel(final CrmConverterData<JsonObject> converterData) {
        final Long orgId = converterData.orgId();
        final JsonObject data = converterData.dataToBeConverted();
        final boolean containsIdKey = data.containsKey(CampaignFields.ID);
        final Campaign campaign = converterData.isPartialData() && containsIdKey
                ? campaignManager.get(orgId, data.getLong(CampaignFields.ID)) : new Campaign();

        if (containsIdKey) {
            campaign.setId(data.getLong(CampaignFields.ID));
        }
        loadCampaignDetails(orgId, converterData.ownerId(), campaign, data);

        return campaign;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterData Represents the {@link CrmConverterData}.
     * @return the converted JSON object.
     */
    @Override
    public JsonObject convertToJson(final CrmConverterData<Campaign> converterData) {
        final Long orgId = converterData.orgId();
        final Campaign campaign = converterData.dataToBeConverted();
        final JsonObject campaignJson = Json.object()
                .put(CampaignFields.ID, campaign.id())
                .put(CampaignFields.NAME, campaign.getName());

        if (converterData.isPartialData()) {
            return campaignJson;
        }
        campaignJson.put(CampaignFields.STATUS, campaign.getStatus().getId());
        campaignJson.put(CampaignFields.IS_ARCHIVED, campaign.isArchived());

        final CampaignFolder folder = campaign.getFolder();

        if (Objects.nonNull(folder) && Objects.nonNull(folder.id())) {
            campaignJson.put(CampaignFields.FOLDER_ID, folder.id());
        }
        final DateTime scheduledTime = campaign.getScheduledTime();

        if (Objects.nonNull(scheduledTime)) {
            campaignJson.put(CampaignFields.SCHEDULED_TIME, scheduledTime.getTimeStamp());
        }
        final TimezoneInfo timezoneInfo = campaign.getTimezoneInfo();

        if (Objects.nonNull(timezoneInfo)) {
            campaignJson.put(CampaignFields.TIME_ZONE_ID, timezoneInfo.getId());
        }
        final Collection<CampaignRecipient> recipients = campaign.getRecipients();

        if (Objects.nonNull(recipients) && !recipients.isEmpty()) {
            final JsonArray recipientArray = Json.array();

            for (final CampaignRecipient recipient : recipients) {
                final short recipientTypeId = recipient.getRecipientType().getId();

                if (CampaignRecipient.RecipientType.MARKETING_LIST.getId() == recipientTypeId) {
                    final MarketingList marketingList = (MarketingList) recipient;

                    recipientArray.put(Json.object()
                            .put(CampaignFields.ID, marketingList.id())
                            .put(CampaignFields.CONTACT_COUNT, marketingList.getContactCount())
                            .put(CampaignFields.NAME, marketingList.getName())
                            .put(CampaignFields.TYPE, recipientTypeId));
                } else if (CampaignRecipient.RecipientType.SEGMENT.getId() == recipientTypeId) {
                    final Segment segment = (Segment) recipient;

                    recipientArray.put(Json.object()
                            .put(CampaignFields.ID, segment.id())
                            .put(CampaignFields.CONTACT_COUNT, segment.getContactCount())
                            .put(CampaignFields.NAME, segment.getName())
                            .put(CampaignFields.TYPE, recipientTypeId));
                }
            }

            campaignJson.put(CampaignFields.RECIPIENTS, recipientArray);
        }

        final JsonObject createdJson = Json.object();
        final Long createdById = campaign.getCreatedById();

        if (Objects.nonNull(createdById)) {
            final CrmUser user = userService.getUser(orgId, createdById);

            createdJson.put(CampaignFields.BY, Json.object()
                    .put(CampaignFields.ID, createdById)
                    .put(CampaignFields.NAME, Objects.nonNull(user) ? user.getFullName() : null));
        }
        final DateTime createdTime = campaign.getCreatedTime();

        if (Objects.nonNull(createdTime)) {
            createdJson.put(CampaignFields.TIME, createdTime.getTimeStamp());
        }
        final JsonObject updatedJson = Json.object();
        final Long updatedById = campaign.getUpdatedById();

        if (Objects.nonNull(updatedById)) {
            final CrmUser user = userService.getUser(orgId, updatedById);

            updatedJson.put(CampaignFields.BY, Json.object()
                    .put(CampaignFields.ID, updatedById)
                    .put(CampaignFields.NAME, Objects.nonNull(user) ? user.getFullName() : null));
        }
        final DateTime updatedTime = campaign.getUpdatedTime();

        if (Objects.nonNull(updatedTime)) {
            updatedJson.put(CampaignFields.TIME, updatedTime.getTimeStamp());
        }
        campaignJson.put(CampaignFields.AUDIT, Json.object()
                .put(CampaignFields.CREATED, createdJson)
                .put(CampaignFields.UPDATED, updatedJson));

        return campaignJson;
    }

    /**
     * <p>
     * Converts marketing list JSON into {@link MarketingList} model.
     * </p>
     *
     * @param orgId           Represents the organization id.
     * @param marketingListId Represents the marketing list id.
     * @return Collection of {@link MarketingList}.
     */
    private MarketingList convertMarketingList(final Long orgId, final Long marketingListId) {
        final MarketingList marketingList = new MarketingList();

        marketingList.setId(marketingListId);
        marketingList.setOrgId(orgId);

        return marketingList;
    }

    /**
     * <p>
     * Converts segment JSON into {@link Segment} model.
     * </p>
     *
     * @param orgId     Represents the organization id.
     * @param segmentId Represents the segment id.
     * @return Collection of {@link Segment}.
     */
    private Segment convertSegment(final Long orgId, final Long segmentId) {
        final Segment segment = new Segment();

        segment.setId(segmentId);
        segment.setOrgId(orgId);

        return segment;
    }

    /**
     * <p>
     * Loads campaign details from the provided JSON object into the model
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param userId   Represents the user id
     * @param campaign Represents the campaign model.
     * @param data     Represents the JSON object containing campaign details.
     */
    public void loadCampaignDetails(final Long orgId, final Long userId, final Campaign campaign, final JsonObject data) {
        campaign.setOrgId(orgId);

        if (data.containsKey(CampaignFields.NAME)) {
            campaign.setName(data.getString(CampaignFields.NAME));
        }

        if (data.containsKey(CampaignFields.STATUS)) {
            campaign.setStatus(CampaignStatus.get(data.getShort(CampaignFields.STATUS)));
        }

        if (data.containsKey(CampaignFields.IS_ARCHIVED)) {
            campaign.setArchived(data.getBoolean(CampaignFields.IS_ARCHIVED));
        }

        if (data.containsKey(CampaignFields.FOLDER_ID)) {
            final CampaignFolder folder = new CampaignFolder(data.getLong(CampaignFields.FOLDER_ID));

            folder.setOrgId(orgId);
            campaign.setFolder(folder);
        }

        if (data.containsKey(CampaignFields.SCHEDULED_TIME)) {
            campaign.setScheduledTime(new DateTime(data.getLong(CampaignFields.SCHEDULED_TIME)));
        }

        if (data.containsKey(CampaignFields.TIME_ZONE_ID)) {
            campaign.setTimezoneInfo(TimezoneInfo.getTimezoneById(data.getInt(CampaignFields.TIME_ZONE_ID)));
        }

        if (data.containsKey(CampaignFields.RECIPIENTS)) {
            final Collection<CampaignRecipient> recipients = new ArrayList<>();
            final JsonArray recipientArray = data.getJsonArray(CampaignFields.RECIPIENTS);
            final int size = recipientArray.size();

            for (int index = 0; index < size; index++) {
                final JsonObject recipientJson = recipientArray.getJsonObject(index);
                final CampaignRecipient.RecipientType recipientType = CampaignRecipient.RecipientType.get(recipientJson.getShort(CampaignFields.TYPE));
                final Long recipientId = recipientJson.getLong(CampaignFields.ID);

                recipients.add(CampaignRecipient.RecipientType.MARKETING_LIST == recipientType
                        ? convertMarketingList(orgId, recipientId)
                        : convertSegment(orgId, recipientId));
            }

            if (!recipients.isEmpty()) {
                campaign.setRecipients(recipients);
            }
        }

        if (Objects.isNull(campaign.getCreatedById())) {
            campaign.setCreatedById(userId);
        }
        campaign.setUpdatedById(userId);
    }
}
