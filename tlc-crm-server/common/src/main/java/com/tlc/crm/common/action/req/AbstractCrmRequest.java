package com.tlc.crm.common.action.req;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.internal.IamErrorCodes;

import com.tlc.crm.common.resolver.I18NCRMResolutionAdapter;
import com.tlc.i18n.I18nResolver;
import com.tlc.web.WebExchange;
import com.tlc.web.action.req.AbstractHttpRequest;

import java.util.Objects;

/**
 * Provides abstract services to manage the request
 *
 * @author Sundar
 * @version 1.0
 * @see CrmRequest
 */
public abstract class AbstractCrmRequest extends AbstractHttpRequest implements CrmRequest {

    private CrmUser crmUser;
    private final ClientType clientType;

    protected final I18NCRMResolutionAdapter i18NCRMResolutionAdapter;

    AbstractCrmRequest(final WebExchange exchange) {
        super(exchange);
        this.clientType = ClientType.get(header("Client-Type"));
        this.i18NCRMResolutionAdapter = new I18NCRMResolutionAdapter();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmUser Represents the {@link CrmUser}
     */
    @Override
    public void setUser(final CrmUser crmUser) {
        this.crmUser = crmUser;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link CrmUser}
     */
    @Override
    public CrmUser getUser() {
        if (Objects.nonNull(crmUser)) {
            return crmUser;
        }
        throw ErrorCode.get(IamErrorCodes.CRM_USER_NOT_FOUND, "i18n.common.user.not.found");
    }

    /**
     * {@inheritDoc}
     *
     * @return The organization id
     */
    @Override
    public Long orgId() {
        return getUser().orgId();
    }

    /**
     * {@inheritDoc}
     *
     * @return the user ID
     */
    @Override
    public Long userId() {
        return getUser().id();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link ClientType}
     */
    public ClientType getClientType() {
        return clientType;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link I18NCRMResolutionAdapter}
     */
    @Override
    public I18NCRMResolutionAdapter getI18NCRMResolutionAdapter() {
        return i18NCRMResolutionAdapter;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents the organisation Id
     * @param i18nResolver Represents the {@link I18nResolver}
     */
    @Override
    public void initializeI18nResolutionAdapter(final Long orgId, final I18nResolver i18nResolver) {
        i18NCRMResolutionAdapter.initializeResolver(orgId, i18nResolver);
    }
}
