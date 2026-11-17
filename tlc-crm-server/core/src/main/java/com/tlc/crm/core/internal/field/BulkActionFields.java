package com.tlc.crm.core.internal.field;

/**
 * <p>
 * Represents the model bulk action fields
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public final class BulkActionFields {

    private BulkActionFields() {
    }

    public static final String CONSUMER_NAME = "consumer.name";
    public static final String CONSUMER_MAX_RECORDS = "consumer.max.records";
    public static final String CONSUMER_POLL_DURATION = "consumer.poll.duration";
    public static final String CONSUMER_MAX_QUEUE_SIZE = "consumer.max.queue.size";
    public static final String SOURCE = "source";
    public static final String DATA = "data";
    public static final String IDS = "ids";
    public static final String TYPE = "type";
    public static final String ID = "id";
    public static final String UPDATE_ITEM = "updateItem";
    public static final String TOPIC = "topic";
    public static final String EVENT = "event";
    public static final String UPDATE = "update";
    public static final String PARTIAL_BULK_UPDATE = "pBulkUpdate";
    public static final String BULK_UPDATE = "bUpdate";
    public static final String ASYNC_BULK_ACTION_NAMES = "asyncBulkActionNames";
    public static final String THREAD_POOL = "threadPool";
    public static final String TASK_CONTROLLER = "taskController";
    public static final String USER_ID = "userId";
    public static final String FOLLOWER_IDS = "followerIds";
    public static final String BULK_ASSIGN_OWNER = "bAssignOwner";
    public static final String BULK_ASSIGN_FOLLOWERS = "bAssignFollowers";
    public static final String BULK_UPDATE_FOLLOWERS = "bUpdateFollowers";
    public static final String ADDED_FOLLOWER_IDS = "addedFollowerIds";
    public static final String REMOVED_FOLLOWER_IDS = "removedFollowerIds";
    public static final String DELETE_MAPPED_TAG = "deleteMappedTag";
    public static final String MAP_TAG = "mapTag";
    public static final String TAGS = "tags";
    public static final String TAG = "tag";
    public static final String TAG_IDS = "tagIds";
    public static final String BULK_UPDATE_ARCHIVE_STATE = "bUpdateArchiveState";
    public static final String IS_ARCHIVED = "isArchived";
    public static final String BULK_CREATE_ACTIVITY = "bCreateActivity";
    public static final String ACTIVITY = "activity";
    public static final String BULK_COMMIT = "bulkCommit";
    public static final String BULK_COPY_CONTACTS = "bCopyContacts";
    public static final String BULK_MOVE_CONTACTS = "bMoveContacts";
    public static final String BULK_REMOVE_CONTACTS = "bRemoveContacts";
    public static final String BULK_UPDATE_SUBSCRIPTION_DATA = "bUpdateSubscriptionData";
    public static final String BULK_ADD_TO_MARKETING_LIST = "bAddToMarketingList";
    public static final String COLOR = "color";
}
