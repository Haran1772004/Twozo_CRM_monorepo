package com.tlc.entity.converter;

import com.tlc.entity.Entity;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.SelectQuery;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Factory class for retrieving converters to convert data between different types.
 * It provides static methods to retrieve converters for specific conversion tasks.
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class ConverterFactory
{
    /**
     * Retrieves a converter for converting data from {@link Entity} to {@link DataContainer}.
     *
     * @return A converter instance for {@link DataContainer} conversion.
     */
    public static Converter<DataContainer> getEntityToDataContainerConverter()
    {
        return EntityToDCConvertor.INSTANCE();
    }

    /**
     * Retrieves a converter for converting data from {@link DataContainer} to {@link Entity}.
     *
     * @param pkId The primary key ID associated with the entity.
     * @return A converter instance for {@link DataContainer} to {@link Entity} conversion.
     */
    public static <T> Converter<DataContainer> getDataContainerToEntityConverter( @NonNull final T pkId )
    {
        return DCToEntityConverter.getInstance( pkId );
    }

    /**
     * Retrieves a converter for converting data from {@link Entity} to {@link SelectQuery}.
     *
     * @return A converter instance for {@link Entity} to {@link SelectQuery} conversion.
     */
    public static Converter<SelectQuery> getEntityToSelQueryConverter()
    {
        return SelQueryConverter.INSTANCE();
    }

    /**
     * Retrieves an instance of the {@link ReferenceFieldRemover} for removing reference fields.
     *
     * @return An instance of {@link ReferenceFieldRemover} for use in removing reference fields recursively.
     */
    public static ReferenceFieldRemover getReferenceFieldRemover()
    {
        return ReferenceFieldRemoverImpl.INSTANCE();
    }

    /**
     * Returns a singleton instance of the {@link EntityCloner} for cloning entities.
     *
     * @param <ID> The type of the entity's identifier.
     * @param <E>  The type of the entity to be cloned.
     * @return A singleton instance of {@link EntityCloner}.
     */
    @SuppressWarnings("unchecked")
    public static <ID, E extends Entity<ID>> EntityCloner<ID, E> getEntityCloner()
    {
        return ( EntityCloner<ID, E> ) EntityClonerImpl.INSTANCE();
    }
}
