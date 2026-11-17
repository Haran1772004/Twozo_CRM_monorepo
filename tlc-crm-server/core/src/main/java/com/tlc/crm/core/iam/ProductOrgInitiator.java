package com.tlc.crm.core.iam;

import com.tlc.crm.common.iam.OrgInitiator;
import com.tlc.crm.common.iam.OrgInitiatorAction;
import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.dropdown.product.Category;
import com.tlc.crm.core.field.product.ProductFields;
import com.tlc.crm.entity.handler.DefaultOptionHandler;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * An action performed by the product org initializer to initialize organizational
 * fields with predefined values upon the creation of a new organization.
 *
 * @author Kishore J
 * @version 1.0
 */
@OrgInitiator(name = "Product")
public class ProductOrgInitiator implements OrgInitiatorAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineOrgInitiator.class);

    private final DefaultOptionHandler defaultOptionHandler;
    private final FieldService fieldService;

    public ProductOrgInitiator() {
        defaultOptionHandler = DefaultOptionHandler.getInstance();
        fieldService = FieldService.getInstance();
    }

    @Override
    public void execute(final CrmOrg crmOrg, final CrmUser crmUser) {
        final Long orgId = crmOrg.id();
        final Long userId = crmUser.id();
        final Map<String, FieldInfo> productFields = fieldService.groupDefaultFieldsByAliasName(Product.ENTITY_TYPE,
                ProductFields.CATEGORY, ProductFields.TYPE, ProductFields.ACTIVE_STATUS,
                ProductFields.TAXABLE);

        for (final FieldInfo fieldInfo: productFields.values()) {

            switch (fieldInfo.aliasName()) {
                case "category" -> defaultOptionHandler.create(orgId, fieldInfo.id(), Category.getOptions());
                case "type" -> defaultOptionHandler.create(orgId, fieldInfo.id(), Product.Type.getOptions());
                case "activeStatus" -> defaultOptionHandler.create(orgId, fieldInfo.id(), Product.ActiveStatus.getOptions());
                case "taxable" -> defaultOptionHandler.create(orgId, fieldInfo.id(), Product.Taxable.getOptions());
            }
        }

        LOGGER.info("Org Init Action: Product initialization completed. Org ID: {}, User ID: {}", orgId, userId);
    }
}
