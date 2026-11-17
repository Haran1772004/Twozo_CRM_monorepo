package com.tlc.crm.entity.service.filter;

import com.tlc.crm.entity.filter.api.models.CriteriaField;
import com.tlc.crm.entity.filter.api.models.DropdownDetails;
import com.tlc.crm.entity.service.internal.FilterFieldServiceImpl;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;

/**
 * <p>
 * Provides an abstract way to deal with filter field
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface FilterFieldService {

    /**
     * <p>
     * Gets the {@link FilterFieldService} instance.
     * </p>
     *
     * @return {@link FilterFieldService}.
     */
    static FilterFieldService getInstance() {
        return FilterFieldServiceImpl.getInstance();
    }

    /**
     * <p>
     * Gets the non-secure {@link FilterFieldService} instance.
     * Use this in system/internal operations where no user security context is
     * available (e.g., org initialization) to avoid recursive field-permission lookups.
     * </p>
     *
     * @return non-secure {@link FilterFieldService}.
     */
    static FilterFieldService getSecureInstance() {
        return FilterFieldServiceImpl.getSecureInstance();
    }

    /**
     * <p>
     * Fetches the entities available for the specified entity source id.
     * </p>
     *
     * @param orgId                   Represents the organization id.
     * @param sourceId                Used for fetch the corresponding entities.
     * @param comparator              Used to compare the field entities.
     * @param isDropdownDetailsNeeded A flag that indicates the dropdown details are needed or not.
     * @return Collection of {@link CriteriaField}.
     */
    Collection<CriteriaField> getFieldsBySourceId(final Long orgId, final Long sourceId,
                                                  final Comparator<CriteriaField> comparator,
                                                  final boolean isDropdownDetailsNeeded);

    /**
     * <p>
     * Fetches the entities available for the specified entity source ids.
     * </p>
     *
     * @param orgId                   Represents the organization id.
     * @param sourceIds               Used for fetch the corresponding entities.
     * @param comparator              Used to compare the field entities.
     * @param isDropdownDetailsNeeded A flag that indicates the dropdown details are needed or not.
     * @return Collection of {@link CriteriaField}.
     */
    Collection<CriteriaField> getFieldsBySourceIds(final Long orgId, final Set<Long> sourceIds,
                                                   final Comparator<CriteriaField> comparator,
                                                   final boolean isDropdownDetailsNeeded);

    /**
     * <p>
     * Fetches the {@link CriteriaField} to the corresponding id.
     * </p>
     *
     * @param orgId   Represents the organization id.
     * @param isField Represents the required filter field is field or option.
     * @param id      Used for fetch the corresponding entity.
     * @return {@link CriteriaField}.
     */
    CriteriaField getFieldById(final Long orgId, final Long id, final boolean isField, final boolean isDropdownDetailsNeeded);

    /**
     * <p>
     * Checks the given field id is exists or not.
     * </p>
     *
     * @param orgId   Represents the organization id.
     * @param id      Filter field id.
     * @param isField Represents the required filter field is field or option.
     * @return If the filter field details is exists it will return true, otherwise it will return false
     */
    boolean exists(final Long orgId, final Long id, final boolean isField);

    /**
     * <p>
     * Fetches the criteria values {@link DropdownDetails} of the given field id.
     * </p>
     *
     * @param fieldTypeId        For fetch the corresponding {@link DropdownDetails} entity.
     * @param isAdditionalOption A flag that represents the additional options or values dropdown details.
     * @return {@link DropdownDetails} entity that holds the values of the dropdown details.
     */
    Collection<DropdownDetails> getDropdownDetailsByFieldTypeId(final Long fieldTypeId, final boolean isAdditionalOption);

    /**
     * <p>
     * Fetches the {@link CriteriaField} to the corresponding name.
     * </p>
     *
     * @param orgId   Represents the organization id.
     * @param name    Represents the name of the field.
     * @param isField A flag that indicates whether the required filter field is field or option.
     * @return {@link CriteriaField} entity.
     */
    <T extends CriteriaField> Collection<T> getFieldsByName(final Long orgId, final String name, final boolean isField);

    /**
     * <p>
     * Fetches the {@link CriteriaField} to the corresponding name
     * </p>
     *
     * @param orgId  Represents an organization id.
     * @param name   Represents the name of the field.
     * @param source Represents the given source.
     * @return {@link CriteriaField} entity
     */
    <T extends CriteriaField> Collection<T> getFieldsByName(final long orgId, final String name, final String source);
}