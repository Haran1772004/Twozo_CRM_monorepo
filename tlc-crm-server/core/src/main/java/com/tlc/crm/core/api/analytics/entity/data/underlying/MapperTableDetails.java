package com.tlc.crm.core.api.analytics.entity.data.underlying;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.MCANAMAPPERTABLES;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the field mapper table details
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table( name = MCANAMAPPERTABLES.TABLE )
public class MapperTableDetails extends CRMBaseModel
{
    @NotNull( message = "i18n.crm.analytics.invalid.modular.id" )
    @Column( name = MCANAMAPPERTABLES.MODULE_ID )
    private Integer moduleId;

    @NotNull( message = "i18n.crm.analytics.invalid.mapper.table" )
    @Column( name = MCANAMAPPERTABLES.NAME )
    private String tableName;
}
