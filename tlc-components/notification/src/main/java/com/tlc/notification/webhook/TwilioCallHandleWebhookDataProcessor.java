package com.tlc.notification.webhook;

import com.tlc.commons.annotation.InstanceProvider;
import com.tlc.commons.data.annotation.RequestProcessor;
import com.tlc.commons.data.consumer.DataConsumer;
import com.tlc.commons.data.model.RequestData;
import com.tlc.commons.data.processor.DataProcessor;
import com.tlc.commons.json.JsonObject;

import com.tlc.l10n.model.country.Country;

import com.tlc.notification.key.NotificationKeys;
import com.tlc.notification.model.call.CallType;
import com.tlc.notification.webhook.model.CallHandleWebhookData;

import java.util.Objects;
import java.util.Optional;

/**
 * Singleton processor that handles Twilio call handling webhook callbacks.
 * Processes incoming webhook requests, extracts transaction information,
 * and forwards the data to the configured webhook consumer.
 * <p>
 * Annotated as a Webhook handler for Twilio-specific webhook requests.
 *
 * @author Sathish
 * @version 1.0
 */
@RequestProcessor(name = "twilio_call_handler")
public class TwilioCallHandleWebhookDataProcessor implements DataProcessor<CallHandleWebhookData> {

    private static final String FROM = "From";

    private TwilioCallHandleWebhookDataProcessor() {
    }

    private static final class InstanceHolder {
        private static final TwilioCallHandleWebhookDataProcessor TELE_CMI_WEBHOOK_PROCESSOR = new TwilioCallHandleWebhookDataProcessor();
    }

    /**
     * Provides access to the singleton instance of {@link TwilioCallHandleWebhookDataProcessor}.
     *
     * @return Singleton instance of {@link TwilioCallHandleWebhookDataProcessor}
     */
    @InstanceProvider
    public static TwilioCallHandleWebhookDataProcessor getInstance() {
        return InstanceHolder.TELE_CMI_WEBHOOK_PROCESSOR;
    }

    /**
     * Processes the incoming webhook request and passes the processed data to the provided webhook consumer.
     *
     * @param webhookRequest  Incoming webhook request containing the raw data.
     * @param webhookConsumer Consumer instance that handles processed webhook data.
     */
    @Override
    public void process(final RequestData<JsonObject> webhookRequest, final DataConsumer<CallHandleWebhookData> webhookConsumer) {
        final JsonObject data = webhookRequest.getData();
        final CallHandleWebhookData callHandleWebhookData = new CallHandleWebhookData();
        final String type = data.getNullableString(NotificationKeys.CALL_TYPE.getKey());
        final CallType callType = CallType.get(Objects.isNull(type) ? data.getString(NotificationKeys.DIRECTION.getKey()) : type);

        switch (callType) {
            case OUTGOING -> {
                callHandleWebhookData.setCaller(data.getString(NotificationKeys.FROM_NUMBER.getKey()));
                callHandleWebhookData.setCallerName(data.getString(NotificationKeys.CALLER_NAME.getKey()));
                callHandleWebhookData.setCustomParams(data);
            }
            case INCOMING -> {
                final Optional<Country> optionalCountry = Country.getCountryByName(data.getString(NotificationKeys.CALLER_COUNTRY.getKey()));

                callHandleWebhookData.setCaller(data.getString(FROM));
                optionalCountry.ifPresent(callHandleWebhookData::setCallerCountry);
            }
        }
        callHandleWebhookData.setCallId(data.getString(NotificationKeys.CALL_SERVICE_ID.getKey()));
        callHandleWebhookData.setReceiver(data.getString(NotificationKeys.TO.getKey()));
        callHandleWebhookData.setCallType(callType);
        webhookConsumer.consume(callHandleWebhookData);
    }
}
