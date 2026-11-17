package com.tlc.scheduler.task;

import com.tlc.commons.executor.ExecutorEntry;
import com.tlc.scheduler.internal.ScheduleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Abishek
 * @version 1.0
 */
public abstract class Task extends ExecutorEntry
{
	private final TaskController controller;
	private final AtomicInteger reScheduleCount = new AtomicInteger(-1);
	private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleManager.class);

	public Task(TaskController controller, int reScheduleCount)
	{
		this.controller = Objects.requireNonNull(controller);
		this.reScheduleCount.set(reScheduleCount);
	}

	public Task(TaskController controller)
	{
		this.controller = Objects.requireNonNull(controller);
	}

	public final void start()
	{
		setStatus(ExecutorStatus.STARTED);
		controller.setStartTime(System.currentTimeMillis());
		try
		{
			if(preCallback(controller))
			{
				execute(controller);
			}
			else
			{
				LOGGER.error("PreCallback failed for schedule {}", controller.getScheduleName());
			}
		}
		catch(Throwable throwable)
		{
			LOGGER.error("Problem  to execute schedule {}", controller.getScheduleName(), throwable);
		}
		finally
		{
			postCallback(controller);
			controller.setEndTime(System.currentTimeMillis());
			setStatus(ExecutorStatus.COMPLETED);
		}
	}
	
	public final void setQueuedStatus()
	{
		setStatus(ExecutorStatus.QUEUED);
	}
	
	public final boolean checkProcessReschedule()
	{
		final int count = reScheduleCount.get();
		if(count == -1)
		{
			return true;
		}
		else
		{
			if(count > 0)
			{
				reScheduleCount.decrementAndGet();	
				return true;
			}
			return false;
		}
	}

	public String getScheduleName()
	{
		return controller.getScheduleName();
	}
	
	public long getActualScheduledTime()
	{
		return controller.getActualScheduledTime();
	}

	public long getNextInterval()
	{
		return controller.getNextInterval();
	}

	protected boolean preCallback(TaskController controller)
	{
		return true;
	}
	
	protected void postCallback(TaskController controller)
	{
		
	}

	public boolean stopTask()
	{
		return true;
	}

	public abstract void execute(TaskController taskController);
}
