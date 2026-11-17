package com.tlc.commons.executor;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Abishek
 * @version 1.0
 */
public abstract class ExecutorEntry
{
	public enum ExecutorStatus
	{
		STARTED, IDLE, QUEUED, COMPLETED, FAILED
	}

	private ExecutorStatus status = ExecutorStatus.IDLE;
	private final Lock sync = new ReentrantLock();
	public void setStatus(ExecutorStatus status)
	{
		sync.lock();
		try
		{
			this.status = status;
		}
		finally
		{
			sync.unlock();
		}
	}
	
	public boolean isBusy()
	{
		sync.lock();
		try
		{
			return this.status == ExecutorStatus.QUEUED || this.status == ExecutorStatus.STARTED;
		}
		finally
		{
			sync.unlock();
		}

	}
	
	public boolean isRunning()
	{
		sync.lock();
		try
		{
			return this.status == ExecutorStatus.STARTED;
		}
		finally
		{
			sync.unlock();
		}

	}

	public ExecutorStatus getStatus()
	{
		sync.lock();
		try
		{
			return this.status;
		}
		finally
		{
			sync.unlock();
		}
	}
}
