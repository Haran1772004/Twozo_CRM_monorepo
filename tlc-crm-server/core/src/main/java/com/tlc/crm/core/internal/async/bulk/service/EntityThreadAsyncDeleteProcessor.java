package com.tlc.crm.core.internal.async.bulk.service;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.core.internal.async.bulk.processor.AsyncDeleteProcessor;
import com.tlc.crm.core.internal.async.bulk.processor.model.AsyncActionRequest;
import com.tlc.crm.core.notification.email.api.model.EntityThread;
import com.tlc.crm.core.notification.email.bulk.service.DeleteMode;
import com.tlc.crm.core.notification.email.bulk.service.EntityThreadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * <p>
 * Processor responsible for handling async delete operations for {@link EntityThread} entities
 * </p>
 *
 * @author  Mohammed Asif J
 * @version 1.0
 */
public class EntityThreadAsyncDeleteProcessor extends AsyncDeleteProcessor<EntityThread> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityThreadAsyncDeleteProcessor.class);

    private final EntityThreadService entityThreadService;

    EntityThreadAsyncDeleteProcessor() {
        this.entityThreadService = EntityThreadService.getInstance();
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
        final long threadId = asyncActionRequest.modelId();

        try {
            LOGGER.debug("Bulk Delete Email: Initiating delete operation. Org ID: {}, User ID: {}, Thread ID: {}",
                    orgId, userId, threadId);
            entityThreadService.delete(orgId, userId, Set.of(threadId), DeleteMode.get(asyncActionRequest.requestJson()));
        } catch (final Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Bulk Delete Email: Email Delete failed. Org ID: {}, User ID: {}, Id: {}, Reason: {}",
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
