package com.tlc.crm.core.iam.role.permission.validator;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.entity.manager.EntityConfigManager;

/**
 * <p>
 * A validator that validates the relationship(Owner of company/Territory company/Follower of company) between the current user and the given company.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see AbstractUserEntityRelationshipValidator
 */
public class UserCompanyRelationshipValidator extends AbstractUserEntityRelationshipValidator {

    private static final String VALIDATOR_NAME = "USER_COMPANY_RELATIONSHIP_VALIDATOR";

    private final CompanyManager companyManager;

    private UserCompanyRelationshipValidator() {
        companyManager = CompanyManager.getInstance();
    }

    private static final class Instance {
        private static final UserCompanyRelationshipValidator RELATIONSHIP_VALIDATOR = new UserCompanyRelationshipValidator();
    }

    /**
     * Gets the {@link UserCompanyRelationshipValidator} instance
     *
     * @return {@link UserCompanyRelationshipValidator}
     */
    public static UserCompanyRelationshipValidator getInstance() {
        return Instance.RELATIONSHIP_VALIDATOR;
    }

    /**
     * {@inheritDoc}
     *
     * @return the name of the value provider
     */
    @Override
    public String name() {
        return VALIDATOR_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link EntityConfigManager}.
     */
    @Override
    protected EntityConfigManager<?> getEntityConfigManager() {
        return companyManager;
    }
}