package com.tlc.commons.executor;

import com.tlc.commons.executor.context.ContextHolder;
import com.tlc.commons.executor.context.registry.ContextHolderRegistry;
import com.tlc.commons.executor.task.RunnableTaskFuture;
import com.tlc.commons.executor.task.ScheduledTaskFuture;
import com.tlc.commons.internal.executor.ContextAwareExecutorService;
import com.tlc.commons.internal.executor.FixedThreadPoolExecutor;
import com.tlc.commons.internal.executor.ForkJoinTaskExecutor;
import com.tlc.commons.internal.executor.ScheduledTaskExecutor;
import com.tlc.commons.internal.executor.TaskExecutor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.concurrent.RunnableFuture;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import java.util.function.Consumer;

/**
 * Offers executor services for task processing.
 *
 * @author Abishek
 * @version 1.0
 */
public final class ExecutorsHandler {

//    private static final Consumer<Runnable> CONTEXT_REMOVER = (runnable) -> ContextHolderRegistry.getInstance().getContextHolders().forEach(ContextHolder::clear);
    private static final Runnable CONTEXT_REMOVER = () -> ContextHolderRegistry.getInstance().getContextHolders().forEach(ContextHolder::clear);

    /**
     * Creates a new {@link Executor} for task processing
     *
     * @param max      The maximum pool size to be configured
     * @param name     The designated name for the workers
     * @param isDaemon Indicates whether the executor should run in the background
     * @return A created {@link Executor}
     */
    public static Executor createTaskExecutor(final int max, final String name, final boolean isDaemon) {
        return createTaskExecutor(max, name, new LinkedBlockingQueue<>(), isDaemon);
    }

    /**
     * Creates a new {@link Executor} for task processing
     *
     * @param max      The maximum pool size to be configured
     * @param name     The designated name for the worker
     * @param queue    The queue designated for sequential task execution. Tasks will be queued here when no workers are available to process them.
     * @param isDaemon Indicates whether the executor should run in the background
     * @return A created {@link Executor}
     */
    public static Executor createTaskExecutor(final int max, final String name, final BlockingQueue<Runnable> queue,
                                              final boolean isDaemon) {
        return new TaskExecutor(buildThreadPoolExecutor(max, name, queue, isDaemon));
    }

    /**
     * Creates a new {@link ThreadPoolExecutor} to process the tasks.
     * <strong>Note</strong>: Please avoid using the {@link ThreadPoolExecutor} directly and instead opt for the {@link Executor} whenever possible
     *
     * @param max      Represents the maximum pool size
     * @param name     Represents the name to be set for the workers
     * @param isDaemon Represents whether the executor should run in background or not
     * @return {@link ThreadPoolExecutor}
     */
    public static ThreadPoolExecutor createFixedThreadExecutor(final int max, final String name, final boolean isDaemon) {
        return createFixedThreadExecutor(max, name, new LinkedBlockingQueue<>(), isDaemon);
    }

    /**
     * Creates a new {@link ThreadPoolExecutor} to process the tasks.
     * <strong>Note</strong>: It is not recommended to get and use the {@link ThreadPoolExecutor}
     *
     * @param max      The maximum pool size to be configured
     * @param name     The designated name for the worker
     * @param queue    The queue designated for sequential task execution. Tasks will be queued here when no workers are available to process them.
     * @param isDaemon Indicates whether the executor should run in the background
     * @return {@link ThreadPoolExecutor}
     */
    public static ThreadPoolExecutor createFixedThreadExecutor(final int max, final String name, final BlockingQueue<Runnable> queue, final boolean isDaemon) {
        return buildThreadPoolExecutor(max, name, queue, isDaemon);
    }

    /**
     * Creates a new {@link ScheduledExecutor} to handle tasks either immediately or on a scheduled basis
     *
     * @param max      Represents the maximum pool size
     * @param name     Represents the name to be set for the workers
     * @param isDaemon Represents whether the executor should run in background or not
     * @return A new {@link ScheduledExecutor}
     */
    public static ScheduledExecutor createScheduledTaskExecutor(final int max, final String name, final boolean isDaemon) {
        return new ScheduledTaskExecutor(new ScheduledThreadPoolExecutor(max, getThreadFactory(isDaemon)) {
            @Override
            protected void beforeExecute(final Thread thread, final Runnable runnable) {
                thread.setName(name);
                super.beforeExecute(thread, runnable);
            }

            @Override
            protected void afterExecute(final Runnable runnable, final Throwable throwable) {
                CONTEXT_REMOVER.run();
            }

            @Override
            protected <T> RunnableFuture<T> newTaskFor(final Runnable runnable, final T result) {
                return new RunnableTaskFuture<>(runnable, result);
            }

            @Override
            protected <T> RunnableScheduledFuture<T> decorateTask(final Runnable runnable, final RunnableScheduledFuture<T> future) {
                return new ScheduledTaskFuture<>(future);
            }
        });

    }

    /**
     * Creates a {@link Executor} to execute tasks in parallel
     *
     * @param parallelism the level of parallelism
     * @return a {@link Executor} instance
     */
    public static Executor createForkJoinTaskExecutor(final int parallelism) {
        return new ForkJoinTaskExecutor(parallelism);
    }

    /**
     * Builds the {@link ThreadPoolExecutor} based on the given configuration
     *
     * @param max      The maximum pool size to be configured
     * @param name     The designated name for the worker
     * @param queue    The queue designated for sequential task execution. Tasks will be queued here when no workers are available to process them.
     * @param isDaemon Indicates whether the executor should run in the background
     * @return {@link ThreadPoolExecutor}
     */
    private static ThreadPoolExecutor buildThreadPoolExecutor(final int max, final String name, final BlockingQueue<Runnable> queue,
                                                              final boolean isDaemon) {
        return new FixedThreadPoolExecutor(max, max, 0L, TimeUnit.MILLISECONDS, queue, getThreadFactory(isDaemon)) {
            @Override
            protected void beforeExecute(final Thread thread, final Runnable runnable) {
                thread.setName(name);
                super.beforeExecute(thread, runnable);
            }

            @Override
            protected void afterExecute(final Runnable runnable, final Throwable throwable) {
                CONTEXT_REMOVER.run();
            }

            @Override
            protected <T> RunnableFuture<T> newTaskFor(final Runnable runnable, final T result) {
                return new RunnableTaskFuture<>(runnable, result);
            }
        };
    }

    /**
     * Gets the {@link ThreadFactory}
     *
     * @param isDaemon Indicates whether the executor should run in the background
     * @return {@link ThreadFactory}
     */
    private static ThreadFactory getThreadFactory(final boolean isDaemon) {
        final ThreadFactory threadFactory;

        if (isDaemon) {
            threadFactory = runnable -> {
                final Thread thread = Executors.defaultThreadFactory().newThread(runnable);

                thread.setDaemon(true);
                return thread;
            };
        } else {
            threadFactory = Executors.defaultThreadFactory();
        }
        return threadFactory;
    }

    public static ExecutorService createVirtualThreadExecutor(final String name) {
        final ExecutorService delegate = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors());
        return new ContextAwareExecutorService(delegate, CONTEXT_REMOVER);
    }
}
