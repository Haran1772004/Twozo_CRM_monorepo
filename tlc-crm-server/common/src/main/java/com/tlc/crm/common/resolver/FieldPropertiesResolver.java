package com.tlc.crm.common.resolver;

import com.tlc.crm.common.resolver.impl.FieldPropertiesResolverImpl;
import com.tlc.validator.TlcModel;

import java.lang.reflect.Field;

import java.util.Collection;

/**
 * <p>
 * Resolves the meta data of the field.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface FieldPropertiesResolver {

    static FieldPropertiesResolver getInstance() {
        return FieldPropertiesResolverImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the declared {@link Field} of the given class.
     * </p>
     *
     * @param modelClass Holds the metadata of the class.
     * @return Collection of {@link Field} holds the details of the metadata of the fields.
     */
    Collection<Field> getFields(final Class<? extends TlcModel> modelClass);
}
