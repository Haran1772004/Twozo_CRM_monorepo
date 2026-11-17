package com.tlc.crm.entity.iam.initiator;

import com.tlc.crm.common.iam.OrgInitiator;
import com.tlc.crm.common.iam.OrgInitiatorAction;
import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.entity.features.api.activity.manager.ActivityTypeManager;
import com.tlc.crm.entity.features.api.activity.model.ActivityType;
import com.tlc.crm.icon.model.Icon;
import com.tlc.crm.icon.service.IconService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

/**
 * <p>
 * Provides the initializer to execute the create action of the default {@link ActivityType} for the organisation.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@OrgInitiator(name = "activity")
public class ActivityInitializer implements OrgInitiatorAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityInitializer.class);

    private final ActivityTypeManager activityTypeManager;
    private final IconService iconService;

    public ActivityInitializer() {
        activityTypeManager = ActivityTypeManager.getInstance();
        iconService = IconService.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(final CrmOrg crmOrg, final CrmUser crmUser) {
        final Long orgId = crmOrg.id();
        final long userId = crmUser.id();
        LOGGER.info("Org Init Action: Activity initialization started. Org ID: {}, User ID: {}", orgId, userId);

        final Collection<ActivityType> activityTypes = defaultActivityTypes();

        for (final ActivityType activityType : activityTypes) {
            activityType.setOrgId(orgId);
            activityTypeManager.create(activityType);
        }
        LOGGER.info("Org Init Action: Activity initialization completed. Org ID: {}, User ID: {}", orgId, userId);
    }

    /**
     * <p>
     * Retrieves a collection of {@link ActivityType} based on the default activity types defined in the {@link DefaultActivityType} enum.
     * </p>
     *
     * @return a collection of {@link ActivityType} with the properties set based on the {@link DefaultActivityType} enum values.
     */
    private Collection<ActivityType> defaultActivityTypes() {
        final Collection<ActivityType> defaultActivityTypes = new ArrayList<>();

        for (final DefaultActivityType defaultActivityType : DefaultActivityType.values()) {
            final ActivityType activityType = new ActivityType();

            activityType.setName(defaultActivityType.getName());
            activityType.setActive(true);
            activityType.setActivityIcon(iconService.fetchIconByTypeAndName(Icon.Type.ACTIVITY, defaultActivityType.getIconName()));
            defaultActivityTypes.add(activityType);
        }

        return defaultActivityTypes;
    }

    /**
     * <p>
     * Enum representing the default activity types with their corresponding names and icon names.
     * </p>
     */
    private enum DefaultActivityType {

        QUOTE("Quote", "quote"),
        LUNCH("Lunch", "lunch"),
        TASK("Task", "task"),
        MEETING("Meeting", "meeting"),
        MAIL("Mail", "mail"),
        CALL("Call", "call"),
        ;

        private final String name;
        private final String iconName;

        DefaultActivityType(final String name, final String iconName) {
            this.name = name;
            this.iconName = iconName;
        }

        public String getName() {
            return name;
        }

        public String getIconName() {
            return iconName;
        }
    }
}
