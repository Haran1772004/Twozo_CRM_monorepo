package com.tlc.crm.core.api.insights.models;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.internal.status.CoreErrorCodes;
import com.tlc.crm.entity.fields.source.Source;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * <p>
 * Represents the entity insights group.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public enum EntityInsightsGroup {

    ACTIVITY((short) 1, EnumSet.of(Source.CONTACT, Source.COMPANY, Source.DEAL)),
    EMAIL((short) 2, EnumSet.of(Source.CONTACT, Source.COMPANY, Source.DEAL)),
    DEAL((short) 3, EnumSet.of(Source.CONTACT, Source.COMPANY)),
    ;

    private final short id;
    private final Collection<Source> applicableSources;

    EntityInsightsGroup(final short id, final Set<Source> applicableSources) {
        this.id = id;
        this.applicableSources = Collections.unmodifiableSet(applicableSources);
    }

    /**
     * <p>
     * Returns the numeric id stored in the database for this group.
     * </p>
     *
     * @return the group id
     */
    public short getId() {
        return id;
    }

    /**
     * <p>
     * Resolves an {@code EntityInsightsGroup} from its numeric database id.
     * </p>
     *
     * @param id the numeric id
     * @return the matching group
     */
    public static EntityInsightsGroup fromId(final short id) {
        for (final EntityInsightsGroup group : values()) {

            if (group.id == id) {
                return group;
            }
        }
        throw ErrorCode.get(CoreErrorCodes.INVALID_INSIGHTS_GROUP);
    }

    /**
     * <p>
     * Applies to.
     * </p>
     *
     * @param source the source
     * @return the applies to value
     */
    public boolean appliesTo(final Source source) {
        return applicableSources.contains(source);
    }

    /**
     * <p>
     * Checks if this group is ACTIVITY.
     * </p>
     *
     * @return true if this is ACTIVITY group
     */
    public boolean isActivity() {
        return this == ACTIVITY;
    }

    /**
     * <p>
     * Checks if this group is EMAIL.
     * </p>
     *
     * @return true if this is EMAIL group
     */
    public boolean isEmail() {
        return this == EMAIL;
    }

    /**
     * <p>
     * Checks if this group is DEAL.
     * </p>
     *
     * @return true if this is DEAL group
     */
    public boolean isDeal() {
        return this == DEAL;
    }

    /**
     * <p>
     * Gets the entity insights group.
     * </p>
     *
     * @return the all value
     */
    public static EntityInsightsGroup[] getAll() {
        return Arrays.copyOf(values(), values().length);
    }
}