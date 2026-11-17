package com.tlc.crm.core.internal.async.bulk.processor;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.internal.async.bulk.processor.model.AsyncActionRequest;
import com.tlc.crm.notification.app.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.validator.TlcModel;

import java.util.List;

/**
 * <p>
 * Abstract class for handling asynchronous deletion of models
 * </p>
 *
 * @param <T> The type of model that extends {@link TlcModel}
 * @author Kishore J
 * @version 1.0
 */
public abstract class AsyncDeleteProcessor<T extends TlcModel> extends AsyncActionProcessor<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncDeleteProcessor.class);

    /**
     * {@inheritDoc}
     *
     * @param asyncActionRequest Represents the {@link AsyncActionRequest}
     */
    @Override
    protected void process(final AsyncActionRequest asyncActionRequest) {
        final long orgId = asyncActionRequest.orgId();
        final long userId = asyncActionRequest.userId();
        final long modelId = asyncActionRequest.modelId();

        try {
            final AbstractConfigManager<T> abstractConfigManager = getAbstractConfigManager();
            final T model = abstractConfigManager.get(orgId, modelId);

            loadSummaryDetails(model);
            final SecureEventData secureEventData = new SecureEventData(orgId, userId);

            secureEventData.setValues(List.of(model));
            secureEventData.setPreviousValues(List.of(model));
            secureEventData.setAction(Action.DELETE);
            eventDataHandler.handleEvents(secureEventData, Event.Type.PRE);

            abstractConfigManager.delete(model);

            eventDataHandler.handleEvents(secureEventData, Event.Type.POST);
        } catch (final Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Bulk Action: Bulk delete failed. Org ID: {}, User ID: {}, Id: {}, Error: {}",
                    orgId, userId, modelId, exception.getMessage());
        }
    }

    /**
     * <p>
     * Loads the summary details into model
     * </p>
     *
     * @param model Represents the {@link TlcModel}
     */
    protected void loadSummaryDetails(final T model) {
    }

}
