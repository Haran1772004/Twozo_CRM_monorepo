package com.tlc.email.common.codec;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.email.common.fields.EmailFields;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.EmailStateTransitionInfo;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.request.DeleteRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Responsible for encoding and decoding delete request data related to email threads or individual emails.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see AbstractEmailCodec
 */
public final class DeleteRequestCodec extends AbstractEmailCodec<DeleteRequest> {

    private DeleteRequestCodec() {
    }

    /**
     * <p>
     * Holds the single instance of {@link DeleteRequestCodec}.
     * </p>
     */
    private static class Instance {
        private static final DeleteRequestCodec INSTANCE = new DeleteRequestCodec();
    }

    public static DeleteRequestCodec getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param deleteRequest the {@link DeleteRequest} to be encoded
     * @return bytes of request codec
     */
    @Override
    public byte[] encode(final DeleteRequest deleteRequest) {
        final JsonObject deleteRequestJson = Json.object();
        final Map<String, Collection<EmailStateTransitionInfo>> transitionMap = deleteRequest.getEmailStateTransitionInfos();
        final JsonObject stateTransitionObject = Json.object();

        if (Objects.nonNull(transitionMap)) {
            for (final Map.Entry<String, Collection<EmailStateTransitionInfo>> entry : transitionMap.entrySet()) {
                final JsonArray array = Json.array();

                for (final EmailStateTransitionInfo emailStateTransitionInfo : entry.getValue()) {
                    final JsonObject stateTransitionJson = Json.object();

                    stateTransitionJson.put(EmailFields.OLD_STATE, emailStateTransitionInfo.getOldState());
                    stateTransitionJson.put(EmailFields.NEW_STATE, emailStateTransitionInfo.getNewState());
                    stateTransitionJson.put(EmailFields.MESSAGE_CATEGORY_ID, emailStateTransitionInfo.getMessageCategory().getId());
                    stateTransitionJson.put(EmailFields.NEW_MESSAGE_ID, emailStateTransitionInfo.getNewMessageId());
                    stateTransitionJson.put(EmailFields.OLD_MESSAGE_ID, emailStateTransitionInfo.getOldMessageId());
                    stateTransitionJson.put(EmailFields.PARENT_FOLDER_ID, emailStateTransitionInfo.getParentFolderId());

                    array.put(stateTransitionJson);
                }

                stateTransitionObject.put(entry.getKey(), array);
            }
        }
        final Email.MessageCategory messageCategory = deleteRequest.getMessageCategory();

        if (Objects.nonNull(messageCategory)) {
            deleteRequestJson.put(EmailFields.MESSAGE_CATEGORY_ID, messageCategory.getId());
        }

        deleteRequestJson.put(EmailFields.STATE_TRANSITION, stateTransitionObject);
        deleteRequestJson.put(EmailFields.STATE_DATA, deleteRequest.getStateData());

        return deleteRequestJson.getBytes();

    }

    /**
     * {@inheritDoc}
     *
     * @param data the bytes to decode
     * @return {@link RequestCodec}
     */
    @Override
    public DeleteRequest decode(final byte[] data) {

        if (Objects.isNull(data) || data.length == 0) {
            return null;
        }
        final JsonObject deleteRequestJson = Json.object(data);
        final DeleteRequest deleteRequest = new DeleteRequest();
        final JsonObject emailStateTransitionInfos = deleteRequestJson.getJsonObject(EmailFields.STATE_TRANSITION);

        if (Objects.nonNull(emailStateTransitionInfos)) {
            final Map<String, Collection<EmailStateTransitionInfo>> transitionMap = new HashMap<>();

            for (final String email : emailStateTransitionInfos.keySet()) {
                final JsonArray jsonArray = emailStateTransitionInfos.getJsonArray(email);
                final Collection<EmailStateTransitionInfo> stateTransitions = new ArrayList<>();

                for (int index = 0; index < jsonArray.size(); index++) {
                    final JsonObject updateJson = jsonArray.getJsonObject(index);
                    final EmailStateTransitionInfo stateTransition = new EmailStateTransitionInfo();

                    stateTransition.setNewState(Email.State.valueOf(updateJson.getString(EmailFields.NEW_STATE)));
                    stateTransition.setOldState(Email.State.valueOf(updateJson.getString(EmailFields.OLD_STATE)));
                    stateTransition.setMessageCategory(Email.MessageCategory.get(updateJson.getShort(EmailFields.MESSAGE_CATEGORY_ID)));
                    stateTransition.setOldMessageId(updateJson.getString(EmailFields.OLD_MESSAGE_ID));
                    stateTransition.setNewMessageId(updateJson.optString(EmailFields.NEW_MESSAGE_ID, null));
                    stateTransition.setParentFolderId(updateJson.optString(EmailFields.PARENT_FOLDER_ID, null));

                    stateTransitions.add(stateTransition);
                }

                transitionMap.put(email, stateTransitions);
            }
            deleteRequest.setEmailStateTransitionInfos(transitionMap);
        }
        deleteRequest.setStateData(getStateDataAsJson(deleteRequestJson));

        if (deleteRequestJson.containsKey(EmailFields.MESSAGE_CATEGORY_ID)) {
            deleteRequest.setMessageCategory(Email.MessageCategory.get(deleteRequestJson.getShort(EmailFields.MESSAGE_CATEGORY_ID)));
        }

        return deleteRequest;
    }
}
