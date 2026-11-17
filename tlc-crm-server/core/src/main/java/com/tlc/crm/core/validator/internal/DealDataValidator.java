package com.tlc.crm.core.validator.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.iam.api.manager.CrmUserManager;
import com.tlc.crm.common.iam.api.manager.TerritoryManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Territory;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.PipelineStageManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.api.deal.model.ProductDetail;
import com.tlc.crm.core.api.deal.model.ProductDetailsHolder;
import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.api.product.manager.VariationManager;
import com.tlc.crm.core.internal.deal.status.DealErrorCodes;
import com.tlc.crm.core.internal.product.status.ProductErrorCodes;
import com.tlc.crm.core.validator.EntityDataValidator;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Validates the {@link Deal}
 *
 * @author Maharaja S
 * @version 1.0
 */
public class DealDataValidator implements EntityDataValidator<Deal> {

    private final CompanyManager companyManager;
    private final ContactManager contactManager;
    private final PipelineStageManager pipelineStageManager;
    private final ProductManager productManager;
    private final VariationManager variationManager;
    private final CrmUserManager crmUserManager;
    private final TerritoryManager territoryManager;

    private DealDataValidator() {
        companyManager = CompanyManager.getInstance();
        contactManager = ContactManager.getInstance();
        pipelineStageManager = PipelineStageManager.getInstance();
        productManager = ProductManager.getInstance();
        variationManager = VariationManager.getInstance();
        crmUserManager = CrmUserManager.getInstance();
        territoryManager = TerritoryManager.getInstance();
    }

    /**
     * Holds the instance of {@link DealDataValidator}
     */
    private static final class InstanceHolder {
        private static final DealDataValidator DEAL_CONTEXT_VALIDATOR = new DealDataValidator();
    }

    /**
     * <p>
     * Gets the instance of {@link DealDataValidator}
     * </p>
     *
     * @return the instance of {@link DealDataValidator}
     */
    public static DealDataValidator getInstance() {
        return InstanceHolder.DEAL_CONTEXT_VALIDATOR;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the org id
     * @param deals Represents the deals to be validated
     */
    @Override
    public void validate(final Long orgId, final Collection<Deal> deals) {
        final Set<Long> productIds = new HashSet<>();
        final Set<Long> variationIds = new HashSet<>();
        final Set<Long> companyIds = new HashSet<>();
        final Set<Long> contactIds = new HashSet<>();
        final Set<Long> salesOwnerIds = new HashSet<>();
        final Set<Long> territoryIds = new HashSet<>();
        final Map<Long, Collection<Long>> pipelineAndStageIds = new HashMap<>();

        for (final Deal deal : deals) {
            final Contact dealContact = deal.getPrimaryContact();
            final Long contactId = Objects.nonNull(dealContact) ? dealContact.id() : null;
            final ProductDetailsHolder productDetailsHolder = deal.getProductDetailsHolder();
            final Company dealCompany = deal.getCompany();
            final Territory territory = deal.getTerritory();
            final CrmUser salesOwner = deal.getSalesOwner();
            final Long territoryId = Objects.nonNull(territory) ? territory.id() : null;
            final Long salesOwnerId = Objects.nonNull(salesOwner) ? salesOwner.id() : null;
            final Pipeline pipeline = deal.getPipeline();
            final PipelineStage pipelineStage = deal.getPipelineStage();

            if (Objects.nonNull(contactId)) {
                contactIds.add(contactId);
            }

            if (Objects.nonNull(territoryId)) {
                territoryIds.add(territoryId);
            }

            if (Objects.nonNull(salesOwnerId)) {
                salesOwnerIds.add(salesOwnerId);
            }

            if (Objects.nonNull(dealCompany)) {
                final Long companyId = dealCompany.id();

                if (Objects.nonNull(companyId)) {
                    companyIds.add(companyId);
                }
            }

            if (Objects.nonNull(productDetailsHolder)) {
                final Collection<ProductDetail> productDetails = productDetailsHolder.getProductDetails();

                if (Objects.nonNull(productDetails)) {

                    for (final ProductDetail productDetail : productDetails) {
                        final Long id = productDetail.getProduct().id();

                        if (Objects.nonNull(id)) {
                            productIds.add(id);
                        }

                        if (Objects.nonNull(productDetail.getVariationId())) {
                            variationIds.add(productDetail.getVariationId());
                        }
                    }
                }
            }

            if (Objects.nonNull(pipeline) && Objects.nonNull(pipelineStage)) {
                pipelineAndStageIds.computeIfAbsent(pipeline.id(), pipelineIdKey -> new HashSet<>()).add(pipelineStage.id());
            }
        }

        if (!companyIds.isEmpty() && !companyManager.exists(orgId, companyIds)) {
            throw ErrorCode.get(DealErrorCodes.COMPANY_NOT_FOUND, "i18n.company.not.found");
        }

        if (!contactIds.isEmpty() && !contactManager.exists(orgId, contactIds)) {
            throw ErrorCode.get(DealErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found");
        }

        if (!productIds.isEmpty() && !productManager.exists(orgId, productIds)) {
            throw ErrorCode.get(DealErrorCodes.PRODUCT_NOT_FOUND, "i18n.product.not.found");
        }

        if (!pipelineAndStageIds.isEmpty() && !pipelineStageManager.exists(orgId, pipelineAndStageIds)) {
            throw ErrorCode.get(DealErrorCodes.INVALID_PIPELINE_OR_STAGE, "i18n.pipeline.invalid.id");
        }

        if (!variationIds.isEmpty() && !variationManager.exists(orgId, variationIds)) {
            throw ErrorCode.get(ProductErrorCodes.VARIATION_NOT_FOUND, "i18n.product.variation.not.found");
        }

        if (!salesOwnerIds.isEmpty() && !crmUserManager.exists(orgId, salesOwnerIds)) {
            throw ErrorCode.get(DealErrorCodes.CRM_USER_NOT_FOUND, "i18n.deal.crm.user.not.found");
        }

        if (!territoryIds.isEmpty() && !territoryManager.exists(orgId, territoryIds)) {
            throw ErrorCode.get(DealErrorCodes.TERRITORY_NOT_FOUND, "i18n.deal.territory.not.found");
        }
    }
}
