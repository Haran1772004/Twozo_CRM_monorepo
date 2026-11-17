package com.tlc.analytics.config.model.table;

import com.tlc.analytics.config.keys.table.TableKeys;
import com.tlc.analytics.config.model.BaseModel;
import com.tlc.analytics.sql.resource.ANATABLEDETAILS;
import com.tlc.analytics.sql.resource.ANATABLERELATION;
import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Join;
import com.tlc.entity.annotation.JoinColumn;
import com.tlc.entity.annotation.JoinTable;
import com.tlc.entity.annotation.JsonKey;
import com.tlc.entity.annotation.Relation;
import com.tlc.entity.annotation.Table;

import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * A model that manages the table details
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table( name = ANATABLEDETAILS.TABLE )
public class TableDetail extends BaseModel
{
    @JsonKey( key = TableKeys.NAME )
    @Column( name = ANATABLEDETAILS.TABLE_NAME )
    @NotBlank( groups = { Group.Create.class, Group.Update.class }, message = "i18n.analytics.table.invalid.table.name" )
    @Size( max = 20 )
    private String tableName;

    @Column( name = ANATABLEDETAILS.ALIAS_NAME )
    private String aliasName;

    @JsonKey( key = TableKeys.LEFT_TO_RIGHT_RELATION )
    @Join( relation = Relation.ONE_TO_MANY,
           nullable = true,
           joinTables = @JoinTable( aliasName = "leftToRightRelations",
                                    joinColumns = @JoinColumn( referenceColumn = ANATABLERELATION.PARENT_TABLE_ID ),
                                    entityClass = TableRelationMapping.class ) )
    private Collection<TableRelationMapping> leftToRightRelations;

    @JsonKey( key = TableKeys.RIGHT_TO_LEFT_RELATION )
    @Join( relation = Relation.ONE_TO_MANY,
           nullable = true,
           joinTables = @JoinTable( aliasName = "rightToLeftRelations",
                                    joinColumns = @JoinColumn( referenceColumn = ANATABLERELATION.CHILD_TABLE_ID ),
                                    entityClass = TableRelationMapping.class ) )
    private Collection<TableRelationMapping> rightToLeftRelations;

    /**
     * {@inheritDoc}
     *
     * @param object the reference object with which to compare
     * @return true if this object is the same as the obj argument; false otherwise
     */
    @Override
    public boolean equals( final Object object )
    {
        if ( this == object )
        {
            return true;
        }

        if ( !( object instanceof final TableDetail tableDetail) )
        {
            return false;
        }

        return Objects.equals( tableName, tableDetail.tableName );
    }

    /**
     * {@inheritDoc}
     *
     * @return A hash code value for this object
     */
    @Override
    public int hashCode()
    {
        return Objects.hash( tableName );
    }
}
