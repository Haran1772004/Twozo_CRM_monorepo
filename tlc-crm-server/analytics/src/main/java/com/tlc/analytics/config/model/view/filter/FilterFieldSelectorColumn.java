package com.tlc.analytics.config.model.view.filter;

import com.tlc.analytics.config.model.BaseModel;
import com.tlc.analytics.sql.resource.ANAFILTERFIELDSELECTORCOLUMN;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the report field details
 * </p>
 *
 * @author saravanan
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table(name = ANAFILTERFIELDSELECTORCOLUMN.TABLE)
public class FilterFieldSelectorColumn extends BaseModel {

    @Column(name = ANAFILTERFIELDSELECTORCOLUMN.COLUMN_ID)
    private Long columnId;

    @Column(name = ANAFILTERFIELDSELECTORCOLUMN.FILTER_FIELD_ID)
    private Long filterFieldId;

    @Column(name = ANAFILTERFIELDSELECTORCOLUMN.ORDER)
    private Long order;

}