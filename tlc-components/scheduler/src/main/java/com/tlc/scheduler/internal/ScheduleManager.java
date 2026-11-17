package com.tlc.scheduler.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.executor.Executor;
import com.tlc.commons.executor.ExecutorsHandler;
import com.tlc.commons.executor.task.RunnableTask;
import com.tlc.commons.executor.ScheduledExecutor;
import com.tlc.scheduler.internal.resource.SchedulerErrorCodes;
import com.tlc.scheduler.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Abishek
 * @version 1.0
 */
public class ScheduleManager
{
	private final Map<String, ScheduledExecutor> scheduled_executors;
	private final Map<Long, TaskWrapper> work_collection;
	private final Map<Long, ScheduledFuture<?>> work_future;
	private final Map<Long, TaskWrapper> running_task;

	private final Lock lock = new ReentrantLock();
	private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleManager.class);

	private static final AtomicLong ID_GENERATOR = new AtomicLong(0);

	public ScheduleManager(Map<String, Integer> schedulerPoll)
	{
		this.work_collection = new ConcurrentHashMap<>();
		this.work_future = new ConcurrentHashMap<>();
		this.scheduled_executors = new ConcurrentHashMap<>();
		this.running_task = new ConcurrentHashMap<>();
		for(Entry<String , Integer> entry : schedulerPoll.entrySet())
		{
			final String pollName = entry.getKey();
			final Integer maxThreads = entry.getValue();
			final String executorPollName = "Scheduler_"+pollName;
			final ScheduledExecutor schExecutor = ExecutorsHandler.createScheduledTaskExecutor(maxThreads , executorPollName , true);
			scheduled_executors.put(pollName , schExecutor);
		}
	}
	
	public Long schedule(String poolName, Task task)
	{		
		final Long identity = ID_GENERATOR.incrementAndGet();
		final TaskWrapper taskWrapper = new TaskWrapper(task, poolName, identity);
		final ScheduledExecutor schExecutor  = getExecutor(taskWrapper.getPoolName());
		task.setQueuedStatus();
		work_collection.put(identity , taskWrapper);
		final ScheduledFuture<?> future = schExecutor.schedule(taskWrapper, task.getActualScheduledTime(), TimeUnit.MILLISECONDS);
		work_future.put(identity , future);
		return identity;
	}
	
	public void runNow(Long id)
	{
		final TaskWrapper taskWrapper = work_collection.get(id);
		if (taskWrapper != null)
		{
			final Task task = taskWrapper.getWork();
			if(task.isRunning())
			{
				LOGGER.info("Current Task is already running {}", task.getScheduleName());
				return;
			}
			final ScheduledExecutor schExecutor = getExecutor(taskWrapper.getPoolName());
			task.setQueuedStatus();
			final ScheduledFuture<?> future = schExecutor.schedule(taskWrapper, 0, TimeUnit.MILLISECONDS);
			work_future.put(id , future);
		}
	}

	public void shutdown()
	{
		final Executor executor = ExecutorsHandler.createTaskExecutor(1, "Scheduler_destroy", true);
		final Future<Boolean> future = executor.submit(()->
		{
			final Executor customThreadPool = ExecutorsHandler.createForkJoinTaskExecutor(10);
			customThreadPool.submit( () ->
					running_task.values().parallelStream().forEach( taskWrapper ->
					{
						final Task task = taskWrapper.getWork();
						try
						{
							task.stopTask();
						}
						catch(Throwable throwable)
						{
							LOGGER.error("Unable to stop Schedule {}", task.getScheduleName(), throwable);
						}
					}));
			return Boolean.TRUE;
		});

		try
		{
			future.get(1, TimeUnit.MINUTES);
		}
		catch(Exception exp)
		{
			LOGGER.error("Unable to stop Schedule pool within time limit 1 min");
		}
		finally
		{
			executor.shutdown();
		}
	}
	
	public boolean isRunning(Long identity)
	{
		return running_task.containsKey(identity);
	}
	
	public void removeScheduledWork(Long identity)
	{
		lock.lock();
		try
		{
			final ScheduledFuture<?> future = removeTaskInfo(identity);
			if (future != null)
			{
				future.cancel(false);
			}
		}
		finally
		{
			lock.unlock();
		}
		
	}
	
	private void checkAndReScheduleTask(Long identity)
	{
		lock.lock();
		try
		{
			final TaskWrapper taskWrapper = work_collection.get(identity);
			if (taskWrapper != null)
			{
				final ScheduledExecutor schExecutor = getExecutor(taskWrapper.getPoolName());
				final Task task = taskWrapper.getWork();
				task.setQueuedStatus();
				final ScheduledFuture<?> future = schExecutor.schedule(taskWrapper, task.getNextInterval(), TimeUnit.MILLISECONDS);
				work_future.put(identity, future);
			}
		}
		finally
		{
			lock.unlock();
		}
	}
	
	private ScheduledExecutor getExecutor(String poolName)
	{
		final ScheduledExecutor schExecutor = scheduled_executors.get(poolName);
		if(schExecutor == null)
		{
			throw ErrorCode.get(SchedulerErrorCodes.INVALID_SCHEDULER_POOL);
		}
		return schExecutor;
	}
	
	private ScheduledFuture<?> removeTaskInfo(Long identity)
	{
		final TaskWrapper taskWrapper = work_collection.remove(identity);
		if (taskWrapper != null)
		{
			return work_future.remove(identity);
		}
		return null;		
	}

	private class TaskWrapper implements RunnableTask
	{
		private final Task task;
		private final String poolName;
		private final Long id;

		TaskWrapper(Task task, String poolName, Long id)
		{
			this.id = Objects.requireNonNull(id);
			this.task = task;
			this.poolName = Objects.requireNonNull(poolName);
		}
		
		public Task getWork()
		{
			return task;
		}
		
		public String getPoolName()
		{
			return poolName;
		}
		
		@Override
		public void run()
		{
			try
			{
				running_task.put(id, this);
				task.start();
			}
			finally
			{
				running_task.remove(id);
				if(task.checkProcessReschedule())
				{
					checkAndReScheduleTask(id);
				}
				else
				{
					removeTaskInfo(id);
				}
			}
		}
	}
}
