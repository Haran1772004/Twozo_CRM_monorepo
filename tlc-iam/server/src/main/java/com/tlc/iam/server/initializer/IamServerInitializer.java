package com.tlc.iam.server.initializer;

import com.tlc.commons.service.initializer.ServiceInitializer;
import com.tlc.commons.util.Configurations;
import com.tlc.iam.server.cache.UserCache;
import com.tlc.iam.server.internal.IamServerService;
import com.tlc.iam.server.internal.session.SessionCleaner;
import com.tlc.iam.server.listener.UserRowListener;
import com.tlc.iam.server.sql.resource.MIAMOUSER;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.WritableDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;

/**
 * Initializes the IAM Server by setting up necessary services.
 *
 * @author Maharaja S
 * @version 1.0
 */
public class IamServerInitializer implements ServiceInitializer {

    private final UserRowListener userRowListener;
    private final WritableDataStore writableDataStore;

    private IamServerInitializer() {
        writableDataStore = SqlServiceProvider.get().getAdminDataStore();
        userRowListener = UserRowListener.getInstance();
    }

    private static final class Instance {
        private static final IamServerInitializer IAM_SERVER_INITIALIZER = new IamServerInitializer();
    }

    /**
     * <p>
     * Gets the instance of {@link  IamServerInitializer}
     * </p>
     *
     * @return the instance of {@link IamServerInitializer}
     */
    public static IamServerInitializer getInstance() {
        return Instance.IAM_SERVER_INITIALIZER;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void preInitialize(final Configurations configurations) {
        IamServerService.getInstance().start(configurations);
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void postInitialize(final Configurations configurations) {
        SessionCleaner.getInstance().start(null);
        writableDataStore.registerRowListener(Table.get(MIAMOUSER.TABLE), userRowListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        UserCache.getInstance().clear();
    }
}
