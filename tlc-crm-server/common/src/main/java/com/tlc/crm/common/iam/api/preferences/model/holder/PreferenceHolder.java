package com.tlc.crm.common.iam.api.preferences.model.holder;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.api.preferences.model.Preference;
import com.tlc.crm.common.iam.api.preferences.model.builder.PreferenceBuilder;
import com.tlc.crm.common.iam.api.preferences.model.builder.internal.store.PreferenceBuilderStore;
import com.tlc.crm.common.iam.api.preferences.model.type.PreferenceType;
import com.tlc.crm.common.iam.field.preferences.PreferenceFields;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Holds the {@link PreferenceHolder} details.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public abstract class PreferenceHolder<T extends PreferenceType, P extends Preference<T>> implements TlcModel {

    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.preference.orgId.required")
    private Long orgId;

    private final Map<T, P> preferences;
    private final PreferenceBuilderStore builderStore;

    protected PreferenceHolder() {
        this.preferences = new HashMap<>();
        this.builderStore = PreferenceBuilderStore.getInstance();
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    /**
     * <p>
     * Adds the given preference into holder.
     * </p>
     *
     * @param preference Represents the {@link Preference}.
     */
    public void add(final P preference) {
        preferences.put(preference.getType(), preference);
    }

    /**
     * <p>
     * Sets the given preference into holder.
     * </p>
     *
     * @param preferences Represents the {@link Preference}.
     */
    public void addAll(final Collection<P> preferences) {
        for (final P preference : preferences) {
            add(preference);
        }
    }

    /**
     * <p>
     * Gets the preference based on the type.
     * </p>
     *
     * @param type Represents the {@link PreferenceType}.
     * @return the {@link Preference}.
     */
    public <U extends P> U get(final T type) {
        return (U) preferences.get(type);
    }

    /**
     * <p>
     * Gets all the available preferences.
     * </p>
     *
     * @return The {@link Preference}.
     */
    public Collection<P> getAll() {
        return preferences.values();
    }

    /**
     * <p>
     * Adds the {@link Preference} JSON.
     * </p>
     *
     * @param preferencesJson Holds the details of the preference.
     */
    public void addPreferencesAsJson(final JsonObject preferencesJson) {
        for (final String preferenceName : preferencesJson.keySet()) {
            final PreferenceBuilder<P> preferenceBuilder = builderStore.get(preferenceName);

            if (Objects.nonNull(preferenceBuilder)) {
                add(preferenceBuilder.build(preferencesJson.getJsonObject(preferenceName)));
            }
        }
    }

    /**
     * <p>
     * Provides the {@link Preference} as JSON.
     * </p>
     *
     * @return {@link JsonObject} Holds the details of the preference.
     */
    public JsonObject toJson(final boolean includePreferenceId) {
        final JsonObject preferencesJson = Json.object();

        for (final Map.Entry<T, P> preferencesMap : preferences.entrySet()) {
            final T preferenceType = preferencesMap.getKey();
            final PreferenceBuilder<P> preferenceBuilder = builderStore.get(preferenceType);

            preferencesJson.put(preferenceType.type(), preferenceBuilder.build(preferencesMap.getValue()));
        }

        if (includePreferenceId && Objects.nonNull(id)) {
            preferencesJson.put(PreferenceFields.ID.getName(), id);
        }
        return preferencesJson;
    }

    /**
     * <p>
     * Migrates the current preference holder to the specified class type
     * </p>
     *
     * @param <C>   The target type that extends {@link  PreferenceHolder}
     * @param clazz The {@code Class} object of the target type to which the migration is intended
     * @return An instance of the specified type {@code C} after migration
     */
    protected abstract <C extends PreferenceHolder<?, ?>> C migrateTo(final Class<C> clazz);
}
