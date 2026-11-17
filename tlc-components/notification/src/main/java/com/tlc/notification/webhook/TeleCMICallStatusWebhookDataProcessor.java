package com.tlc.notification.webhook;

import com.tlc.commons.annotation.InstanceProvider;
import com.tlc.commons.data.annotation.RequestProcessor;
import com.tlc.commons.data.consumer.DataConsumer;
import com.tlc.commons.data.model.RequestData;
import com.tlc.commons.data.processor.DataProcessor;
import com.tlc.commons.json.JsonObject;

import com.tlc.notification.key.NotificationKeys;
import com.tlc.notification.model.call.CallStatus;
import com.tlc.notification.model.call.CallType;
import com.tlc.notification.webhook.model.CallStatusWebhookData;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Singleton processor that handles TeleCMI webhook callbacks.
 * Processes incoming webhook requests, extracts transaction information,
 * and forwards the data to the configured webhook consumer.
 * <p>
 * Annotated as a Webhook handler for TeleCMI-specific webhook requests.
 *
 * @author Sathish
 * @version 1.0
 */
@RequestProcessor(name = "telecmi_call_status_monitor")
public class TeleCMICallStatusWebhookDataProcessor implements DataProcessor<CallStatusWebhookData> {
    
    private static final Collection<NotificationKeys> REQUEST_KEYS = List.of(NotificationKeys.REQUEST_ID, NotificationKeys.CONVERSATION_UUID, NotificationKeys.CMI_UUID, NotificationKeys.CALL_ID);
    private static final String CDR_TYPE = "cdr";
    private static final String EVENT_TYPE = "event";
    private static final String OUTBOUND_DIRECTION = "outbound";
    private static final String AGENT_LEG = "a";
    private static final String CUSTOMER_LEG = "b";
    private static final String ANSWERED_STATUS = "answered";
    private static final Collection<String> CALL_START_EVENT_STATUSES = List.of("started", "waiting");

    private TeleCMICallStatusWebhookDataProcessor() {
    }

    private static final class InstanceHolder {
        private static final TeleCMICallStatusWebhookDataProcessor TELE_CMI_WEBHOOK_PROCESSOR = new TeleCMICallStatusWebhookDataProcessor();
    }

    /**
     * Provides access to the singleton instance of {@link TeleCMICallStatusWebhookDataProcessor}.
     *
     * @return Singleton instance of {@link TeleCMICallStatusWebhookDataProcessor}
     */
    @InstanceProvider
    public static TeleCMICallStatusWebhookDataProcessor getInstance() {
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

        if (!isProcessable(data)) {
            return;
        }
        final CallStatusWebhookData callStatusWebhookData = new CallStatusWebhookData();
        final boolean isCdr = isCdr(data);
        final String fromKey = NotificationKeys.FROM.getKey();
        final String callerIdKey = NotificationKeys.CALLER_ID.getKey().toLowerCase();
        final String virtualNumberKey = NotificationKeys.VIRTUAL_NUMBER.getKey().toLowerCase();
        final String toKey = NotificationKeys.TO.getKey().toLowerCase();
        final Long duration = data.getNullableValue(NotificationKeys.ANSWERED_SEC.getKey());

        callStatusWebhookData.setClickToCall(true);
        callStatusWebhookData.setCallId(getCallId(data));
        callStatusWebhookData.setCallType(CallType.get(data.getString(NotificationKeys.DIRECTION.getKey().toLowerCase())));
        callStatusWebhookData.setCallStatus(CallStatus.get(data.getString(NotificationKeys.STATUS.getKey())));
        callStatusWebhookData.setUserId(data.getNullableString(NotificationKeys.USER.getKey()));
        callStatusWebhookData.setExtraParams(data.getNullableValue(NotificationKeys.EXTRA_PARAMS.getKey()));
        callStatusWebhookData.setRecordFileName(data.getNullableValue(NotificationKeys.FILE_NAME.getKey()));
        callStatusWebhookData.setStartTime(data.getNullableValue(NotificationKeys.TIME.getKey()));

        if (isCdr) {
            callStatusWebhookData.setCaller(getFirstAvailableValue(data, fromKey, callerIdKey, virtualNumberKey));
            callStatusWebhookData.setReceiver(getValueAsString(data, toKey));
        } else {
            callStatusWebhookData.setCaller(data.containsKey(fromKey) ? data.getString(fromKey)
                    : data.containsKey(callerIdKey) ? data.getString(callerIdKey) : data.getNullableString(virtualNumberKey));
            callStatusWebhookData.setReceiver(data.getNullableString(toKey));
        }

        if (Objects.nonNull(duration)) {
            callStatusWebhookData.setDuration(String.valueOf(duration));
        }
        webhookConsumer.consume(callStatusWebhookData);
    }

    /**
     * Checks whether the TeleCMI webhook should be processed based on its event type and call details.
     *
     * @param data The webhook payload data
     * @return {@code true} if the webhook should be processed
     */
    private boolean isProcessable(final JsonObject data) {
        final String type = getValueAsString(data, NotificationKeys.WEBHOOK_TYPE.getKey());

        if (CDR_TYPE.equalsIgnoreCase(type)) {
            return isRequiredCdr(data);
        }

        if (EVENT_TYPE.equalsIgnoreCase(type)) {
            final String status = getValueAsString(data, NotificationKeys.STATUS.getKey());

            return Objects.nonNull(status) && CALL_START_EVENT_STATUSES.contains(status.toLowerCase());
        }
        return true;
    }

    /**
     * Selects the valid TeleCMI {@code cdr} webhook based on call direction and leg details.
     *
     * @param data The webhook payload data
     * @return {@code true} if the webhook should be processed
     */
    private boolean isRequiredCdr(final JsonObject data) {
        final String direction = getValueAsString(data, NotificationKeys.DIRECTION.getKey().toLowerCase());

        if (OUTBOUND_DIRECTION.equalsIgnoreCase(direction)) {
            final String leg = getValueAsString(data, NotificationKeys.LEG.getKey());
            final String status = getValueAsString(data, NotificationKeys.STATUS.getKey());

            return Objects.isNull(leg) || CUSTOMER_LEG.equalsIgnoreCase(leg)
                    || (AGENT_LEG.equalsIgnoreCase(leg) && !ANSWERED_STATUS.equalsIgnoreCase(status));
        }
        final String userId = getValueAsString(data, NotificationKeys.USER.getKey());
        final String receiver = getValueAsString(data, NotificationKeys.TO.getKey().toLowerCase());

        return Objects.nonNull(userId) && !userId.isBlank() && Objects.nonNull(receiver) && !receiver.isBlank();
    }

    /**
     * Checks whether the webhook is a <code>cdr</code> webhook.
     *
     * @param data Webhook payload data.
     * @return <code>true</code> if the webhook type is <code>cdr</code>.
     */
    private boolean isCdr(final JsonObject data) {
        return CDR_TYPE.equalsIgnoreCase(getValueAsString(data, NotificationKeys.WEBHOOK_TYPE.getKey()));
    }

    /**
     * Reads the value of the key as text, as the webhook sends numbers such as phone numbers as numeric values.
     *
     * @param data Webhook payload data.
     * @param key  Key of the value.
     * @return Value as text, or {@code null} when the key is not available.
     */
    private String getValueAsString(final JsonObject data, final String key) {
        final Object value = data.getNullableValue(key);

        return Objects.isNull(value) ? null : String.valueOf(value);
    }

    /**
     * Reads the value of the first available key as text.
     *
     * @param data Webhook payload data.
     * @param keys Keys in priority order.
     * @return First available value as text, or {@code null} when none of the keys is available.
     */
    private String getFirstAvailableValue(final JsonObject data, final String... keys) {
        for (final String key : keys) {
            final String value = getValueAsString(data, key);

            if (Objects.nonNull(value)) {
                return value;
            }
        }
        return null;
    }

    /**
     * Resolves the call identifier from the webhook payload.
     * <p>
     * TeleCMI webhook events may provide different identifiers based on the event type.
     * This method checks the supported identifier keys in priority order and returns
     * the first available value.
     *
     * @param data Webhook payload data.
     * @return Resolved call identifier, or {@code null} when no supported identifier is found.
     */
    private String getCallId(final JsonObject data) {
        for (final NotificationKeys requestKey : REQUEST_KEYS) {
            final String identifierKey = requestKey.getKey();

            if (data.containsKey(identifierKey)) {
                return data.getString(identifierKey);
            }
        }

        return null;
    }
}