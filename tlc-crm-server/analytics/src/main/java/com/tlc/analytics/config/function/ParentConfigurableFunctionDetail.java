package com.tlc.analytics.config.function;

import com.tlc.analytics.config.model.BaseModel;
import com.tlc.analytics.sql.resource.ANAPARENTCONFIGURABLEFUNCTIONDETAILS;

import com.tlc.entity.Entity;
import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * Represents the {@link ParentConfigurableFunctionDetail}
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table( name = ANAPARENTCONFIGURABLEFUNCTIONDETAILS.TABLE )
public class ParentConfigurableFunctionDetail extends BaseModel implements Entity<Long>
{

    @Column( name = ANAPARENTCONFIGURABLEFUNCTIONDETAILS.INDEX )
    @NotNull( groups = { Group.Create.class, Group.Update.class }, message = "i18n.analytics.invalid.function.index" )
    private Integer index;

    @Column( name = ANAPARENTCONFIGURABLEFUNCTIONDETAILS.PARENT_COLUMN_ID )
    @NotNull( groups = { Group.Create.class, Group.Update.class }, message = "i18n.analytics.invalid.parent.column.id" )
    private Long parentColumnId;

    @Column( name = ANAPARENTCONFIGURABLEFUNCTIONDETAILS.COLUMN_FUNCTION_ID )
    @NotNull( groups = { Group.Create.class, Group.Update.class }, message = "i18n.analytics.invalid.column.function.id" )
    private Long columnFunctionId;
}
