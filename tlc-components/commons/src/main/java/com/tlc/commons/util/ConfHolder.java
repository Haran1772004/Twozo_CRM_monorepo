package com.tlc.commons.util;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads the configuration that are needed to initialize the application.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ConfHolder {

    private final Map<String, Map<String, String>> groupedConfigs;

    private ConfHolder() {
        this.groupedConfigs = new ConcurrentHashMap<>();
    }

    private static class Instance {
        private static final ConfHolder INSTANCE = new ConfHolder();
    }

    /**
     * Gets the instance of {@link ConfHolder}.
     *
     * @return The {@link ConfHolder} instance.
     */
    public static ConfHolder getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Gets the properties of the config for the name provided.
     *
     * @param name Refers the name of config.
     * @return
     */
    public Configurations getProperties(final String name) {
        final Map<String, String> configs = groupedConfigs.get(name);

        if (Objects.isNull(configs)) {
            throw ErrorCode.get(ErrorCodes.CONFIG_NOT_FOUND, String.format("Configuration group name: %s", name));
        }
        return new Configurations(configs);
    }

    /**
     * Loads the configs grouped by modules.
     *
     * @param groupedConfigs Refers the grouped config properties.
     */
    public void loadGroupedConfigs(final Map<String, Map<String, String>> groupedConfigs) {
        this.groupedConfigs.putAll(groupedConfigs);
    }

    /**
     * <p>
     * Loads the configuration from the files
     * </p>
     */                 //TODO not yet tested in karaf as AWS dependency for S3 is not resolved in OSGI
    public void loadConfigFromFiles() {
        final File[] files = new File(Env.getConfDirectory()).listFiles();

        if (Objects.nonNull(files)) {
            final String configFileExtension = ".cfg";

            for (final File file : files) {
                final String name = file.getName();

                if (file.isFile() && name.endsWith(configFileExtension)) {
                    final String fileName = name.replace(configFileExtension, "");

                    try (final FileReader reader = new FileReader(file)) {
                        final Properties properties = new Properties();

                        properties.load(reader);

                        for (final Map.Entry<Object, Object> entry : properties.entrySet()) {
                            final String key = String.valueOf(entry.getKey());

                            if (key.startsWith(fileName)) {
                                final Map<String, String> configs = new HashMap<>();

                                configs.put(key.replace(String.format("%s%s", fileName, "."), ""), String.valueOf(entry.getValue()));
                                groupedConfigs.computeIfAbsent(fileName, k -> new HashMap<>()).putAll(configs);
                            } else {
                                throw ErrorCode.get(ErrorCodes.INVALID_CONFIG_FORMAT);
                            }
                        }
                    } catch (Exception e) {
                        throw ErrorCode.get(ErrorCodes.FILE_NOT_FOUND);
                    }
                }
            }
        }
    }
}
