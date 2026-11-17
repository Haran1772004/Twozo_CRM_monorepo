package com.tlc.scheduler.task;

/**
 * @author Abishek
 * @version 1.0
 */
public class CalendarController extends TaskController
{
	public CalendarController(String scheduleName)
	{
		super(scheduleName);
	}

	@Override
	public long getNextInterval()
	{
		return 100 * 1000; //TODO 
	} 
}
