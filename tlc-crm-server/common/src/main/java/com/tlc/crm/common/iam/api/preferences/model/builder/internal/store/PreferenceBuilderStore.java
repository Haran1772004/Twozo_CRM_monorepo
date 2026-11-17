package com.tlc.crm.common.iam.api.preferences.model.builder.internal.store;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.iam.api.preferences.model.Preference;
import com.tlc.crm.common.iam.api.preferences.model.builder.PreferenceBuilder;
import com.tlc.crm.common.iam.api.preferences.model.type.PreferenceType;
import com.tlc.crm.common.iam.internal.status.PreferenceErrorCodes;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Holds all the registered {@link PreferenceBuilder}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class PreferenceBuilderStore implements Serializable {

    private final Map<String, Map<String, PreferenceBuilder<? extends Preference<?>>>> registeredPreferencesBuilders;

    private static final class Instance {
        private static final PreferenceBuilderStore PREFERENCE_BUILDER_STORE = new PreferenceBuilderStore();
    }

    private PreferenceBuilderStore() {
        this.registeredPreferencesBuilders = new HashMap<>();
    }

    public static PreferenceBuilderStore getInstance() {
        return Instance.PREFERENCE_BUILDER_STORE;
    }

    /**
     * <p>
     * Adds the {@link PreferenceBuilder} into the store.
     * </p>
     *
     * @param group       Represents the name of the module.
     * @param builderName Represents the builder name.
     * @param builder     Represents the {@link PreferenceBuilder} to builds preference models.
     */
    public void add(final String group, final String builderName, final PreferenceBuilder<? extends Preference<?>> builder) {
        final Map<String, PreferenceBuilder<? extends Preference<?>>> preferenceBuilders =
                registeredPreferencesBuilders.computeIfAbsent(group, grp -> new HashMap<>());

        if (preferenceBuilders.containsKey(builderName)) {
            throw ErrorCode.get(PreferenceErrorCodes.PREFERENCE_BUILDER_ALREADY_REGISTERED, String.format("Builder Name : %s", builderName));
        }
        preferenceBuilders.put(builderName, builder);
    }

    /**
     * <p>
     * Fetches the {@link PreferenceBuilder} based on the given builder name.
     * </p>
     *
     * @param preferenceName Represents the preference name.
     * @return {@link PreferenceBuilder}.
     */
    public <T extends Preference<?>> PreferenceBuilder<T> get(final String preferenceName) {
        final Collection<Map<String, PreferenceBuilder<? extends Preference<?>>>> builders
                = registeredPreferencesBuilders.values();

        for (final Map<String, PreferenceBuilder<? extends Preference<?>>> buildersMap : builders) {
            if (buildersMap.containsKey(preferenceName)) {
                return (PreferenceBuilder<T>) buildersMap.get(preferenceName);
            }
        }
        return null;
    }

    /**
     * <p>
     * Fetches the {@link PreferenceBuilder} based on the given builder name.
     * </p>
     *
     * @param preferenceType Represents the preference type.
     * @return {@link PreferenceBuilder}.
     */
    public <T extends Preference<?>> PreferenceBuilder<T> get(final PreferenceType preferenceType) {
        return get(preferenceType.type());
    }
}
