package com.tlc.crm.common.action.iam.web;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.client.internal.action.SecureAction;
import com.tlc.iam.common.action.req.IamHttpRequest;
import com.tlc.iam.common.action.res.IamHttpResponse;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.SessionFields;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.converter.UserConverter;
import com.tlc.iam.common.status.IamErrorCodes;
import com.tlc.web.HttpMethod;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.Event;
import com.tlc.web.client.event.EventCallback;
import com.tlc.web.client.event.EventNotifier;

/**
 * Fetches all the sessions associated to the logged-in user
 *
 * @author Selvakumar
 * @version 1.0
 */
@WebAction(path = "/accounts/{application}/currentUser/sessions", method = HttpMethod.GET)
public class LoggedInUserSession extends SecureAction {

    private final UserConverter userConverter;

    public LoggedInUserSession() {
        userConverter = UserConverter.getInstance();
    }

    @Override
    public void sProcess(final IamHttpRequest request, final IamHttpResponse response) {
        final User user = request.getUser();
        final JsonObject sessionRequest = Json.object();

        sessionRequest.put(UserFields.USER_ID, user.id());
        sessionRequest.put(UserFields.ORG_ID, user.orgId());

        final JsonObject userJson = userConverter.convert(request.getUser());
        final Event<byte[]> sessionFetchEvent = new Event<>(IamEvents.FETCH_USER_SESSIONS, userJson.getBytes());
        final EventCallback<byte[]> sessionRetrievalCallback = new EventCallback<>() {

            @Override
            public void onSuccess(byte[] bytes) {
                //TODO direct conversion of byte array to json array causes error. Need to fix it
                final JsonArray sessionsArray = Json.array(new String(bytes));

                response.put(SessionFields.SESSIONS, sessionsArray);
                response.send();
            }

            @Override
            public void onFailure(Throwable throwable) {
                sendError(response, null, ErrorCode.get(IamErrorCodes.SESSIONS_RETRIEVAL_FAILED, "i18n.iam.accounts.session.retrieval.failed", throwable));
            }
        };

        EventNotifier.get().send(sessionFetchEvent, sessionRetrievalCallback);
    }
}
