package com.tlc.crm.core.dropdown.contact;

import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.entity.dropdown.DefaultOption;

import java.util.Collection;
import java.util.Set;

/**
 * <p>
 * An enum contains the various {@link ContactLifecycleStatus} data.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum ContactLifecycleStatus implements DefaultOption {

    NEW((short) 1, "New", true),
    CONTACTED((short) 2, "Contacted", false),
    INTERESTED((short) 3, "Interested", false),
    UNQUALIFIED((short) 10, "Unqualified", LifeCycleStatus.Label.CLOSED_LOST, false),
    QUALIFIED((short) 1, "Qualified", true),
    LOST((short) 10, "Lost", LifeCycleStatus.Label.CLOSED_LOST, false),
    WON((short) 9, "Won", LifeCycleStatus.Label.CLOSED_WON, true),
    CHURNED((short) 10, "Churned", LifeCycleStatus.Label.CLOSED_LOST, false),
    ;

    private final String name;
    private final LifeCycleStatus.Label label;
    private final short order;
    private final boolean isDefault;

    ContactLifecycleStatus(final short order, final String name, final boolean isDefault) {
        this(order, name, null, isDefault);
    }

    ContactLifecycleStatus(final short order, final String name, final LifeCycleStatus.Label label, final boolean isDefault) {
        this.name = name;
        this.label = label;
        this.order = order;
        this.isDefault = isDefault;
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
     * Provides the label of the lifecycle status.
     * </p>
     *
     * @return Lifecycle status label.
     */
    public LifeCycleStatus.Label getLabel() {
        return label;
    }

    /**
     * <p>
     * Provides the order of the lifecycle status.
     * </p>
     *
     * @return Lifecycle status order.
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
        return isDefault;
    }

    /**
     * <p>
     * Retrieves a collection of default options represented by the {@link ContactLifecycleStatus} enum values.
     * </p>
     *
     * @return The collection of {@link DefaultOption}
     */
    public static Collection<ContactLifecycleStatus> get(final ContactLifecycleStage lifecycleStage) {
        return switch (lifecycleStage) {
            case LEAD -> Set.of(NEW, CONTACTED, INTERESTED, UNQUALIFIED);
            case SALES_QUALIFIED_LEAD -> Set.of(QUALIFIED, LOST);
            case CUSTOMER -> Set.of(WON, CHURNED);
        };
    }
}
