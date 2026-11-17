package com.tlc.crm.core.api.analytics.integ.managers;

import com.tlc.crm.common.config.manager.ConfigManager;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;

import com.tlc.sql.common.api.ds.OrgDataStore;
import lombok.NonNull;

import java.util.Collection;

/**
 * <p>
 * Manages CRM Manager API
 * </p>
 *
 * @author James
 * @version 1.0
 */

public interface CRMViewConfigManager<M extends CRMBaseModel> extends ConfigManager<M>
{
    Table getTable();

    Collection<M> getAll( final @NonNull Long orgId, final @NonNull Criteria criteria ) throws Exception;

    Collection<M> getAll( final @NonNull Long orgId, final @NonNull Collection<Criteria> criteriaList ) throws Exception;

    void loadDataIntoContainer( final @NonNull Collection<M> entities, final DataContainer dataContainer ) throws Exception;

    void loadUpdateDataIntoContainer ( final @NonNull Collection<M> entities, final DataContainer dataContainer ) throws Exception;

    OrgDataStore getOrgDataStore ( final @NonNull Long orgId ) throws Exception;
}
