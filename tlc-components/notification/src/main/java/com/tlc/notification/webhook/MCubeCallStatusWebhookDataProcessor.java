package com.tlc.notification.webhook;

import com.tlc.commons.annotation.InstanceProvider;
import com.tlc.commons.data.annotation.RequestProcessor;
import com.tlc.commons.data.consumer.DataConsumer;
import com.tlc.commons.data.model.RequestData;
import com.tlc.commons.data.processor.DataProcessor;
import com.tlc.commons.json.JsonObject;

import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.notification.model.call.CallStatus;
import com.tlc.notification.model.call.CallType;
import com.tlc.notification.webhook.model.CallStatusWebhookData;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Singleton processor that handles MCube webhook callbacks.
 * <p>
 * Processes incoming MCube webhook requests containing call status updates,
 * extracts call information (caller, callee, duration, recording URL, etc.),
 * and forwards the data to the configured webhook consumer.
 * </p>
 * <p>
 * MCube sends webhook events when a call transitions through states:
 * RINGING → ANSWERED → COMPLETED / MISSED / FAILED.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@RequestProcessor(name = "mcube_call_status_monitor")
public class MCubeCallStatusWebhookDataProcessor implements DataProcessor<CallStatusWebhookData> {

    private final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private MCubeCallStatusWebhookDataProcessor() {
    }

    private static final class InstanceHolder {
        private static final MCubeCallStatusWebhookDataProcessor M_CUBE_WEBHOOK_PROCESSOR = new MCubeCallStatusWebhookDataProcessor();
    }

    /**
     * Provides access to the singleton instance of {@link MCubeCallStatusWebhookDataProcessor}.
     *
     * @return Singleton instance of {@link MCubeCallStatusWebhookDataProcessor}
     */
    @InstanceProvider
    public static MCubeCallStatusWebhookDataProcessor getInstance() {
        return InstanceHolder.M_CUBE_WEBHOOK_PROCESSOR;
    }

    /**
     * Processes the incoming MCube webhook request and passes the processed data to the provided webhook consumer.
     * <p>
     * MCube webhook payload fields:
     * <ul>
     *   <li>{@code callid} - Unique identifier for the call</li>
     *   <li>{@code emp_phone} - The agent's phone number (caller for outbound)</li>
     *   <li>{@code clicktocalldid} - The DID number used for click-to-call</li>
     *   <li>{@code callto} - The customer's phone number (receiver)</li>
     *   <li>{@code dialstatus} - Call status (e.g., ANSWER, NOANSWER, BUSY, CANCEL)</li>
     *   <li>{@code direction} - Call direction (inbound/outbound)</li>
     *   <li>{@code starttime} - Call start time</li>
     *   <li>{@code answeredtime} - Duration of the answered call (HH:mm:ss format)</li>
     *   <li>{@code filename} - URL of the call recording (if available)</li>
     * </ul>
     *
     * @param webhookRequest  Incoming webhook request containing the raw MCube event data.
     * @param webhookConsumer Consumer instance that handles processed webhook data.
     */
    @Override
    public void process(final RequestData<JsonObject> webhookRequest, final DataConsumer<CallStatusWebhookData> webhookConsumer) {
        final JsonObject data = webhookRequest.getData();
        final CallStatusWebhookData callStatusWebhookData = new CallStatusWebhookData();
        final String answeredTime = data.getNullableString(MCubeWebhookFields.ANSWERED_TIME);
        final String startTime = data.getNullableValue(MCubeWebhookFields.START_TIME);

        callStatusWebhookData.setClickToCall(data.containsKey(MCubeWebhookFields.CLICK_TO_CALL_DID));
        callStatusWebhookData.setCallId(data.getString(MCubeWebhookFields.CALL_ID));
        callStatusWebhookData.setCallType(CallType.get(data.getString(MCubeWebhookFields.DIRECTION)));
        callStatusWebhookData.setCallStatus(CallStatus.get(data.getString(MCubeWebhookFields.DIAL_STATUS)));
        callStatusWebhookData.setCaller(data.getNullableString(MCubeWebhookFields.CALL_FROM));
        callStatusWebhookData.setReceiver(data.getNullableString(MCubeWebhookFields.CALL_TO));
        callStatusWebhookData.setRoutingNumber(data.getNullableString(MCubeWebhookFields.CLICK_TO_CALL_DID));
        callStatusWebhookData.setRecordFileName(data.getNullableString(MCubeWebhookFields.FILENAME));
        callStatusWebhookData.setStartTime(Objects.isNull(startTime) ? null : LocalDateTime.parse(startTime,
                DATE_TIME_FORMATTER).atZone(ZoneId.of(TimezoneInfo.ASIA_KOLKATA.getTimeZone().getID())).toInstant().toEpochMilli());

        if (Objects.nonNull(answeredTime)) {
            callStatusWebhookData.setDuration(convertToSeconds(answeredTime));
        }
        webhookConsumer.consume(callStatusWebhookData);
    }

    /**
     * Converts MCube's answered time format (HH:mm:ss) to total seconds.
     *
     * @param time the duration string in HH:mm:ss format
     * @return the duration in seconds as a string
     */
    private String convertToSeconds(final String time) {
        final String[] parts = time.split(":");

        if (parts.length == 3) {
            final int hours = Integer.parseInt(parts[0]);
            final int minutes = Integer.parseInt(parts[1]);
            final int seconds = Integer.parseInt(parts[2]);

            return String.valueOf((hours * 3600) + (minutes * 60) + seconds);
        }
        return time;
    }

    /**
     * MCube webhook-specific field names as received in the webhook payload.
     */
    private static final class MCubeWebhookFields {

        private static final String CLICK_TO_CALL_DID = "clicktocalldid";
        private static final String CALL_ID = "callid";
        private static final String CALL_FROM = "callfrom";
        private static final String CALL_TO = "callto";
        private static final String DIAL_STATUS = "dialstatus";
        private static final String DIRECTION = "direction";
        private static final String ANSWERED_TIME = "answeredtime";
        private static final String FILENAME = "filename";
        private static final String START_TIME = "starttime";

        private MCubeWebhookFields() {
        }
    }
}
