package com.tlc.crm.entity.filter.action.dropdown;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.page.Page;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.iam.service.UserService;

import com.tlc.crm.entity.dropdown.AsyncDropdownAction;
import com.tlc.crm.entity.dropdown.CriteriaType;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.annotation.AsyncDropdown;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.filter.api.manager.FilterManager;
import com.tlc.crm.entity.filter.api.models.Filter;
import com.tlc.crm.entity.filter.fields.FilterFields;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.models.settings.SourceSettings;
import com.tlc.crm.fields.service.settings.SourceSettingsService;
import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.iam.client.dropdown.SearchFilter;
import com.tlc.iam.common.models.User;

import com.tlc.iam.common.models.filter.UserStateFilter;
import com.tlc.web.client.event.EventCallback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages sales owner filter related dropdown actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@AsyncDropdown(name = "salesOwnerFilterList")
public class SalesOwnerFilterList implements AsyncDropdownAction {

    private static final String SALES_OWNER_FILTER_NAME_FORMAT = "%s's %s";

    private final FilterManager filterManager;
    private final UserService crmUserService;
    private final OrganizationService organizationService;
    private final com.tlc.iam.client.UserService userService;
    private final SourceSettingsService sourceSettingsService;

    public SalesOwnerFilterList() {
        filterManager = FilterManager.getInstance();
        crmUserService = UserService.getInstance();
        organizationService = OrganizationService.getInstance();
        userService = com.tlc.iam.client.UserService.getInstance();
        sourceSettingsService = SourceSettingsService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     */
    @Override
    public void fetchAndDispatchData(final DropdownRequest dropdownRequest, final CrmResponse crmResponse) {
        final Long orgId = dropdownRequest.getOrgId();
        final Long userId = dropdownRequest.getUserId();
        final JsonObject data = dropdownRequest.data();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final int start = dropdownLimit.start();
        final int limit = dropdownLimit.limit();
        final I18nResolutionAdapter i18NResolutionAdapter = dropdownRequest.getI18NResolutionAdapter();
        final SearchFilter searchFilter = new SearchFilter();
        final DropdownCriteria criteria = dropdownRequest.getCriteria();
        final CriteriaType criteriaType = criteria.getCriteriaType();
        final String criteriaValue = (String) criteria.getValue();
        final boolean isCriteriaNull = Objects.isNull(criteriaValue) || criteriaValue.isBlank();
        final SearchFilter stateFilter = new SearchFilter();
        final Collection<SearchFilter> searchFilters = new ArrayList<>();
        final String stateKey = FieldKeys.STATE.getName();
        final UserStateFilter userStateFilter = data.containsKey(stateKey) ? UserStateFilter.get(data.getString(stateKey)) : UserStateFilter.ALL_USERS;
        final Long sourceId = data.getLong(FilterFields.SOURCE_ID.getName());

        stateFilter.setType(stateKey);
        stateFilter.setValue(userStateFilter);
        searchFilters.add(stateFilter);
        searchFilter.setStart(start);
        searchFilter.setLimit(limit);
        searchFilter.setType(Objects.isNull(criteriaType) ? null : CriteriaType.getCriteriaType(criteriaType));
        searchFilter.setValue(isCriteriaNull ? null : criteriaValue);
        searchFilters.add(searchFilter);
        crmResponse.convertIntoAsync();

        if (isCriteriaNull) {
            filterManager.getRecentlyAppliedFilterUserId(orgId, userId, sourceId)
                    .ifPresent(salesOwnerId -> stateFilter.setUserIds(Set.of(crmUserService.getUser(orgId, salesOwnerId).getIamId())));
        }

        final EventCallback<Page<User>> usersFetchCallBack = new EventCallback<>() {

            @Override
            public void onSuccess(final Page<User> userPage) {
                final JsonArray jsonArray = Json.array();
                final int totalRecords = userPage.getNumber();
                final Collection<User> users = userPage.getData();

                if (!users.isEmpty()) {
                    final Collection<Long> iamUserIds = users.stream().map(User::id).collect(Collectors.toUnmodifiableSet());
                    final Collection<CrmUser> crmUsers = crmUserService.getUsersByIamId(orgId, iamUserIds);
                    final int size = crmUsers.size();
                    final Collection<Long> crmUserIds = new HashSet<>(size);
                    final Map<Long, CrmUser> userMapByIamIds = new HashMap<>(size);

                    for (final CrmUser crmUser : crmUsers) {
                        crmUserIds.add(crmUser.id());
                        userMapByIamIds.put(crmUser.getIamId(), crmUser);
                    }

                    final Map<Long, Filter> filterMap = filterManager.getSalesOwnerFilters(orgId, userId, sourceId, crmUserIds).stream()
                            .collect(Collectors.toMap(filter -> NumberUtils.parseNumber(filter.getName()).longValue(), Function.identity()));

                    for (final User user : users) {
                        final CrmUser crmUser = userMapByIamIds.get(user.id());
                        final User.State userState = user.getUserState();
                        final Filter filter = filterMap.get(crmUser.id());

                        if (Objects.nonNull(filter)) {
                            if (User.State.ACTIVE == userState || User.State.DEACTIVATED == userState) {
                                final String fullName = user.getFullName();
                                final FieldSource fieldSource = filter.getFilterProvider().getFieldSource();
                                final SourceSettings sourceSettings = sourceSettingsService.getSourceSettingsBySource(orgId, fieldSource.getName());
                                final String alternateName = sourceSettings.getAlternateName();

                                filter.setName(String.format(SALES_OWNER_FILTER_NAME_FORMAT, fullName, Objects.isNull(alternateName) ?
                                        i18NResolutionAdapter.get(fieldSource.getDisplayName()) : alternateName));
                                final JsonObject jsonObject = convertFilterIntoJson(filter);

                                jsonObject.put(FilterFields.USER_NAME.getName(), fullName);
                                jsonArray.put(jsonObject);
                            }
                        }
                    }
                }
                crmResponse.put(FieldKeys.TOTAL_RECORDS.getName(), totalRecords);
                crmResponse.put(FieldKeys.HAS_MORE.getName(), start + limit <= totalRecords);
                crmResponse.put(FieldKeys.SIZE.getName(), jsonArray.size());
                crmResponse.put(FieldKeys.OPTIONS.getName(), jsonArray);
                crmResponse.send();
            }

            @Override
            public void onFailure(final Throwable throwable) {
                crmResponse.put(FieldKeys.OPTIONS.getName(), Json.array());
                crmResponse.put(FieldKeys.SIZE.getName(), 0);
                crmResponse.send();
            }
        };
        userService.fetchUserPageByState(organizationService.getIamOrgId(orgId), searchFilters, usersFetchCallBack);
    }

    /**
     * <p>
     * Converts the {@link Filter} into {@link JsonObject}.
     * </p>
     *
     * @param filter Which holds the values of filter.
     * @return Converted filter.
     */
    private JsonObject convertFilterIntoJson(final Filter filter) {
        final JsonObject filterJson = Json.object();

        filterJson.put(FilterFields.ID.getName(), filter.id());
        filterJson.put(FilterFields.NAME.getName(), filter.getName());
        filterJson.put(FilterFields.IS_FAVORITE.getName(), filter.isFavorite());
        filterJson.put(FilterFields.IS_DEFAULT.getName(), filter.isDefault());
        filterJson.put(FilterFields.IS_ACTIVE.getName(), filter.isRecentlyApplied());
        filterJson.put(FilterFields.IS_EDITABLE.getName(), false);
        filterJson.put(FilterFields.IS_DELETABLE.getName(), false);
        filterJson.put(FilterFields.IS_SHAREABLE.getName(), false);

        return filterJson;
    }
}