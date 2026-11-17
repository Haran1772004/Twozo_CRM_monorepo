package com.tlc.crm.entity.manager;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.manager.impl.CrmFieldsManagerImpl;
import com.tlc.crm.entity.manager.impl.SecureCrmFieldsManager;
import com.tlc.crm.entity.models.internal.Field;
import com.tlc.crm.entity.models.internal.FieldConfig;

import com.tlc.crm.fields.manager.ReadableConfigManager;

import com.tlc.sql.common.api.Row;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Provides the {@link Field} related DB actions
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 */
public interface CrmFieldsManager extends ReadableConfigManager<Field> {

    /**
     * Provides the instance of the {@link CrmFieldsManager}.
     *
     * @return {@link CrmFieldsManager} instance.
     */
    static CrmFieldsManager getFieldsManager() {
        return CrmFieldsManagerImpl.getInstance();
    }

    /**
     * Provides the instance of the secure {@link CrmFieldsManager}.
     *
     * @return {@link CrmFieldsManager} instance.
     */
    static CrmFieldsManager getSecureFieldsManager() {
        return SecureCrmFieldsManager.getInstance();
    }

    /**
     * Gets the {@link FieldConfig} of the field
     *
     * @param orgId   Organization ID
     * @param fieldId Represents the Field ID
     * @return {@link FieldConfig} of the field
     */
    FieldConfig getFieldConfig(final Long orgId, final Long fieldId);

    /**
     * Converts the field config row to model
     *
     * @param fieldConfigRow which contains the field config data
     * @return {@link FieldConfig}
     */
    FieldConfig convertRowIntoFieldConfig(final Row fieldConfigRow);

    /**
     * <p>
     * Returns the entity of the specified keys in a map
     * </p>
     *
     * @param orgId Organisation ID
     * @param ids   Field ids
     * @return Field id is the key and Field object might be the value of response
     */
    Map<Long, Field> get(final Long orgId, final Collection<Long> ids);

    /**
     * <p>
     * Retrieves a collection fields for the given organization ID.
     * </p>
     *
     * @param orgId Represents an organization id
     * @return The collection of {@link Field}
     */
    Collection<Field> getFields(final Long orgId);

    /**
     * <p>
     * Saves new entity into database
     * </p>
     *
     * @param fields Collection of {@link Field} entities
     * @return Created entity and transaction failed entity information
     */
    Map<String, Object> createFields(final Collection<Field> fields);

    /**
     * <p>
     * Saves the {@link  Field} info to database
     * </p>
     *
     * @param field Holds the attributes of {@link Field}
     * @return If the field is saved into database without errors, it will return the field entity with the identifier
     * @throws ErrorCode If the field name already exists error will be thrown
     */
    Field createField(final Field field);

    /**
     * <p>
     * Updates the given crm field into database
     * </p>
     *
     * @param field An entity that holds the attributes of {@link Field}
     */
    void update(final Field field);

    /**
     * <p>
     * Updates the given crm entity into database
     * </p>
     *
     * @param orgId  Organization id
     * @param fields Collection of CRM entity
     */
    void update(final Long orgId, final Collection<Field> fields);

    /**
     * <p>
     * Disables the field that has the given field id
     * </p>
     *
     * @param orgId   Organization id
     * @param fieldId Field id
     */
    void disableField(final Long orgId, final Long fieldId);

    /**
     * <p>
     * Updates the order of fields for the specified organization
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param fieldIds Represents the collection of field ids
     * @param start    Represents the start of count based on the immovable fields order
     */
    void updateFieldsOrder(final Long orgId, final Set<Long> fieldIds, final int start);

    /**
     * <p>
     * Retrieves a collection of field configurations for the given organization ID.
     * </p>
     *
     * @param orgId Represents an organization id
     * @return The collection of {@link FieldConfig}
     */
    Collection<FieldConfig> getFieldConfigs(final Long orgId);

    /**
     * <p>
     * Retrieves the collection of fields for the given source id.
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param sourceId Represents the source id.
     * @return the collection of {@link Field}.
     */
    Collection<Field> getFieldsBySource(final Long orgId, final Long sourceId);

    /**
     * <p>
     * Retrieves the collection of fields for the given source ids.
     * </p>
     *
     * @param orgId     Represents the organization id.
     * @param sourceIds Represents the source ids.
     * @return the collection of {@link Field}.
     */
    Collection<Field> getFieldsBySource(final Long orgId, final Collection<Long> sourceIds);

    /**
     * <p>
     * Retrieves a collection default fields.
     * </p>
     *
     * @return The collection of {@link Field}
     */
    Collection<Field> getDefaultFields();
}
