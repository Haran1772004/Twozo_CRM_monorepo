package com.tlc.crm.entity.list.filter.source;

/**
 * <p>
 * Provides the enum for the irrelevant source groups.
 * <p>
 * (eg: Case - Contact and product are irrelevant sources, But some cases we have need to combine those sources so,
 *             the mapper source is introduced, in this case Deal is acts as mapper source)
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum SourceLinkGroup {

    PRODUCT_CONTACT(FilterSourceType.PRODUCT, FilterSourceType.DEAL, FilterSourceType.CONTACT),
    PRODUCT_COMPANY(FilterSourceType.PRODUCT, FilterSourceType.DEAL, FilterSourceType.COMPANY),
    PRODUCT_ACTIVITY(FilterSourceType.PRODUCT, FilterSourceType.DEAL, FilterSourceType.ACTIVITY),
    CONTACT_PRODUCT(FilterSourceType.CONTACT, FilterSourceType.DEAL, FilterSourceType.PRODUCT),
    COMPANY_PRODUCT(FilterSourceType.COMPANY, FilterSourceType.DEAL, FilterSourceType.PRODUCT),
    ACTIVITY_PRODUCT(FilterSourceType.ACTIVITY, FilterSourceType.DEAL, FilterSourceType.PRODUCT),
    ;

    private final FilterSourceType currentSource;
    private final FilterSourceType mapperSource;
    private final FilterSourceType irrelevantSource;

    SourceLinkGroup(final FilterSourceType currentSource, final FilterSourceType mapperSource, final FilterSourceType irrelevantSource) {
        this.currentSource = currentSource;
        this.mapperSource = mapperSource;
        this.irrelevantSource = irrelevantSource;
    }

    public FilterSourceType getIrrelevantSource() {
        return irrelevantSource;
    }

    public FilterSourceType getMapperSource() {
        return mapperSource;
    }

    public FilterSourceType getCurrentSource() {
        return currentSource;
    }
}
