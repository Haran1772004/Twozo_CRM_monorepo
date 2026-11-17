package com.tlc.payment.gateway.webhook;

import com.tlc.commons.annotation.InstanceProvider;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.data.annotation.RequestProcessor;
import com.tlc.commons.data.consumer.DataConsumer;
import com.tlc.commons.data.model.RequestData;

import com.tlc.commons.data.processor.DataProcessor;
import com.tlc.payment.gateway.internal.fields.PaymentFields;
import com.tlc.payment.gateway.models.PaymentStatus;
import com.tlc.payment.gateway.webhook.model.PaymentWebhookData;

import java.util.Optional;

/**
 * Singleton dispatcher that handles PayU payment webhook callbacks.
 * Dispatches incoming webhook requests, extracts transaction information,
 * and forwards the data to the configured webhook populator.
 * <p>
 * Annotated as a RequestProcessor handler for PAYU-specific webhook requests.
 *
 * @author Sathish
 * @version 1.0
 */
@RequestProcessor(name = "payu")
public class PayUWebhookDataProcessor implements DataProcessor<PaymentWebhookData> {

    private PayUWebhookDataProcessor() {
    }

    private static final class InstanceHolder {
        private static final PayUWebhookDataProcessor PAY_U_WEBHOOK_REQUEST_DATA_PROCESSOR = new PayUWebhookDataProcessor();
    }

    /**
     * Provides access to the singleton instance of {@link PayUWebhookDataProcessor}.
     *
     * @return Singleton instance of {@link PayUWebhookDataProcessor}
     */
    @InstanceProvider
    public static PayUWebhookDataProcessor getInstance() {
        return InstanceHolder.PAY_U_WEBHOOK_REQUEST_DATA_PROCESSOR;
    }

    /**
     * Dispatches the incoming webhook request, extracts transaction ID and payment status,
     * prepares a PaymentWebhookData object, and invokes the webhook consumer.
     *
     * @param requestData         Incoming request containing the raw data.
     * @param dataConsumer The consumer which will handle the converted webhook data
     */
    @Override
    public void process(final RequestData<JsonObject> requestData, final DataConsumer<PaymentWebhookData> dataConsumer) {
        final JsonObject jsonData = requestData.getData();
        final Optional<PaymentStatus> optionalPaymentStatus = PaymentStatus.getByName(jsonData.getString(PaymentFields.STATUS));

        if (optionalPaymentStatus.isPresent()) {
            final PaymentWebhookData paymentWebhookData = new PaymentWebhookData();

            paymentWebhookData.setTransactionId(jsonData.getString(PaymentFields.TXN_ID));
            paymentWebhookData.setPaymentStatus(optionalPaymentStatus.get());
            paymentWebhookData.setLocalTransactionId(jsonData.getLong(PaymentFields.UDF1));
            paymentWebhookData.setAllowedOrigin(jsonData.getString(PaymentFields.UDF2));

            dataConsumer.consume(paymentWebhookData);
        }
    }
}
