package com.tlc.crm.core.api.analytics.manager.config.field;

import com.tlc.crm.core.api.analytics.entity.config.fields.AnalyticsField;
import com.tlc.crm.core.api.analytics.entity.config.fields.FieldTypeComparator;
import com.tlc.crm.core.api.analytics.entity.config.fields.bucket.FieldBucketUnit;
import com.tlc.crm.core.api.analytics.entity.config.fields.filter.Comparator;
import com.tlc.crm.core.api.analytics.entity.config.fields.filter.ComparatorFilterType;
import com.tlc.crm.core.api.analytics.entity.config.fields.filter.ComparatorFunction;
import com.tlc.crm.core.api.analytics.entity.config.modules.CRMCustomModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.CRMSystemModularField;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;

/**
 * <p>
 * Manages CRM Manager API
 * </p>
 *
 * @author James
 * @version 1.0
 */
public interface FieldManagerAPI
{
    /**
     * <p>
     * Retrieves the manager responsible for handling the field comparators
     * </p>
     *
     * @return an instance of {@link CRMViewConfigManager} that manages {@link Comparator}
     */
    default CRMViewConfigManager<Comparator> getComparatorManager()
    {
        return ComparatorManager.INSTANCE();
    }

    /**
     * <p>
     * Retrieves the manager responsible for managing analytics related fields
     * </p>
     *
     * @return an instance of {@link CRMViewConfigManager} that manages {@link AnalyticsField}
     */
    default CRMViewConfigManager<AnalyticsField> getANAFieldManager()
    {
        return AnalyticsFieldManager.INSTANCE();
    }

    /**
     * <p>
     * Retrieves the manager responsible for field type comparators
     * </p>
     *
     * @return an instance of {@link CRMViewConfigManager} that manages {@link FieldTypeComparator}
     */
    default CRMViewConfigManager<FieldTypeComparator> getFieldTypeComparatorManager()
    {
        return FieldTypeComparatorManager.INSTANCE();
    }

    /**
     * <p>
     * Retrieves the manager responsible for field related bucket unit
     * </p>
     *
     * @return an instance of {@link CRMViewConfigManager} that manages {@link FieldBucketUnit}
     */
    default CRMViewConfigManager<FieldBucketUnit> getFieldBucketUnitManager()
    {
        return FieldBucketUnitManager.INSTANCE();
    }

    /**
     * <p>
     * Retrieves the manager responsible for handling the field comparators
     * </p>
     *
     * @return an instance of {@link CRMViewConfigManager} that manages {@link Comparator}
     */
    default CRMViewConfigManager<ComparatorFunction> getComparatorFunctionManager()
    {
        return ComparatorFunctionManager.INSTANCE();
    }

    /**
     * <p>
     * Retrieves the manager responsible for handling the field comparators filter value type
     * </p>
     *
     * @return an instance of {@link CRMViewConfigManager} that manages {@link Comparator}
     */
    default CRMViewConfigManager<ComparatorFilterType> getComparatorFilterTypeManager()
    {
        return ComparatorFilterTypeManager.INSTANCE();
    }

    /**
     * <p>
     * Retrieves the manager responsible for handling the report modular field
     * </p>
     *
     * @return an instance of {@link CRMViewConfigManager} that manages {@link ReportModularField}
     */
    default CRMViewConfigManager<ReportModularField> getReportFieldManager()
    {
        return ReportModularFieldManager.INSTANCE();
    }

    /**
     * <p>
     * Retrieves the manager responsible for handling the system modular field
     * </p>
     *
     * @return an instance of {@link CRMViewConfigManager} that manages {@link CRMSystemModularField}
     */
    default CRMViewConfigManager<CRMSystemModularField> getCRMSystemModularFieldManager()
    {
        return SystemModularFieldManager.INSTANCE();
    }

    /**
     * <p>
     * Retrieves the manager responsible for handling the custom modular field
     * </p>
     *
     * @return an instance of {@link CRMViewConfigManager} that manages {@link com.tlc.crm.core.api.analytics.entity.config.modules.CRMCustomModularField}
     */
    default CRMViewConfigManager<CRMCustomModularField> getCRMCustomModularFieldManager()
    {
        return CRMCustomModularFieldManager.INSTANCE();
    }
}
