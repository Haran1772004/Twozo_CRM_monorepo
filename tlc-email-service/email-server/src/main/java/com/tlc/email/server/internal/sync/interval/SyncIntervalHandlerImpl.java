package com.tlc.email.server.internal.sync.interval;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.email.common.fields.EmailFields;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.config.EmailInterval;
import com.tlc.email.common.model.config.EmailReceiverConfiguration;
import com.tlc.email.common.model.config.FolderType;
import com.tlc.email.common.model.config.FolderType.FilterMode;
import com.tlc.email.common.request.SyncRequest;
import com.tlc.email.common.model.config.FolderInterval;

import com.tlc.email.server.fields.FolderIntervalFields;
import com.tlc.email.server.internal.manager.SyncSettingsManager;

import java.time.Instant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.stream.IntStream;

/**
 * Handles the sync of email intervals for different folders as per the configuration.
 * Processes {@link PriorityQueue<FolderInterval>}, retrieves folder-specific email intervals,
 * and identifies non-overlapping intervals between the input request and existing intervals in each folder.
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see SyncIntervalHandler
 */
class SyncIntervalHandlerImpl implements SyncIntervalHandler {

    private static final String START_TIME = "startTime";
    private static final String END_TIME = "endTime";

    private SyncIntervalHandlerImpl() {
    }

    /**
     * <p>
     * Holds the single instance of {@link SyncIntervalHandler}.
     * </p>
     */
    private static class Instance {
        private static final SyncIntervalHandler EMAIL_SYNC_HANDLER = new SyncIntervalHandlerImpl();
    }

    public static SyncIntervalHandler getInstance() {
        return Instance.EMAIL_SYNC_HANDLER;
    }

    /**
     * <p>
     * Fetches the {@link FolderInterval} for auto sync.
     * </p>
     *
     * @param emailAddress Represents the {@link EmailAddress}.
     * @return {@link FolderInterval}
     */
    @Override
    public PriorityQueue<FolderInterval> fetchAutoSyncIntervals(final EmailAddress emailAddress) {
        final Optional<String> dateConfig = SyncSettingsManager.getInstance().getDateConfig(emailAddress.getEmailAddress());
        final PriorityQueue<FolderInterval> folderIntervals = new PriorityQueue<>();

        if (dateConfig.isEmpty()) {
            return folderIntervals;
        }
        final Map<FolderType, List<EmailInterval>> folderIntervalMap = retrieveEmailIntervalsByFolderType(dateConfig.get());

        if (folderIntervalMap.isEmpty()) {
            return folderIntervals;
        }
        final long currentTime = Instant.now().toEpochMilli();

        for (Map.Entry<FolderType, List<EmailInterval>> entry : folderIntervalMap.entrySet()) {
            final FolderType folderType = entry.getKey();
            final List<EmailInterval> emailIntervals = entry.getValue();

            if (Objects.isNull(emailIntervals) || emailIntervals.isEmpty()) {
                continue;
            }
            final long lowestStartTime = emailIntervals.stream()
                    .mapToLong(EmailInterval::startTime)
                    .min()
                    .orElseGet(() -> Long.MIN_VALUE);
            final Queue<EmailInterval> intervalQueue = new LinkedList<>();

            intervalQueue.add(new EmailInterval(lowestStartTime, currentTime));
            final FolderInterval folderInterval = new FolderInterval();

            folderInterval.setFolder(folderType);
            folderInterval.setEmailIntervals(intervalQueue);

            folderIntervals.add(folderInterval);
        }

        return folderIntervals;
    }

    /**
     * <p>
     * Retrieves email intervals for each {@link FolderType} using the provided date configuration.
     * Each queue contains {@link EmailInterval}, sorted by start time in descending order.
     * </p>
     *
     * @param dateConfig Represents the data config
     * @return A map where each {@link FolderType} is associated with a {@link Queue} of {@link EmailInterval},
     * containing intervals sorted by start time.
     */
    @Override
    public Map<FolderType, List<EmailInterval>> retrieveEmailIntervalsByFolderType(final String dateConfig) {
        final Map<FolderType, List<EmailInterval>> intervals = new EnumMap<>(FolderType.class);
        final JsonObject dateConfigJson = Json.object(dateConfig);

        for (final FolderType folderType : FolderType.values()) {
            final String folderName = folderType.name();

            if (dateConfigJson.containsKey(folderName)) {
                final JsonArray intervalArray = dateConfigJson.getJsonArray(folderName);
                final List<EmailInterval> intervalQueue = new ArrayList<>();
                final int size = intervalArray.size();

                for (int intervalIndex = 0; intervalIndex < size; intervalIndex++) {
                    final JsonObject intervalJson = intervalArray.getJsonObject(intervalIndex);

                    intervalQueue.add(new EmailInterval(intervalJson.getLong(FolderIntervalFields.START_TIME),
                            intervalJson.getLong(FolderIntervalFields.END_TIME)));
                }
                intervals.put(folderType, intervalQueue);
            }
        }

        return intervals;
    }

    /**
     * <p>
     * Converts the {@link EmailInterval} to {@link JsonObject}.
     * </p>
     *
     * @param interval Represents the {@link EmailInterval}
     * @return to interval json
     */
    private JsonObject toJson(final EmailInterval interval) {
        return toJson(interval.startTime(), interval.endTime());
    }

    /**
     * <p>
     * Converts the {@link EmailInterval} to {@link JsonObject}.
     * </p>
     *
     * @param startTime Represents the startTime
     * @param endTime   Represents the endTime
     * @return to interval json
     */
    private JsonObject toJson(final long startTime, final long endTime) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(EmailFields.START_TIME, startTime);
        jsonObject.put(EmailFields.END_TIME, endTime);

        return jsonObject;
    }

    /**
     * <p>
     * Loads the synchronized and unsynchronized email intervals based on the given new email interval
     * and existing email intervals.
     * </p>
     *
     * @param newEmailInterval      The new email interval to process.
     * @param existingEmailInterval The list of existing email intervals.
     * @param syncedEmailInterval   The list to store synced email intervals.
     * @param unSyncedEmailInterval The list to store unsynced email intervals.
     */
    private void loadSyncAndUnsyncInterval(final EmailInterval newEmailInterval, final List<EmailInterval> existingEmailInterval,
                                           final List<EmailInterval> syncedEmailInterval, final List<EmailInterval> unSyncedEmailInterval) {
        final int existingEmailIntervalSize = existingEmailInterval.size();
        int indexTrackingPointer = 0;

        final long newStartTime = newEmailInterval.startTime();
        final long newEndTime = newEmailInterval.endTime();
        long lastProcessedTime = newStartTime;

        while (indexTrackingPointer < existingEmailIntervalSize) {
            final EmailInterval existingInterval = existingEmailInterval.get(indexTrackingPointer);
            final long existingStartTime = existingInterval.startTime();
            final long existingEndTime = existingInterval.endTime();

            if (existingStartTime >= newEndTime) {
                break;
            }

            if (lastProcessedTime < existingStartTime) {
                unSyncedEmailInterval.add(new EmailInterval(lastProcessedTime, existingStartTime));
            }

            if (newStartTime < existingEndTime) {
                syncedEmailInterval.add(new EmailInterval(Math.max(newStartTime, existingStartTime), Math.min(newEndTime, existingEndTime)));
            }

            lastProcessedTime = Math.max(lastProcessedTime, existingEndTime);
            indexTrackingPointer++;
        }

        if (lastProcessedTime < newEndTime) {
            unSyncedEmailInterval.add(new EmailInterval(lastProcessedTime, newEndTime));
        }
    }

    /**
     * <p>
     * Loads synchronized and un synchronized intervals for email folders.
     * </p>
     *
     * @param syncRequest            The sync request containing email sync configurations.
     * @param syncedFolderInterval   The list to store synced folder intervals.
     * @param unSyncedFolderInterval The list to store un synced folder intervals.
     */
    public void loadSyncAndUnsyncedIntervals(final SyncRequest syncRequest,
                                             final List<FolderInterval> syncedFolderInterval,
                                             final List<FolderInterval> unSyncedFolderInterval) {
        final EmailReceiverConfiguration configuration = syncRequest.getReceiverConfiguration();
        final EmailAddress emailAddress = configuration.getSyncEmailAddress();
        final Optional<String> dateConfig = SyncSettingsManager.getInstance().getDateConfig(emailAddress.getEmailAddress());
        final Map<FolderType, EmailInterval> emailIntervalMap = configuration.getEmailIntervals();
        final Collection<FolderType> folderTypes = configuration.getFolderTypes();

        if (dateConfig.isPresent()) {
            final Map<FolderType, List<EmailInterval>> folderIntervalMap = retrieveEmailIntervalsByFolderType(dateConfig.get());

            if (!folderIntervalMap.isEmpty()) {

                for (final FolderType folder : folderTypes) {
                    final EmailInterval emailInterval = emailIntervalMap.get(folder);

                    if (folderIntervalMap.containsKey(folder)) {
                        final List<EmailInterval> syncedEmailInterval = new ArrayList<>();
                        final List<EmailInterval> unSyncedEmailInterval = new ArrayList<>();

                        loadSyncAndUnsyncInterval(emailInterval, folderIntervalMap.get(folder), syncedEmailInterval, unSyncedEmailInterval);

                        if (!syncedEmailInterval.isEmpty()) {
                            syncedFolderInterval.add(getFolderIntervals(folder, syncedEmailInterval));
                        }

                        if (!unSyncedEmailInterval.isEmpty()) {
                            unSyncedFolderInterval.add(getFolderIntervals(folder, unSyncedEmailInterval));
                        }
                    } else {
                        unSyncedFolderInterval.add(getFolderIntervals(folder, List.of(emailInterval)));
                    }
                }
            }
        } else {

            for (final FolderType folder : folderTypes) {
                unSyncedFolderInterval.add(getFolderIntervals(folder, List.of(emailIntervalMap.get(folder))));
            }
        }
    }

    /**
     * <p>
     * Creates a FolderInterval object with the specified folder type and email intervals.
     * </p>
     *
     * @param folderType     The type of folder.
     * @param emailIntervals The email intervals associated with the folder.
     * @return A FolderInterval object containing the folder type and email intervals.
     */
    private FolderInterval getFolderIntervals(final FolderType folderType, final Collection<EmailInterval> emailIntervals) {
        final FolderInterval folderInterval = new FolderInterval();

        folderInterval.setFolder(folderType);
        folderInterval.setEmailIntervals(emailIntervals);

        return folderInterval;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param dateConfig    Represents the date config
     * @param folderName    Represents the folder name
     * @param emailInterval Represents the {@link EmailInterval}
     * @return the data config json as string
     */
    @Override
    public String getUpdatedDateConfig(final String dateConfig, final String folderName, final EmailInterval emailInterval) {
        final JsonObject dateConfigJson = Objects.isNull(dateConfig) ? Json.object() : Json.object(dateConfig);

        if (dateConfigJson.containsKey(folderName)) {
            dateConfigJson.put(folderName, mergeInterval(emailInterval, dateConfigJson.getJsonArray(folderName)));
        } else {
            final JsonArray array = Json.array();

            array.put(toJson(emailInterval));
            dateConfigJson.put(folderName, array);
        }

        return dateConfigJson.toString();
    }

    /**
     * <p>
     * Merges a given email interval with an existing list of email intervals.
     * If the new interval overlaps or is adjacent to any existing interval(s),
     * they will be merged into a single continuous interval.
     * </p>
     *
     * <p>
     * For more details, see:
     * </p>
     *
     * @param currentEmailInterval   The new {@link JsonObject} representing the email interval to be merged.
     * @param existingEmailIntervals The {@link JsonArray} of existing email intervals.
     * @return A {@link JsonArray} containing the updated list of merged email intervals.
     */

    private JsonArray mergeInterval(final EmailInterval currentEmailInterval, final JsonArray existingEmailIntervals) {
        final long currentStartTime = currentEmailInterval.startTime();
        final long currentEndTime = currentEmailInterval.endTime();

        final List<JsonObject> newEmailIntervals = new ArrayList<>();
        final int existingIntervalSize = existingEmailIntervals.size();

        int indexTrackingPointer = 0;
        boolean merged = false;

        final List<JsonObject> sortedIntervals = IntStream.range(0, existingIntervalSize)
                .mapToObj(existingEmailIntervals::getJsonObject)
                .sorted(Comparator.comparing(interval -> interval.getLong(START_TIME)))
                .toList();

        while (indexTrackingPointer < existingIntervalSize) {
            final JsonObject existingEmailInterval = sortedIntervals.get(indexTrackingPointer);
            long existingStartTime = existingEmailInterval.getLong(START_TIME);
            long existingEndTime = existingEmailInterval.getLong(END_TIME);

            if (currentStartTime >= existingStartTime && currentEndTime <= existingEndTime) {
                return existingEmailIntervals;
            }

            if (currentStartTime <= existingEndTime && currentEndTime >= existingStartTime) {
                final long mergedStart = Math.min(existingStartTime, currentStartTime);
                long mergedEnd = Math.max(existingEndTime, currentEndTime);

                while (indexTrackingPointer + 1 < existingIntervalSize) {
                    final JsonObject nextInterval = sortedIntervals.get(indexTrackingPointer + 1);
                    long nextStartTime = nextInterval.getLong(START_TIME);
                    long nextEndTime = nextInterval.getLong(END_TIME);

                    if (mergedEnd >= nextStartTime) {
                        mergedEnd = Math.max(mergedEnd, nextEndTime);
                        indexTrackingPointer++;
                    } else {
                        break;
                    }
                }

                newEmailIntervals.add(toJson(mergedStart, mergedEnd));
                merged = true;
            } else {
                newEmailIntervals.add(existingEmailInterval);
            }

            indexTrackingPointer++;
        }

        if (!merged) {
            newEmailIntervals.add(toJson(currentStartTime, currentEndTime));
        }
        final JsonArray mergedJsonArray = Json.array();

        newEmailIntervals.forEach(mergedJsonArray::put);

        return mergedJsonArray;
    }
}
