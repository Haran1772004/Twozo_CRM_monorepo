package com.tlc.crm.entity.role.permission.provider;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.service.list.ProviderService;

import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.SecureDefaultValueProvider;
import com.tlc.crm.security.permission.model.SubCategory;

import java.util.Optional;

/**
 * <p>
 * Provider for providing the data provider ID
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see SecureDefaultValueProvider
 */
public class DataProviderSupplier implements SecureDefaultValueProvider<Long> {

    private static final String PROVIDER_NAME = "DATA_PROVIDER_SUPPLIER";

    private final ProviderService providerService;

    private DataProviderSupplier() {
        providerService = ProviderService.getInstance();
    }

    private static final class Instance {
        private static final DataProviderSupplier DATA_PROVIDER_SUPPLIER = new DataProviderSupplier();
    }

    /**
     * Gets the {@link DataProviderSupplier} instance
     *
     * @return {@link DataProviderSupplier}
     */
    public static DataProviderSupplier getInstance() {
        return Instance.DATA_PROVIDER_SUPPLIER;
    }

    /**
     * {@inheritDoc}
     *
     * @return the name of the value provider
     */
    @Override
    public String name() {
        return PROVIDER_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @return the user ID
     */
    @Override
    public Long get() {
        final Optional<RolePermission> rolePermission = Optional.ofNullable(LoggedInUserInfo.getSecurityContext().getRolePermission());

        return rolePermission.map(permission -> {
            final Optional<Source> optionalSource = Optional.ofNullable(findSource(permission.getSubCategory().getType()));

            return optionalSource.map(providerService::getIdBySource).orElseGet(() -> null);
        }).orElseGet(() -> null);
    }

    /**
     * <p>
     * Finds the {@link Source} for the given {@link SubCategory.Type}.
     * </p>
     *
     * @param subCategory Represents the subcategory.
     * @return {@link Source}.
     */
    private Source findSource(final SubCategory.Type subCategory) {
        return switch (subCategory) {
            case CONTACTS -> Source.CONTACT;
            case COMPANIES -> Source.COMPANY;
            case DEALS -> Source.DEAL;
            case PRODUCTS -> Source.PRODUCT;
            default -> null;
        };
    }
}