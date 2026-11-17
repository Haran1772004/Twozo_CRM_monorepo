package com.tlc.commons.internal.executor;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A delegating {@link ExecutorService} that wraps each submitted task
 * to ensure context cleanup after execution. This mirrors the {@code afterExecute}
 * behavior in {@link java.util.concurrent.ThreadPoolExecutor} for executors
 * that don't support that hook (e.g., virtual-thread-per-task executors).
 * <p>
 * Context propagation (setting context on the new thread) is the caller's responsibility
 * via {@code ContextSnapshot}/{@code ContextScope}. This class handles the <em>cleanup</em> side.
 *
 * @author Selvakumar
 * @version 1.0
 */
public final class ContextAwareExecutorService implements ExecutorService {

    private final ExecutorService delegate;
    private final Runnable afterExecute;

    public ContextAwareExecutorService(final ExecutorService delegate, final Runnable afterExecute) {
        this.delegate = delegate;
        this.afterExecute = afterExecute;
    }

    @Override
    public void execute(final Runnable command) {
        delegate.execute(wrap(command));
    }

    @Override
    public Future<?> submit(final Runnable task) {
        return delegate.submit(wrap(task));
    }

    @Override
    public <T> Future<T> submit(final Runnable task, final T result) {
        return delegate.submit(wrap(task), result);
    }

    @Override
    public <T> Future<T> submit(final Callable<T> task) {
        return delegate.submit(wrap(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(tasks.stream().map(this::wrap).toList());
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks, final long timeout,
                                         final TimeUnit unit) throws InterruptedException {
        return delegate.invokeAll(tasks.stream().map(this::wrap).toList(), timeout, unit);
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        return delegate.invokeAny(tasks.stream().map(this::wrap).toList());
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout,
                           final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(tasks.stream().map(this::wrap).toList(), timeout, unit);
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

//    @Override
    public void close() {
//        delegate.close();
    }

    private Runnable wrap(final Runnable task) {
        return () -> {
            try {
                task.run();
            } finally {
                afterExecute.run();
            }
        };
    }

    private <T> Callable<T> wrap(final Callable<T> task) {
        return () -> {
            try {
                return task.call();
            } finally {
                afterExecute.run();
            }
        };
    }
}
