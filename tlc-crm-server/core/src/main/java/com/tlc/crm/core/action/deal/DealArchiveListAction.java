package com.tlc.crm.core.action.deal;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.list.JoinLoader;
import com.tlc.crm.common.list.ListInterpreter;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.list.DealArchiveSQLListInterpreter;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.common.list.model.ListType;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.web.WebAction;

/**
 * <p>
 * Process the deal archive list action
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
@SourceAuthorizer(subCategory = SubCategory.Type.DEALS, validateFieldPermission = true)
@WebAction(path = "/deal/archive/list")
public class DealArchiveListAction extends DealListAction {

    private final DealManager dealManager;

    public DealArchiveListAction() {
        dealManager = DealManager.getInstance();
    }

    /**
     * <p>
     * Gets the {@link DealArchiveSQLListInterpreter} for the given request json and {@link Provider}.
     * </p>
     *
     * @param request                 Represents the request {@link JsonObject}
     * @param provider                Represents the {@link Provider}
     * @param joinLoader              Represents the {@link JoinLoader}
     * @param i18NResolutionAdapter Represents the {@link I18nResolutionAdapter}.
     * @return {@link DealArchiveSQLListInterpreter}
     */
    @Override
    protected DealArchiveSQLListInterpreter getEntityListInterpreter(final long orgId, final Long userId, final JsonObject request,
            final Provider provider, final JoinLoader joinLoader, final I18nResolutionAdapter i18NResolutionAdapter) {
        return new DealArchiveSQLListInterpreter(userId, orgId, provider, joinLoader, i18NResolutionAdapter, maxFieldSelectionLimit, maxFilterCriteriaLimit);
    }

    /**
     * {@inheritDoc}
     *
     * @param config Represents the {@link FieldConfig.Config}
     * @return True if the field is enabled, false otherwise
     */
    @Override
    protected boolean isFieldEnabled(final FieldConfig.Config config, final Source source) {
        return config.isMandatoryField() || config.isEnabledForDealArchiveList();
    }

    /**
     * <p>
     * Applies the default criteria for the entity list action
     * </p>
     *
     * @param listInterpreter Represents the {@link ListInterpreter}
     */
    @Override
    protected void applyDefaultCriteria(final ListInterpreter listInterpreter) {
        listInterpreter.setWhereClause(new WhereClause(Criteria.eq(dealManager.getTable().getColumn(MDODEAL.STATUS), Deal.Status.ARCHIVED.getId())));
    }

    /**
     * <p>
     * Gets the {@link ListType}
     * </p>
     *
     * @return The {@link ListType}
     */
    protected ListType getListType() {
        return ListType.DEAL_ARCHIVE_LIST;
    }
}