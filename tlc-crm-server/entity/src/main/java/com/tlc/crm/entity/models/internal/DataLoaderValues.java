package com.tlc.crm.entity.models.internal;

import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;


/**
 * <p>
 * A record which stores the data loader related values
 * </p>
 *
 * @author Ajith venakdesh
 * @version 1.0
 */
public record DataLoaderValues<T extends Entity>(boolean isCreateEvent, Table table, T entityModel, Collection<FieldInfo> fields,
                               DataContainer sourceContainer, DataContainer targetContainer) {
}
