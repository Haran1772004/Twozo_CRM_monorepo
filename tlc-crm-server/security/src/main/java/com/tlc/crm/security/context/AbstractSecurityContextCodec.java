package com.tlc.crm.security.context;

import com.tlc.commons.executor.context.Context;
import com.tlc.commons.executor.context.ContextCodec;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.security.iam.BaseCrmUser;
import com.tlc.crm.security.internal.context.PermissionSecurityContext;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.service.RolePermissionService;

import java.util.Objects;

/**
 * Codec for encoding and decoding operations on the {@link SecurityContext}
 *
 * @author sundar
 * @version 1.0
 * @see ContextCodec
 */
public abstract class AbstractSecurityContextCodec implements ContextCodec {

    private static final String ID = "id";
    private static final String ORG_ID = "orgId";
    private static final String ROLE_ID = "roleId";
    private static final String CONTEXT_ID = "contextId";
    private static final String CATEGORY = "category";
    private static final String SUBCATEGORY = "subCategory";
    private static final String ACTION = "access";

    /**
     * {@inheritDoc}
     *
     * @param context the {@link Context} to be encoded
     * @return the resulting encoded bytes
     */
    @Override
    public byte[] encode(final Context context) {

        if (context instanceof final SecurityContext securityContext) {
            final JsonObject securityContextJson = Json.object();
            final RolePermission permission = securityContext.getRolePermission();
            final BaseCrmUser user = securityContext.getUser();

            securityContextJson.put(ID, user.id());
            securityContextJson.put(ORG_ID, user.orgId());
            securityContextJson.put(CONTEXT_ID, securityContext.id());

            if (Objects.nonNull(permission)) {
                securityContextJson.put(ROLE_ID, permission.getRoleId());
                securityContextJson.put(CATEGORY, permission.getCategory().getType().getName());
                securityContextJson.put(SUBCATEGORY, permission.getSubCategory().getType().getName());
                securityContextJson.put(ACTION, permission.getAction().getType().getName());
            }
            return securityContextJson.getBytes();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param data the bytes to decode
     * @return the {@link Context}
     */
    @Override
    public Context decode(final byte[] data) {
        final JsonObject securityContextJson = Json.object(data);
        final long orgId = securityContextJson.getLong(ORG_ID);
        final BaseCrmUser user = getUser(orgId, securityContextJson.getLong(ID));
        final RolePermission permission;

        if (securityContextJson.containsKey(ROLE_ID) && securityContextJson.containsKey(SUBCATEGORY) && securityContextJson.containsKey(ACTION)) {
            permission = RolePermissionService.getInstance().getPermission(orgId, securityContextJson.getLong(ROLE_ID),
                    Category.Type.get(securityContextJson.getString(CATEGORY)), SubCategory.Type.get(securityContextJson.getString(SUBCATEGORY)),
                    Action.Type.get(securityContextJson.getString(ACTION)));
        } else {
            permission = null;
        }
        return Objects.isNull(user) ? null : new PermissionSecurityContext(user, securityContextJson.getString(CONTEXT_ID), permission);
    }

    /**
     * Gets the user information
     *
     * @param orgId  Represents the organization ID
     * @param userId Represents the user ID
     * @return the {@link BaseCrmUser}
     */
    protected abstract BaseCrmUser getUser(final long orgId, final long userId);
}