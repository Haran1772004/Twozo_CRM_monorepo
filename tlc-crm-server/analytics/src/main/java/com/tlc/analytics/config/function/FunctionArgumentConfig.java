package com.tlc.analytics.config.function;

import com.tlc.analytics.config.model.BaseModel;
import com.tlc.analytics.sql.resource.ANAFUNCTIONARGUMENTCONFIG;

import com.tlc.entity.Entity;
import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * Represents the {@link FunctionArgumentConfig}
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table( name = ANAFUNCTIONARGUMENTCONFIG.TABLE )
public class FunctionArgumentConfig extends BaseModel implements Entity<Long>
{

    @Column( name = ANAFUNCTIONARGUMENTCONFIG.FUNCTION_TYPE_ID )
    @NotNull( groups = { Group.Create.class, Group.Update.class }, message = "i18n.analytics.function.type.id.invalid" )
    private Long functionTypeId;

    @Column( name = ANAFUNCTIONARGUMENTCONFIG.COLUMN_FUNCTION_APPLIED )
    @NotNull( groups = { Group.Create.class, Group.Update.class }, message = "i18n.analytics.invalid.column.function.applied" )
    private Boolean columnFunctionApplied;

    @Column( name = ANAFUNCTIONARGUMENTCONFIG.VALUE_FUNCTION_APPLIED )
    @NotNull( groups = { Group.Create.class, Group.Update.class }, message = "i18n.analytics.invalid.value.function.applied" )
    private Boolean valueFunctionApplied;

    @Column( name = ANAFUNCTIONARGUMENTCONFIG.DATA_TYPE )
    @NotNull( groups = { Group.Create.class, Group.Update.class }, message = "i18n.analytics.invalid.function.data.type" )
    private String dataType;

    @Column( name = ANAFUNCTIONARGUMENTCONFIG.INDEX )
    @NotNull( groups = { Group.Create.class, Group.Update.class }, message = "i18n.analytics.invalid.function.index" )
    private Integer index;
}
