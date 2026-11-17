package com.tlc.crm.core.internal.async.bulk.service;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.core.features.action.call.manager.CallLogManager;
import com.tlc.crm.core.features.action.call.model.CallLog;
import com.tlc.crm.core.internal.async.bulk.processor.AsyncDeleteProcessor;
import com.tlc.crm.core.internal.async.bulk.processor.model.AsyncActionRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Processor responsible for handling async delete operations for {@link CallLog} entities
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class CallLogAsyncDeleteProcessor extends AsyncDeleteProcessor<CallLog> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CallLogAsyncDeleteProcessor.class);

    private final CallLogManager callLogManager;

    public CallLogAsyncDeleteProcessor() {
        callLogManager = CallLogManager.getInstance();
    }

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
            final CallLog callLog = new CallLog();

            callLog.setId(modelId);
            callLog.setOrgId(orgId);

            callLogManager.delete(callLog);
        } catch (final Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Bulk Action: Call bulk delete failed. Org ID: {}, User ID: {}, Id: {}, Error: {}",
                    orgId, userId, modelId, exception.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link AbstractConfigManager} instance
     */
    @Override
    protected AbstractConfigManager<CallLog> getAbstractConfigManager() {
        return null;
    }
}
