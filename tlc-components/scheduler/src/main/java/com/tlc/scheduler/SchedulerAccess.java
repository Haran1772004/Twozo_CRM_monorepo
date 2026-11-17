package com.tlc.scheduler;

import com.tlc.commons.service.registry.ServiceRegistry;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.scheduler.internal.registry.SchedulerServiceRegistry;
import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.internal.ScheduleServiceImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.io.IOException;

/**
 * @author Abishek
 * @version 1.0
 */
public class SchedulerAccess implements BundleActivator
{
	private final ServiceRegistry<SchedulerService> schedulerServiceRegistry = SchedulerServiceRegistry.getInstance();

	@Override
	public void start(BundleContext bundleContext) throws IOException
	{
		final ScheduleServiceImpl schedulerService = new ScheduleServiceImpl();
		final Configurations configurations = ConfHolder.getInstance().getProperties("scheduler");

		schedulerService.start(configurations);
		schedulerServiceRegistry.register(schedulerService);
	}

	@Override
	public void stop(BundleContext bundleContext)
	{
		final ScheduleServiceImpl scheduleService = (ScheduleServiceImpl) schedulerServiceRegistry.unregister();
		scheduleService.stop();
	}
}
