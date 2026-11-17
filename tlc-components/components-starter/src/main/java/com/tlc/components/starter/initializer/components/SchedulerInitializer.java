package com.tlc.components.starter.initializer.components;

import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.commons.util.Configurations;
import com.tlc.components.starter.fields.ComponentsStarterFields;
import com.tlc.components.starter.initializer.AbstractModuleInitializer;

import com.tlc.scheduler.internal.ScheduleServiceImpl;
import com.tlc.scheduler.internal.registry.SchedulerServiceRegistry;
import com.tlc.scheduler.service.SchedulerService;

import org.springframework.stereotype.Component;

/**
 * Responsible for triggering the initialization process of the scheduler services.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Component
final class SchedulerInitializer extends AbstractModuleInitializer {

    private final ServiceRegistry<SchedulerService> schedulerServiceRegistry;

    private SchedulerInitializer() {
        this.schedulerServiceRegistry = SchedulerServiceRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        if (isServiceEnabled()) {
            final ScheduleServiceImpl schedulerService = new ScheduleServiceImpl();
            final Configurations configurations = getProperties(ComponentsStarterFields.SCHEDULER);

            schedulerService.start(configurations);
            schedulerServiceRegistry.register(schedulerService);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isServiceEnabled() {
        return isServiceEnabled(ComponentsStarterFields.SCHEDULER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ServiceRegistry<? extends Service> getServiceRegistry() {
        return schedulerServiceRegistry;
    }
}
