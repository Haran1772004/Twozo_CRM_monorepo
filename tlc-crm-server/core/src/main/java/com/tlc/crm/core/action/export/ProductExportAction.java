package com.tlc.crm.core.action.export;

import com.tlc.crm.common.export.action.DataExporter;
import com.tlc.crm.common.export.field.DataExportFields;
import com.tlc.crm.common.list.model.ListType;
import com.tlc.crm.common.export.model.ExportProviderType;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.product.manager.VariationManager;
import com.tlc.crm.core.api.product.model.PriceTag;
import com.tlc.crm.core.api.product.model.Variation;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.SubCategory;

import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.export.action.AbstractEntityDataExportAction;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Exports the product information.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@DataExporter(name = "product")
public class ProductExportAction extends AbstractEntityDataExportAction {

    private static final String BLANK_SPACE = " ";
    private static final String COMMA = ", ";
    private static final String LEFT_PARENTHESIS = "(";
    private static final String RIGHT_PARENTHESIS = ")";
    private static final String VARIATION = "Variation";
    private static final String PRICE_TAGS = "PriceTags";
    private static final String DEALS = "Deals";

    private final VariationManager variationManager;
    private final DealManager dealManager;

    public ProductExportAction() {
        variationManager = VariationManager.getInstance();
        dealManager = DealManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organisation id.
     * @param roleId Represents the role id.
     * @return Holds the permission details of the data export.
     */
    @Override
    protected RolePermission getPermission(final Long orgId, final Long roleId) {
        return rolePermissionService.getPermission(orgId, roleId, Category.Type.SOURCES, SubCategory.Type.PRODUCTS, Action.Type.EXPORT_RECORDS);
    }

    /**
     * {@inheritDoc}
     *
     * @return The provider name.
     */
    @Override
    protected Provider getProvider() {
        return Provider.PRODUCT_LIST;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link ListType}.
     */
    @Override
    protected ListType getListType() {
        return ListType.PRODUCT_LIST;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId              Represents an organization id
     * @param exportProviderType Represents the {@link ExportProviderType}
     * @param entityRows         Which contains the collection of entity rows
     */
    @Override
    protected void attachAdditionalEntityData(final long orgId, final ExportProviderType exportProviderType,
                                              final Collection<Map<String, Object>> entityRows) {

        if (Objects.equals(ExportProviderType.DETAIL, exportProviderType)) {

            for (final Map<String, Object> entityRow : entityRows) {
                final Long productId = (Long) entityRow.get(DataExportFields.ID);
                final Collection<Variation> variations = variationManager.getVariations(orgId, productId);
                final Collection<PriceTag> defaultPriceTags = variationManager.getDefaultVariationPriceTags(orgId, productId);
                final Collection<Deal> deals = dealManager.getDealsByProduct(orgId, productId);

                entityRow.put(VARIATION, getFormattedVariation(variations));
                entityRow.put(PRICE_TAGS, defaultPriceTags.isEmpty() ? null : getFormattedPriceTag(defaultPriceTags));
                entityRow.put(DEALS, deals.isEmpty() ? null :
                        deals.stream().map(Deal::getTitle).collect(Collectors.joining(",")));
            }
        }
    }

    /**
     * <p>
     * Gets the formatted variation for the given {@link Collection} of {@link Variation}.
     * </p>
     *
     * @param variations Represents the {@link Collection} of {@link Variation}
     * @return The formatted variation
     */
    private String getFormattedVariation(final Collection<Variation> variations) {
        final StringBuilder valueBuilder = new StringBuilder();

        for (final Variation variation : variations) {
            valueBuilder.append(getFormattedPriceTag(variation.getPriceTags()));
            valueBuilder.append(BLANK_SPACE).append(LEFT_PARENTHESIS)
                    .append(variation.getName()).append(RIGHT_PARENTHESIS)
                    .append(BLANK_SPACE);
        }

        return valueBuilder.toString();
    }

    /**
     * <p>
     * Gets the formatted price tag from the given {@link Collection} of {@link PriceTag}.
     * </p>
     *
     * @param priceTags Represents the {@link Collection} of {@link PriceTag}
     * @return The formatted price tag
     */
    private String getFormattedPriceTag(final Collection<PriceTag> priceTags) {
        final StringBuilder valueBuilder = new StringBuilder();
        final Iterator<PriceTag> priceTagIterator = priceTags.iterator();

        while (priceTagIterator.hasNext()) {
            final PriceTag priceTag = priceTagIterator.next();

            valueBuilder.append(priceTag.getCurrency().getCode())
                    .append(priceTag.getPrice());
            if (priceTagIterator.hasNext()) {
                valueBuilder.append(COMMA);
            }
        }

        return valueBuilder.toString();
    }

    /**
     * {@inheritDoc}
     *
     * @return A collection of additional headers as strings
     */
    @Override
    protected Collection<String> getAdditionalHeaders(final ExportProviderType exportProviderType) {
        return Objects.equals(ExportProviderType.DETAIL, exportProviderType) ? Set.of(VARIATION, PRICE_TAGS, DEALS) : Collections.emptySet();
    }
}
