package com.tlc.crm.core.iam.role.permission.validator;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.entity.manager.EntityConfigManager;

/**
 * <p>
 * A validator that validates the relationship(Owner of contact/Territory contact/Follower of contact) between the current user and the given contact.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see AbstractUserEntityRelationshipValidator
 */
public class UserContactRelationshipValidator extends AbstractUserEntityRelationshipValidator {

    private static final String VALIDATOR_NAME = "USER_CONTACT_RELATIONSHIP_VALIDATOR";

    private final ContactManager contactManager;

    private UserContactRelationshipValidator() {
        contactManager = ContactManager.getInstance();
    }

    private static final class Instance {
        private static final UserContactRelationshipValidator RELATIONSHIP_VALIDATOR = new UserContactRelationshipValidator();
    }

    /**
     * Gets the {@link UserContactRelationshipValidator} instance
     *
     * @return {@link UserContactRelationshipValidator}
     */
    public static UserContactRelationshipValidator getInstance() {
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
        return contactManager;
    }
}