package com.tlc.crm.core.api.analytics.manager.data.underlying;

import com.tlc.crm.core.api.analytics.entity.data.underlying.MapperTableDetails;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;

import com.tlc.crm.core.sql.resource.MCANAMAPPERTABLES;
import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * <p>
 * Provides an abstract way to manage the field mapper table details for underlying data
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 * @see CRMAbstractReportsManager
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class MapperTableManager extends CRMAbstractReportsManager<MapperTableDetails>
{
    private static class Instance
    {
        private static final MapperTableManager INSTANCE = new MapperTableManager();
    }

    public static MapperTableManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    @Override
    public Table getTable()
    {
        return Table.get( MCANAMAPPERTABLES.TABLE );
    }

    @NonNull
    @Override
    protected Class<MapperTableDetails> getModelClass()
    {
        return MapperTableDetails.class;
    }
}
