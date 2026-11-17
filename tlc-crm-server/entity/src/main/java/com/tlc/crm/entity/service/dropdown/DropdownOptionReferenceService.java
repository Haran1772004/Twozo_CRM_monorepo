package com.tlc.crm.entity.service.dropdown;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.dropdown.internal.DropdownOptionReferenceServiceImpl;

import java.util.Collection;

/**
 * <p>
 * Applies data-store updates when entity dropdown values must move off removed mutable options.
 * </p>
 *
 * @author Ajay
 * @version 1.0
 */
public interface DropdownOptionReferenceService {

    /**
     * <p>
     * Gets the {@link DropdownOptionReferenceService} instance.
     * </p>
     *
     * @return {@link DropdownOptionReferenceService}.
     */
    static DropdownOptionReferenceService getInstance() {
        return DropdownOptionReferenceServiceImpl.getInstance();
    }

    /**
     * <p>
     * Determines whether removing an option for the given field should trigger reference reassignment.
     * </p>
     *
     * @param fieldInfo {@link FieldInfo}.
     * @return {@code true} when reassignment applies; otherwise {@code false}.
     */
    boolean isReassignRequired(FieldInfo fieldInfo);

    /**
     * <p>
     * Reassigns entity references from each removed option id to the replacement resolved from {@code fieldConfigJson}
     * or persisted options.
     * </p>
     *
     * @param orgId            Organization id.
     * @param fieldId          Field id.
     * @param removedOptionIds Collection of removed option ids.
     */
    void reassign(long orgId, long fieldId, Collection<Long> removedOptionIds);

    /**
     * <p>
     * Reassigns entity references using retained options from the field update config when present.
     * </p>
     *
     * @param orgId            Organization id.
     * @param fieldId          Field id.
     * @param removedOptionIds Collection of removed option ids.
     * @param fieldConfigJson  Field config from the update request (may be {@code null}).
     */
    void reassign(long orgId, long fieldId, Collection<Long> removedOptionIds, JsonObject fieldConfigJson);

    /**
     * <p>
     * Reassigns entity references from the removed option id.
     * </p>
     *
     * @param orgId           Organization id.
     * @param fieldId         Field id.
     * @param removedOptionId Removed option id.
     */
    void reassign(long orgId, long fieldId, long removedOptionId);
}
