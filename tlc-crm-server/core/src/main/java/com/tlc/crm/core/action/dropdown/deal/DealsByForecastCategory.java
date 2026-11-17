package com.tlc.crm.core.action.dropdown.deal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.internal.IamErrorCodes;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.models.SortConfig;
import com.tlc.crm.common.models.SortType;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.internal.status.CoreErrorCodes;
import com.tlc.crm.entity.dropdown.AsyncDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.annotation.AsyncDropdown;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.forecastcategory.fields.ForecastCategoryFields;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.type.converter.MonetaryConverter;
import com.tlc.iam.client.UserService;
import com.tlc.iam.common.models.User;
import com.tlc.l10n.temporal.resolver.DateUtil;
import com.tlc.web.client.event.EventCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Manages {@link DealsByForecastCategory} related dropdown actions
 *
 * @author kishore J
 * @version 1.0
 */
@AsyncDropdown(name = "fetchDealsByForecastCategory")
public class DealsByForecastCategory implements AsyncDropdownAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(DealsByForecastCategory.class);

    private final UserService userService;
    private final OrganizationService organizationService;
    private final DealManager dealManager;
    private final DateUtil dateUtil;
    private final MonetaryConverter monetaryConverter;
    private final CrmUserConverter crmUserConverter;

    public DealsByForecastCategory() {
        dealManager = DealManager.getInstance();
        userService = UserService.getInstance();
        organizationService = OrganizationService.getInstance();
        dateUtil = DateUtil.getInstance();
        monetaryConverter = MonetaryConverter.getInstance();
        crmUserConverter = CrmUserConverter.getInstance();
    }

    /***
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @param crmResponse     Represents the {@link CrmResponse}
     */
    @Override
    public void fetchAndDispatchData(final DropdownRequest dropdownRequest, final CrmResponse crmResponse) {
        final Long orgId = dropdownRequest.getOrgId();
        final DropdownCriteria dropdownCriteria = dropdownRequest.getCriteria();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final long totalRecords = dealManager.getForecastCategoryDealCount(orgId, dropdownCriteria);

        if (totalRecords > NumericalConstant.ZERO && dropdownLimit.start() <= totalRecords) {
            final JsonObject data = dropdownRequest.data();
            final JsonObject sortJson = data.optJsonObject(DealFields.SORT);
            final SortConfig sortConfig;

            if (Objects.isNull(sortJson)) {
                sortConfig = new SortConfig(DealFields.TITLE, SortType.ASCENDING);
            } else {
                sortConfig = new SortConfig(
                        sortJson.getString(DealFields.FIELD),
                        SortType.getSortType(sortJson.getString(DealFields.TYPE))
                );
            }
            final Collection<Deal> deals = dealManager.getForecastCategoryDeals(orgId, dropdownCriteria, dropdownLimit, sortConfig);
            final Collection<CrmUser> crmUsers = deals.stream().map(Entity::getSalesOwner).toList();

            crmResponse.convertIntoAsync();
            final EventCallback<Collection<User>> eventCallback = new EventCallback<>() {

                @Override
                public void onSuccess(final Collection<User> users) {
                    try {
                        final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));
                        final Map<Long, CrmUser> crmUserMap = new HashMap<>(crmUsers.size());

                        for (final CrmUser crmUser : crmUsers) {
                            crmUser.setUser(userMap.get(crmUser.getIamId()));
                            crmUserMap.put(crmUser.id(), crmUser);
                        }
                        final JsonArray dealArray = Json.array();
                        final String titleKey = DealFields.TITLE;
                        final String expectedCloseDateKey = DealFields.EXPECTED_CLOSE_DATE;
                        final String dealValueKey = DealFields.DEAL_VALUE;
                        final String userKey = DealFields.USER;

                        for (final Deal deal : deals) {
                            final JsonObject dealJson = Json.object();

                            dealJson.put(ForecastCategoryFields.ID, deal.id());
                            dealJson.put(titleKey, deal.getTitle());
                            dealJson.put(expectedCloseDateKey, dateUtil.getTimeStamp(deal.getExpectedCloseDate()));

                            dealJson.put(dealValueKey, monetaryConverter.convertPartialData(deal.getDealValue()));
                            dealJson.put(userKey, crmUserConverter.convert(crmUserMap.get(deal.getOwnerId())));

                            dealArray.put(dealJson);
                        }
                        crmResponse.put(FieldKeys.OPTIONS.getName(), dealArray);
                        crmResponse.put(FieldKeys.SIZE.getName(), totalRecords);
                        crmResponse.put(FieldKeys.HAS_MORE.getName(), dropdownLimit.start() + dropdownLimit.limit() <= totalRecords);
                    } catch (final Exception exception) {
                        LoggerUtil.logError(LOGGER, exception, "Deal list forecast category : Failed to deals retrieval action. Reason: {}, Org Id: {}", exception.getMessage(), orgId);
                        crmResponse.setErrorCode(ErrorCode.get(CoreErrorCodes.DEALS_RETRIEVAL_ACTION_FAILED));
                    } finally {
                        crmResponse.send();
                    }
                }

                @Override
                public void onFailure(final Throwable cause) {
                    crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", cause));
                    crmResponse.send();
                }
            };
            userService.fetchUsers(organizationService.getIamOrgId(orgId), crmUsers.stream().map(CrmUser::getIamId).collect(Collectors.toSet()), eventCallback);
        } else {
            crmResponse.put(FieldKeys.OPTIONS.getName(), Collections.emptyList());
            crmResponse.put(FieldKeys.SIZE.getName(), NumericalConstant.ZERO);
            crmResponse.send();
        }
    }
}
