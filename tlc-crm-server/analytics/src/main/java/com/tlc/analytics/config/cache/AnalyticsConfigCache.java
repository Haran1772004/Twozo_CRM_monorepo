package com.tlc.analytics.config.cache;

import com.tlc.analytics.config.entity.cache.CacheAdapter;
import com.tlc.analytics.config.entity.cache.CachedValue;
import com.tlc.analytics.config.entity.cache.ConfigCacheManager;
import com.tlc.analytics.config.function.ConfigurableFunctionDetail;
import com.tlc.analytics.config.manager.AnalyticsConfigManager;
import com.tlc.analytics.config.manager.AnalyticsConfigManagerImpl;
import com.tlc.analytics.config.model.BaseModel;
import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.config.model.table.TableDetail;
import com.tlc.analytics.config.model.table.TableRelation;
import com.tlc.analytics.config.model.view.report.ChartView;
import com.tlc.analytics.config.model.view.report.KPIView;
import com.tlc.analytics.sql.resource.ANAVIEWDETAILS;

import com.tlc.commons.functions.TriFunction;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.provider.SqlServiceProvider;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * <p>
 * The {@link AnalyticsConfigCache} class provides a centralized caching mechanism for various configurations related to
 * Analytics Configurations, such as table details, column details, table relations, and chart views.
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@Getter
@Accessors( fluent = true )
public class AnalyticsConfigCache
{
    private static class Instance
    {
        private static final AnalyticsConfigCache INSTANCE = new AnalyticsConfigCache();
    }

    public static AnalyticsConfigCache INSTANCE()
    {
        return Instance.INSTANCE;
    }

    private final ConfigCacheManager<TableDetail> tableManager;
    private final ConfigCacheManager<ColumnDetail> columnManager;
    private final ConfigCacheManager<TableRelation> tableRelationManger;
    private final ConfigCacheManager<ChartView> chartViewManager;
    private final ConfigCacheManager<KPIView> kpiViewManager;
    private final ConfigCacheManager<ConfigurableFunctionDetail> configurableFunctionDetailsManager;

    private AnalyticsConfigCache()
    {
        final AnalyticsConfigManagerImpl configManager = AnalyticsConfigManagerImpl.INSTANCE();
        tableManager = createConfigCacheManager( configManager.getTableManager() );
        columnManager = createConfigCacheManager( configManager.getColumnManager() );
        tableRelationManger = createConfigCacheManager( configManager.getLookupManager() );
        chartViewManager = createConfigCacheManager( configManager.getChartViewManager(), getModificationTimePredicateForView() );
        kpiViewManager = createConfigCacheManager( configManager.getKPIManager(), getModificationTimePredicateForView() );
        configurableFunctionDetailsManager = createConfigCacheManager( configManager.getConfigurableManager() );
    }

    /**
     * Creates a predicate that checks whether the cached value for a view has been modified in the database
     * based on the modification time.
     *
     * @param <M> The type of the model being cached, which extends BaseModel.
     * @return A TriFunction that returns true if the cached value is older than the modified time in the database, indicating that it should be updated.
     */
    private static <M extends BaseModel> TriFunction<Long, Long, CachedValue<M>, Boolean> getModificationTimePredicateForView()
    {
        return ( orgId, id, cachedValue ) ->
        {
            final Table table = Table.get( ANAVIEWDETAILS.TABLE );
            final SelectQuery selectQuery = SelectQuery.get( table );
            final Column modTimeCol = table.getColumn( ANAVIEWDETAILS.MODIFIED_TIME );
            final WhereClause whereClause = new WhereClause( Criteria.eq( table.getPKColumn(), id ) );

            selectQuery.addSelectClause( modTimeCol );
            selectQuery.setWhereClause( whereClause );
            final Row row = SqlServiceProvider.get()
                                              .getOrgDataStore( orgId )
                                              .get( selectQuery )
                                              .getRow( table );

            return !Objects.isNull( row ) && cachedValue.getCacheCreatedTime() < ( long ) row.get( modTimeCol );
        };
    }

    /**
     * Creates a configuration cache manager for managing cached models of type M without a custom cache modification predicate.
     * By default, the cache is considered always valid unless explicitly defined otherwise.
     *
     * @param <M>          The type of the model being cached, which extends BaseModel.
     * @param configManager The configuration manager that handles fetching the data.
     * @return A new instance of ConfigCacheManager that manages cached values for the provided model type.
     */
    private static <M extends BaseModel> ConfigCacheManager<M> createConfigCacheManager( final @NonNull AnalyticsConfigManager<M> configManager )
    {
        return createConfigCacheManager( configManager, ( orgId, id, cachedValue ) -> true );
    }

    /**
     * Creates a configuration cache manager for managing cached models of type M with a custom predicate to check
     * if the cache has been modified. The predicate is used to determine if the cache is stale and should be refreshed.
     *
     * @param <M>             The type of the model being cached, which extends BaseModel.
     * @param configManager    The configuration manager that handles fetching the data.
     * @param isCacheModified  A custom TriFunction predicate that determines whether the cached value is modified and needs to be updated.
     * @return A new instance of ConfigCacheManager that manages cached values for the provided model type, using the provided modification predicate.
     */
    private static <M extends BaseModel> ConfigCacheManager<M> createConfigCacheManager( final @NonNull AnalyticsConfigManager<M> configManager,
                                                                                         final @NonNull TriFunction<Long, Long, CachedValue<M>, Boolean> isCacheModified )
    {
        final CacheAdapter<Long, CachedValue<M>> cacheAdapter = CacheAdapterImpl.create();

        return ConfigCacheManagerImpl.create( cacheAdapter, configManager, isCacheModified );
    }
}
