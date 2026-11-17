package com.tlc.crm.core.list.dynamic;

import com.tlc.crm.core.conversation.api.manager.EmailEntityConversationManager;
import com.tlc.crm.core.list.dynamic.model.ListDynamicFields;
import com.tlc.crm.core.notification.email.api.manager.EntityMailManager;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.list.dynamic.model.DynamicFieldRequest;
import com.tlc.crm.entity.list.dynamic.model.DynamicFieldsData;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * <p>
 * Implements {@link DynamicFieldsFetcher} the for fetching dynamic field data to email fields
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class EmailFieldsFetcher extends DynamicFieldsFetcher {

    private final EntityMailManager entityMailManager;
    private final EmailEntityConversationManager emailEntityConversationManager;

    private EmailFieldsFetcher() {
        entityMailManager = EntityMailManager.getInstance();
        emailEntityConversationManager = EmailEntityConversationManager.getInstance();
    }

    private static class InstanceHolder {
        private static final EmailFieldsFetcher INSTANCE = new EmailFieldsFetcher();
    }

    /**
     * <p>
     * Gets the {@link EmailFieldsFetcher} instance
     * </p>
     *
     * @return The {@link EmailFieldsFetcher}
     */
    public static EmailFieldsFetcher getInstance() {
        return EmailFieldsFetcher.InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param dynamicFieldRequest Represents the {@link DynamicFieldRequest}
     * @return The {@link DynamicFieldsData}
     */
    @Override
    public DynamicFieldsData get(final DynamicFieldRequest dynamicFieldRequest) {
        final Collection<String> dynamicFields = dynamicFieldRequest.dynamicFields();
        final Source source = dynamicFieldRequest.provider().getSource();
        final Collection<Long> entityIds = dynamicFieldRequest.entityIds();
        final long orgId = dynamicFieldRequest.orgId();
        final boolean isEmailMessageCountRequired = dynamicFields.stream().anyMatch(ListDynamicFields.EMAIL_MESSAGE_COUNT::contains);
        final boolean isLastEmailReceivedRequired = dynamicFields.stream().anyMatch(ListDynamicFields.LAST_EMAIL_RECEIVED::contains);
        final boolean isLastEmailSentRequired = dynamicFields.stream().anyMatch(ListDynamicFields.LAST_EMAIL_SENT::contains);
        final Map<String, Map<String, Object>> entityRows = new HashMap<>();
        final DynamicFieldsData dynamicFieldsData = new DynamicFieldsData();

        final Map<Long, Long> entityEmailMessageCounts = isEmailMessageCountRequired
                ? emailEntityConversationManager.getEmailMessageCounts(orgId, entityIds, source)
                : Collections.emptyMap();

        final Map<Long, Long> entityRecentEmailReceivedTimes = isLastEmailReceivedRequired
                ? entityMailManager.getEntityRecentEmailReceivedTimes(orgId, entityIds, source)
                : Collections.emptyMap();

        final Map<Long, Long> entityRecentEmailSentTimes = isLastEmailSentRequired
                ? entityMailManager.getEntityRecentEmailSentTimes(orgId, entityIds, source)
                : Collections.emptyMap();

        final BiConsumer<Map<Long, Long>, String> populateFieldValuesInEntities = (dataMap, fieldKey) ->
                dataMap.forEach((entityId, value) -> entityRows.computeIfAbsent(entityId.toString(),
                        id -> new HashMap<>()).put(fieldKey, value));

        populateFieldValuesInEntities.accept(entityEmailMessageCounts, ListDynamicFields.EMAIL_MESSAGE_COUNT);
        populateFieldValuesInEntities.accept(entityRecentEmailReceivedTimes, ListDynamicFields.LAST_EMAIL_RECEIVED);
        populateFieldValuesInEntities.accept(entityRecentEmailSentTimes, ListDynamicFields.LAST_EMAIL_SENT);

        dynamicFieldsData.addAll(entityRows);

        return dynamicFieldsData;
    }
}
