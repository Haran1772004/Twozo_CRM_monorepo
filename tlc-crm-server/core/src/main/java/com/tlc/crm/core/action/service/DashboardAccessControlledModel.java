package com.tlc.crm.core.action.service;

import com.tlc.crm.common.models.AccessControlledModel;

/**
 * <p>
 * Represents a model with configurable access control for certain operations.
 * This interface defines accessibility criteria that determine whether
 * clone, restore move to trash and delete actions are enabled for the implementing model
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
public interface DashboardAccessControlledModel extends AccessControlledModel
{

    /**
     * <p>
     * Checks whether the model is favourite or not
     * </p>
     *
     * @return True if the model can favourite, false otherwise
     */
    boolean isFavourite();

    /**
     * <p>
     * Checks whether the model is restore or not
     * </p>
     *
     * @return True if the model can restore, false otherwise
     */
    boolean canRestore();

    /**
     * <p>
     * Checks whether the model is move to trash or not
     * </p>
     *
     * @return True if the model can move to trash, false otherwise
     */
    boolean canMoveToTrash();

    /**
     * <p>
     * Checks whether the model is cloneable or not
     * </p>
     *
     * @return True if the model can clone, false otherwise
     */
    boolean canClone();
}
