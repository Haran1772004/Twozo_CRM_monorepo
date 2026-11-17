package com.tlc.crm.core.api.analytics.manager;

import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.CRMRViewFieldMapper;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;

import com.tlc.crm.core.sql.resource.CRMREPORTVIEWFIELDMAPPER;
import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * Managing CRM reports with support for {@link CRMRViewFieldMapper} CRUD operations.
 *
 * @author jamesh
 * @version 1.0
 */
@Accessors( fluent = true )
@NoArgsConstructor( access = AccessLevel.PACKAGE )
class ViewFieldMapperManager extends CRMAbstractReportsManager<CRMRViewFieldMapper>
{
    private static class Instance
    {
        private static final ViewFieldMapperManager INSTANCE = new ViewFieldMapperManager();
    }

    /**
     * Returns singleton instance.
     *
     * @return {@link ViewFieldMapperManager} singleton
     */
    public static ViewFieldMapperManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * Returns supported model class.
     *
     * @return view-field mapper model class
     */
    @NonNull
    @Override
    protected Class<CRMRViewFieldMapper> getModelClass()
    {
        return CRMRViewFieldMapper.class;
    }

    /**
     * Returns associated table metadata.
     *
     * @return SQL table metadata
     */
    @Override
    public Table getTable()
    {
        return Table.get( CRMREPORTVIEWFIELDMAPPER.TABLE );
    }

}
