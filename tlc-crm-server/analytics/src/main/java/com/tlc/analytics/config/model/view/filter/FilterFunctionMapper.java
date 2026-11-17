package com.tlc.analytics.config.model.view.filter;

import com.tlc.analytics.config.model.BaseModel;
import com.tlc.analytics.sql.resource.ANAFILTERFUNCTIONMAPPER;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the filter field details
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table( name = ANAFILTERFUNCTIONMAPPER.TABLE )
public class FilterFunctionMapper extends BaseModel
{
    @Column( name = ANAFILTERFUNCTIONMAPPER.FILTER_FIELD_ID )
    private Long filterFieldId;

    @Column( name = ANAFILTERFUNCTIONMAPPER.FUNCTION_NAME )
    private String functionName;
}
