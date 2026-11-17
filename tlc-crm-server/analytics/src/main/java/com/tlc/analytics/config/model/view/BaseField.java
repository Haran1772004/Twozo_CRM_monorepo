package com.tlc.analytics.config.model.view;

import com.tlc.analytics.config.keys.view.BaseFieldKeys;
import com.tlc.analytics.config.model.BaseModel;
import com.tlc.analytics.config.model.view.report.CaseWhenConfig;
import com.tlc.analytics.config.model.view.report.AnalyticsCustomFieldConfig;
import com.tlc.analytics.sql.resource.ANAREPORTFIELD;

import com.tlc.commons.json.Json;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.JsonKey;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * A model that manages the base field details
 * </p>
 *
 * @author Aswath
 * @version 1.0
 * @see BaseModel
 */
@SuperBuilder
@Getter
public class BaseField extends BaseModel
{
    @JsonKey( key = BaseFieldKeys.VIEW_ID )
    @Column( name = ANAREPORTFIELD.VIEW_ID )
    @NotNull( groups = { Group.Create.class },
              message = "i18n.analytics.base.field.invalid.view.id" )
    private Long viewId;

    @JsonKey( key = BaseFieldKeys.COLUMN_ID )
    @Column( name = ANAREPORTFIELD.COLUMN_ID )
    @NotNull
    private Long columnId;

    @Column( name = ANAREPORTFIELD.CREATED_TIME )
    @NotNull( groups = { Group.Create.class },
              message = "i18n.analytics.base.field.invalid.created.time" )
    private Long createdTime;

    @Column( name = ANAREPORTFIELD.MODIFIED_TIME )
    @NotNull( groups = { Group.Create.class },
              message = "i18n.analytics.base.field.invalid.modified.time" )
    private Long modifiedTime;

    @JsonKey( key = BaseFieldKeys.ORDER )
    @Column( name = ANAREPORTFIELD.ORDER )
    @NotNull( groups = { Group.Create.class },
              message = "i18n.analytics.base.field.invalid.order" )
    private Long order;

    @JsonKey( key = BaseFieldKeys.OPERATION_ID )
    @Column( name = ANAREPORTFIELD.OPERATION_ID )
    @NotNull( groups = { Group.Create.class },
              message = "i18n.analytics.base.field.invalid.operation.id" )
    private Integer operationId;

    @Column(name = ANAREPORTFIELD.CUSTOM_FIELD_CONFIG)
    private String customFieldConfigAsString;

    @Column( name = ANAREPORTFIELD.FIELD_ID )
    private Long fieldId;

    @Column( name = ANAREPORTFIELD.CASE_WHEN_CONFIG )
    private String caseWhenAsString;

    @Column( name = ANAREPORTFIELD.STORE_TYPE_ID )
    private Integer storeTypeId;

    private boolean isPreCalculated;

    /**
     * Parses configured case-when configuration entries.
     *
     * @return Collection of {@link CaseWhenConfig}, or {@code null} when unavailable.
     */
    public Collection<CaseWhenConfig> getCaseWhenConfig() {
        return Objects.isNull(caseWhenAsString) ? null : CaseWhenConfig.parseArray(caseWhenAsString);
    }

    /**
     * Parses configured custom-field metadata.
     *
     * @return The {@link AnalyticsCustomFieldConfig}, or {@code null} when unavailable.
     */
    public Optional<AnalyticsCustomFieldConfig> getCustomFieldConfig() {
        return Optional.ofNullable(customFieldConfigAsString).map(config -> new AnalyticsCustomFieldConfig(Json.object(config)));
    }

    /**
     * Resolves operation type for this field.
     *
     * @return The resolved {@link OperationType}.
     */
    public OperationType getOperationType()
    {
        return OperationType.getOperationType( operationId );
    }

    /**
     * Resolves store type for this field.
     *
     * @return The resolved {@link StoreType}.
     */
    public StoreType getStoreType() {
        return StoreType.valueOf(storeTypeId);
    }
}
