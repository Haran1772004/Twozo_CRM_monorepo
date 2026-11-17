package com.tlc.scheduler.internal;

import com.tlc.commons.util.Configurations;
import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.task.Task;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Abishek
 * @version 1.0
 */
public class ScheduleServiceImpl implements SchedulerService
{
	private ScheduleManager scheduleManager;
	private static final String DEFAULT_SCHEDULER_POOL = "default";

	@Override
	public void start(final Configurations configurations)
	{
		final Map<String, Integer> schedulerPool = new HashMap<>();

		final String poolConfigStr = configurations.getConfig("pool");
		final String[] poolConfig = poolConfigStr.split(",");
		for(String threadConfig : poolConfig)
		{
			final String[] threadConfigArray = threadConfig.split(":");
			schedulerPool.put(threadConfigArray[0], Integer.parseInt(threadConfigArray[1]));
		}
		this.scheduleManager = new ScheduleManager(schedulerPool);
	}

	@Override
	public void stop()
	{
		if(scheduleManager != null)
		{
			scheduleManager.shutdown();
		}
	}

	@Override
	public Long schedule(Task task)
	{
		return schedule(DEFAULT_SCHEDULER_POOL, task);
	}
	
	@Override
	public Long schedule(String poolName, Task task)
	{
		final Task localTask =  Objects.requireNonNull(task);
		return scheduleManager.schedule(poolName , localTask);
	}

	@Override
	public void runNow(Long id)
	{
		scheduleManager.runNow(id);
	}

	@Override
	public void removeTask(Long id)
	{
		scheduleManager.removeScheduledWork(id);
	}

	@Override
	public boolean isRunning(Long id)
	{
		return scheduleManager.isRunning(id);
	}

}
