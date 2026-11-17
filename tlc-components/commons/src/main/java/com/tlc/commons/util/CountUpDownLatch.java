package com.tlc.commons.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Abishek
 * @version 1.0
 */
public final class CountUpDownLatch
{
    private final CountDownLatch latch;
    private final AtomicInteger count = new AtomicInteger(0);

    public CountUpDownLatch()
    {
        this.latch = new CountDownLatch(1);
    }

    public void await() throws InterruptedException
    {
        if (count.get() == 0)
        {
            return;
        }
        latch.await();
    }

    public boolean await(long time, TimeUnit timeUnit) throws InterruptedException
    {
        return latch.await(time, timeUnit);
    }

    public void countDown()
    {
        if(count.decrementAndGet() == 0)
        {
            latch.countDown();
        }
    }

    public long getCount()
    {
        return count.get();
    }

    public void countUp()
    {
        count.incrementAndGet();
    }

    public void setCount(int count)
    {
        if(count > 0)
        {
           this.count.set(count);
        }
    }
}
