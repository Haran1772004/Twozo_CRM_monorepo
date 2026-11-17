package com.tlc.crm.core.features.internal.dataimport.loader.source;

import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.core.features.internal.dataimport.builder.model.ModelBuilder;
import com.tlc.crm.core.features.internal.dataimport.builder.model.SourceActivityBuilder;
import com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer;
import com.tlc.crm.core.features.internal.dataimport.container.SourceRecordContainer;
import com.tlc.crm.core.features.internal.dataimport.loader.AbstractModelLoader;
import com.tlc.crm.core.features.internal.dataimport.loader.DataHolder;
import com.tlc.crm.core.features.internal.dataimport.tracker.ErrorTracker;
import com.tlc.crm.entity.features.api.activity.manager.ActivityTypeManager;
import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.features.api.activity.model.ActivityType;
import com.tlc.crm.entity.features.api.activity.model.Availability;
import com.tlc.crm.entity.features.api.activity.model.Priority;
import com.tlc.crm.entity.features.api.activity.model.Reminder;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.I18nService;
import com.tlc.i18n.service.provider.I18nServiceProvider;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Loader to load the {@link Activity} models into the {@link SourceModelContainer}
 *
 * @author sundar
 * @version 1.0
 */
public final class SourceActivityLoader extends AbstractModelLoader<ActivityHolder> {

    private final SourceActivityBuilder sourceActivityBuilder;
    private final ActivityTypeManager activityTypeManager;
    private final I18nService i18nService;

    private SourceActivityLoader() {
        super(Source.ACTIVITY);
        this.sourceActivityBuilder = SourceActivityBuilder.getInstance();
        this.activityTypeManager = ActivityTypeManager.getInstance();
        this.i18nService = I18nServiceProvider.get();
    }

    private static final class Instance {
        private static final SourceActivityLoader SOURCE_ACTIVITY_LOADER = new SourceActivityLoader();
    }

    /**
     * Gets the instance of {@link SourceActivityLoader}
     *
     * @return {@link SourceActivityLoader} instance
     */
    public static SourceActivityLoader getInstance() {
        return Instance.SOURCE_ACTIVITY_LOADER;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link SourceActivityBuilder}
     */
    @Override
    protected ModelBuilder<ActivityHolder> getModelBuilder() {
        return sourceActivityBuilder;
    }

    /**
     * {@inheritDoc}
     *
     * @param dataHolder   Which holds the data to be converted to {@link Activity} and load into the {@link SourceModelContainer}
     * @param errorTracker Tracks the errors that arise while loading the {@link Activity} into the {@link SourceModelContainer}
     */
    @Override
    public void load(final DataHolder dataHolder, final ErrorTracker errorTracker) {
        final SourceRecordContainer sourceRecordContainer = dataHolder.sourceRecordContainer();
        final Map<Integer, ActivityHolder> convertedActivityHolders = new HashMap<>();

        loadDistinctValues(dataHolder.orgId(), sourceRecordContainer, errorTracker.getImportId());
        loadModels(dataHolder, sourceRecordContainer.getMergedIndicesWithinTheSheet(source), convertedActivityHolders, errorTracker);
        loadMissingValues(convertedActivityHolders);
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceRecordContainer Represents the {@link SourceRecordContainer}
     * @param importField           Represents the {@link FieldInfo}
     * @param distinctValues        Contains a collection of distinct values used to fetch relevant values and map them to the corresponding JSON for the given field
     * @return {@link DistinctValueHolder} containing the main values of distinct values and a default value
     */
    protected DistinctValueHolder getDistinctValueHolder(final long orgId, final SourceRecordContainer sourceRecordContainer, final FieldInfo importField,
                                                         final Collection<Object> distinctValues) {
        final String fieldAliasName = importField.aliasName();

        return switch (fieldAliasName) {
            case "typeId" -> getActivityTypeValueHolder(orgId, distinctValues);
            case "availability" -> getAvailabilityValueHolder();
            case "isDone" -> new DistinctValueHolder(Map.of("yes", true), false);
            case "priority" -> getPriorityValueHolder();
            case "reminder" -> getReminderValueHolder();
            default -> null;
        };
    }

    /**
     * Gets the {@link DistinctValueHolder} containing the pertinent values associated with the specified distinct values of the reminder
     *
     * @return {@link DistinctValueHolder} containing the relevant distinct values associated with the specified distinct values of the reminder
     */
    private DistinctValueHolder getReminderValueHolder() {
        final Map<String, Integer> reminderIds = new HashMap<>();
        final int defaultReminderId = Reminder.NO_REMINDER.getId();
        final I18nResolver i18nResolver = i18nService.getResolver(Locale.US); // TODO: Locale should be get from the current user's preferences

        for (final Reminder reminder : Reminder.values()) {
            final String reminderName = i18nResolver.get(reminder.getDisplayName());

            if (Objects.nonNull(reminderName)) {
                reminderIds.put(reminderName.toLowerCase(), reminder.getId());
            }
        }
        return new DistinctValueHolder(Collections.unmodifiableMap(reminderIds), defaultReminderId);
    }

    /**
     * Gets the {@link DistinctValueHolder} containing the pertinent values associated with the specified distinct values of the priority
     *
     * @return {@link DistinctValueHolder} containing the relevant distinct values associated with the specified distinct values of the priority
     */
    private DistinctValueHolder getPriorityValueHolder() {
        final Map<String, Integer> priorityIds = new HashMap<>();
        final int defaultPriorityId = Priority.MEDIUM.getId();
        final I18nResolver i18nResolver = i18nService.getResolver(Locale.US); // TODO: Locale should be get from the current user's preferences

        for (final Priority priority : Priority.values()) {
            final String priorityName = i18nResolver.get(priority.getDisplayName());

            if (Objects.nonNull(priorityName)) {
                priorityIds.put(priorityName.toLowerCase(), priority.getId());
            }
        }
        return new DistinctValueHolder(Collections.unmodifiableMap(priorityIds), defaultPriorityId);
    }

    /**
     * Gets the {@link DistinctValueHolder} containing the pertinent values associated with the specified distinct values of the availability
     *
     * @return {@link DistinctValueHolder} containing the relevant distinct values associated with the specified distinct values of the availability
     */
    private DistinctValueHolder getAvailabilityValueHolder() {
        final Map<String, Integer> availabilityIds = new HashMap<>();
        final int defaultAvailabilityId = Availability.BUSY.getId();
        final I18nResolver i18nResolver = i18nService.getResolver(Locale.US); // TODO: Locale should be get from the current user's preferences

        for (final Availability availability : Availability.values()) {
            final String availabilityName = i18nResolver.get(availability.getDisplayName());

            if (Objects.nonNull(availabilityName)) {
                availabilityIds.put(availabilityName.toLowerCase(), availability.getId());
            }
        }
        return new DistinctValueHolder(Collections.unmodifiableMap(availabilityIds), defaultAvailabilityId);
    }

    /**
     * Gets the {@link DistinctValueHolder} containing the pertinent values associated with the specified distinct values of the activity type
     *
     * @param activityTypeValues Contains the distinct values
     * @return {@link DistinctValueHolder} containing the relevant distinct values associated with the specified distinct values of the activity type
     */
    private DistinctValueHolder getActivityTypeValueHolder(final long orgId, final Collection<Object> activityTypeValues) {
        final Map<String, Long> groupedDistinctActivityTypes;

        if (!activityTypeValues.isEmpty()) {
            final Collection<String> typeNames = activityTypeValues.stream().map(Objects::toString)
                    .collect(Collectors.toUnmodifiableSet());
            final Collection<ActivityType> activityTypes = activityTypeManager.fetchActivityTypeByName(orgId, typeNames);
            groupedDistinctActivityTypes = activityTypes.stream()
                    .collect(Collectors.toMap(activityType -> activityType.getName().toLowerCase(), ActivityType::id, (existing, replacement) -> existing));
        } else {
            groupedDistinctActivityTypes = Collections.emptyMap();
        }
        final ActivityType defaultActivityType = activityTypeManager.getDefaultActivityType(orgId);

        return new DistinctValueHolder(Collections.unmodifiableMap(groupedDistinctActivityTypes), defaultActivityType.id());
    }

    /**
     * Loads the missing values like activity title, status of the converted {@link ActivityHolder}
     *
     * @param convertedActivityHolders Which contains the {@link Collection} of {@link ActivityHolder} grouped with row index
     */
    private void loadMissingValues(final Map<Integer, ActivityHolder> convertedActivityHolders) {

        if (!convertedActivityHolders.isEmpty()) {
            final Map<Long, String> activityNameMap = new HashMap<>();

            for (final Map.Entry<Integer, ActivityHolder> activityHolderEntry : convertedActivityHolders.entrySet()) {
                final Activity activity = activityHolderEntry.getValue().getActivity();
                final ActivityType activityType = activity.getType();

                if (Objects.isNull(activity.getTitle()) && Objects.nonNull(activityType)) {
                    activity.setTitle(activityNameMap.computeIfAbsent(activityType.id(),
                            activityTypeId -> activityTypeManager.fetchName(activity.orgId(), activityTypeId)));
                }
            }
        }
    }
}