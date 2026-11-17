package com.tlc.crm.common.iam.internal;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.field.CrmUserFields;

import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

import static com.tlc.crm.common.iam.internal.UserTransferStatusHolder.UserTransferStatus.PROCESSING;
import static com.tlc.crm.common.iam.internal.UserTransferStatusHolder.UserTransferStatus.TRANSFERRED;
import static com.tlc.crm.common.iam.internal.UserTransferStatusHolder.UserTransferStatus.TRANSFER_FAILED;

/**
 * <p>
 * A Processor that process the user transfer requests and triggers the respective user migrator actions based on the request parameters
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class UserDataMigrationProcessor implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDataMigrationProcessor.class);
    private final UserRecordMigrationHandler userRecordMigrationHandler;
    private final UserTransferStatusHolder userTransferStatusHolder;

    public UserDataMigrationProcessor() {
        userRecordMigrationHandler = UserRecordMigrationHandler.getInstance();
        userTransferStatusHolder = UserTransferStatusHolder.getInstance();
    }

    /**
     * <p>
     * Triggers the user data transfer action.
     * </p>
     *
     * @param streamMessage A record that holds the user transfer request parameters
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        final JsonObject requestParameters = Json.object(streamMessage.data());
        final Long orgId = requestParameters.getLong(CrmUserFields.ORG_ID.getName());
        final Long targetUserId = requestParameters.getLong(CrmUserFields.TARGET_USER_ID.getName());
        final String requestId = streamMessage.id();
        final JsonArray sourceUserArray = requestParameters.getJsonArray(CrmUserFields.SOURCE_USER_IDS.getName());
        final Collection<Long> sourceUserIds = new ArrayList<>();

        for (int index = 0; index < sourceUserArray.size(); index++) {
            sourceUserIds.add(sourceUserArray.getLong(index));
        }

        try {
            userTransferStatusHolder.setTransferStatus(requestId, PROCESSING);
            userRecordMigrationHandler.transferRecords(orgId, targetUserId, sourceUserIds);
            userTransferStatusHolder.setTransferStatus(requestId, TRANSFERRED);
        } catch (Throwable throwable) {
            LOGGER.error("User data migration action failed : {}", throwable.getMessage());
            LOGGER.debug("Stack : ", throwable);
            userTransferStatusHolder.setTransferStatus(requestId, TRANSFER_FAILED);
        }
    }
}
