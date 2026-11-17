package com.tlc.crm.core.internal.async.bulk.service;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.core.internal.async.bulk.processor.AsyncUpdateProcessor;
import com.tlc.crm.core.internal.async.bulk.processor.model.AsyncActionRequest;
import com.tlc.crm.core.notification.email.api.model.EntityThread;
import com.tlc.crm.core.notification.email.bulk.service.EntityThreadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * <p>
 * Processor responsible for handling async update operations for {@link EntityThread} entities
 * </p>
 *
 * @author  Mohammed Asif J
 * @version 1.0
 */
public class EntityThreadAsyncUpdateProcessor extends AsyncUpdateProcessor<EntityThread> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityThreadAsyncUpdateProcessor.class);

    private final EntityThreadService entityThreadService;

    EntityThreadAsyncUpdateProcessor() {
        this.entityThreadService = EntityThreadService.getInstance();
    }

    /**
     * <p>
     * Processes the asynchronous action request
     * </p>
     *
     * @param asyncActionRequest Represents the {@link AsyncActionRequest}
     */
    @Override
    public void process(final AsyncActionRequest asyncActionRequest) {
        final long orgId = asyncActionRequest.orgId();
        final long userId = asyncActionRequest.userId();
        final long threadId = asyncActionRequest.modelId();

        try {
            LOGGER.debug("Bulk Update Email: Initiating update operation. Org ID: {}, User ID: {}, Thread ID: {}",
                    orgId, userId, threadId);
            entityThreadService.updateState(orgId, userId, Set.of(threadId), asyncActionRequest.requestJson());
        } catch (final Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Bulk Update Email: Email Update failed. Org ID: {}, User ID: {}, Id: {}, Reason: {}",
                    orgId, userId, threadId, exception.getMessage());
        }
    }

    /**
     * <p>
     * Fetches the {@link AbstractConfigManager} instance
     * </p>
     *
     * @return The {@link AbstractConfigManager} instance
     */
    @Override
    protected AbstractConfigManager<EntityThread> getAbstractConfigManager() {
        throw ErrorCode.get(CommonErrorCodes.UNSUPPORTED_ACTION);
    }
}
