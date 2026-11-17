package com.tlc.crm.core.api.analytics.entity.config.modules;

import com.tlc.analytics.config.model.BaseModel;
import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;
import com.tlc.crm.core.sql.resource.ANAMODULETABLEMAPPER;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * ClickHouse-optimized representation of module table mapper.
 * Maps modules to their associated ClickHouse tables.
 *
 * @author Migration Team
 * @version 1.0
 */
@Getter
@SuperBuilder
@Table(name = ANAMODULETABLEMAPPER.TABLE)
public class ModuleTableMapper extends BaseModel {

    @Column(name = ANAMODULETABLEMAPPER.MODULE_ID)
    private Integer moduleId;

    @Column(name = ANAMODULETABLEMAPPER.TABLE_ID)
    private Long tableId;
}
