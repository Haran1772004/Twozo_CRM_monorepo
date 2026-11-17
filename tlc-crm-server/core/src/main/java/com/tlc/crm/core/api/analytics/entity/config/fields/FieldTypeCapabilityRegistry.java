package com.tlc.crm.core.api.analytics.entity.config.fields;

import com.tlc.analytics.config.model.view.OperationType;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.core.api.analytics.entity.config.axis.AxisTypes;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModularFieldConfig;

import com.tlc.commons.util.StringUtil;

import com.tlc.crm.fields.models.StoreType;

import java.util.List;
import java.util.Map;

/**
 * Registry that maps CRM store types to analytics field capability configuration.
 * Author: karthick V
 * Version: 1.0
 */
public class FieldTypeCapabilityRegistry {
    private static final Map<StoreType, ModularFieldConfig> CAPABILITY_MAP = Map.ofEntries(
                    Map.entry(StoreType.SINGLE_STRING, new ModularFieldConfig(AxisTypes.X_ONLY.getTypeCode(), false, true, false, false, false, true, true, false, false, null, StringUtil.EMPTY_STRING, NumericalConstant.ZERO)),
                    Map.entry(StoreType.DATE, new ModularFieldConfig(AxisTypes.X_ONLY.getTypeCode(), true, true, false, true, true, true, true, false, false, null, StringUtil.EMPTY_STRING, NumericalConstant.ZERO)),
                    Map.entry(StoreType.DROPDOWN, new ModularFieldConfig(AxisTypes.X_ONLY.getTypeCode(), false, true, false, false, false, false, true, false, false, null, StringUtil.EMPTY_STRING, NumericalConstant.ZERO)),
                    Map.entry(StoreType.MULTI_SELECT, new ModularFieldConfig(AxisTypes.X_ONLY.getTypeCode(), false, true, false, false, false, false, true, false,  false, null, StringUtil.EMPTY_STRING, NumericalConstant.ZERO)),
                    Map.entry(StoreType.OWNER, new ModularFieldConfig(AxisTypes.X_ONLY.getTypeCode(), false, true, false, false, false, false, true, false, false, null, StringUtil.EMPTY_STRING, NumericalConstant.ZERO)),
                    Map.entry(StoreType.CONTACT, new ModularFieldConfig(AxisTypes.X_ONLY.getTypeCode(), false, true, false, false, false, false, true, false, false, null, StringUtil.EMPTY_STRING, NumericalConstant.ZERO)),
                    Map.entry(StoreType.COMPANY, new ModularFieldConfig(AxisTypes.X_ONLY.getTypeCode(), false, true, false, false, false, false, true, false, false, null, StringUtil.EMPTY_STRING, NumericalConstant.ZERO)),
                    Map.entry(StoreType.PHONE, new ModularFieldConfig(AxisTypes.X_ONLY.getTypeCode(), false, true, false, false, false, false, true, false, false, null, StringUtil.EMPTY_STRING, NumericalConstant.ZERO)),
                    Map.entry(StoreType.EMAIL, new ModularFieldConfig(AxisTypes.X_ONLY.getTypeCode(), false, true, false, false, false, false, true, false, false, null, StringUtil.EMPTY_STRING, NumericalConstant.ZERO)),
                    Map.entry(StoreType.SINGLE_NUMBER, new ModularFieldConfig(AxisTypes.X_Y.getTypeCode(), false, true, true, false, false, true, true, false, false, List.of(OperationType.SUM.getTypeCode(),
                            OperationType.AVG.getTypeCode(), OperationType.MIN.getTypeCode(), OperationType.MAX.getTypeCode()), StringUtil.EMPTY_STRING, NumericalConstant.ZERO)),
                    Map.entry(StoreType.MONETARY, new ModularFieldConfig(AxisTypes.X_Y.getTypeCode(), false, true, true, false, false, false, true, false, false,  List.of(OperationType.SUM.getTypeCode(),
                            OperationType.AVG.getTypeCode(), OperationType.MIN.getTypeCode(), OperationType.MAX.getTypeCode()), StringUtil.EMPTY_STRING, NumericalConstant.ZERO))
    );

    private FieldTypeCapabilityRegistry() {
    }

    /**
     * Returns analytics capability configuration for the provided store type.
     *
     * @param storeType Represents CRM store type.
     * @return The mapped {@link ModularFieldConfig}.
     */
    public static ModularFieldConfig getConfig(final StoreType storeType) {
        return CAPABILITY_MAP.get(storeType);
    }

    /**
     * Checks whether analytics capability is available for a store type.
     *
     * @param storeType Represents CRM store type.
     * @return {@code true} if analytics supports the store type.
     */
    public static boolean isAnalyticsCapable(final StoreType storeType) {
        return CAPABILITY_MAP.containsKey(storeType);
    }
}
