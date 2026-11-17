package com.tlc.crm.core.iam;

import com.tlc.crm.common.iam.OrgInitiator;
import com.tlc.crm.common.iam.OrgInitiatorAction;
import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.dropdown.company.BusinessType;
import com.tlc.crm.core.dropdown.company.IndustryType;
import com.tlc.crm.core.dropdown.company.OrganizationStatus;
import com.tlc.crm.core.field.contact.CompanyFields;
import com.tlc.crm.entity.handler.DefaultOptionHandler;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * An action performed by the company org initializer to initialize organizational fields
 * with predefined values upon the creation of a new organization.
 *
 * @author Kishore J
 * @version 1.0
 */
@OrgInitiator(name = "company")
public class CompanyOrgInitiator implements OrgInitiatorAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompanyOrgInitiator.class);

    private final DefaultOptionHandler defaultOptionHandler;
    private final FieldService fieldService;

    public CompanyOrgInitiator() {
        defaultOptionHandler = DefaultOptionHandler.getInstance();
        fieldService = FieldService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmOrg  Represents the {@link CrmOrg}
     * @param crmUser Represents the {@link CrmUser}
     */
    @Override
    public void execute(final CrmOrg crmOrg, final CrmUser crmUser) {
        final Long orgId = crmOrg.id();
        final Long userId = crmUser.id();
        LOGGER.info("Org Init Action: Company initialization started. Org ID: {}, User ID: {}", orgId, userId);

        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Company.ENTITY_TYPE,
                CompanyFields.INDUSTRY_TYPE, CompanyFields.ORGANIZATION_STATUS, CompanyFields.BUSINESS_TYPE, CompanyFields.SALES_OWNER);

        for (final FieldInfo fieldInfo : fields.values()) {

            switch (fieldInfo.aliasName()) {
                case "industryType" -> defaultOptionHandler.create(orgId, fieldInfo.id(), IndustryType.getOptions());
                case "organizationStatus" -> defaultOptionHandler.create(orgId, fieldInfo.id(), OrganizationStatus.getOptions());
                case "businessType" -> defaultOptionHandler.create(orgId, fieldInfo.id(), BusinessType.getOptions());
            }
        }
        LOGGER.info("Org Init Action: Company initialization completed. Org ID: {}, User ID: {}", orgId, userId);
    }
}
