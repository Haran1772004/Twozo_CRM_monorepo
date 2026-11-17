package com.tlc.file.common.internal;

import com.tlc.attachment.io.Extension;

import com.tlc.commons.util.Configurations;

import com.tlc.file.common.fields.AttachmentAttributes;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages and provides access to {@link AttachmentPolicy} based on category IDs.
 * It loads policy configurations from a provided {@link Configurations} source.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class AttachmentPolicyRegistry {

    private final Map<Short, AttachmentPolicy> policies;

    private AttachmentPolicyRegistry() {
        this.policies = new HashMap<>();
    }

    private static class InstanceHolder {
        private static final AttachmentPolicyRegistry INSTANCE = new AttachmentPolicyRegistry();
    }

    /**
     * Returns the instance of {@code AttachmentPolicyRegistry}.
     *
     * @return The {@code AttachmentPolicyRegistry} instance.
     */
    public static AttachmentPolicyRegistry getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Loads attachment policies from the given {@link Configurations} source.
     *
     * @param configurations The configuration source containing attachment policy definitions.
     */
    public void loadPolicies(final Configurations configurations) {
        final Optional<String> key = configurations.getFirstKey();

        if (key.isPresent()) {
            final String config = key.get();
            final String applicationName = config.substring(0, config.indexOf("."));
            final Collection<Map<String, String>> groupedConfigs = configurations.groupedConfigArray(String.join(".", applicationName, AttachmentAttributes.CATEGORY));
            final int defaultMaxSize = Integer.parseInt(configurations.optConfig("default-max-size-mb", "50"));
            final Map<String, Integer> extensions = Stream.of(Extension.values()).map(Extension::getName).collect(Collectors.toMap(Function.identity(), extension -> defaultMaxSize));

            for (final Map<String, String> policy : groupedConfigs) {
                final short categoryId = Short.parseShort(policy.get("value"));
                final String categoryName = policy.get("name");
                final Map<String, Integer> allowedExtensions = Arrays.stream(policy.get( "allowed-extension").split(",")).map(extension -> extension.trim().split(":")).collect(Collectors.toMap(parts -> parts[0], parts -> parts.length > 1 ? Integer.parseInt(parts[1]) : defaultMaxSize));

                this.policies.put(categoryId, new AttachmentPolicy(categoryName, categoryId, defaultMaxSize, allowedExtensions.isEmpty() ? extensions : allowedExtensions));
            }
        }
    }

    /**
     * Returns the {@link AttachmentPolicy} associated with the given category ID.
     *
     * @param id The category ID.
     * @return The corresponding {@link AttachmentPolicy}, or {@code null} if not found.
     */
    public AttachmentPolicy get(final short id) {
        return policies.get(id);
    }

    /**
     * Clears the loaded policies.
     */
    public void clear() {
        this.policies.clear();
    }

}
