package com.tlc.entity.converter;

import com.tlc.entity.annotation.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.lang.reflect.AnnotatedElement;

/**
 * Helper class for accessing annotations related to database mappings.
 */
@NoArgsConstructor( staticName = "create", access = AccessLevel.PRIVATE )
@Accessors( fluent = true )
public class AnnotationHelper
{
    @Getter
    private static final AnnotationHelper INSTANCE = AnnotationHelper.create();

    /**
     * Retrieves the {@link Table} annotation from the given  {@link AnnotatedElement}.
     *
     * @param element The annotated element
     * @return The {@link Table} annotation
     */
    public final Table getTable( final AnnotatedElement element )
    {
        return element.getAnnotation( getTableClass() );
    }

    /**
     * Retrieves the {@link Column} annotation from the given  {@link AnnotatedElement}.
     *
     * @param element The annotated element
     * @return The {@link Column} annotation
     */
    public final Column getColumn( final AnnotatedElement element )
    {
        return element.getAnnotation( getColumnClass() );
    }

    /**
     * Retrieves the Join annotation from the provided {@link AnnotatedElement}.
     *
     * @param element The annotated element from which to retrieve the Join annotation.
     * @return The Join annotation associated with the provided element, or null if not present.
     */
    public final Join getJoin( final AnnotatedElement element )
    {
        return element.getAnnotation( getJoinClass() );
    }

    /**
     * Retrieves the {@link JoinColumn} annotation from the given  {@link AnnotatedElement}.
     *
     * @param element The annotated element
     * @return The {@link JoinColumn} annotation
     */
    public final JoinColumn getJoinColumn( final AnnotatedElement element )
    {
        return element.getAnnotation( getJoinColumnClass() );
    }

    /**
     * Retrieves the {@link JoinTable} annotation from the given {@link AnnotatedElement}.
     *
     * @param element The annotated element
     * @return The {@link JoinTable} annotation
     */
    public final JoinTable getJoinTable( final AnnotatedElement element )
    {
        return element.getAnnotation( getJoinTableClass() );
    }

    /**
     * Retrieves the {@link Table} annotation class.
     *
     * @return The {@link Table} annotation class
     */
    public final Class<Table> getTableClass()
    {
        return Table.class;
    }

    /**
     * Retrieves the {@link JoinTable} annotation class.
     *
     * @return The {@link JoinTable} annotation class
     */
    public final Class<JoinTable> getJoinTableClass()
    {
        return JoinTable.class;
    }

    /**
     * Retrieves the Class object representing the Join annotation.
     *
     * @return The Class object representing the Join annotation.
     */
    public final Class<Join> getJoinClass()
    {
        return Join.class;
    }

    /**
     * Retrieves the {@link Column} annotation class.
     *
     * @return The {@link Column} annotation class
     */
    public final Class<Column> getColumnClass()
    {
        return Column.class;
    }

    /**
     * Retrieves the {@link JoinColumn} annotation class.
     *
     * @return The {@link JoinColumn} annotation class
     */
    public final Class<JoinColumn> getJoinColumnClass()
    {
        return JoinColumn.class;
    }
}
