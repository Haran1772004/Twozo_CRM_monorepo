package com.tlc.cluster;

import java.util.concurrent.TimeUnit;

/**
 * @author Abishek
 * @version 1.0
 */
public interface DistributedLock
{
    void acquire() throws Exception;

    boolean acquire(long time, TimeUnit unit) throws Exception;

    void release() throws Exception;
}
