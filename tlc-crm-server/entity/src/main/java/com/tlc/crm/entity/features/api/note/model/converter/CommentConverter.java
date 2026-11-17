package com.tlc.crm.entity.features.api.note.model.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.features.api.note.model.Comment;
import com.tlc.crm.entity.features.field.note.NoteFields;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;

import java.util.Objects;

/**
 * <p>
 * Provides different ways to convert the data from / into {@link Comment}
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class CommentConverter extends EntityConverter<Comment> {

    private final UserService userService;
    private final CrmUserConverter userConverter;
    private final DateTimeUtil dateTimeUtil;

    private CommentConverter() {
        userService = UserService.getInstance();
        userConverter = CrmUserConverter.getInstance();
        dateTimeUtil = DateTimeUtil.getInstance();
    }

    private static class Instance {
        private static final CommentConverter INSTANCE = new CommentConverter();
    }

    /**
     * <p>
     * Gets the {@link CommentConverter} Instance
     * </p>
     *
     * @return {@link CommentConverter}
     */
    public static CommentConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Constructs the comment for the Json provided
     * </p>
     *
     * @param converterConfig Converter configuration
     * @param jsonObject      Contains the Json data
     * @return {@link Comment}
     */
    @Override
    public Comment convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Comment comment = new Comment();
        final String idKey = NoteFields.ID.getName();

        if (jsonObject.containsKey(idKey)) {
            comment.setId(jsonObject.getLong(idKey));
        }
        setJsonDataToModel(converterConfig, comment, jsonObject);

        return comment;
    }

    /**
     * <p>
     * Converts the comment into Json data
     * </p>
     *
     * @param comment An entity that holds the comment attributes
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convert(final Comment comment) {
        final JsonObject jsonObject = Json.object();
        final CrmUser createdBy = comment.getCreatedBy();
        final CrmUser updatedBy = comment.getUpdatedBy();
        final Long createdTime = dateTimeUtil.getTimeStamp(comment.getCreatedTime());
        final Long updatedTime = dateTimeUtil.getTimeStamp(comment.getUpdatedTime());

        jsonObject.put(NoteFields.ID.getName(), comment.id());
        jsonObject.put(NoteFields.ORG_ID.getName(), comment.orgId());
        jsonObject.put(NoteFields.CONTENT.getName(), comment.getContent());
        jsonObject.put(NoteFields.CREATED_TIME.getName(), createdTime);
        jsonObject.put(NoteFields.UPDATED_TIME.getName(), updatedTime);
        jsonObject.put(NoteFields.IS_EDITED.getName(), Objects.nonNull(updatedTime));
        jsonObject.put(NoteFields.IS_PINNED.getName(), comment.isPinned());
        jsonObject.put(NoteFields.LAST_ACTION_TIME.getName(), Objects.isNull(updatedTime) ? createdTime : updatedTime);

        if (Objects.nonNull(createdBy)) {
            jsonObject.put(NoteFields.CREATED_BY.getName(), userConverter.convert(createdBy, true, true));
        }

        if (Objects.nonNull(updatedBy)) {
            jsonObject.put(NoteFields.UPDATED_BY.getName(), userConverter.convert(updatedBy, true, true));
        }
        return jsonObject;
    }

    /**
     * Converts {@link Comment} into partial {@link JsonObject}
     *
     * @param comment An entity that holds the attributes of comment details
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertPartialData(final Comment comment) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(NoteFields.ID.getName(), comment.id());
        jsonObject.put(NoteFields.CONTENT.getName(), comment.getContent());
        return jsonObject;
    }

    /**
     * <p>
     * Sets the Json to comment
     * </p>
     *
     * @param converterConfig Converter configuration
     * @param comment         An entity that holds the comment attributes
     * @param jsonObject      Contains the Json data
     */
    public void setJsonDataToModel(final ConverterConfig converterConfig, final Comment comment, final JsonObject jsonObject) {
        final String commentKey = NoteFields.CONTENT.getName();
        final String createdTimeKey = NoteFields.CREATED_TIME.getName();
        final String updatedTimeKey = NoteFields.UPDATED_TIME.getName();
        final Long orgId = converterConfig.orgId();
        final CrmUser user = userService.getUser(orgId, converterConfig.ownerId());

        comment.setOrgId(orgId);
        comment.setCreatedBy(user);
        comment.setUpdatedBy(user);
        comment.setContent(jsonObject.containsKey(commentKey) ? jsonObject.getString(commentKey) : comment.getContent());
        comment.setCreatedTime(jsonObject.containsKey(createdTimeKey) ? new DateTime(jsonObject.getLong(createdTimeKey)) : null);
        comment.setUpdatedTime(jsonObject.containsKey(updatedTimeKey) ? new DateTime(jsonObject.getLong(updatedTimeKey)) : null);
    }
}
