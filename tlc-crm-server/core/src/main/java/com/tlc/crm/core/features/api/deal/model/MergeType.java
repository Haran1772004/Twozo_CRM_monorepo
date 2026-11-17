package com.tlc.crm.core.features.api.deal.model;

import java.util.Objects;

/**
 * <p>
 * A model represents the {@link MergeType} details
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public enum MergeType {

    REDIRECT("Redirect", 1),
    SUCCESS("Success", 2),
    FAILURE("Failure", 3);

    private final int id;

    private final String name;

    MergeType(final String name, final int id) {
        this.name = Objects.requireNonNull(name);
        this.id = id;
    }

    /**
     * <p>
     * Gets the id of the state
     * </p>
     *
     * @return the id of State
     */
    public int getId() {
        return this.id;
    }

    /**
     * <p>
     * Gets the name of the state
     * </p>
     *
     * @return the name of State
     */
    public String getName() {
        return this.name;
    }
}
