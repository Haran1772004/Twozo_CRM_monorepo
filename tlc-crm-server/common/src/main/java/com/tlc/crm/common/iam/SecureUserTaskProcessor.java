package com.tlc.crm.common.iam;

import com.tlc.commons.executor.task.RunnableTask;

import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.security.context.SecureTaskProcessor;
import com.tlc.crm.security.context.SecurityContext;

/**
 * A task processor that securely executes the specified task by first loading the {@link SecurityContext}
 * using the provided user details before execution
 *
 * @author sundar
 * @version 1.0
 * @see SecureTaskProcessor
 */
public interface SecureUserTaskProcessor extends SecureTaskProcessor {

    /**
     * Executes the specified task securely by applying the {@link SecurityContext} value,
     * which is fetched using the provided user details
     *
     * @param orgId  Represents the organization ID
     * @param userId Represents the userID
     * @param task   the task to be executed, with the security context value loaded before execution
     */
    default void secureProcess(final long orgId, final long userId, final RunnableTask task) {
        secureProcess(UserService.getInstance().getUser(orgId, userId), task);
    }
}
