package com.tlc.crm.common.action.iam.web;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.client.internal.action.SecureAction;
import com.tlc.iam.common.action.req.IamHttpRequest;
import com.tlc.iam.common.action.res.IamHttpResponse;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.status.IamErrorCodes;
import com.tlc.web.HttpStatus;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.Event;
import com.tlc.web.client.event.EventCallback;
import com.tlc.web.client.event.EventNotifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Manages {@link User} related web actions
 *
 * @author AswiniN
 * @version 1.0
 */
@WebAction(path = "/accounts/user")
public class UserManagement extends SecureAction {

    private static final String USER_UPDATE_FAILED_I18N_KEY = "i18n.iam.accounts.user.update.failed";
    private static final String USER_DELETE_FAILED_I18N_KEY = "i18n.iam.accounts.user.delete.failed";
    private final static Logger LOGGER = LoggerFactory.getLogger(UserManagement.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void sProcess(final IamHttpRequest request, final IamHttpResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(UserFields.TYPE);
        final JsonObject data = requestJson.getJsonObject(UserFields.DATA);
        final User user = request.getUser();

        data.put(UserFields.ORG_ID, user.orgId());

        switch (type) {
            case "update" -> {
                final Event<byte[]> event = new Event(IamEvents.CHECK_USER_EXISTENCE, data.getBytes());
                final EventCallback<Boolean> userExistenceCallback = new EventCallback<>() {

                    @Override
                    public void onSuccess(final Boolean userExists) {
                        if (userExists) {

                            if (user.isAdminUser() || user.id().equals(data.getLong(UserFields.ID))) {
                                final Event<byte[]> userUpdateEvent = new Event<>(IamEvents.USER_UPDATE, data.getBytes());
                                final EventCallback<String> userUpdateCallback = new EventCallback<>() {

                                    @Override
                                    public void onSuccess(final String message) {
                                        if (Objects.isNull(message)) {
                                            sendError(response, null, ErrorCode.get(IamErrorCodes.USER_UPDATE_FAILED, USER_UPDATE_FAILED_I18N_KEY));
                                        } else {
                                            response.put(IamFields.MESSAGE, response.resolveI18nString("i18n.iam.accounts.user.update.success"));
                                            response.send();
                                        }
                                    }

                                    @Override
                                    public void onFailure(final Throwable throwable) {
                                        LOGGER.error("User update action failed, Reason : {}", throwable.getMessage());
                                        sendError(response, null, ErrorCode.get(IamErrorCodes.USER_UPDATE_FAILED, USER_UPDATE_FAILED_I18N_KEY, throwable));
                                    }
                                };

                                EventNotifier.get().send(userUpdateEvent, userUpdateCallback);
                            } else {
                                sendError(response, HttpStatus.UNAUTHORIZED, ErrorCode.get(IamErrorCodes.AUTHORIZATION_FAILED, "i18n.iam.accounts.authorization.failed"));
                            }
                        } else {
                            sendError(response, null, ErrorCode.get(IamErrorCodes.USER_NOT_FOUND, "i18n.iam.accounts.user.not.found"));
                        }
                    }

                    @Override
                    public void onFailure(final Throwable throwable) {
                        LOGGER.error("User update action failed, Reason : {}", throwable.getMessage());
                        sendError(response, null, ErrorCode.get(IamErrorCodes.USER_UPDATE_FAILED, USER_UPDATE_FAILED_I18N_KEY));
                    }
                };

                EventNotifier.get().send(event, userExistenceCallback);
            }
            case "delete" -> {
                final Event<byte[]> event = new Event(IamEvents.CHECK_USER_EXISTENCE, data.getBytes());
                final EventCallback<Boolean> userExistenceCallback = new EventCallback<>() {

                    @Override
                    public void onSuccess(final Boolean userExists) {
                        if (userExists) {

                            if (user.isAdminUser() || user.id().equals(data.getLong("id"))) {
                                final Event<byte[]> userDeleteEvent = new Event<>(IamEvents.USER_DEACTIVATE, data.getBytes());
                                final EventCallback<Boolean> userDeleteCallback = new EventCallback<>() {

                                    @Override
                                    public void onSuccess(final Boolean isDeleted) {
                                        if (isDeleted) {
                                            response.put(IamFields.MESSAGE, response.resolveI18nString("i18n.iam.accounts.user.delete.success"));
                                            response.send();
                                        } else {
                                            sendError(response, null, ErrorCode.get(IamErrorCodes.USER_DELETE_FAILED, USER_DELETE_FAILED_I18N_KEY));
                                        }
                                    }

                                    @Override
                                    public void onFailure(final Throwable throwable) {
                                        LOGGER.error("User delete action failed, Reason : {}", throwable.getMessage());
                                        sendError(response, null, ErrorCode.get(IamErrorCodes.USER_DELETE_FAILED, USER_DELETE_FAILED_I18N_KEY));
                                    }
                                };

                                EventNotifier.get().send(userDeleteEvent, userDeleteCallback);
                            } else {
                                sendError(response, HttpStatus.UNAUTHORIZED, ErrorCode.get(IamErrorCodes.AUTHORIZATION_FAILED, "i18n.iam.accounts.authorization.failed"));
                            }
                        } else {
                            sendError(response, null, ErrorCode.get(IamErrorCodes.USER_NOT_FOUND, "i18n.iam.accounts.user.not.found"));
                        }
                    }

                    @Override
                    public void onFailure(final Throwable throwable) {
                        LOGGER.error("User delete action failed, Reason : {}", throwable.getMessage());
                        sendError(response, null, ErrorCode.get(IamErrorCodes.USER_DELETE_FAILED, USER_DELETE_FAILED_I18N_KEY));
                    }
                };

                EventNotifier.get().send(event, userExistenceCallback);
            }
        }
    }
}