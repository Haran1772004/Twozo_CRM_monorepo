package com.tlc.analytics.config.function;

import com.tlc.analytics.config.model.BaseModel;
import com.tlc.analytics.sql.resource.ANAFUNCTIONVALUECOLUMNMAPPING;

import com.tlc.entity.Entity;
import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * Represents the {@link FunctionValueColumnMapping}
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table( name = ANAFUNCTIONVALUECOLUMNMAPPING.TABLE )
public class FunctionValueColumnMapping extends BaseModel implements Entity<Long>
{

    @Column( name = ANAFUNCTIONVALUECOLUMNMAPPING.VALUE )
    @NotNull( groups = { Group.Create.class, Group.Update.class }, message = "i18n.analytics.invalid.function.value" )
    private String value;

    @Column( name = ANAFUNCTIONVALUECOLUMNMAPPING.FUNCTION_TYPE_ID )
    @NotNull( groups = { Group.Create.class, Group.Update.class }, message = "i18n.analytics.function.type.id.invalid" )
    private Long functionTypeId;

    @Column( name = ANAFUNCTIONVALUECOLUMNMAPPING.COLUMN_FUNCTION_ID )
    @NotNull( groups = { Group.Create.class, Group.Update.class }, message = "i18n.analytics.invalid.column.function.id" )
    private Long columnFunctionId;

    @Column( name = ANAFUNCTIONVALUECOLUMNMAPPING.INDEX )
    @NotNull( groups = { Group.Create.class, Group.Update.class }, message = "i18n.analytics.invalid.function.index" )
    private Integer index;
}
