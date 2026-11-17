package com.tlc.crm.email.core.internal.impl;

import com.tlc.crm.core.notification.email.api.model.EntityThread;
import com.tlc.crm.email.core.InboundEmailLinkerService;
import com.tlc.crm.email.core.manager.InboundEmailLinkerManager;
import com.tlc.sql.common.api.DataContainer;

/**
 * <p>
 * Provides the inbound mail related services
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class InboundEmailLinkerServiceImpl implements InboundEmailLinkerService {

    private final InboundEmailLinkerManager inboundEmailLinkerManager;

    private InboundEmailLinkerServiceImpl() {
        inboundEmailLinkerManager = InboundEmailLinkerManager.getInstance();
    }

    private static class Instance {
        private static final InboundEmailLinkerServiceImpl INSTANCE = new InboundEmailLinkerServiceImpl();
    }

    public static InboundEmailLinkerServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param thread          Represents the {@link EntityThread}
     * @param sourceContainer Holds the user defined values of existing mail
     * @param targetContainer Holds the updated information of email defined
     */
    @Override
    public void loadRetrievedMails(final EntityThread thread, final DataContainer sourceContainer, final DataContainer targetContainer) {
        inboundEmailLinkerManager.loadRetrievedMails(thread, sourceContainer, targetContainer);
    }

    /**
     * <p>
     * Creates a new thread in the system using the given {@link EntityThread}.
     * </p>
     *
     * @param entityThread the {@link EntityThread} to be created
     */
    @Override
    public void createThread(final EntityThread entityThread) {
        inboundEmailLinkerManager.create(entityThread);
    }
}
