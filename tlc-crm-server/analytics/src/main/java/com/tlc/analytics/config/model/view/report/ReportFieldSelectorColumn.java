package com.tlc.analytics.config.model.view.report;

import com.tlc.analytics.config.model.BaseModel;
import com.tlc.analytics.sql.resource.ANAREPORTFIELDSELECTORCOLUMN;

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
@Table(name = ANAREPORTFIELDSELECTORCOLUMN.TABLE)
public class ReportFieldSelectorColumn extends BaseModel {

    @Column(name = ANAREPORTFIELDSELECTORCOLUMN.COLUMN_ID)
    private Long columnId;

    @Column(name = ANAREPORTFIELDSELECTORCOLUMN.REPORT_FIELD_ID)
    private Long reportFieldId;

    @Column(name = ANAREPORTFIELDSELECTORCOLUMN.ORDER)
    private Long order;

}