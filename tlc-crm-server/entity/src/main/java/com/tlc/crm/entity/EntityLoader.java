package com.tlc.crm.entity;

import com.tlc.crm.entity.internal.EntityLoaderImpl;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * An interface which provides abstract way to deal with {@link Entity} objects
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 */
public interface EntityLoader {

    /**
     * <p>
     * Fetches the instance of the {@link EntityLoader}
     * </p>
     *
     * @return {@link EntityLoader}
     */
    static EntityLoader getInstance() {
        return EntityLoaderImpl.getInstance();
    }

    /**
     * <p>
     * Loads the given entity information into the target container
     * </p>
     *
     * @param isCreateAction  A flag which indicates whether the action is related to create or update event
     * @param model           CRM Entity
     * @param table           Entity table
     * @param sourceContainer A source container which contains the rows of existing records
     * @param targetContainer A target container to which all the new/updated rows will be added
     * @return entityRow Entity row
     */
    void buildEntity(final boolean isCreateAction, final Entity model, final Table table,
                    final DataContainer sourceContainer, final DataContainer targetContainer);

    /**
     * <p>
     * Loads the given entity information into the target container
     * </p>
     *
     * @param isCreateAction  A flag which indicates whether the action is related to create or update event
     * @param model           CRM Entity
     * @param fields          Fields that are mentioned here only loaded into the target container.
     * @param table           Entity table
     * @param sourceContainer A source container which contains the rows of existing records
     * @param targetContainer A target container to which all the new/updated rows will be added
     */
    void buildEntity(final boolean isCreateAction, final Entity model, final Collection<FieldInfo> fields, final Table table,
                    final DataContainer sourceContainer, final DataContainer targetContainer);

    /**
     * <p>
     * Fetches the rows of the given table that matches given criteria and loads the matched rows into the container
     * </p>
     *
     * @param orgId        Organization id
     * @param table        Represents a table that holds sub entity rows
     * @param entitySource FieldNameHolder of the given source will also be fetched along with other rows
     * @param whereClause  Represents the criteria to fetch the rows from the given table
     * @return A container that holds the row information of multiple entities
     */
    DataContainer getEntityContainer(final Long orgId, final Table table, final String entitySource,
                                     final WhereClause whereClause);

    /**
     * <p>
     * Fetches the rows of the given table that matches given criteria and loads the matched rows into the container
     * </p>
     *
     * @param fields      Collection of entity that has to be fetched from the custom field data warehouse
     * @param orgId       Organization id
     * @param table       Represents a table that holds sub entity rows
     * @param whereClause Represents the criteria to fetch the rows from the given table
     * @return A container that holds the row information of multiple entities
     */
    DataContainer getEntityContainer(final Collection<FieldInfo> fields, final Long orgId, final Table table,
                                     final WhereClause whereClause);

    /**
     * <p>
     * Fetches the rows of the given table that matches given criteria and loads the matched rows into the container
     * </p>
     *
     * @param fieldsByType Collection of entity that has to be fetched from the custom field data warehouse
     * @param orgId        Organization id
     * @param table        Represents a table that holds sub entity rows
     * @param whereClause  Represents the criteria to fetch the rows from the given table
     * @return A container that holds the row information of multiple entities
     */
    DataContainer getEntityContainer(final Map<Integer, Collection<FieldInfo>> fieldsByType, final Long orgId,
                                     final Table table, final WhereClause whereClause);

    /**
     * <p>
     * Fetches the rows of the given table that matches one of the given ids and loads the matched rows into the container
     * </p>
     *
     * @param orgId  Organization id
     * @param table  Represents a table that holds sub entity rows
     * @param fields Collection of entity that has to be fetched from the custom field data warehouse
     * @param ids    Collection of CRM sub entities id
     * @return A container that holds the row information of multiple entities
     */
    DataContainer getEntityContainerByIds(final Long orgId, final Table table,
                                          final Collection<FieldInfo> fields,
                                          final Collection<Long> ids);

    /**
     * <p>
     * Fetches the base entity value from the data container and sets those value to the given entity
     * </p>
     *
     * @param entity        An entity to which the base entity values have to be added
     * @param table         Represents the entity table
     * @param dataContainer A container that holds the row information of base entity and other entities
     */
    void setEntityValues(final Entity entity, final Table table, final DataContainer dataContainer);

    /**
     * <p>
     * Gets the data container which contains field values for the provided fields
     * </p>
     *
     * @param orgId       Represents the organization id
     * @param fields      Represents the collection of fields
     * @param table       Represents the entity table
     * @param whereClause Represents the where clause
     * @return {@link DataContainer}
     */
    DataContainer getFieldsDataContainer(final Long orgId, final Collection<FieldInfo> fields, final Table table,
                                         final WhereClause whereClause);
}
