package com.tlc.analytics.config.function;

import com.tlc.analytics.config.model.BaseModel;
import com.tlc.analytics.sql.resource.ANAFUNCTION;
import com.tlc.analytics.sql.resource.ANAFUNCTIONTYPE;

import com.tlc.entity.Entity;
import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Join;
import com.tlc.entity.annotation.JoinColumn;
import com.tlc.entity.annotation.JoinTable;
import com.tlc.entity.annotation.Relation;
import com.tlc.entity.annotation.Table;

import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Collection;

/**
 * <p>
 * Represents the {@link Function} details
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table( name = ANAFUNCTION.TABLE )
public class Function extends BaseModel implements Entity<Long>
{

    @Column( name = ANAFUNCTION.NAME )
    @NotNull( groups = { Group.Create.class, Group.Update.class }, message = "i18n.analytics.function.name.invalid" )
    @Size( max = 30, groups = { Group.Create.class, Group.Update.class }, message = "i18n.analytics.function.max.size.exceed" )
    private String name;

    @Join( relation   = Relation.ONE_TO_MANY,
           joinTables = @JoinTable( joinColumns = @JoinColumn( referenceColumn = ANAFUNCTIONTYPE.FUNCTION_ID ),
                                    entityClass = FunctionType.class ) )
    private Collection<FunctionType> functionTypes;
}
