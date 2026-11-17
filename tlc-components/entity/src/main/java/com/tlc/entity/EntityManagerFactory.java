package com.tlc.entity;

import com.tlc.entity.converter.ConverterUtil;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.OrgDataStore;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.function.Supplier;

/**
 * A factory class for creating instances of {@link EntityManager}.
 * This class provides a static method to create an instance of {@link EntityManager}.
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class EntityManagerFactory
{
    /**
     * Creates an instance of {@link EntityManager} for the specified entity class and data store supplier.
     *
     * @param <ID>               The type of the identifier (primary key) for the entities managed by the entity manager.
     * @param <E>                The type of the entities managed by the entity manager.
     *                           These entities are assumed to implement the {@link Entity} interface with identifiers of type {@code ID}.
     * @param eClass             The class of the entity managed by the entity manager.
     * @param orgDataStoreGetter A supplier function to obtain the {@link OrgDataStore} instance.
     * @return An instance of {@link EntityManager} for the specified entity class and data store supplier.
     */
    public static <ID, E extends Entity<ID>> EntityManager<ID, E> create( @NonNull final Class<E> eClass,
                                                                          @NonNull final Supplier<OrgDataStore> orgDataStoreGetter )
    {
        final Table table = ConverterUtil.getTable( eClass );

        return new EntityManagerImpl<>()
        {
            @Override
            protected @NonNull Class<E> getEntityClass()
            {
                return eClass;
            }

            @Override
            protected @NonNull Table getTable()
            {
                return table;
            }

            @Override
            protected @NonNull OrgDataStore getOrgDataStore()
            {
                return orgDataStoreGetter.get();
            }
        };
    }
}
