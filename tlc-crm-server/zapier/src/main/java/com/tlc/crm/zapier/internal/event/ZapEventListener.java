package com.tlc.crm.zapier.internal.event;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.JavaUtil;

import com.tlc.crm.common.event.listener.PostEventListener;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.zapier.ZapTrigger;
import com.tlc.crm.core.zapier.event.ZapEventType;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.notification.app.Event;

import com.tlc.crm.zapier.api.manager.ZapEventWebhookManager;
import com.tlc.crm.zapier.api.models.ZapEvent;
import com.tlc.crm.zapier.fields.ZapAttributes;
import com.tlc.crm.zapier.internal.publish.ZapEventDataPublisher;

import com.tlc.validator.TlcModel;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Collection;
import java.util.Objects;

/**
 * Indicates the {@link ZapEventListener} that listens for the event
 * during trigger process that are to be processed.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ZapEventListener implements PostEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZapEventListener.class);

    private final ZapEventDataPublisher zapEventDataPublisher = ZapEventDataPublisher.getInstance();
    private final ZapEventWebhookManager zapEventWebhookManager = ZapEventWebhookManager.getInstance();

    private static class InstanceHolder {
        private static final ZapEventListener INSTANCE = new ZapEventListener();
    }

    public static ZapEventListener getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Event}.
     */
    @Override
    public Event event() {
        return ZapEventType.TRIGGER;
    }

    /**
     * {@inheritDoc}
     *
     * @param eventData Represents {@link EventData}.
     */
    @Override
    public void processPostEvent(final EventData eventData) {
        final Collection<TlcModel> models = eventData.getValues();

        models.stream().findFirst().ifPresent(tlcModel -> {
            final ZapTrigger zapTrigger = tlcModel.getClass().getAnnotation(ZapTrigger.class);

            if (Objects.nonNull(zapTrigger)) {
                final Source source = zapTrigger.source();
                final long orgId = eventData.getOrgId();
                final long userId = eventData.getUserId();
                final Action action = eventData.getAction();
                final String apiType = eventData.getApiType();
                final String languageTag = eventData.getLocale().toLanguageTag();

                if (zapEventWebhookManager.exists(orgId, userId, Objects.isNull(apiType) ? ZapEvent.getBySource(source, action) : ZapEvent.getByType(apiType))) {
                    final Collection<Collection<TlcModel>> splitCollection = JavaUtil.splitCollection(models, 3);

                    for (final Collection<TlcModel> modelList : splitCollection) {

                        final JsonObject jsonObject = Json.object();
                        final Collection<Long> modelIds = modelList.stream().map(TlcModel::id).filter(Objects::nonNull).toList();

                        jsonObject.put(ZapAttributes.MODEL_IDS, modelIds);
                        jsonObject.put(ZapAttributes.SOURCE, source);
                        jsonObject.put(ZapAttributes.ACTION, action);
                        jsonObject.put(ZapAttributes.API_TYPE, apiType);
                        jsonObject.put(ZapAttributes.LOCALE, languageTag);
                        jsonObject.put(ZapAttributes.ORG_ID, orgId);
                        jsonObject.put(ZapAttributes.USER_ID, userId);

                        final String requestId = Base64.getEncoder().encodeToString(String.format("%s:%s:%s", orgId, userId, modelIds).getBytes());

                        zapEventDataPublisher.publish(orgId, requestId, jsonObject);
                    }
                }
            }
        });
    }
}
