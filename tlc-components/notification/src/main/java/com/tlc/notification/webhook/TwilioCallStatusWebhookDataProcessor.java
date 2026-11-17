package com.tlc.notification.webhook;

import com.tlc.commons.annotation.InstanceProvider;
import com.tlc.commons.data.annotation.RequestProcessor;
import com.tlc.commons.data.consumer.DataConsumer;
import com.tlc.commons.data.model.RequestData;
import com.tlc.commons.data.processor.DataProcessor;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.StringUtil;
import com.tlc.notification.key.NotificationKeys;
import com.tlc.notification.model.call.CallStatus;
import com.tlc.notification.model.call.CallType;
import com.tlc.notification.webhook.model.CallStatusWebhookData;

import java.util.Optional;

/**
 * Singleton processor that handles Twilio call status webhook callbacks.
 * Processes incoming webhook requests, extracts transaction information,
 * and forwards the data to the configured webhook consumer.
 * <p>
 * Annotated as a Webhook handler for Twilio-specific webhook requests.
 *
 * @author Sathish
 * @version 1.0
 */
@RequestProcessor(name = "twilio_call_status_monitor")
public class TwilioCallStatusWebhookDataProcessor implements DataProcessor<CallStatusWebhookData> {

    private TwilioCallStatusWebhookDataProcessor() {
    }

    private static final class InstanceHolder {
        private static final TwilioCallStatusWebhookDataProcessor TELE_CMI_WEBHOOK_PROCESSOR = new TwilioCallStatusWebhookDataProcessor();
    }

    /**
     * Provides access to the singleton instance of {@link TwilioCallStatusWebhookDataProcessor}.
     *
     * @return Singleton instance of {@link TwilioCallStatusWebhookDataProcessor}
     */
    @InstanceProvider
    public static TwilioCallStatusWebhookDataProcessor getInstance() {
        return InstanceHolder.TELE_CMI_WEBHOOK_PROCESSOR;
    }

    /**
     * Processes the incoming webhook request and passes the processed data to the provided webhook consumer.
     *
     * @param webhookRequest  Incoming webhook request containing the raw data.
     * @param webhookConsumer Consumer instance that handles processed webhook data.
     */
    @Override
    public void process(final RequestData<JsonObject> webhookRequest, final DataConsumer<CallStatusWebhookData> webhookConsumer) {
        final JsonObject data = webhookRequest.getData();
        final CallStatusWebhookData callStatusWebhookData = new CallStatusWebhookData();
        final Optional<String> callType = webhookRequest.getParam(NotificationKeys.CALL_TYPE.getKey());

        callStatusWebhookData.setCallId(data.getString(NotificationKeys.CALL_SERVICE_ID.getKey()));
        callStatusWebhookData.setParentCallId(data.getNullableString(NotificationKeys.PARENT_CALL_SID.getKey()));
        callStatusWebhookData.setCallStatus(CallStatus.get(data.getString(NotificationKeys.CALL_STATUS.getKey())));
        callStatusWebhookData.setReceiver(data.getNullableString(NotificationKeys.TO.getKey()));
        callStatusWebhookData.setCallType(CallType.get(callType.orElse(StringUtil.EMPTY_STRING)));
        webhookRequest.getParam(NotificationKeys.TRANSFER_TO.getKey()).ifPresent(callStatusWebhookData::setTransferTo);
        webhookRequest.getParam(NotificationKeys.TRANSFERRED_BY.getKey()).ifPresent(callStatusWebhookData::setTransferredBy);
        webhookRequest.getParam(NotificationKeys.IS_RING_BACK_ENABLED.getKey()).ifPresent(isRingBackEnabled ->
                callStatusWebhookData.setRingBackEnabled(Boolean.parseBoolean(isRingBackEnabled)));

        webhookConsumer.consume(callStatusWebhookData);
    }
}
