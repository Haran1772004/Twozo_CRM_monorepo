package com.tlc.scheduler.service;

import com.tlc.commons.service.Service;
import com.tlc.scheduler.task.Task;

/**
 * @author Abishek
 * @version 1.0
 */
public interface SchedulerService extends Service
{
	Long schedule(Task task);

	Long schedule(String poolName, Task task);

	void runNow(Long id);

	void removeTask(Long id);

	boolean isRunning(Long id);
}
