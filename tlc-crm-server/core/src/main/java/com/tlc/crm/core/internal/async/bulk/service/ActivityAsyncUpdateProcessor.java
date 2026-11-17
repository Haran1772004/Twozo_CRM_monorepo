package com.tlc.crm.core.internal.async.bulk.service;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.core.features.api.activity.model.converter.SourceActivityConverter;
import com.tlc.crm.core.internal.async.bulk.processor.AsyncUpdateProcessor;
import com.tlc.crm.core.internal.event.email.EmailAlertDataPublisher;
import com.tlc.crm.core.internal.field.BulkActionFields;
import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.notification.app.Event;
import com.tlc.crm.security.iam.LoggedInUserInfo;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * <p>
 * Processor responsible for handling async update operations for {@link ActivityHolder} entities
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
final class ActivityAsyncUpdateProcessor extends AsyncUpdateProcessor<ActivityHolder> {

    private final SourceActivityConverter sourceActivityConverter;
    private final SourceActivityManager sourceActivityManager;
    private final EmailAlertDataPublisher emailAlertDataPublisher;

    ActivityAsyncUpdateProcessor() {
        this.sourceActivityConverter = SourceActivityConverter.getInstance();
        this.sourceActivityManager = SourceActivityManager.getInstance();
        this.emailAlertDataPublisher = EmailAlertDataPublisher.getInstance();
    }

    /**
     * <p>
     * Updates the field for the given request
     * </p>
     *
     * @param orgId         Represents an org id
     * @param userId        Represents a user id
     * @param id            Represents the activity id
     * @param isPartialData A flag indicating whether the update is partial or full
     * @param dataJson      Represents data {@link JsonObject}
     */
    protected void updateField(final Long orgId, final Long userId, final long id,
                               final boolean isPartialData, final JsonObject dataJson) {
        final CrmUser crmUser = LoggedInUserInfo.getUser();
        final UserRegionalPreference userRegionalPreference = crmUser.getPreferenceHolder().get(UserRegionalPreference.TYPE);
        final Locale locale = Locale.forLanguageTag(userRegionalPreference.getLanguage().getName());
        final ActivityHolder holder = sourceActivityManager.get(orgId, id);
        final EventData eventData = new SecureEventData(orgId, userId);
        final ActivityHolder existingHolder = cloneModel(orgId, holder);
        final boolean isCompleted = existingHolder.getActivity().getStatus().isCompleted();

        eventData.setPreviousValues(List.of(existingHolder));
        mapJsonToModel(new ConverterConfig(orgId, userId, locale,
                isPartialData, dataJson.getJsonObject(BulkActionFields.UPDATE_ITEM)), holder);

        eventData.setAction(Action.UPDATE);
        eventData.setValues(List.of(holder));

        eventDataHandler.handleEvents(eventData, Event.Type.PRE);
        sourceActivityManager.update(holder);
        eventDataHandler.handleEvents(eventData, Event.Type.POST);

        if (!isCompleted && holder.getActivity().getStatus().isCompleted()) {
            emailAlertDataPublisher.publish(orgId, userId, Action.CREATE,
                    List.of(id), EmailNotificationTemplate.ACTIVITY_COMPLETED.getName());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Refers the organization id.
     * @param model Refers the model to be cloned.
     * @return The cloned {@link ActivityHolder}
     */
    @Override
    protected ActivityHolder cloneModel(final long orgId, final ActivityHolder model) {
        final ActivityHolder activityHolder = super.cloneModel(orgId, model);
        final Activity activity = activityHolder.getActivity();

        if (Objects.nonNull(activity)) {
            activity.setOrgId(orgId);
            activity.setId(model.id());
        }
        return activityHolder;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Source}
     */
    protected Source getSource() {
        return Source.ACTIVITY;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterConfig Represents the {@link ConverterConfig}
     * @param activityHolder  Represents the model to be updated
     */
    @Override
    protected void mapJsonToModel(final ConverterConfig converterConfig, final ActivityHolder activityHolder) {
        final JsonObject dataJson = converterConfig.dataToBeConverted();

        if (converterConfig.isPartialData()) {
            sourceActivityConverter.setJsonToActivity(converterConfig, activityHolder, dataJson);
        } else {
            sourceActivityConverter.convert(converterConfig, dataJson);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link AbstractConfigManager} instance
     */
    @Override
    protected AbstractConfigManager<ActivityHolder> getAbstractConfigManager() {
        return sourceActivityManager;
    }
}
