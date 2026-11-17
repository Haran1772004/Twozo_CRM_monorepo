package com.tlc.crm.core.api.analytics.entity.config.modules;


import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import lombok.Getter;

/**
 * <p>
 * Manages CRM Modules
 * </p>
 *
 * @author James
 * @version 1.0
 */
@Getter
public enum ModuleType
{
    CONTACT(1, "i18n.analytics.module.name.contact.placeholder", "CONTACT", true, true),
    COMPANY(2, "i18n.analytics.module.name.company.placeholder", "COMPANY", true, true),
    DEAL(3, "i18n.analytics.module.name.deal.placeholder", "DEAL", true, true),
    PRODUCT(4, "i18n.analytics.module.name.product.placeholder", "PRODUCT", true, false),
    DEAL_LINE_ITEMS(5, "i18n.analytics.module.name.deal.line.items.placeholder", "DEAL_LINE_ITEMS", true, false),
    TIME_PERIOD(6, "i18n.analytics.module.name.time.period", "TIME_PERIOD", false, false),
    ACTIVITY(7, "i18n.analytics.module.name.activity", "ACTIVITY", true, true),
    TERRITORY(8, "i18n.analytics.module.name.territory", "TERRITORY", false, false),
    USER(9, "i18n.analytics.module.name.user", "USER", false, false);

    private final int typeCode;
    private final String i18nKey;
    private final String name;
    private final boolean isEnabled;
    private final boolean isTemplateEnabled;

    ModuleType(int typeCode, String i18nKey, String name, boolean isEnabled, boolean isTemplateEnabled)
    {
        this.typeCode = typeCode;
        this.i18nKey = i18nKey;
        this.name = name;
        this.isEnabled = isEnabled;
        this.isTemplateEnabled = isTemplateEnabled;
    }

    /**
     * Retrieves the {@link ModuleType} enum corresponding to a given type code
     *
     * @param typeCode The type code to look up
     * @return The {@link ModuleType} enum associated with the provided code
     */
    public static ModuleType getModuleType( final int typeCode )
    {
        return switch (typeCode) {
            case 1 -> CONTACT;
            case 2 -> COMPANY;
            case 3 -> DEAL;
            case 4 -> PRODUCT;
            case 5 -> DEAL_LINE_ITEMS;
            case 6 -> TIME_PERIOD;
            case 7 -> ACTIVITY;
            case 8 -> TERRITORY;
            case 9 -> USER;
            default -> throw ErrorCode.get( AnalyticsErrorCodes.INVALID_MODULE,
                    "i18n.analytics.config.invalid.module" );
        };
    }

    /**
     * Retrieves the enum name corresponding to a given type code
     *
     * @param name The module name to look up
     * @return The {@link ModuleType} name associated with the provided code
     */
    public static ModuleType getModuleTypeByName( final String name )
    {
        return switch (name.toLowerCase())
                {
                    case "contact" -> CONTACT;
                    case "company" -> COMPANY;
                    case "deal" -> DEAL;
                    case "product" -> PRODUCT;
                    case "deallineitems" -> DEAL_LINE_ITEMS;
                    case "time_period" -> TIME_PERIOD;
                    case "activity" -> ACTIVITY;
                    case "territory" -> TERRITORY;
                    case "user" -> USER;
                    default -> null;
                };
    }
}
