package com.tlc.crm.core.iam.role.permission.validator;

import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.entity.manager.EntityConfigManager;

/**
 * <p>
 * A validator that validates the relationship(Owner of deal/Territory deal/Follower of deal) between the current user and the given deal.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see AbstractUserEntityRelationshipValidator
 */
public class UserDealRelationshipValidator extends AbstractUserEntityRelationshipValidator {

    private static final String VALIDATOR_NAME = "USER_DEAL_RELATIONSHIP_VALIDATOR";

    private final DealManager dealManager;

    private UserDealRelationshipValidator() {
        dealManager = DealManager.getInstance();
    }

    private static final class Instance {
        private static final UserDealRelationshipValidator RELATIONSHIP_VALIDATOR = new UserDealRelationshipValidator();
    }

    /**
     * Gets the {@link UserDealRelationshipValidator} instance
     *
     * @return {@link UserDealRelationshipValidator}
     */
    public static UserDealRelationshipValidator getInstance() {
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
        return dealManager;
    }
}