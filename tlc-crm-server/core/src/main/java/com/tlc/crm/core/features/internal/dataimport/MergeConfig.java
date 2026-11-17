package com.tlc.crm.core.features.internal.dataimport;

import com.tlc.crm.entity.models.FieldInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * Holds the configuration that are used to merge the source data
 *
 * @author sundar
 * @version 1.0
 */
public final class MergeConfig {

    private final Collection<FieldInfo> importFields;

    public MergeConfig() {
        this.importFields = new HashSet<>();
    }

    /**
     * Adds the importField or sub-importField map with given source
     *
     * @param importField Which contains the importable field info
     */
    public void add(final FieldInfo importField) {
        importFields.add(importField);
    }

    /**
     * Checks whether the configuration exists or not for the source and importField ID
     *
     * @param importField Which contains the importable field info
     * @return true if any configurations exists for given source and importField ID
     */
    public boolean exists(final FieldInfo importField) {
        return importFields.contains(importField);
    }

    /**
     * <p>
     * Gets the collection of mergeable import fields
     * </p>
     *
     * @return The collection of mergeable import fields
     */
    public Collection<FieldInfo> getImportFields() {
        return Collections.unmodifiableCollection(importFields);
    }
}
