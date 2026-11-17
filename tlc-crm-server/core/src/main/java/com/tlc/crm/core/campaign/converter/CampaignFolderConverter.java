package com.tlc.crm.core.campaign.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.converter.CrmConverterData;
import com.tlc.crm.common.converter.CrmModelConverter;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.core.campaign.fields.CampaignFields;
import com.tlc.crm.core.campaign.manager.CampaignFolderManager;
import com.tlc.crm.core.campaign.model.CampaignFolder;
import com.tlc.crm.core.campaign.model.CampaignCategory;

import java.util.Objects;

/**
 * <p>
 * Converts {@link CampaignFolder} from and into JSON.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class CampaignFolderConverter extends CrmModelConverter<CampaignFolder, JsonObject> {

    private final UserService userService;
    private final CampaignFolderManager campaignFolderManager;

    private static final class InstanceHolder {
        private static final CampaignFolderConverter INSTANCE = new CampaignFolderConverter();
    }

    private CampaignFolderConverter() {
        userService = UserService.getInstance();
        campaignFolderManager = CampaignFolderManager.getInstance();
    }

    /**
     * <p>
     * Gets the instance of {@link CampaignFolderConverter}.
     * </p>
     *
     * @return {@link CampaignFolderConverter}.
     */
    public static CampaignFolderConverter getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterData Represents the {@link CrmConverterData}
     * @return the {@link CampaignFolder} model converted from the provided JSON data
     */
    @Override
    public CampaignFolder convertToModel(final CrmConverterData<JsonObject> converterData) {
        final Long orgId = converterData.orgId();
        final Long userId = converterData.ownerId();
        final JsonObject data = converterData.dataToBeConverted();
        final boolean containsIdKey = data.containsKey(CampaignFields.ID);
        final CampaignFolder folder = converterData.isPartialData() && containsIdKey
                ? campaignFolderManager.get(orgId, data.getLong(CampaignFields.ID)) : new CampaignFolder();

        if (containsIdKey) {
            folder.setId(data.getLong(CampaignFields.ID));
        }
        folder.setOrgId(orgId);

        if (Objects.isNull(folder.getCreatedById())) {
            folder.setCreatedById(userId);
        }
        folder.setUpdatedById(userId);

        if (data.containsKey(CampaignFields.NAME)) {
            folder.setName(data.getString(CampaignFields.NAME));
        }

        if (data.containsKey(CampaignFields.IS_STARRED)) {
            folder.setStarred(data.getBoolean(CampaignFields.IS_STARRED));
        }

        if (data.containsKey(CampaignFields.CATEGORY)) {
            folder.setCategory(CampaignCategory.get(data.getShort(CampaignFields.CATEGORY)));
        }

        return folder;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterData Represents the {@link CrmConverterData}.
     * @return the converted JSON object.
     */
    @Override
    public JsonObject convertToJson(final CrmConverterData<CampaignFolder> converterData) {
        final Long orgId = converterData.orgId();
        final CampaignFolder folder = converterData.dataToBeConverted();
        final Long campaignCount = folder.getCampaignCount();
        final JsonObject folderJson = Json.object()
                .put(CampaignFields.ID, folder.id())
                .put(CampaignFields.NAME, folder.getName());

        if (converterData.isPartialData()) {
            return folderJson;
        }
        folderJson.put(CampaignFields.IS_STARRED, folder.isStarred());
        folderJson.put(CampaignFields.CAMPAIGN_COUNT, Objects.isNull(campaignCount) ? 0L : campaignCount);

        final JsonObject auditJson = Json.object();
        final JsonObject createdJson = Json.object();
        final Long createdById = folder.getCreatedById();

        if (Objects.nonNull(createdById)) {
            final JsonObject userJson = Json.object().put(CampaignFields.ID, createdById);
            final CrmUser user = userService.getUser(orgId, createdById);

            if (Objects.nonNull(user)) {
                userJson.put(CampaignFields.NAME, user.getFullName());
            }

            createdJson.put(CampaignFields.BY, userJson).put(CampaignFields.TIME, folder.getCreatedTime().getTimeStamp());
            auditJson.put(CampaignFields.CREATED, createdJson);
        }
        final JsonObject updatedJson = Json.object();
        final Long updatedById = folder.getUpdatedById();

        if (Objects.nonNull(updatedById)) {
            final JsonObject userJson = Json.object().put(CampaignFields.ID, updatedById);
            final CrmUser user = userService.getUser(orgId, updatedById);

            if (Objects.nonNull(user)) {
                userJson.put(CampaignFields.NAME, user.getFullName());
            }

            updatedJson.put(CampaignFields.BY, userJson).put(CampaignFields.TIME, folder.getUpdatedTime().getTimeStamp());
            auditJson.put(CampaignFields.UPDATED, updatedJson);
        }

        folderJson.put(CampaignFields.AUDIT, auditJson);

        return folderJson;
    }
}