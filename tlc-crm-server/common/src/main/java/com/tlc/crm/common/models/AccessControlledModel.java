package com.tlc.crm.common.models;

import com.tlc.validator.TlcModel;

/**
 * <p>
 * Represents a model with configurable access control for certain operations.
 * This interface defines accessibility criteria that determine whether
 * update and delete actions are enabled for the implementing model
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public interface AccessControlledModel extends TlcModel {

    /**
     * <p>
     * Checks whether the model is editable or not
     * </p>
     *
     * @return True if the model can be edited, false otherwise
     */
    boolean isEditable();

    /**
     * <p>
     * Checks whether the model is deletable or not
     * </p>
     *
     * @return True if the model can be deleted, false otherwise
     */
    boolean isDeletable();
}