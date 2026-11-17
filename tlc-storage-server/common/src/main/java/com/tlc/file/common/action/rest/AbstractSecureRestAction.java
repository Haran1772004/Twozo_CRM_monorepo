package com.tlc.file.common.action.rest;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.file.common.action.RequestService;
import com.tlc.file.common.action.WebActionProcessor;
import com.tlc.file.common.action.req.FileRequest;
import com.tlc.file.common.action.res.FileResponse;

import com.tlc.iam.common.models.User;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.models.converter.UserConverter;

import com.tlc.web.HttpStatus;
import com.tlc.web.WebExchange;
import com.tlc.web.client.event.EventCallback;
import com.tlc.web.client.event.Event;
import com.tlc.web.client.event.EventNotifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the secure REST web actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public abstract class AbstractSecureRestAction extends WebActionProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSecureRestAction.class);

    /**
     * <p>
     * Processes the incoming request
     * </p>
     *
     * @param request  Http request
     * @param response Http response
     * @throws Exception In case any error occurs
     */
    @Override
    public void process(final FileRequest request, final FileResponse response) throws Exception {
        final String xApiKeyHeader = request.header(WebExchange.Header.X_API_KEY.getName());
        final Event<byte[]> authenticateApiKeyEvent = new Event<>(IamEvents.API_KEY_AUTHENTICATION, Json.object()
                .put(IamFields.ACCESS_TOKEN, xApiKeyHeader).getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            /**
             * {@inheritDoc}
             *
             * @param bytes Response from the consumer.
             */
            @Override
            public void onSuccess(final byte[] bytes) {
                final JsonObject jsonObject = Json.object(bytes);

                if (jsonObject.containsKey(IamFields.ERROR)) {
                    sendError(response, HttpStatus.UNAUTHORIZED, ErrorCode.get(ErrorCodes.AUTHORIZATION_FAILED, "i18n.storage.authorization.failed"));
                } else {
                    try {
                        final User user = UserConverter.getInstance().convert(jsonObject.getLong(IamFields.ORG_ID), jsonObject);

                        request.setUser(user);

                        RequestService.get().getRequestHandler().handle(() -> {
                            sProcess(request, response);

                            if (!response.isAsync()) {
                                response.send();
                            }
                        });
                    } catch (final Throwable throwable) {
                        LOGGER.error("Web request Failed, Reason : {}", throwable.getMessage());
                        LOGGER.debug("Stack: ", throwable);
                        response.setErrorCode(ErrorCode.get(ErrorCodes.UNKNOWN_ERROR));
                        response.send();
                    }
                }
            }

            /**
             * {@inheritDoc}
             *
             * @param throwable Cause of the failure.
             */
            @Override
            public void onFailure(final Throwable throwable) {
                sendError(response, HttpStatus.UNAUTHORIZED, ErrorCode.get(ErrorCodes.AUTHORIZATION_FAILED, "i18n.storage.authorization.failed"));
            }
        };

        EventNotifier.get().send(authenticateApiKeyEvent, eventCallback);
    }

    /**
     * Handles the secured/authenticated request.
     *
     * @param request  Http request.
     * @param response Http response.
     */
    public abstract void sProcess(final FileRequest request, final FileResponse response);
}
