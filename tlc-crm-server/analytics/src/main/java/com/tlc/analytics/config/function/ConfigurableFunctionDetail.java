package com.tlc.analytics.config.function;

import com.tlc.analytics.config.model.BaseModel;
import com.tlc.analytics.sql.resource.ANACONFIGURABLEFUNCTIONDETAILS;
import com.tlc.analytics.sql.resource.ANAPARENTCONFIGURABLEFUNCTIONDETAILS;

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
 * Represents the {@link ConfigurableFunctionDetail}
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table( name = ANACONFIGURABLEFUNCTIONDETAILS.TABLE )
public class ConfigurableFunctionDetail extends BaseModel implements Entity<Long>
{

    @Column( name = ANACONFIGURABLEFUNCTIONDETAILS.FINAL_FUNCTION )
    @NotNull( groups = { Group.Create.class, Group.Update.class }, message = "i18n.analytics.final.function.invalid" )
    @Size( max = 30, groups = { Group.Create.class, Group.Update.class }, message = "i18n.analytics.function.max.size.exceed" )
    private String finalFunction;

    @Column( name = ANACONFIGURABLEFUNCTIONDETAILS.COLUMN_ID )
    @NotNull( groups = { Group.Create.class, Group.Update.class }, message = "i18n.analytics.function.column.id.invalid" )
    private Long columnId;

    @Column( name = ANACONFIGURABLEFUNCTIONDETAILS.FUNCTION_TYPE_ID )
    @NotNull( groups = { Group.Create.class, Group.Update.class }, message = "i18n.analytics.function.type.id.invalid" )
    private Long functionTypeId;

    @Join( relation   = Relation.ONE_TO_MANY,
           nullable   = true,
           joinTables = @JoinTable( joinColumns = @JoinColumn( referenceColumn = ANAPARENTCONFIGURABLEFUNCTIONDETAILS.COLUMN_FUNCTION_ID ),
                                    entityClass = ParentConfigurableFunctionDetail.class ) )
    @NotNull
    private Collection<ParentConfigurableFunctionDetail> parentColumnFunctionDetails;
}
