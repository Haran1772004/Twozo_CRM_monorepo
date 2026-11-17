package com.tlc.email.common.codec;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.email.common.fields.EmailFields;
import com.tlc.email.common.model.EmailStateTransitionInfo;
import com.tlc.email.common.request.UpdateRequest;
import com.tlc.email.common.model.type.Email;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Responsible for encoding and decoding update request data related to email threads or individual emails.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class UpdateRequestCodec extends AbstractEmailCodec<UpdateRequest> {

    private UpdateRequestCodec() {
    }

    public static UpdateRequestCodec getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param updateRequest the {@link UpdateRequest} to be encoded
     * @return
     */
    @Override
    public byte[] encode(final UpdateRequest updateRequest) {
        final JsonObject updateRequestJson = Json.object();
        final Map<String, Collection<EmailStateTransitionInfo>> transitionMap =
                updateRequest.getEmailStateTransitionInfos();
        final JsonObject emailStateTransitionInfos = Json.object();

        if (Objects.nonNull(transitionMap)) {
            for (final Map.Entry<String, Collection<EmailStateTransitionInfo>> entry : transitionMap.entrySet()) {
                final JsonArray stateTransitionArray = Json.array();

                for (final EmailStateTransitionInfo stateTransitionInfo : entry.getValue()) {
                    final JsonObject stateTransitionJson = Json.object();

                    stateTransitionJson.put(EmailFields.OLD_STATE, stateTransitionInfo.getOldState());
                    stateTransitionJson.put(EmailFields.NEW_STATE, stateTransitionInfo.getNewState());
                    stateTransitionJson.put(EmailFields.MESSAGE_CATEGORY_ID, stateTransitionInfo.getMessageCategory().getId());
                    stateTransitionJson.put(EmailFields.NEW_MESSAGE_ID, stateTransitionInfo.getNewMessageId());
                    stateTransitionJson.put(EmailFields.OLD_MESSAGE_ID, stateTransitionInfo.getOldMessageId());
                    stateTransitionJson.put(EmailFields.PARENT_FOLDER_ID, stateTransitionInfo.getParentFolderId());

                    stateTransitionArray.put(stateTransitionJson);
                }
                emailStateTransitionInfos.put(entry.getKey(), stateTransitionArray);
            }
        }

        updateRequestJson.put(EmailFields.STATE_TRANSITION, emailStateTransitionInfos);
        updateRequestJson.put(EmailFields.STATE_DATA, updateRequest.getStateData());

        return updateRequestJson.getBytes();
    }

    /**
     * {@inheritDoc}
     *
     * @param data the bytes to decode
     * @return {@link RequestCodec}
     */
    @Override
    public UpdateRequest decode(final byte[] data) {

        if (Objects.isNull(data) || data.length == 0) {
            return null;
        }

        final JsonObject updateRequestJson = Json.object(data);
        final UpdateRequest updateRequest = new UpdateRequest();
        final JsonObject emailStateTransitionInfos = updateRequestJson.getJsonObject(EmailFields.STATE_TRANSITION);

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
            updateRequest.setEmailStateTransitionInfos(transitionMap);
        }
        updateRequest.setStateData(getStateDataAsJson(updateRequestJson));

        return updateRequest;
    }

    /**
     * <p>
     * Holds the single instance of {@link UpdateRequestCodec}.
     * </p>
     */
    private static class Instance {
        private static final UpdateRequestCodec INSTANCE = new UpdateRequestCodec();
    }
}
