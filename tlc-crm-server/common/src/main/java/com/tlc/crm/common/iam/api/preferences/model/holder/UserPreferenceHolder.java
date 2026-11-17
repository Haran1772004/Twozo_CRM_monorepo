package com.tlc.crm.common.iam.api.preferences.model.holder;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.iam.api.preferences.model.UserPreference;
import com.tlc.crm.common.iam.api.preferences.model.type.UserPreferenceType;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;

import java.io.Serializable;

/**
 * <p>
 * Holds the user {@link UserPreferenceHolder} details.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class UserPreferenceHolder extends PreferenceHolder<UserPreferenceType, UserPreference> implements Serializable {

    private Long userId;
    private boolean isUpdated;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public boolean isUpdated() {
        return isUpdated;
    }

    public void setUpdated(final boolean isUpdated) {
        this.isUpdated = isUpdated;
    }

    /**
     * {@inheritDoc}
     *
     * @param <C>   The target type that extends {@link  PreferenceHolder}
     * @param clazz The {@code Class} object of the target type to which the migration is intended
     * @return An instance of the specified type {@code C} after migration
     */
    @Override
    public <C extends PreferenceHolder<?, ?>> C migrateTo(final Class<C> clazz) {
        throw ErrorCode.get(CommonErrorCodes.UNSUPPORTED_ACTION);
    }
}