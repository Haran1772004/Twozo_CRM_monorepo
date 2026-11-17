package com.tlc.crm.core.internal.async.bulk.service;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.handler.EventDataHandler;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.core.features.api.activity.model.converter.SourceActivityConverter;
import com.tlc.crm.core.internal.field.BulkActionFields;
import com.tlc.crm.notification.app.Event;
import com.tlc.crm.security.iam.LoggedInUserInfo;

import java.util.List;
import java.util.Locale;

/**
 * <p>
 * Processor responsible for handling async create operations for {@link ActivityHolder} entities
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class ActivityAsyncCreateProcessor {

    private final EventDataHandler eventDataHandler;
    private final SourceActivityManager sourceActivityManager;
    private final SourceActivityConverter sourceActivityConverter;

    public ActivityAsyncCreateProcessor() {
        sourceActivityManager = SourceActivityManager.getInstance();
        sourceActivityConverter = SourceActivityConverter.getInstance();
        eventDataHandler = EventDataHandler.getInstance();
    }

    /**
     * <p>
     * Creates the model for the given request
     * </p>
     *
     * @param orgId    Represents an org id
     * @param userId   Represents an user id
     * @param id       Represents the model id
     * @param dataJson Represents the data {@link JsonObject}
     */
    public void create(final long orgId, final long userId, final long id, final JsonObject dataJson) {
        final UserRegionalPreference userRegionalPreference = ((CrmUser) LoggedInUserInfo.getUser()).getPreferenceHolder().get(UserRegionalPreference.TYPE);
        final ActivityHolder activityHolder = sourceActivityConverter.convert(new ConverterConfig(orgId, userId,
                        Locale.forLanguageTag(userRegionalPreference.getLanguage().getName()), false, dataJson),
                dataJson.getJsonObject(BulkActionFields.ACTIVITY));
        final SecureEventData secureEventData = new SecureEventData(orgId, userId);
        final Contact contact = new Contact();

        contact.setId(id);
        activityHolder.setContacts(List.of(contact));
        sourceActivityManager.create(activityHolder);

        secureEventData.setValues(List.of(activityHolder));
        secureEventData.setAction(Action.CREATE);
        eventDataHandler.handleEvents(secureEventData, Event.Type.POST);
    }
}