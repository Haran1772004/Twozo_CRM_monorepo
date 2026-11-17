package com.tlc.social.starter;

import com.tlc.components.starter.ApplicationStarter;
import com.tlc.social.starter.initializer.SocialServiceInitializer;
import com.tlc.social.starter.initializer.SocialServiceModules;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * An activator which activates the social service
 *
 * @author Maharaja S
 * @version 1.0
 */
public class SocialServiceActivator extends ApplicationStarter {

    @Autowired
    private SocialServiceInitializer socialServiceInitializer;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void register() {
        moduleRegistry.register(List.of(SocialServiceModules.values()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void init() {
        socialServiceInitializer.initialize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void postInit() {
        socialServiceInitializer.postInitialize();
    }
}
