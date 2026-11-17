package com.tlc.crm.core.api.analytics.manager;

import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.CRMRMapperColumn;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;

import com.tlc.crm.core.sql.resource.CRMREPORTFIELDMAPPERCOLUMN;
import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * Managing CRM reports with support for {@link CRMRMapperColumn} CRUD operations.
 *
 * @author jamesh
 * @version 1.0
 */
@Accessors( fluent = true )
@NoArgsConstructor( access = AccessLevel.PACKAGE )
class MapperColumnManager extends CRMAbstractReportsManager<CRMRMapperColumn>
{
    private static class Instance
    {
        private static final MapperColumnManager INSTANCE = new MapperColumnManager();
    }

    /**
     * Returns singleton instance.
     *
     * @return {@link MapperColumnManager} singleton
     */
    public static MapperColumnManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * Returns supported model class.
     *
     * @return mapper-column model class
     */
    @NonNull
    @Override
    protected Class<CRMRMapperColumn> getModelClass()
    {
        return CRMRMapperColumn.class;
    }

    /**
     * Returns associated table metadata.
     *
     * @return SQL table metadata
     */
    @Override
    public Table getTable()
    {
        return Table.get( CRMREPORTFIELDMAPPERCOLUMN.TABLE );
    }

}
