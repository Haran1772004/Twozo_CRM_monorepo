package com.tlc.analytics.config.model.table;

import com.tlc.analytics.config.keys.table.ColumnKeys;
import com.tlc.analytics.config.model.BaseModel;
import com.tlc.analytics.sql.resource.ANACOLUMNDETAILS;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.entity.annotation.JsonKey;
import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Relation;
import com.tlc.entity.annotation.Table;
import com.tlc.entity.annotation.Join;
import com.tlc.entity.annotation.JoinColumn;
import com.tlc.entity.annotation.JoinTable;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Objects;

/**
 * <p>
 * A model that manages the column details
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table(name = ANACOLUMNDETAILS.TABLE)
public class ColumnDetail extends BaseModel {
    @JsonKey(key = ColumnKeys.TABLE_ID)
    @Column(name = ANACOLUMNDETAILS.TABLE_ID)
    @NotNull(groups = {Group.Create.class}, message = "i18n.analytics.column.invalid.table.id")
    private Long tableId;

    @JsonKey(key = ColumnKeys.DATA_TYPE)
    @Getter(AccessLevel.NONE)
    @Column(name = ANACOLUMNDETAILS.DATA_TYPE_ID)
    @NotNull(groups = {Group.Create.class}, message = "i18n.analytics.column.invalid.data.type")
    private Integer dataType;

    @JsonKey(key = ColumnKeys.NAME)
    @Column(name = ANACOLUMNDETAILS.COLUMN_NAME)
    @NotBlank(groups = {Group.Create.class}, message = "i18n.analytics.column.invalid.column.name")
    @Size(max = 20)
    private String columnName;

    @JsonKey(key = ColumnKeys.TABLE_INFO)
    @Join(relation = Relation.MANY_TO_ONE,
            joinTables = @JoinTable(joinColumns = @JoinColumn(localColumn = ANACOLUMNDETAILS.TABLE_ID,
                    referenceColumn = ANACOLUMNDETAILS.ID),
                    entityClass = TableDetail.class))
    private TableDetail tableDetail;

    private boolean isDynamic;

    /**
     * Resolves the base data type using configured column data type.
     *
     * @return The resolved {@link AnalyticsDataType}.
     */
    public AnalyticsDataType getDataType() {
        return AnalyticsDataType.resolveFromAnalyticsDataType(dataType);
    }

    /**
     * Resolves the base data type using configured column data type and store type context.
     *
     * @param storeType The store type used for JSON type resolution.
     * @return The resolved {@link AnalyticsDataType}.
     */
    public AnalyticsDataType getDataType(final StoreType storeType) {
        return AnalyticsDataType.resolveFromAnalyticsDataType(dataType, storeType);
    }

    /**
     * {@inheritDoc}
     *
     * @param object the reference object with which to compare
     * @return true if this object is the same as the obj argument; false otherwise
     */
    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof final ColumnDetail columnDetail)) {
            return false;
        }

        return Objects.equals(tableId, columnDetail.tableId) && Objects.equals(columnName, columnDetail.columnName);
    }

    /**
     * {@inheritDoc}
     *
     * @return A hash code value for this object
     */
    @Override
    public int hashCode() {
        return Objects.hash(tableId, columnName);
    }
}
