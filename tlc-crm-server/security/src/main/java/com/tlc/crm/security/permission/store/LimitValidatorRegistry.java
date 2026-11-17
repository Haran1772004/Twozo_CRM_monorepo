package com.tlc.crm.security.permission.store;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.security.internal.status.PermissionErrorCodes;
import com.tlc.crm.security.permission.model.LimitActionType;
import com.tlc.crm.security.permission.annotation.validator.ActionLimitValidator;
import com.tlc.crm.security.permission.validator.LimitValidator;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * The store contains the limit-related permissions validators, each paired with its corresponding {@link ActionLimitValidator}
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public final class LimitValidatorRegistry {

    private final Map<String, Map<LimitActionType, LimitValidator>> limitValidators;

    private static final class Instance {
        private static final LimitValidatorRegistry LIMIT_CHECKER_STORE = new LimitValidatorRegistry();
    }

    /**
     * Gets the {@link LimitValidatorRegistry}
     *
     * @return {@link LimitValidatorRegistry} instance
     */
    public static LimitValidatorRegistry getInstance() {
        return Instance.LIMIT_CHECKER_STORE;
    }

    private LimitValidatorRegistry() {
        this.limitValidators = new ConcurrentHashMap<>();
    }

    /**
     * <p>
     * Adds the {@link LimitValidator} into the container.
     * </p>
     *
     * @param limitActionType Represents the type of the limit validator.
     * @param limitValidator        Represents the validator of limit-related permissions.
     */
    public void addValidator(final String groupId, final LimitActionType limitActionType, final LimitValidator limitValidator) {
        limitValidators.computeIfAbsent(groupId, grpId -> new HashMap<>()).put(limitActionType, limitValidator);
    }

    /**
     * <p>
     * Gets the respective {@link LimitValidator} of given {@link LimitActionType}
     * </p>
     *
     * @param limitActionType Represents the type of the limit validator.
     * @return The corresponding {@link LimitValidator}
     */
    public <T extends LimitValidator> T get(final LimitActionType limitActionType) {
        for (final Map<LimitActionType, LimitValidator> limitValidators : limitValidators.values()) {
            if (limitValidators.containsKey(limitActionType)) {
                return (T) limitValidators.get(limitActionType);
            }
        }
        throw ErrorCode.get(PermissionErrorCodes.NO_LIMIT_VALIDATOR_FOUND,
                String.format("No limit validator found, Type: %s", limitActionType));
    }

    /**
     * <p>
     * Removes the limit permission validator of the given group ID.
     * </p>
     *
     * @param groupId Represents the group ID.
     */
    public void remove(final String groupId) {
        limitValidators.remove(groupId);
    }

    /**
     * Removes all registered limit validators.
     */
    public void clear() {
        limitValidators.clear();
    }
}
