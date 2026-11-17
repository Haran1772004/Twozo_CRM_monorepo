package com.tlc.social.starter.initializer;

import com.tlc.commons.util.Configurations;
import com.tlc.components.starter.initializer.AbstractModuleInitializer;

import com.tlc.social.server.handler.FBTokenRefreshHandler;
import com.tlc.social.server.handler.WATokenRefreshHandler;
import com.tlc.social.server.initializer.SocialServerInitializer;
import com.tlc.social.starter.fields.SocialStarterFields;

import com.tlc.sync.service.oauth.internal.RefreshTokenHandlerRegistry;
import com.tlc.sync.service.oauth.model.SyncServiceCategory;
import org.springframework.stereotype.Component;

import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;

/**
 * Responsible for initiating the initialization process of social service.
 *
 * @author Maharaja S
 * @version 1.0
 */
@Component
public class SocialServiceInitializer extends AbstractModuleInitializer {

    private final RefreshTokenHandlerRegistry refreshTokenHandlerRegistry;

    public SocialServiceInitializer() {
        refreshTokenHandlerRegistry = RefreshTokenHandlerRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isServiceEnabled() {
        return isServiceEnabled(SocialStarterFields.SOCIAL);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        SocialServerInitializer.getInstance().stop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        if (isServiceEnabled()) {
            SocialServerInitializer.getInstance().preInitialize(getProperties(SocialStarterFields.MESSENGER_EXECUTOR));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postInitialize() {
        if (isServiceEnabled()) {
            final Configurations configurations = getProperties(SocialStarterFields.SOCIAL);

            SocialServerInitializer.getInstance().postInitialize(configurations);
        }
        refreshTokenHandlerRegistry.put(SyncServiceCategory.FACEBOOK, FBTokenRefreshHandler.getInstance());
        refreshTokenHandlerRegistry.put(SyncServiceCategory.WHATSAPP, WATokenRefreshHandler.getInstance());
    }
}