package com.tlc.file.common.queue;

import com.tlc.queue.QueueModel;
import com.tlc.queue.SubscriptionTopicHolder;
import com.tlc.queue.config.SubscriptionTopic;

/**
 * Utility to get the subscription topics in attachment client module.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class AttachmentQueueTopics {

    private static final SubscriptionTopicHolder SUBSCRIPTION_TOPIC_HOLDER = SubscriptionTopicHolder.getInstance();
    private static final String ATTACHMENT_UPLOAD_LISTENER = "ATTACHMENT_UPLOAD_LISTENER";
    private static final String ATTACHMENT_DELETE_LISTENER = "ATTACHMENT_DELETE_LISTENER";

    /**
     * Gets the attachment removal {@link SubscriptionTopic}.
     *
     * @param type Refers the queue type.
     * @return The attachment removal {@link SubscriptionTopic}.
     */
    public static SubscriptionTopic getAttachmentRemovalTopic(final QueueModel.Type type) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(type, ATTACHMENT_DELETE_LISTENER);
    }

    /**
     * Gets the attachment upload {@link SubscriptionTopic}.
     *
     * @param type Refers the queue type.
     * @return The attachment upload {@link SubscriptionTopic}.
     */
    public static SubscriptionTopic getAttachmentUploadTopic(final QueueModel.Type type) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(type, ATTACHMENT_UPLOAD_LISTENER);
    }
}
