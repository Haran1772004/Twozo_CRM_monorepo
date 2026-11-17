package com.tlc.crm.entity.list.filter.internal;

/**
 * <p>
 * Provides the holder of the join status. It indicates the joins are loaded or not.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class JoinStatusHolder {

    private boolean isJoinsLoaded;
    private boolean isMapperTableJoinsAreLoaded;

    public boolean isJoinsLoaded() {
        return isJoinsLoaded;
    }

    public void setJoinsLoaded(final boolean joinsLoaded) {
        isJoinsLoaded = joinsLoaded;
    }

    public boolean isMapperTableJoinsAreLoaded() {
        return isMapperTableJoinsAreLoaded;
    }

    public void setMapperTableJoinsAreLoaded(final boolean isMapperTableJoinsAreLoaded) {
        this.isMapperTableJoinsAreLoaded = isMapperTableJoinsAreLoaded;
    }
}
