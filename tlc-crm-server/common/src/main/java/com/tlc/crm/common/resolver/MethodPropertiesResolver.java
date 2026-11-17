package com.tlc.crm.common.resolver;

import com.tlc.crm.common.resolver.impl.MethodPropertiesResolverImpl;
import com.tlc.validator.TlcModel;

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * <p>
 * Resolves the meta data of the {@link Method}.
 * </p>
 *
 * @author Ajay.
 * @version 1.0
 */
public interface MethodPropertiesResolver {

    static MethodPropertiesResolver getInstance() {
        return MethodPropertiesResolverImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the declared {@link Method} of the given class.
     * </p>
     *
     * @param modelClass Holds the metadata of the class.
     * @return Collection of {@link Method} holds the details of the metadata of the fields.
     */
    Collection<Method> getMethods(final Class<? extends TlcModel> modelClass);
}
