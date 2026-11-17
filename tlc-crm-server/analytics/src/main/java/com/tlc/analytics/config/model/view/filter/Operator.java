package com.tlc.analytics.config.model.view.filter;

import com.tlc.analytics.config.keys.view.filter.OperatorKeys;
import com.tlc.analytics.sql.resource.ANAOPERATOR;
import com.tlc.analytics.config.model.BaseModel;

import com.tlc.entity.annotation.JsonKey;
import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.experimental.SuperBuilder;



/**
 * <p>
 * A model that manages the operator details
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@Getter
@SuperBuilder
@Table( name = ANAOPERATOR.TABLE )
public class Operator extends BaseModel
{
    @JsonKey( key = OperatorKeys.OPERATOR_TYPE )
    @Column( name = ANAOPERATOR.OPERATOR_TYPE )
    @Size( max = 20 )
    @NotNull
    private String operatorType;

    @Column( name = ANAOPERATOR.IS_ENABLED )
    @NotNull
    private Boolean isEnabled;

    public OperatorType getOperatorType()
    {
        return OperatorType.getOperatorType( id().intValue() );
    }
}
