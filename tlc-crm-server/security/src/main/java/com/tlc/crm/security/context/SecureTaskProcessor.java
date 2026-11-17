package com.tlc.crm.security.context;

import com.tlc.commons.executor.task.RunnableTask;

import com.tlc.crm.security.iam.BaseCrmUser;
import com.tlc.crm.security.iam.LoggedInUserInfo;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A task processor that securely executes the given task by loading the {@link SecurityContext} prior to execution
 *
 * @author sundar
 * @version 1.0
 */
public interface SecureTaskProcessor {

    /**
     * Executes the specified task securely by applying the {@link SecurityContext} value
     *
     * @param user the user to be set within the {@link SecurityContext}
     * @param task the task to be executed, with the security context value loaded before execution
     */
    default void secureProcess(final BaseCrmUser user, final RunnableTask task) {

        if (SecurityContext.exists()) {
            task.run();
        } else {
            SecurityContext.load(user);

            try {
                task.run();
            } finally {
                SecurityContext.clear();
            }
        }
    }

    /**
     * Executes the given {@link Consumer} on each element of the provided {@link Collection}
     * using secure parallel processing via {@code parallelStream()}.
     *
     * @param list     The collection of items to process; must not be {@code null}.
     * @param consumer The action to perform on each item; must not be {@code null}.
     * @param <T>      The type of elements in the collection.
     */
    static <T> void executeInParallel(final Collection<T> list, final Consumer<T> consumer) {
        final BaseCrmUser user = LoggedInUserInfo.getUser();
        final Thread mainThread = Thread.currentThread();

        list.parallelStream().forEach(data -> {
            try {
                if (!Objects.equals(mainThread, Thread.currentThread())) {
                    SecurityContext.load(user);
                }

                consumer.accept(data);
            } finally {
                if (!Objects.equals(mainThread, Thread.currentThread())) {
                    SecurityContext.clear();
                }
            }
        });
    }
}