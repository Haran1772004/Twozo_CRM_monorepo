package com.tlc.crm.core.internal.event.email;

import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.action.parser.DataParser;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.core.action.parser.impl.NativeParser;
import com.tlc.crm.core.queue.CoreQueueTopics;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.producer.ProducerStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Collection;
import java.util.Map;

import static com.tlc.queue.QueueModel.Type.KAFKA;

/**
 * <p>
 * Facilitates to publish email alert data into consumer.
 * </p>
 *
 * @author Mohammed Asif J
 * @version 1.0
 */
public class EmailAlertDataPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailAlertDataPublisher.class);
    private static final String KEY_FORMAT = "%s:%s:%s";

    private final ProducerStore producerStore;
    private final DataParser dataParser;
    private final Encoder encoder;
    private final SubscriptionTopic emailAlertTopic;

    protected EmailAlertDataPublisher() {
        this.emailAlertTopic = CoreQueueTopics.getEmailAlertTopic(KAFKA);
        this.producerStore = ProducerStore.getInstance();
        this.dataParser = NativeParser.getInstance();
        this.encoder = Base64.getEncoder();
    }

    private static class InstanceHolder {
        private static final EmailAlertDataPublisher INSTANCE = new EmailAlertDataPublisher();
    }

    /**
     * <p>
     * Gets the {@link EmailAlertDataPublisher} Instance
     * </p>
     *
     * @return {@link EmailAlertDataPublisher}
     */
    public static EmailAlertDataPublisher getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Publishes the email alert data to the consumer.
     * </p>
     *
     * @param orgId    Represents the organization ID.
     * @param userId   Represents the user ID.
     * @param action   Represents the {@link Action}
     * @param ids      Represents the collection of model IDs which triggered the event.
     * @param template Represents the template for email alert
     */
    public void publish(final long orgId, final long userId, final Action action,
                        final Collection<Long> ids, final String template) {

        if (action.isCreateOrUpdate()) {
            ids.forEach( id -> {
                producerStore.getProducer(emailAlertTopic).ifPresent(producer -> {
                    producer.send(new StreamMessage<>(
                            encoder.encodeToString(String.format(KEY_FORMAT, orgId, userId, id).getBytes()),
                            emailAlertTopic, dataParser.serialize(Map.of(ActionFields.TEMPLATE, template)))
                    );
                    LOGGER.info("Email Alert Data Publisher: Published Email Alert Data for Org ID: {}, User ID: {}, Model ID: {}", orgId, userId, id);
                });
            });
        }
    }
}