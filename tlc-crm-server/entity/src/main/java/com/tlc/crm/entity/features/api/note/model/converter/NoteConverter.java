package com.tlc.crm.entity.features.api.note.model.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.features.api.note.model.Comment;
import com.tlc.crm.entity.features.api.note.model.Note;
import com.tlc.crm.entity.features.field.note.NoteFields;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Provides different ways to convert the data from / into {@link Note}
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public class NoteConverter extends EntityConverter<Note> {

    private final CommentConverter commentConverter;
    private final UserService userService;
    private final CrmUserConverter userConverter;
    private final DateTimeUtil dateTimeUtil;

    private NoteConverter() {
        commentConverter = CommentConverter.getInstance();
        userService = UserService.getInstance();
        userConverter = CrmUserConverter.getInstance();
        dateTimeUtil = DateTimeUtil.getInstance();
    }

    private static class Instance {
        private static final NoteConverter INSTANCE = new NoteConverter();
    }

    /**
     * <p>
     * Gets the {@link NoteConverter} Instance
     * </p>
     *
     * @return {@link NoteConverter}
     */
    public static NoteConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Constructs the note for the Json provided
     * </p>
     *
     * @param converterConfig Converter configuration
     * @param jsonObject      Contains the Json data
     * @return {@link Note}
     */
    @Override
    public Note convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Note note = new Note();
        final String idKey = NoteFields.ID.getName();

        if (jsonObject.containsKey(idKey)) {
            note.setId(jsonObject.getLong(idKey));
        }
        setJsonDataToModel(converterConfig, note, jsonObject);

        return note;
    }

    /**
     * <p>
     * Converts the note into Json data
     * </p>
     *
     * @param note An entity that holds the note attributes
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convert(final Note note) {
        final JsonObject jsonObject = Json.object();
        final CrmUser createdBy = note.getCreatedBy();
        final CrmUser updatedBy = note.getUpdatedBy();
        final Collection<Comment> comments = note.getComments();
        final Long updatedTime = dateTimeUtil.getTimeStamp(note.getUpdatedTime());
        final Long createdTime = dateTimeUtil.getTimeStamp(note.getCreatedTime());

        jsonObject.put(NoteFields.ID.getName(), note.id());
        jsonObject.put(NoteFields.ORG_ID.getName(), note.orgId());
        jsonObject.put(NoteFields.CONTENT.getName(), note.getContent());
        jsonObject.put(NoteFields.CREATED_TIME.getName(), createdTime);
        jsonObject.put(NoteFields.UPDATED_TIME.getName(), updatedTime);
        jsonObject.put(NoteFields.IS_EDITED.getName(), Objects.nonNull(updatedTime));
        jsonObject.put(NoteFields.IS_PINNED.getName(), note.isPinned());
        jsonObject.put(NoteFields.COMMENTS_COUNT.getName(), note.getCommentsCount());
        jsonObject.put(NoteFields.LAST_ACTION_TIME.getName(), Objects.isNull(updatedTime) ? createdTime : updatedTime);

        if (Objects.nonNull(createdBy)) {
            jsonObject.put(NoteFields.CREATED_BY.getName(), userConverter.convert(createdBy, true, true));
        }

        if (Objects.nonNull(updatedBy)) {
            jsonObject.put(NoteFields.UPDATED_BY.getName(), userConverter.convert(updatedBy, true, true));
        }

        if (Objects.nonNull(comments) && !comments.isEmpty()) {
            final JsonArray jsonArray = Json.array();

            for (final Comment comment : comments) {
                jsonArray.put(commentConverter.convert(comment));
            }
            jsonObject.put(NoteFields.COMMENTS.getName(), jsonArray);
        }
        return jsonObject;
    }

    /**
     * Converts {@link Note} into partial {@link JsonObject}
     *
     * @param note An entity that holds the attributes of note details
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertPartialData(final Note note) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(NoteFields.ID.getName(), note.id());
        jsonObject.put(NoteFields.CONTENT.getName(), note.getContent());
        return jsonObject;
    }

    /**
     * <p>
     * Sets the Json to note
     * </p>
     *
     * @param converterConfig Converter configuration
     * @param note            An entity that holds the note attributes
     * @param jsonObject      Contains the Json data
     */
    public void setJsonDataToModel(final ConverterConfig converterConfig, final Note note, final JsonObject jsonObject) {
        final String contentKey = NoteFields.CONTENT.getName();
        final String createdTimeKey = NoteFields.CREATED_TIME.getName();
        final String updatedTimeKey = NoteFields.UPDATED_TIME.getName();
        final Long orgId = converterConfig.orgId();
        final CrmUser user = userService.getUser(orgId, converterConfig.ownerId());

        note.setOrgId(orgId);
        note.setCreatedBy(user);
        note.setUpdatedBy(user);
        note.setContent(jsonObject.containsKey(contentKey) ? jsonObject.getString(contentKey) : note.getContent());
        note.setCreatedTime(jsonObject.containsKey(createdTimeKey) ? new DateTime(jsonObject.getLong(createdTimeKey)) : null);
        note.setUpdatedTime(jsonObject.containsKey(updatedTimeKey) ? new DateTime(jsonObject.getLong(updatedTimeKey)) : null);
    }
}
