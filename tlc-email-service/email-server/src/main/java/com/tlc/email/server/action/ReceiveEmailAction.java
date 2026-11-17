package com.tlc.email.server.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.email.common.action.PublicAction;
import com.tlc.email.common.action.req.EmailRequest;
import com.tlc.email.common.action.res.EmailResponse;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.server.fields.InboundEmailReceiverFields;
import com.tlc.email.server.internal.publisher.EmailEventPublisher;
import com.tlc.email.server.internal.storage.InboundEmailPayloadBuilder;
import com.tlc.email.server.internal.storage.InboundEmailStorageService;
import com.tlc.web.HttpMethod;
import com.tlc.web.HttpStatus;
import com.tlc.web.WebAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Handles inbound email receive callbacks and publishes them to the message queue.
 *
 * @author sundar
 * @version 1.0
 */
@WebAction(path = "/receiveEmail", method = HttpMethod.POST)
public class ReceiveEmailAction extends PublicAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiveEmailAction.class);

    private final EmailEventPublisher emailEventPublisher;

    public ReceiveEmailAction() {
        this.emailEventPublisher = EmailEventPublisher.get();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Http request
     * @param response Http response
     */
    @Override
    public void pProcess(final EmailRequest request, final EmailResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.optString(InboundEmailReceiverFields.TYPE, null);

        if (!InboundEmailReceiverFields.PUBLISH_INBOUND.equals(type)) {
            throw ErrorCode.get(EmailErrorCodes.UNSUPPORTED_PUBLISH_ACTION);
        }

        final JsonObject data = requestJson.getJsonObject(InboundEmailReceiverFields.DATA);

        if (Objects.isNull(data)
                || !data.containsKey(InboundEmailReceiverFields.BUCKET_NAME)
                || !data.containsKey(InboundEmailReceiverFields.OBJECT_ID)
                || !data.containsKey(InboundEmailReceiverFields.TIMESTAMP)) {
            throw ErrorCode.get(EmailErrorCodes.INVALID_MESSAGE);
        }

        LOGGER.info("Inbound Email Receiver: Received inbound email notification for objectId={}",
                data.getString(InboundEmailReceiverFields.OBJECT_ID));

        emailEventPublisher.publishInboundEmailReceived(
                InboundEmailPayloadBuilder.buildPayload(
                        InboundEmailStorageService.getInstance().getBucketName(),
                        data.getString(InboundEmailReceiverFields.OBJECT_ID),
                        data.getLong(InboundEmailReceiverFields.TIMESTAMP)
                )
        );

        response.setStatus(HttpStatus.OK);
        response.send();
    }
}
