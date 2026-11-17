package com.tlc.web.internal.registry;

import com.tlc.commons.code.ErrorCode;
import com.tlc.web.Action;
import com.tlc.web.HttpMethod;
import com.tlc.web.WebAction;
import com.tlc.web.fields.VersionAttributes;
import com.tlc.web.status.WebErrorCodes;
import com.tlc.web.versioning.ApiVersion;
import com.tlc.web.versioning.VersionedAction;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * Registry for managing versioned actions during application startup.
 * </p>
 *
 * <p>
 * This registry supports multiple bundles registering actions in sequence.
 * Actions are built eagerly via {@link #buildAll()} after all registrations are complete.
 * </p>
 *
 * @version 1.0
 */
public final class VersionedActionRegistry {

    private static final VersionedActionRegistry INSTANCE = new VersionedActionRegistry();

    private final Map<ActionKey, VersionedAction.Builder> registryBuilders;
    private final Map<ActionKey, String> declaredDefaultVersions;

    private VersionedActionRegistry() {
        this.registryBuilders = new ConcurrentHashMap<>();
        this.declaredDefaultVersions = new ConcurrentHashMap<>();
    }

    public static VersionedActionRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * <p>
     * Registers a versioned action.
     * </p>
     *
     * @param webAction  The {@link WebAction} annotation.
     * @param apiVersion The {@link ApiVersion} annotation, or null if not declared.
     * @param action     The action implementation.
     */
    public void register(final WebAction webAction, final ApiVersion apiVersion, final Action action) {
        final String path = webAction.path();
        final boolean isApiVersionNull = Objects.isNull(apiVersion);
        final String version = isApiVersionNull ? VersionAttributes.DEFAULT_VERSION : apiVersion.value();
        final boolean isDefaultVersion = !isApiVersionNull && apiVersion.isDefaultVersion();

        for (final HttpMethod method : webAction.method()) {
            final ActionKey key = new ActionKey(path, method);

            if (isDefaultVersion) {
                resolveDefaultVersion(key, path, version);
            }

            registryBuilders.compute(key, (k, builder) -> {

                if (Objects.isNull(builder)) {
                    builder = VersionedAction.builder(path);
                }
                builder.addVersion(version, action, apiVersion);

                return builder;
            });
        }
    }

    /**
     * <p>
     * Builds all registered versioned actions, applying declared default versions.
     * </p>
     *
     * @return Map of ActionKey → built {@link VersionedAction} instances.
     */
    public Map<ActionKey, VersionedAction> buildAll() {
        final Map<ActionKey, VersionedAction> result = new HashMap<>();

        for (final Map.Entry<ActionKey, VersionedAction.Builder> entry : registryBuilders.entrySet()) {
            final ActionKey key = entry.getKey();
            final VersionedAction.Builder builder = entry.getValue();
            final String defaultVersion = declaredDefaultVersions.getOrDefault(key, VersionAttributes.DEFAULT_VERSION);

            if (!declaredDefaultVersions.containsKey(key) && !builder.hasVersion(VersionAttributes.DEFAULT_VERSION)) {
                throw ErrorCode.get(WebErrorCodes.DEFAULT_VERSIONED_ACTION_NOT_FOUND, "Default version for this path not found, Path: " + builder.build().getPath());
            }

            builder.setDefaultVersion(defaultVersion);
            result.put(key, builder.build());
        }

        return result;
    }

    /**
     * <p>
     * Clears the registry.
     * </p>
     */
    public void clear() {
        registryBuilders.clear();
        declaredDefaultVersions.clear();
    }

    /**
     * <p>
     * Resolves and validates the default version for a given path.
     * </p>
     *
     * @param key     The action key (path + method).
     * @param path    The action path, used for error messages.
     * @param version The resolved version string.
     */
    private void resolveDefaultVersion(final ActionKey key, final String path, final String version) {
        final String existing = declaredDefaultVersions.put(key, version);

        if (Objects.nonNull(existing)) {
            throw ErrorCode.get(WebErrorCodes.VERSIONED_ACTION_NOT_FOUND, String.format("isDefaultVersion already declared for path '%s' method '%s' as version '%s'. " +
                            "isDefaultVersion must be set on exactly one class per path.", path, key.method(), existing));
        }
    }

    /**
     * <p>
     * Composite key for path and HTTP method.
     * </p>
     *
     * @param path   The path.
     * @param method The HTTP method.
     */
    public record ActionKey(String path, HttpMethod method) {

    }
}