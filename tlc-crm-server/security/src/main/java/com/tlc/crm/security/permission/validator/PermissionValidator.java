package com.tlc.crm.security.permission.validator;

/**
 * Validator for handling permission checks across specific models
 *
 * @author Sundar
 * @version 1.0
 */
public interface PermissionValidator {

    /**
     * Gets the permission validator type
     *
     * @return the {@link Type}
     */
    Type getType();

    /**
     * Represents the permission validator type
     */
    interface Type {

        /**
         * Gets the permission validator type's name
         *
         * @return the name of the permission validator type
         */
        String getName();
    }
}