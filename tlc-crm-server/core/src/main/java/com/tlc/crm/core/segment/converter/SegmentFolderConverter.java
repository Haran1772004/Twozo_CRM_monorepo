package com.tlc.crm.core.segment.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.converter.CrmConverterData;
import com.tlc.crm.common.converter.CrmModelConverter;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.core.segment.models.SegmentFolder;
import com.tlc.crm.core.segment.fields.SegmentFolderFields;
import com.tlc.crm.core.segment.service.SegmentFolderService;
import com.tlc.l10n.temporal.model.DateTime;

import java.util.Objects;

/**
 * <p>
 * Converts {@link SegmentFolder} from and into JSON.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class SegmentFolderConverter extends CrmModelConverter<SegmentFolder, JsonObject> {

    private final UserService userService;
    private final SegmentFolderService segmentFolderService;

    private static class Instance {
        private static final SegmentFolderConverter INSTANCE = new SegmentFolderConverter();
    }

    private SegmentFolderConverter() {
        userService = UserService.getInstance();
        segmentFolderService = SegmentFolderService.getInstance();
    }

    /**
     * <p>
     * Gets the instance of {@link SegmentFolderConverter}.
     * </p>
     *
     * @return {@link SegmentFolderConverter}.
     */
    public static SegmentFolderConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SegmentFolder convertToModel(final CrmConverterData<JsonObject> converterData) {
        final Long orgId = converterData.orgId();
        final CrmUser user = userService.getUser(orgId, converterData.ownerId());
        final JsonObject data = converterData.dataToBeConverted();
        final String idKey = SegmentFolderFields.ID.getName();
        final SegmentFolder folder = converterData.isPartialData() && data.containsKey(idKey)
                ? segmentFolderService.get(orgId, data.getLong(idKey)) : new SegmentFolder();

        if (data.containsKey(idKey)) {
            folder.setId(data.getLong(idKey));
        }
        folder.setOrgId(orgId);

        if (Objects.isNull(folder.getCreatedBy())) {
            folder.setCreatedBy(user);
        }
        folder.setModifiedBy(user);

        if (data.containsKey(SegmentFolderFields.NAME.getName())) {
            folder.setName(data.getString(SegmentFolderFields.NAME.getName()).trim());
        }

        if (data.containsKey(SegmentFolderFields.IS_STARRED.getName())) {
            folder.setStarred(data.getBoolean(SegmentFolderFields.IS_STARRED.getName()));
        }

        return folder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject convertToJson(final CrmConverterData<SegmentFolder> converterData) {
        final SegmentFolder folder = converterData.dataToBeConverted();

        final JsonObject folderJson = Json.object()
                .put(SegmentFolderFields.ID.getName(), folder.id())
                .put(SegmentFolderFields.NAME.getName(), folder.getName());

        if (converterData.isPartialData()) {
            return folderJson;
        }
        folderJson.put(SegmentFolderFields.IS_STARRED.getName(), folder.isStarred());
        folderJson.put(SegmentFolderFields.SEGMENT_COUNT.getName(), Objects.isNull(folder.getSegmentCount()) ? 0L : folder.getSegmentCount());
        final JsonObject createdJson = Json.object();
        final CrmUser createdBy = folder.getCreatedBy();

        if (Objects.nonNull(createdBy)) {
            createdJson.put(
                    SegmentFolderFields.BY.getName(),
                    Json.object()
                            .put(SegmentFolderFields.ID.getName(), createdBy.id())
                            .put(SegmentFolderFields.NAME.getName(), createdBy.getFullName())
            );
        }
        final DateTime createdTime = folder.getCreatedTime();

        if (Objects.nonNull(createdTime)) {
            createdJson.put(SegmentFolderFields.TIME.getName(), createdTime.getTimeStamp());
        }
        final JsonObject modifiedJson = Json.object();
        final CrmUser modifiedBy = folder.getModifiedBy();

        if (Objects.nonNull(modifiedBy)) {
            modifiedJson.put(
                    SegmentFolderFields.BY.getName(),
                    Json.object()
                            .put(SegmentFolderFields.ID.getName(), modifiedBy.id())
                            .put(SegmentFolderFields.NAME.getName(), modifiedBy.getFullName())
            );
        }
        final DateTime modifiedTime = folder.getModifiedTime();

        if (Objects.nonNull(modifiedTime)) {
            modifiedJson.put(SegmentFolderFields.TIME.getName(), modifiedTime.getTimeStamp());
        }

        folderJson.put(
                SegmentFolderFields.AUDIT.getName(),
                Json.object()
                        .put(SegmentFolderFields.CREATED.getName(), createdJson)
                        .put(SegmentFolderFields.MODIFIED.getName(), modifiedJson)
        );

        return folderJson;
    }
}