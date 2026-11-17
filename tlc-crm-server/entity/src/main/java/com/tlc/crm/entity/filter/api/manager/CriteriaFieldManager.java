package com.tlc.crm.entity.filter.api.manager;

import com.tlc.crm.entity.filter.api.manager.impl.CriteriaFieldManagerImpl;
import com.tlc.crm.entity.filter.api.models.CriteriaField;
import com.tlc.crm.entity.filter.api.models.CriteriaOption;
import com.tlc.crm.entity.filter.api.models.DropdownDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;

/**
 * <p>
 * Provides the {@link CriteriaField} related DB fetch action.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface CriteriaFieldManager {

    static CriteriaFieldManager getInstance() {
        return CriteriaFieldManagerImpl.getInstance();
    }

    /**
     * <p>
     * Fetches all the filter options are available in db.
     * </p>
     *
     * @return Collection of {@link CriteriaOption}.
     */
    Collection<CriteriaOption> getAllFilterOptions();

    /**
     * <p>
     * Fetches the dropdown details for the given field type ids.
     * </p>
     *
     * @param fieldTypeIds Collection of field type id.
     * @return Map of {@link DropdownDetails}.
     */
    Map<Long, List<DropdownDetails>> fetchDropdownDetailsByFieldTypeId(final Collection<Long> fieldTypeIds);

    /**
     * <p>
     * Fetches the field entity by criteria id.
     * </p>
     *
     * @param orgId               represents the organization id.
     * @param criteriaId          for retrieve the required field entity.
     * @param isField             When it is true, the values are fetched from field table, else from options table.
     * @param requiredSecureFetch Represents whether to fetch secure or not.
     * @return {@link CriteriaField} instance that holds the retrieved values.
     */
    CriteriaField getCriteriaField(final Long orgId, final Long criteriaId, final boolean isField, final boolean requiredSecureFetch);

    /**
     * <p>
     * Fetches collection of criteria option given by source ids.
     * </p>
     *
     * @param sourceIds Represents the source ids.
     * @return Collection of {@link CriteriaOption}.
     */
    Collection<CriteriaOption> getCriteriaOptionBySourceId(final Set<Long> sourceIds);

    /**
     * <p>
     * Fetches the criteria option given by criteria field id.
     * </p>
     *
     * @return The {@link CriteriaOption}
     */
    CriteriaOption getCriteriaOptionById(final Long criteriaFieldId);
}