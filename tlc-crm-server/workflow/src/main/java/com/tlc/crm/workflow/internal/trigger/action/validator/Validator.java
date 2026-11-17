package com.tlc.crm.workflow.internal.trigger.action.validator;

import com.tlc.crm.workflow.internal.trigger.action.ActionExecutionContext;

import com.tlc.crm.workflow.v2.internal.execution.NodeExecutionContext;
import com.tlc.validator.TlcModel;
import com.tlc.iam.common.models.User;

import java.util.Objects;

/**
 * Provides services to validate the fields of {@link TlcModel}.
 *
 * @param <T> Refers the type of the model.
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface Validator<T extends TlcModel> {

    /**
     * Validates the given model by ensuring it possesses all the requisite data.
     *
     * @param model                  Refers the model to be validated.
     * @param actionExecutionContext Refers the {@code ActionExecutionContext}.
     * @return {@code true} if the model is valid, {@code false} otherwise.
     */
    default boolean validate(final T model, final ActionExecutionContext actionExecutionContext) {
        return true;
    }

    /**
     * Validates the given model by ensuring it possesses all the requisite data.
     *
     * @param model                Refers the model to be validated.
     * @param nodeExecutionContext Refers the {@code NodeExecutionContext}.
     * @return {@code true} if the model is valid, {@code false} otherwise.
     */
    default boolean validate(final T model, final NodeExecutionContext nodeExecutionContext) {
        return true;
    }

    /**
     * Checks whether the specified {@link User} is active.
     *
     * @param user The {@link User} instance to check; may be {@code null}.
     * @return {@code true} if the user is not {@code null} and has an active state, {@code false} otherwise.
     */
    default boolean isActiveUser(final User user) {
        if (Objects.nonNull(user)) {
            return user.getUserState().isActive();
        }
        return true;
    }
}
