package com.tlc.crm.security.registrar;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.service.registrar.ResourceRegistrar;
import com.tlc.crm.security.internal.status.PermissionErrorCodes;
import com.tlc.crm.security.permission.annotation.validator.ActionLimitValidator;
import com.tlc.commons.annotation.InstanceProvider;
import com.tlc.crm.security.permission.annotation.verifier.Verifier;
import com.tlc.crm.security.permission.validator.LimitValidator;
import com.tlc.crm.security.permission.verifier.PermissionsVerifier;
import com.tlc.crm.security.permission.store.LimitValidatorRegistry;
import com.tlc.crm.security.permission.store.PermissionsVerifierStore;

import com.tlc.crm.security.registrar.model.SecurityRegistrarData;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * <p>
 * The registrar that registers the given actions.
 * </p>
 *
 * @author sundar
 * @version 1.0
 * @see ResourceRegistrar
 */
public class SecurityResourceRegistrar implements ResourceRegistrar<SecurityRegistrarData> {

    private final PermissionsVerifierStore permissionsVerifierStore;
    private final LimitValidatorRegistry limitValidatorRegistry;

    private SecurityResourceRegistrar() {
        permissionsVerifierStore = PermissionsVerifierStore.getInstance();
        limitValidatorRegistry = LimitValidatorRegistry.getInstance();
    }

    private static final class Instance {
        private static final SecurityResourceRegistrar SECURITY_RESOURCE_REGISTRAR = new SecurityResourceRegistrar();
    }

    public static SecurityResourceRegistrar getInstance() {
        return Instance.SECURITY_RESOURCE_REGISTRAR;
    }

    /**
     * {@inheritDoc}
     *
     * @param registrarData Holds the {@link SecurityRegistrarData} which is needed for registering the resources/actions.
     */
    @Override
    public void register(final SecurityRegistrarData registrarData) {
        final String moduleName = registrarData.getModuleName();

        for (final Class<?> classObj : registrarData.getClasses()) {
            final String className = classObj.getName();

            if (className.contains("permission.verifier")) {
                try {
                    final Verifier verifier = classObj.getAnnotation(Verifier.class);

                    if (Objects.nonNull(verifier)) {
                        for (final Method method : classObj.getMethods()) {
                            if (method.isAnnotationPresent(InstanceProvider.class)
                                    && method.invoke(null) instanceof final PermissionsVerifier<?> permissionsVerifier) {
                                permissionsVerifierStore.addVerifier(moduleName, verifier.authorizerClass(), permissionsVerifier);
                            }
                        }
                    }
                } catch (Exception exception) {
                    throw ErrorCode.get(PermissionErrorCodes.FAILED_TO_REGISTER_PERMISSION_VERIFIER_ACTIONS, exception);
                }
            } else if (className.contains("permission.validator")) {
                try {
                    final ActionLimitValidator validator = classObj.getAnnotation(ActionLimitValidator.class);

                    if (Objects.nonNull(validator)) {
                        for (final Method method : classObj.getMethods()) {
                            if (method.isAnnotationPresent(InstanceProvider.class)
                                    && method.invoke(null) instanceof final LimitValidator limitValidator) {
                                limitValidatorRegistry.addValidator(moduleName, validator.type(), limitValidator);
                                break;
                            }
                        }
                    }
                } catch (Exception exception) {
                    throw ErrorCode.get(PermissionErrorCodes.FAILED_TO_REGISTER_LIMIT_PERMISSION_VALIDATOR_ACTIONS, exception);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param registrarData Holds the {@link SecurityRegistrarData} which is needed for removing the resources/actions.
     */
    @Override
    public void unregister(final SecurityRegistrarData registrarData) {
        final String moduleName = registrarData.getModuleName();

        permissionsVerifierStore.remove(moduleName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterAll() {
        permissionsVerifierStore.clear();
        limitValidatorRegistry.clear();
    }
}
