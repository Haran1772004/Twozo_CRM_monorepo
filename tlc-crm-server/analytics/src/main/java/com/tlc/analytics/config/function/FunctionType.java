package com.tlc.analytics.config.function;

import com.tlc.analytics.config.model.BaseModel;
import com.tlc.analytics.sql.resource.ANAFUNCTIONARGUMENTCONFIG;
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

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Collection;

/**
 * <p>
 * Represents the {@link FunctionType} details
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table( name = ANAFUNCTIONTYPE.TABLE )
public class FunctionType extends BaseModel implements Entity<Long>
{

    @Column( name = ANAFUNCTIONTYPE.FUNCTION_ID )
    @NotNull( groups = { Group.Create.class, Group.Update.class }, message = "i18n.analytics.invalid.function.id" )
    private Long functionId;

    @Column( name = ANAFUNCTIONTYPE.RETURN_TYPE )
    @NotNull( groups = { Group.Create.class, Group.Update.class }, message = "i18n.analytics.invalid.function.return.type" )
    private String returnType;

    @Column( name = ANAFUNCTIONTYPE.ARGUMENT_COUNT )
    @NotNull( groups = { Group.Create.class, Group.Update.class }, message = "i18n.analytics.invalid.function.argument.count" )
    private Integer argumentCount;

    @Join( relation   = Relation.ONE_TO_MANY,
           nullable   = true,
           joinTables = @JoinTable( joinColumns = @JoinColumn( referenceColumn = ANAFUNCTIONARGUMENTCONFIG.FUNCTION_TYPE_ID ),
                                    entityClass = FunctionArgumentConfig.class ) )
    private Collection<FunctionArgumentConfig> functionArgumentDetails;
}
