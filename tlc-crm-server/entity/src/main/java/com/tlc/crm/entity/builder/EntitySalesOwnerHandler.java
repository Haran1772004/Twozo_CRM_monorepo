package com.tlc.crm.entity.builder;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.entity.models.Entity;

import java.util.Objects;

/**
 * <p>
 * Handles the sales owner related functions for an entity
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class EntitySalesOwnerHandler {

    private final UserService userService;

    public EntitySalesOwnerHandler() {
        userService = UserService.getInstance();
    }

    private static class Instance {
        private static final EntitySalesOwnerHandler INSTANCE = new EntitySalesOwnerHandler();
    }

    public static EntitySalesOwnerHandler getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Assigns the sales owner for the given company
     * </p>
     *
     * @param entity  Represents the {@link Entity}
     * @param ownerId Represents the owner id
     * @param orgId   Represents an organization id
     */
    public void assignOwner(final long orgId, final Long ownerId, final Entity entity) {
        final CrmUser salesOwner = entity.getSalesOwner();

        entity.setNotifiable(Objects.isNull(salesOwner) || !Objects.equals(ownerId, salesOwner.id()));
        entity.setSalesOwner(userService.getUser(orgId, ownerId));
        entity.setOwnerId(ownerId);
    }
}
