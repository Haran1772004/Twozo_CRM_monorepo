package com.tlc.commons.util;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Holds the config related properties
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class Configurations {

    private final Map<String, String> configs;

    public Configurations(final Map<String, String> configs) {
        this.configs = configs;
    }

    /**
     * <p>
     * Gets the config based on the group name
     * </p>
     *
     * @param name Represents the group name
     * @return the value
     */
    public String getConfig(final String name) {
        final String config = configs.get(name);

        if (Objects.isNull(config)) {
            throw ErrorCode.get(ErrorCodes.CONFIG_NOT_FOUND);
        }
        return config;
    }

    /**
     * <p>
     * Gets the config based on the group name
     * </p>
     *
     * @param name Represents the group name
     * @return the value
     */
    public String optConfig(final String name, final String defaultValue) {
        final String config = configs.get(name);

        return Objects.isNull(config) ? defaultValue : config;
    }

    /**
     * <p>
     * Fetches the first key present in the configurations
     * </p>
     *
     * @return the key
     */
    public Optional<String> getFirstKey() {
        return configs.keySet().stream().findFirst();
    }

    /**
     * <p>
     * Gets the nested configurations for the given name
     * </p>
     *
     * @param name Represents the name
     * @return the {@link Configurations}
     */
    public Configurations getNestedConfigs(final String name) {
        return new Configurations(getNestedConfigMap(name));
    }

    /**
     * Gets the values {@link Collection} of given configuration name
     *
     * @param name Represents the configuration name
     * @return the {@link Collection} configuration values
     */
    public Collection<String> getValues(final String name) {
        return getNestedConfigMap(name).values();
    }

    /**
     * <p>
     * Checks whether the given value is present in given configs
     * </p>
     *
     * @param value Represents the value
     * @return true, if present
     */
    public boolean containsValue(final String value) {
        return configs.containsValue(value);
    }

    /**
     * Retrieves the integer value associated with the given key from the configuration.
     *
     * @param key the configuration key to retrieve the value.
     * @return the integer value associated with the given key.
     * @throws ErrorCode if the key is not found or the value cannot be parsed as an integer.
     */
    public int getInt(final String key) {
        final String value = configs.get(key);

        if (Objects.isNull(value)) {
            throw ErrorCode.get(ErrorCodes.CONFIG_NOT_FOUND);
        }

        return Integer.parseInt(value);
    }

    /**
     * Retrieves the long value associated with the given key from the configuration.
     *
     * @param key the configuration key to retrieve the value.
     * @return the long value associated with the given key.
     */
    public long getLong(final String key) {
        final String value = configs.get(key);

        if (Objects.isNull(value)) {
            throw ErrorCode.get(ErrorCodes.CONFIG_NOT_FOUND);
        }

        return Long.parseLong(value);
    }

    /**
     * Retrieves the integer value associated with the given key from the configuration,
     * or returns the provided default value if the key is not present.
     *
     * @param key          the configuration key to retrieve the value for.
     * @param defaultValue the default value to return if the key is not found.
     * @return the integer value associated with the given key, or the default value if the key is not found.
     */
    public int optInt(final String key, final int defaultValue) {
        final String value = configs.get(key);

        return Objects.isNull(value) ? defaultValue : Integer.parseInt(value);
    }

    /**
     * <p>
     * Checks whether the config map is empty or not
     * </p>
     *
     * @return true, if it is empty, false otherwise
     */
    public boolean isEmpty() {
        return configs.isEmpty();
    }

    /**
     * Processes and groups configuration entries based on their indexed keys.
     *
     * <p>This method is specifically designed to handle configurations following
     * a structured format like:</p>
     *
     * <pre>
     * type:
     *   - event: event-name
     *     topic: topic-name
     *     consumer:
     *       name: consumer-name
     *   - event: event-name
     *     topic: topic-name
     *     consumer:
     *       name: consumer-name
     * </pre>
     *
     * @param name the name of the configuration set to retrieve and process
     * @return a list of maps, where each map represents a grouped set of configurations
     */
    public Collection<Map<String, String>> groupedConfigArray(final String name) {
        final Map<String, String> nestedConfigMap = getNestedConfigMap(name);
        final Map<String, Map<String, String>> groupedConfigs = new LinkedHashMap<>();

        for (final Map.Entry<String, String> entry : nestedConfigMap.entrySet()) {
            final String key = entry.getKey();

            if (!key.contains(".")) {
                continue;
            }
            final String[] parts = key.split("\\.", 2);

            groupedConfigs.computeIfAbsent(parts[0], indexMap -> new HashMap<>())
                    .put(parts[1], entry.getValue());
        }
        return groupedConfigs.values();
    }

    /**
     * Gets the nested configuration map for the given configuration name
     *
     * @param name Represents the configuration name
     * @return the nested configuration map
     */
    private Map<String, String> getNestedConfigMap(final String name) {

        if (Objects.isNull(name)) {
            throw ErrorCode.get(ErrorCodes.CONFIG_NOT_FOUND);
        }
        final Map<String, String> filteredConfigs = new HashMap<>();
        final String prefix = String.format("%s%s", name, ".");
        final int length = name.length() + 1;

        for (final Map.Entry<String, String> entry : configs.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                filteredConfigs.put(entry.getKey().substring(length), entry.getValue());
            }
        }
        return filteredConfigs;
    }

    /**
     * Returns the configuration properties.
     *
     * @return A map containing configuration key–value pairs.
     */
    public Map<String, String> getConfigs() {
        return configs;
    }
}
