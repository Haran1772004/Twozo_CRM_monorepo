package com.tlc.commons.functions;

/**
 * Functional interface for a runnable task that can handle checked exceptions.
 */
@FunctionalInterface
public interface SafeRunnable
{
   /**
    * Executes this runnable task, potentially throwing an exception.
    *
    * @throws Exception if an exception occurs during execution.
    */
   void run() throws Exception;
}
