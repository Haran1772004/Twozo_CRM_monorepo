package com.tlc.scheduler.task;

/**
 * @author Abishek
 * @version 1.0
 */
public class IntervalController extends TaskController
{
	private final long interval;
	public IntervalController(String scheduleName, long interval)
	{
		super(scheduleName);
		this.interval = interval;
	}

	@Override
	public long getNextInterval()
	{
		return interval;
	}
	
}
