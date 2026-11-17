package com.tlc.crm.entity.list.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Holds the provider names for the list API.
 *
 * @author KarthickV
 * @version 1.0
 */
public enum Provider {

    CONTACT_LIST(Source.CONTACT),
    DEAL_LIST(Source.DEAL),
    PRODUCT_LIST(Source.PRODUCT),
    ACTIVITY_LIST(Source.ACTIVITY),
    COMPANY_LIST(Source.COMPANY),
    REPORTS_LIST(Source.REPORTS),
    REPORTS_TRASH_LIST(Source.REPORTS_TRASH),
    QUOTA_TEAM_VIEW_LIST(Source.QUOTA_TEAM_VIEW),
    QUOTA_HIERARCHY_VIEW_LIST(Source.QUOTA_HIERARCHY_VIEW),
    QUOTATION_LIST(Source.QUOTATION),
    INVOICE_LIST(Source.INVOICE),
    DEFAULT(null),
    ;

    private static final Set<Provider> ALLOWED_FILTER_PROVIDERS = EnumSet.of(CONTACT_LIST,
            DEAL_LIST, PRODUCT_LIST, ACTIVITY_LIST, COMPANY_LIST);

    private final Source source;

    Provider(final Source source) {
        this.source = source;
    }

    public Source getSource() {
        return this.source;
    }

    public static boolean isAllowedForFilter(final Provider provider) {
        return ALLOWED_FILTER_PROVIDERS.contains(provider);
    }

    /**
     * Gets the {@link Provider} by its name.
     *
     * @param providerName Refers the provider name.
     * @return The {@link Provider}.
     */
    public static Provider getByName(final String providerName) {
        return switch (providerName.toLowerCase()) {
            case "contact_list" -> CONTACT_LIST;
            case "deal_list" -> DEAL_LIST;
            case "product_list" -> PRODUCT_LIST;
            case "activity_list" -> ACTIVITY_LIST;
            case "company_list" -> COMPANY_LIST;
            case "reports_list" -> REPORTS_LIST;
            case "reports_trash_list" -> REPORTS_TRASH_LIST;
            case "quota_team_view_list" -> QUOTA_TEAM_VIEW_LIST;
            case "quotation_list" -> QUOTATION_LIST;
            case "invoice_list" -> INVOICE_LIST;
            default -> throw ErrorCode.get(EntityErrorCodes.UNSUPPORTED_PROVIDER);
        };
    }

    /**
     * Gets the {@link Provider} by its source.
     *
     * @param source Refers the source.
     * @return The {@link Provider}.
     */
    public static Provider getBySource(final Source source) {
        return switch (source) {
            case CONTACT -> CONTACT_LIST;
            case DEAL -> DEAL_LIST;
            case PRODUCT -> PRODUCT_LIST;
            case ACTIVITY -> ACTIVITY_LIST;
            case COMPANY -> COMPANY_LIST;
            case REPORTS -> REPORTS_LIST;
            case REPORTS_TRASH -> REPORTS_TRASH_LIST;
            case QUOTA_TEAM_VIEW -> QUOTA_TEAM_VIEW_LIST;
            case QUOTA_HIERARCHY_VIEW -> QUOTA_HIERARCHY_VIEW_LIST;
            case QUOTATION -> QUOTATION_LIST;
            case INVOICE -> INVOICE_LIST;
            default -> throw ErrorCode.get(EntityErrorCodes.INVALID_SOURCE, "i18n.source.invalid");
        };
    }

    /**
     * <p>
     * Provides all the non default providers.
     * </p>
     *
     * @return Collection of {@link Provider}.
     */
    public static Collection<Provider> getAll() {
        return Arrays.stream(values()).filter(Provider::isAllowedForFilter).collect(Collectors.toSet());
    }
}