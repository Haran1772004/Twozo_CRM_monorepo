package com.tlc.iam.client;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.OrganizationFields;
import com.tlc.iam.common.models.Organization;
import com.tlc.iam.common.models.converter.OrganizationConverter;
import com.tlc.iam.common.status.IamErrorCodes;
import com.tlc.web.client.event.Event;
import com.tlc.web.client.event.EventCallback;
import com.tlc.web.client.event.EventNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * A resolver that resolves the organization
 *
 * @author Selvakumar
 * @version 1.0
 */
public final class OrganizationResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrganizationResolver.class);

    private final long id;

    private Organization organization;

    public OrganizationResolver(long id) {
        this.id = id;
    }

    /**
     * Once the organization is resolved, given callback will be called
     *
     * @param callback
     */
    public void resolve(final EventCallback<Organization> callback) {
        if (Objects.nonNull(organization)) {
            callback.onSuccess(organization);
        } else {
            final Event<byte[]> orgFetchEvent = new Event<>(IamEvents.FETCH_ORGANIZATION, Json.object().put(OrganizationFields.ID, id).getBytes());
            final EventCallback<byte[]> callbackWrapper = new EventCallback<>() {

                @Override
                public void onSuccess(byte[] bytes) {
                    if (Objects.nonNull(bytes)) {
                        final JsonObject jsonObject = Json.object(bytes);
                        final Organization fetchedOrganization = OrganizationConverter.getInstance().convert(jsonObject);

                        organization = fetchedOrganization;
                        callback.onSuccess(fetchedOrganization);
                    } else {
                        callback.onFailure(ErrorCode.get(IamErrorCodes.DATA_RETRIEVAL_FAILED));
                    }
                }

                @Override
                public void onFailure(Throwable throwable) {
                    LOGGER.error("Data retrieval failed, Reason : {}", IamErrorCodes.DATA_RETRIEVAL_FAILED.getMessage());
                    callback.onFailure(ErrorCode.get(IamErrorCodes.DATA_RETRIEVAL_FAILED));
                }
            };

            EventNotifier.get().send(orgFetchEvent, callbackWrapper);
        }
    }

}
