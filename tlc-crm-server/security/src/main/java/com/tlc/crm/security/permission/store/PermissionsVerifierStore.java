package com.tlc.crm.security.permission.store;

import com.tlc.crm.security.permission.verifier.PermissionsVerifier;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The store contains the permissions verifiers, each paired with its corresponding authorizer
 *
 * @author sundar
 * @version 1.0
 */
public class PermissionsVerifierStore {

    private final Map<String, Map<String, PermissionsVerifier<?>>> permissionsVerifiers;

    private static final class Instance {
        private static final PermissionsVerifierStore INSTANCE = new PermissionsVerifierStore();
    }

    /**
     * Gets the {@link PermissionsVerifierStore}
     *
     * @return {@link PermissionsVerifierStore} instance
     */
    public static PermissionsVerifierStore getInstance() {
        return PermissionsVerifierStore.Instance.INSTANCE;
    }

    private PermissionsVerifierStore() {
        this.permissionsVerifiers = new HashMap<>();
    }

    /**
     * <p>
     * Adds the {@link PermissionsVerifier} into the container.
     * </p>
     *
     * @param authorizerClass     Represents the authorizer class.
     * @param permissionsVerifier Represents the verifier of permissions.
     */
    public void addVerifier(final String groupId, final Class<?> authorizerClass, final PermissionsVerifier<?> permissionsVerifier) {
        permissionsVerifiers.computeIfAbsent(groupId, grpId -> new HashMap<>()).put(authorizerClass.getName(), permissionsVerifier);
    }

    /**
     * Gets the respective {@link PermissionsVerifier} of given {@link Annotation}
     *
     * @param annotation Which represents the Authorizer annotation
     * @param <T>        The type of authorizer
     * @return The corresponding {@link PermissionsVerifier}
     */
    public <T extends PermissionsVerifier<Annotation>> Optional<T> get(final Annotation annotation) {
        for (final Map<String, PermissionsVerifier<?>> permissionsVerifiers : permissionsVerifiers.values()) {
            final String annotationName = annotation.annotationType().getName();

            if (permissionsVerifiers.containsKey(annotationName)) {
                return Optional.of((T) permissionsVerifiers.get(annotationName));
            }
        }
        return Optional.empty();
    }

    /**
     * <p>
     * Removes the permission verifier of the given group ID.
     * </p>
     *
     * @param groupId Represents the group ID.
     */
    public void remove(final String groupId) {
        permissionsVerifiers.remove(groupId);
    }

    /**
     * Removes all registered permission verifiers.
     */
    public void clear() {
        permissionsVerifiers.clear();
    }
}
