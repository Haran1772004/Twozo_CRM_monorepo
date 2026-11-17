package com.tlc.crm.messenger.action.webhook;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.crypto.HashGenerator;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.pub.CrmPublicAction;
import com.tlc.crm.common.action.req.CrmJsonRequest;
import com.tlc.crm.common.action.res.CrmTextResponse;
import com.tlc.crm.messenger.fields.MessengerFields;
import com.tlc.crm.messenger.internal.status.CrmSocialErrorCodes;
import com.tlc.crm.messenger.internal.status.whatsapp.WAErrorCodes;
import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.producer.Producer;
import com.tlc.queue.producer.ProducerStore;
import com.tlc.social.common.queue.SocialQueueTopics;
import com.tlc.web.HttpMethod;
import com.tlc.web.HttpStatus;
import com.tlc.web.WebAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

/**
 * Manages WhatsApp webhook related web actions
 *
 * @author Maharaja S
 * @version 1.0
 */
@WebAction(path = "/whatsapp/webhook", method = {HttpMethod.GET, HttpMethod.POST})
public class WAWebhookAction extends CrmPublicAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(WAWebhookAction.class);
    private static final String WEBHOOK_SIGNATURE = "X-Hub-Signature-256";

    private final Configurations configurations;
    private final HashGenerator hashGenerator;
    private final ProducerStore producerStore;

    public WAWebhookAction() {
        this.configurations = ConfHolder.getInstance().getProperties(MessengerFields.SYNC_SERVICE).getNestedConfigs(MessengerFields.WHATSAPP);
        this.hashGenerator = HashGenerator.getInstance();
        this.producerStore = ProducerStore.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest Represents the request
     * @param crmResponse Represents the response
     */
    @Override
    public void pProcess(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final HttpMethod httpMethod = crmRequest.getMethod();

        if (HttpMethod.GET == httpMethod) {
            verifyWebhookToken(crmRequest, crmResponse);
        } else if (HttpMethod.POST == httpMethod) {
            processWebhookPayload(crmRequest, crmResponse);
        }
    }


    /**
     * <p>
     * Verifies the WhatsApp webhook token
     * </p>
     *
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void verifyWebhookToken(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final CrmTextResponse textResponse = crmResponse.convertTo(CrmTextResponse.class);
        final String hubMode = crmRequest.getParameter(MessengerFields.WEBHOOK_HUB_MODE);
        final String hubChallenge = crmRequest.getParameter(MessengerFields.WEBHOOK_HUB_CHALLENGE);
        final String verifyToken = crmRequest.getParameter(MessengerFields.WEBHOOK_HUB_VERIFY_TOKEN);

        if (Objects.equals(MessengerFields.SUBSCRIBE, hubMode) && Objects.equals(configurations.getConfig(MessengerFields.WEBHOOK_VERIFY_TOKEN), verifyToken)) {
            LOGGER.info("Whatsapp: Webhook verified successfully");
            textResponse.put(hubChallenge);
        } else {
            textResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            LoggerUtil.logError(LOGGER, ErrorCode.get(WAErrorCodes.WEBHOOK_VERIFICATION_FAILED), "Whatsapp: Webhook verification failed");
        }
        textResponse.send();
    }

    /**
     * <p>
     * Processes the WhatsApp webhook payload
     * </p>
     *
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void processWebhookPayload(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final JsonObject data = crmRequest.getRequestJson();
        final String signature = crmRequest.header(WEBHOOK_SIGNATURE);
        final byte[] requestBodyInBytes =((CrmJsonRequest) crmRequest).getRequestBodyAsBytes();

        if (Objects.nonNull(signature) && verifySignature(requestBodyInBytes, signature, configurations.getConfig(MessengerFields.APP_SECRET))
                && Objects.equals(MessengerFields.WHATSAPP_BUSINESS_ACCOUNT, data.getString(MessengerFields.OBJECT))) {
            final JsonArray entries = data.getJsonArray(MessengerFields.ENTRY);

            if (entries.size() > NumericalConstant.ZERO) {
                final JsonObject entryObject = entries.getJsonObject(NumericalConstant.ZERO);
                final SubscriptionTopic dataConsumeTopic = SocialQueueTopics.getWAWebhookDataConsumeTopic(QueueModel.Type.KAFKA);
                final String requestId = Base64.getEncoder().encodeToString(String.format("WA:%d", entryObject.getLong(MessengerFields.ID)).getBytes());
                final Optional<Producer<byte[]>> optionalProducer = producerStore.getProducer(dataConsumeTopic);

                optionalProducer.ifPresentOrElse(producer -> {
                    producer.send(new StreamMessage<>(requestId, dataConsumeTopic, data.getBytes()));
                    LOGGER.info("Whatsapp: WhatsApp webhook data consume request published. Request ID: {}", requestId);
                }, () -> LOGGER.error("Whatsapp: Kafka producer not found for the topic:{}", dataConsumeTopic.getName()));
            }
        } else {
            crmResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            LoggerUtil.logError(LOGGER, ErrorCode.get(CrmSocialErrorCodes.WEBHOOK_PAYLOAD_VALIDATION_FAILED), "Webhook payload validation failed");
        }
    }

    /**
     * <p>
     * Verifies the webhook payload signature
     * </p>
     *
     * @param bytes     Represents the payload as bytes
     * @param signature Represents the signature to be verified
     * @param appSecret Represents the app secret
     * @return true if both the signature matched, false otherwise
     */
    private boolean verifySignature(final byte[] bytes, final String signature, final String appSecret) {
        final String expectedSignature = "sha256=" + hashGenerator.getHmac(bytes, appSecret);

        return Objects.equals(signature, expectedSignature);
    }
}