package com.tlc.crm.core.action.product;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.pair.Pair;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.list.JoinLoader;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.common.list.model.ListRequestData;

import com.tlc.crm.core.api.deal.manager.ProductDetailsHolderManager;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.field.product.ProductFields;
import com.tlc.crm.core.list.EntitySummaryListAction;
import com.tlc.crm.core.list.ProductSummarySQLListInterpreter;
import com.tlc.crm.core.list.ContactSummarySQLListInterpreter;
import com.tlc.crm.core.sql.resource.MDOPRODUCTDETAIL;
import com.tlc.crm.core.sql.resource.OMDOPRODUCTDETAILSHOLDER;

import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.filter.api.models.FilterConnector;
import com.tlc.crm.entity.filter.fields.CriteriaFields;
import com.tlc.crm.entity.filter.fields.CriteriaValueAttributes;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.common.list.model.ListType;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Process the list actions in the product summary
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public class ProductSummaryListAction extends EntitySummaryListAction {

    private final ProductDetailsHolderManager productDetailsHolderManager;

    public ProductSummaryListAction() {
        productDetailsHolderManager = ProductDetailsHolderManager.getInstance();
    }

    /**
     * <p>
     * Gets the {@link ContactSummarySQLListInterpreter} for the given request json and {@link Provider}.
     * </p>
     *
     * @param orgId                 Represents the organisation Id
     * @param userId                Represents the user Id
     * @param request               Represents the request {@link JsonObject}
     * @param provider              Represents the {@link Provider}
     * @param joinLoader            Represents the {@link JoinLoader}
     * @param i18NResolutionAdapter Represents the {@link I18nResolutionAdapter}.
     * @return {@link ProductSummarySQLListInterpreter}
     */
    @Override
    public ProductSummarySQLListInterpreter getEntityListInterpreter(final long orgId, final Long userId, final JsonObject request,
                                                                        final Provider provider, final JoinLoader joinLoader, final I18nResolutionAdapter i18NResolutionAdapter) {
        return new ProductSummarySQLListInterpreter(userId, orgId, provider, joinLoader, i18NResolutionAdapter, maxFieldSelectionLimit, maxFilterCriteriaLimit);
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Provider}
     */
    @Override
    public Provider getProvider(final ListRequestData listRequestData) {
        final Source source = getSource();

        return switch (source) {
            case DEAL -> Provider.DEAL_LIST;
            default -> throw ErrorCode.get(EntityErrorCodes.INVALID_SOURCE, "i18n.source.invalid");
        };
    }

    /**
     * <p>
     * Checks whether the field is enabled or not
     * </p>
     *
     * @param config Represents the {@link FieldConfig.Config}
     * @return True if the field is enabled, false otherwise
     */
    @Override
    public boolean isFieldEnabled(final FieldConfig.Config config, final Source source) {
        return config.isMandatoryField() || switch (source) {
            case DEAL -> config.isEnabledForProductSummaryDealList();
            default -> throw ErrorCode.get(EntityErrorCodes.INVALID_SOURCE, "i18n.source.invalid");
        };
    }

    /**
     * Performs the list action process.
     *
     * @param listRequestData Represents the {@link ListRequestData}
     * @param crmResponse     Refers the {@link CrmResponse} object.
     */
    @Override
    public void processListAction(final ListRequestData listRequestData, final CrmResponse crmResponse) {
        final JsonObject requestJson = listRequestData.getRequestJson();

        if (requestJson.optBoolean(ListActionFields.FETCH_DATA, true)) {
            final long productId = NumberUtils.parseNumber(listRequestData.getParameter(FieldKeys.ID.getName())).longValue();
            final Source source = getSource();

            final JsonArray criteriaArray = switch (source) {
                case DEAL -> getDealListCriteria(productId, requestJson);
                default -> null;
            };

            if (Objects.nonNull(criteriaArray)) {
                requestJson.put(CriteriaFields.CRITERIA.getName(), criteriaArray);
                listRequestData.setRequestJson(requestJson);
            }
        }
        super.processListAction(listRequestData, crmResponse);
    }

    /**
     * <p>
     * Loads the product related deal criteria
     * </p>
     *
     * @param productId     Represents the entity Id
     * @param selectQuery   Represent the {@link SelectQuery} to fetch deal list summary data
     */
    @Override
    public void loadDealEntityCriteria(final long productId, final SelectQuery selectQuery) {
        final Table dealTable = dealManager.getTable();
        final Table productDetailsHolderTable = productDetailsHolderManager.getTable();
        final Table productDetailsTable = productDetailsHolderManager.getProductDetailTable();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        selectQuery.addJoinClause(List.of(new JoinClause(dealTable.getPKColumn(), productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID), leftJoin),
                new JoinClause(productDetailsHolderTable.getPKColumn(), productDetailsTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID), leftJoin)));
        selectQuery.setWhereClause(WhereClauseUtil.connect(selectQuery.getWhereClause(), new WhereClause(Criteria.eq(productDetailsTable
                .getColumn(MDOPRODUCTDETAIL.PRODUCT_ID), productId)), Connector.AND));
    }

    /**
     * <p>
     * Gets the deal list criteria
     * </p>
     *
     * @param productId Represents the product Id
     * @return The deal list criteria json
     */
    private JsonArray getDealListCriteria(final long productId, final JsonObject requestJson) {
        final String idKey = ProductFields.ID;
        final Long productSourceId = fieldSourceService.getSource(Source.PRODUCT.getName()).id();
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Product.ENTITY_TYPE, idKey);

        return getDealCriteriaArray(productSourceId, productId, requestJson, Map.of(fields.get(idKey).id(), Pair.of(CriteriaValueAttributes.VALUE.getName(), FilterConnector.AND)));
    }

    /**
     * <p>
     * Gets the {@link ListType}
     * </p>
     *
     * @return The {@link ListType}
     */
    protected ListType getListType() {
        return ListType.PRODUCT_SUMMARY_DEAL_LIST;
    }
}