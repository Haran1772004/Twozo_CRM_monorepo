package com.tlc.crm.core.api.field.manager;

import com.tlc.crm.core.api.field.manager.internal.DependableFieldsDataManagerImpl;
import com.tlc.crm.core.fields.api.model.DependableFieldsData;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;

import java.util.Collection;

/**
 * Provides an abstract way for managing the database actions for dependable field values
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface DependableFieldsDataManager {

    /**
     * <p>
     * Gets the instance of {@link DependableFieldsDataManager}
     * </p>
     *
     * @return {@link DependableFieldsDataManager}
     */
    static DependableFieldsDataManager getInstance() {
        return DependableFieldsDataManagerImpl.getInstance();
    }

    /**
     * <p>
     * Gets the dependable field values of the provided dependable fields
     * </p>
     * @param orgId            Represents the organization id
     * @param entityId         Represents the entity id
     * @param source           Represents the source
     * @param dependableFields Represents the collection of dependable fields
     * @return {@link DependableFieldsData}
     */
    DependableFieldsData getDependableFieldsData(final Long orgId, final Long entityId, final Source source, final Collection<FieldInfo> dependableFields);
}
