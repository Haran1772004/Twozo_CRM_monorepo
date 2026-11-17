package com.tlc.email.starter;

import com.tlc.components.starter.ApplicationStarter;

import com.tlc.email.starter.initializer.EmailServiceInitializer;
import com.tlc.email.starter.initializer.EmailServiceModules;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * <p>
 * Serves as the entry point for initializing and launching the application. Responsible
 * for setting up the necessary configurations, starting essential services,
 * and managing the application lifecycle.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see ApplicationStarter
 */
class Activator extends ApplicationStarter {

    @Autowired
    private EmailServiceInitializer emailServiceInitializer;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void register() {
        moduleRegistry.register(List.of(EmailServiceModules.values()));
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception If an error occurs during pre-run initialization.
     */
    @Override
    protected void init() throws Exception {
        emailServiceInitializer.initialize();
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception If an error occurs during post-run tasks.
     */
    @Override
    protected void postInit() throws Exception {
        emailServiceInitializer.postInitialize();
    }
}