package com.tlc.scheduler.task;

import java.util.Objects;

/**
 * @author Abishek
 * @version 1.0
 */
public abstract class TaskController
{
	private final String scheduleName;
	
	private long actualScheduledTime;
	
	private long startTime;

	private long endTime;

	public TaskController(String scheduleName)
	{
		this.scheduleName = Objects.requireNonNull(scheduleName);
	}

	public String getScheduleName()
	{
		return scheduleName;
	}
	
	public long getActualScheduledTime()
	{
		return actualScheduledTime;
	}

	public void setActualScheduledTime(long actualScheduledTime)
	{
		this.actualScheduledTime = actualScheduledTime;
	}

	public long getStartTime()
	{
		return startTime;
	}

	public void setStartTime(long startTime)
	{
		this.startTime = startTime;
	}

	public long getEndTime()
	{
		return endTime;
	}

	public void setEndTime(long endTime)
	{
		this.endTime = endTime;
	}	
	
	public abstract long getNextInterval();
}
