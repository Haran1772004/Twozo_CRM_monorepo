package com.tlc.analytics.config.model.view.report;

import com.tlc.analytics.config.model.BaseModel;
import com.tlc.analytics.sql.resource.ANACUSTOMRANGE;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.JsonKey;
import com.tlc.entity.annotation.Table;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model to manage the custom range of a report
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table( name = ANACUSTOMRANGE.TABLE )
public class ReportFieldCustomRange extends BaseModel
{
    @Column( name = ANACUSTOMRANGE.REPORT_COLUMN_ID )
    private Long reportColumnId;

    @JsonKey( key = ANACUSTOMRANGE.FROM )
    @Column( name = ANACUSTOMRANGE.FROM )
    private Long from;

    @JsonKey( key = ANACUSTOMRANGE.TO )
    @Column( name = ANACUSTOMRANGE.TO )
    private Long to;
}
