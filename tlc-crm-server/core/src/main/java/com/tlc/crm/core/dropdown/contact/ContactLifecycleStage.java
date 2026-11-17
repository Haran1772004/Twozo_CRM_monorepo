package com.tlc.crm.core.dropdown.contact;

import com.tlc.crm.entity.dropdown.DefaultOption;

/**
 * An enum contains the various {@link ContactLifecycleStage} data
 *
 * @author Kishore J
 * @version 1.0
 */
public enum ContactLifecycleStage implements DefaultOption {

    LEAD((short) 1, "Lead"),
    SALES_QUALIFIED_LEAD((short) 2, "Sales qualified lead"),
    CUSTOMER((short) 10, "Customer", true);

    private final String name;
    private final short order;
    private final boolean isLastStage;

    ContactLifecycleStage(final short order, final String name) {
        this(order, name, false);
    }

    ContactLifecycleStage(final short order, final String name, final boolean isLastStage) {
        this.name = name;
        this.order = order;
        this.isLastStage = isLastStage;
    }

    /**
     * {@inheritDoc}
     *
     * @return Lifecycle stage name.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * <p>
     * Provides the order of the lifecycle stage.
     * </p>
     *
     * @return Lifecycle stage order.
     */
    public short getOrder() {
        return order;
    }

    /**
     * {@inheritDoc}
     *
     * @return true If the option is editable, otherwise it will return false
     */
    @Override
    public boolean isEditable() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @return true If the option is default, otherwise it will return false
     */
    @Override
    public boolean isDefault() {
        return false;
    }

    /**
     * <p>
     * Provides a flag that indicates whether the stage is last stage or not.
     * </p>
     *
     * @return true If the stage is last one, otherwise it will return false.
     */
    public boolean isLastStage() {
        return isLastStage;
    }
}
