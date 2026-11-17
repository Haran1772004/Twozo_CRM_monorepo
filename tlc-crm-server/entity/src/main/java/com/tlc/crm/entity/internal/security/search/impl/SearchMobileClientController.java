package com.tlc.crm.entity.internal.security.search.impl;

import com.tlc.commons.Counter;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.entity.internal.security.search.filter.PresetSearchFilter;
import com.tlc.crm.entity.internal.security.search.filter.mobile.PresetSearchFilterMobile;
import com.tlc.crm.entity.search.api.model.Index;
import com.tlc.crm.entity.search.api.model.RecentlyViewedEntriesContext;
import com.tlc.crm.entity.search.fields.CommonSearchFields;
import com.tlc.crm.entity.search.loader.SearchRequestLoader;
import com.tlc.crm.security.iam.BaseCrmUser;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.Scope;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.search.model.Comparator;
import com.tlc.search.model.Criteria;
import com.tlc.search.model.FilterClause;
import com.tlc.search.model.SearchRequest;
import com.tlc.search.request.index.multi.read.MultiSingleIndexRequest;
import com.tlc.search.request.index.single.read.SingleIndexSearchRequest;
import com.tlc.search.service.ReadableDocumentService;

import java.util.Collections;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Controller for search api mobile client requests.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class SearchMobileClientController extends AbstractSearchAccessController {

    private final SearchRequestLoader searchRequestLoader;

    private SearchMobileClientController() {
        searchRequestLoader = SearchRequestLoader.getMobileClientRequestLoader();
    }

    private static final class Instance {
        private static final SearchMobileClientController INSTANCE = new SearchMobileClientController();
    }

    /**
     * Gets the {@link SearchMobileClientController} instance.
     *
     * @return {@link SearchMobileClientController}.
     */
    public static SearchMobileClientController getInstance() {
        return SearchMobileClientController.Instance.INSTANCE;
    }

    /**
     * <p>
     * Builds recently viewed requests.
     * </p>
     *
     * @param viewedEntities Holds relevant index and it's ids.
     * @return {@link Collection} of search requests.
     */
    public RecentlyViewedEntriesContext build(final Map<Index, Collection<Long>> viewedEntities) {
        final Collection<SingleIndexSearchRequest> singleIndexRequests = new ArrayList<>();
        final Map<String, Boolean> permissibleByIndex = new HashMap<>();
        final BaseCrmUser user = LoggedInUserInfo.getUser();
        final Long orgId = user.orgId();
        final Long userId = user.id();

        if (isAdmin(orgId, userId)) {
            return buildGloballyAccessibleSearchRequests(viewedEntities, searchRequestLoader, orgId);
        }
        final Long roleId = user.getRoleId();
        final Collection<Long> territoryIds = user.getTerritoryIds();
        final ReadableDocumentService docService = service.getReadableService(orgId);
        final boolean isProductEnabled = isProductEnabled(orgId);

        final RolePermission contactPermission = getSourcesPermission(orgId, roleId, SubCategory.Type.CONTACTS);
        final RolePermission companyPermission = getSourcesPermission(orgId, roleId, SubCategory.Type.COMPANIES);
        final RolePermission dealPermission = getSourcesPermission(orgId, roleId, SubCategory.Type.DEALS);
        final RolePermission productPermission = isProductEnabled ?
                getSourcesPermission(orgId, roleId, SubCategory.Type.PRODUCTS) : null;

        final boolean areContactsAccessible = contactPermission.isGranted();
        final boolean areCompaniesAccessible = companyPermission.isGranted();
        final boolean areDealsAccessible = dealPermission.isGranted();
        final boolean areProductsAccessible = isProductEnabled && productPermission.isGranted();

        final Scope dealScope = dealPermission.getScope();
        final Scope contactScope = contactPermission.getScope();
        final Scope companyScope = companyPermission.getScope();
        final Scope productScope = isProductEnabled ? productPermission.getScope() : null;

        final boolean areContactsGloballyAccessible;
        final boolean areCompaniesGloballyAccessible;
        final boolean areDealsGloballyAccessible;

        final Map<String, Map<String, Object>> contactPermissionMap;
        final Map<String, Map<String, Object>> companyPermissionMap;
        final Map<String, Map<String, Object>> dealPermissionMap;
        final Map<String, Map<String, Object>> emptyMap = Collections.emptyMap();

        final boolean isContactRecordsFollower = getFollowersPermission(orgId, roleId, SubCategory.Type.CONTACT_FOLLOWERS_RECORDS).isGranted();
        final boolean isCompanyRecordsFollower = getFollowersPermission(orgId, roleId, SubCategory.Type.COMPANY_FOLLOWERS_RECORDS).isGranted();
        final boolean isDealRecordsFollower = getFollowersPermission(orgId, roleId, SubCategory.Type.DEAL_FOLLOWERS_RECORDS).isGranted();

        if (dealPermission.isGranted()) {
            areDealsGloballyAccessible = Scope.GLOBAL_ACCESS == dealScope;
            dealPermissionMap = areDealsGloballyAccessible ? emptyMap : buildParamsMap(CommonSearchFields.DEAL,
                    dealScope, userId, isDealRecordsFollower, territoryIds);
        } else {
            areDealsGloballyAccessible = false;
            dealPermissionMap = null;
        }

        if (contactPermission.isGranted()) {
            areContactsGloballyAccessible = Scope.GLOBAL_ACCESS == contactScope;
            contactPermissionMap = areContactsGloballyAccessible ? emptyMap : buildParamsMap(CommonSearchFields.CONTACT,
                    contactScope, userId, isContactRecordsFollower, territoryIds);
        } else {
            areContactsGloballyAccessible = false;
            contactPermissionMap = null;
        }

        if (companyPermission.isGranted()) {
            areCompaniesGloballyAccessible = Scope.GLOBAL_ACCESS == companyScope;
            companyPermissionMap = areCompaniesGloballyAccessible ? emptyMap : buildParamsMap(CommonSearchFields.COMPANY,
                    companyScope, userId, isCompanyRecordsFollower, territoryIds);
        } else {
            areCompaniesGloballyAccessible = false;
            companyPermissionMap = null;
        }
        final boolean areAllEntitiesGloballyAccessible = areContactsGloballyAccessible && areCompaniesGloballyAccessible
                && areDealsGloballyAccessible;

        for (final Map.Entry<Index, Collection<Long>> recentlyViewedEntity : viewedEntities.entrySet()) {
            final Index index = recentlyViewedEntity.getKey();
            final Collection<Long> entryIds = recentlyViewedEntity.getValue();
            final String indexName = index.name();
            final SearchRequest searchRequest = new SearchRequest();
            final FilterClause filterClause = new FilterClause(new Criteria(entryIds, Comparator.EQUAL, CommonSearchFields.ID));

            searchRequest.setFilter(filterClause);

            switch (indexName) {
                case CommonSearchFields.CONTACT -> {
                    if (areContactsAccessible && NumericalConstant.ZERO < getCount(getPermissionBasedFilter(
                                    userId, territoryIds, filterClause, contactScope, isContactRecordsFollower),
                            CommonSearchFields.CONTACT, docService)) {
                        searchRequestLoader.loadContactIndexRequest(searchRequest, companyPermissionMap);
                        permissibleByIndex.put(indexName, true);
                        singleIndexRequests.add(SingleIndexSearchRequest.get(indexName, searchRequest));
                    }
                }
                case CommonSearchFields.COMPANY -> {
                    if (areCompaniesAccessible && NumericalConstant.ZERO < getCount(getPermissionBasedFilter(
                                    userId, territoryIds, filterClause, companyScope, isCompanyRecordsFollower),
                            CommonSearchFields.COMPANY, docService)) {
                        searchRequestLoader.loadCompanyIndexRequest(searchRequest);
                        permissibleByIndex.put(indexName, true);
                        singleIndexRequests.add(SingleIndexSearchRequest.get(indexName, searchRequest));
                    }
                }
                case CommonSearchFields.DEAL -> {
                    if (areDealsAccessible && NumericalConstant.ZERO < getCount(getPermissionBasedFilter(
                                    userId, territoryIds, filterClause, dealPermission.getScope(), isDealRecordsFollower),
                            CommonSearchFields.DEAL, docService)) {
                        searchRequestLoader.loadDealIndexRequest(searchRequest, contactPermissionMap, companyPermissionMap);
                        permissibleByIndex.put(indexName, true);
                        singleIndexRequests.add(SingleIndexSearchRequest.get(indexName, searchRequest));
                    }
                }
                case CommonSearchFields.ACTIVITY -> {
                    final FilterClause activityFilter = filterClause.and(getOwnerAndTerritoryIdsFilter(userId, territoryIds));

                    if (NumericalConstant.ZERO < getCount(activityFilter, CommonSearchFields.ACTIVITY, docService)) {
                        searchRequestLoader.loadActivityIndexRequest(searchRequest, null, null, null);
                        permissibleByIndex.put(indexName, true);
                        singleIndexRequests.add(SingleIndexSearchRequest.get(indexName, searchRequest));
                    }
                }
                case CommonSearchFields.PRODUCT -> {
                    if (areProductsAccessible && NumericalConstant.ZERO < getCount(getPermissionBasedFilter(
                                    userId, territoryIds, filterClause, productScope, false),
                            indexName, docService)) {
                        searchRequestLoader.loadProductIndexRequest(searchRequest);
                        permissibleByIndex.put(indexName, true);
                        singleIndexRequests.add(SingleIndexSearchRequest.get(indexName, searchRequest));
                    }
                }
                case CommonSearchFields.FILE, CommonSearchFields.NOTE -> {
                    if (NumericalConstant.ZERO < getCount(getEntitiesScopeAppendedFilter(filterClause, dealPermissionMap,
                            contactPermissionMap, companyPermissionMap), indexName, docService)) {
                        if (areAllEntitiesGloballyAccessible) {
                            searchRequestLoader.loadRequestForGlobalAccessibleModules(indexName, searchRequest);
                        } else {
                            searchRequestLoader.loadNoteAndFileIndexRequest(searchRequest, contactPermissionMap, companyPermissionMap, indexName);
                        }
                        permissibleByIndex.put(indexName, true);
                        singleIndexRequests.add(SingleIndexSearchRequest.get(indexName, searchRequest));
                    }
                }
            }
        }

        final MultiSingleIndexRequest multiSearchRequest = singleIndexRequests.isEmpty()
                ? null
                : MultiSingleIndexRequest.get(singleIndexRequests);

        return RecentlyViewedEntriesContext.get(multiSearchRequest, permissibleByIndex);
    }

    /**
     * <p>
     * Builds search requests for mobile.
     * </p>
     *
     * @param indexes    Index names.
     * @param searchText Searched text.
     * @param start      Indicates the point from where doc should be fetched.
     * @param fetchCount Total amount of docs to be fetched.
     * @return {@link Collection} of {@link SearchRequest}.
     */
    @Override
    public MultiSingleIndexRequest build(final Collection<String> indexes, final String searchText,
                                         final int start, final int fetchCount, final Counter totalDocCounter) {
        final Collection<SingleIndexSearchRequest> singleIndexRequests = new ArrayList<>();
        final BaseCrmUser user = LoggedInUserInfo.getUser();
        final Long orgId = user.orgId();
        final Long userId = user.id();
        final PresetSearchFilterMobile presetSearchFilterMobile = PresetSearchFilterMobile.getInstance(searchText);
        final ReadableDocumentService docService = service.getReadableService(orgId);

        if (isAdmin(orgId, userId)) {
            return buildGloballyAccessibleSearchRequests(presetSearchFilterMobile, docService, indexes, orgId);
        }
        final Long roleId = user.getRoleId();
        final Collection<Long> territoryIds = user.getTerritoryIds();
        final Map<String, FilterClause> filterMap = new HashMap<>();

        final RolePermission contactPermission = getSourcesPermission(orgId, roleId, SubCategory.Type.CONTACTS);
        final RolePermission companyPermission = getSourcesPermission(orgId, roleId, SubCategory.Type.COMPANIES);
        final RolePermission dealPermission = getSourcesPermission(orgId, roleId, SubCategory.Type.DEALS);

        final boolean areContactsGloballyAccessible;
        final boolean areCompaniesGloballyAccessible;
        final boolean areDealsGloballyAccessible;

        final Map<String, Map<String, Object>> contactPermissionMap;
        final Map<String, Map<String, Object>> companyPermissionMap;
        final Map<String, Map<String, Object>> dealPermissionMap;
        final Map<String, Map<String, Object>> emptyMap = Collections.emptyMap();

        final boolean isContactRecordsFollower = getFollowersPermission(orgId, roleId, SubCategory.Type.CONTACT_FOLLOWERS_RECORDS).isGranted();
        final boolean isCompanyRecordsFollower = getFollowersPermission(orgId, roleId, SubCategory.Type.COMPANY_FOLLOWERS_RECORDS).isGranted();
        final boolean isDealRecordsFollower = getFollowersPermission(orgId, roleId, SubCategory.Type.DEAL_FOLLOWERS_RECORDS).isGranted();

        if (dealPermission.isGranted()) {
            final Scope dealScope = dealPermission.getScope();

            areDealsGloballyAccessible = Scope.GLOBAL_ACCESS == dealScope;
            dealPermissionMap = areDealsGloballyAccessible ? emptyMap : buildParamsMap(CommonSearchFields.DEAL,
                    dealScope, userId, isDealRecordsFollower, territoryIds);
            filterMap.put(CommonSearchFields.DEAL, getPermissionBasedFilter(
                    userId, territoryIds, presetSearchFilterMobile.getDealFilter(), dealScope, isDealRecordsFollower));
        } else {
            dealPermissionMap = null;
            areDealsGloballyAccessible = false;
        }

        if (contactPermission.isGranted()) {
            final Scope contactScope = contactPermission.getScope();

            areContactsGloballyAccessible = Scope.GLOBAL_ACCESS == contactScope;
            contactPermissionMap = areContactsGloballyAccessible ? emptyMap : buildParamsMap(CommonSearchFields.CONTACT,
                    contactScope, userId, isContactRecordsFollower, territoryIds);
            filterMap.put(CommonSearchFields.CONTACT, getPermissionBasedFilter(
                    userId, territoryIds, presetSearchFilterMobile.getContactFilter(), contactScope, isContactRecordsFollower));
        } else {
            contactPermissionMap = null;
            areContactsGloballyAccessible = false;
        }

        if (companyPermission.isGranted()) {
            final Scope companyScope = companyPermission.getScope();

            areCompaniesGloballyAccessible = Scope.GLOBAL_ACCESS == companyScope;
            companyPermissionMap = areCompaniesGloballyAccessible ? emptyMap : buildParamsMap(CommonSearchFields.COMPANY,
                    companyScope, userId, isCompanyRecordsFollower, territoryIds);
            filterMap.put(CommonSearchFields.COMPANY, getPermissionBasedFilter(
                    userId, territoryIds, presetSearchFilterMobile.getCompanyFilter(), companyScope, isCompanyRecordsFollower));
        } else {
            companyPermissionMap = null;
            areCompaniesGloballyAccessible = false;
        }

        filterMap.put(CommonSearchFields.ACTIVITY, presetSearchFilterMobile.getActivityFilter().and(
                getOwnerAndTerritoryIdsFilter(userId, territoryIds)));

        if (isProductEnabled(orgId)) {
            final RolePermission productPermission = getSourcesPermission(orgId, roleId, SubCategory.Type.PRODUCTS);

            if (productPermission.isGranted()) {
                filterMap.put(CommonSearchFields.PRODUCT, getPermissionBasedFilter(userId, territoryIds,
                        presetSearchFilterMobile.getProductFilter(), productPermission.getScope(), false));
            }
        }

        if (areContactsGloballyAccessible && areCompaniesGloballyAccessible && areDealsGloballyAccessible) {
            loadNoteAndFileFilter(presetSearchFilterMobile.getNoteAndFileFilter(), filterMap);
        } else {
            final FilterClause entitiesFilter = getEntitiesScopeAppendedFilter(presetSearchFilterMobile.getNoteAndFileFilter(),
                    dealPermissionMap, contactPermissionMap, companyPermissionMap);

            if (Objects.nonNull(entitiesFilter)) {
                loadNoteAndFileFilter(entitiesFilter, filterMap);
            }
        }
        final Map<String, FilterClause> sortedFilterMap = indexes.stream().filter(filterMap::containsKey).collect(
                Collectors.toMap(index -> index, filterMap::get));
        final Map<String, Integer> countMap = getCountMap(sortedFilterMap, docService);

        for (final Map.Entry<String, Integer> countMapEntry : countMap.entrySet()) {
            final String index = countMapEntry.getKey();
            final int docCount = countMap.get(index);

            if (start < docCount) {
                singleIndexRequests.add(getSearchRequest(new SearchContext(NumericalConstant.ZERO, NumericalConstant.FOUR,
                                countMap.get(index), index, sortedFilterMap.get(index), docService),
                        dealPermissionMap, contactPermissionMap, companyPermissionMap, searchRequestLoader));
            }
        }

        return singleIndexRequests.isEmpty() ? null : MultiSingleIndexRequest.get(singleIndexRequests);
    }

    /**
     * <p>
     * Builds {@link SearchRequest}.
     * </p>
     *
     * @param searchText Searched text.
     * @param index      Represents the index name.
     * @param start      Indicates the point from where doc should be fetched.
     * @param fetchCount Total amount of docs to be fetched.
     * @return {@link SearchRequest}.
     */
    @Override
    public SingleIndexSearchRequest build(final String index, final String searchText, final int start, final int fetchCount) {

        return buildSingleIndexRequest(start, fetchCount, index, PresetSearchFilterMobile.getInstance(searchText), searchRequestLoader);
    }

    /**
     * <p>
     * Gets activity search request.
     * </p>
     *
     * @param start              Start point.
     * @param fetchCount         Docs to be fetched.
     * @param index              Index name.
     * @param presetSearchFilter {@link PresetSearchFilter}.
     * @return {@link SingleIndexSearchRequest}.
     */
    protected SingleIndexSearchRequest buildActivityIndexRequest(final int start, final int fetchCount, final String index,
                                                                 final PresetSearchFilter presetSearchFilter) {
        final BaseCrmUser user = LoggedInUserInfo.getUser();
        final Long orgId = user.orgId();
        final Long userId = user.id();
        final ReadableDocumentService documentService = service.getReadableService(orgId);
        final FilterClause activityFilter = presetSearchFilter.getActivityFilter();

        if (isAdmin(orgId, userId)) {
            final int totalCount = getCount(activityFilter, index, documentService);

            if (start < totalCount) {
                final SearchRequest searchRequest = buildSearchRequest(new SearchContext(start, fetchCount,
                        getCount(activityFilter, index, documentService), index, activityFilter, documentService));
                searchRequestLoader.loadRequestForGlobalAccessibleModules(index, searchRequest);

                return SingleIndexSearchRequest.get(index, searchRequest);
            }
            return null;
        }
        final Collection<Long> territoryIds = user.getTerritoryIds();
        final FilterClause activityPermissionFilter = activityFilter.and(getOwnerAndTerritoryIdsFilter(userId, territoryIds));
        final int totalCount = getCount(activityPermissionFilter, CommonSearchFields.ACTIVITY, documentService);

        if (start < totalCount) {
            final SearchRequest searchRequest = buildSearchRequest(new SearchContext(start, fetchCount, totalCount,
                    CommonSearchFields.ACTIVITY, activityFilter, documentService));

            searchRequestLoader.loadActivityIndexRequest(searchRequest, null, null, null);

            return SingleIndexSearchRequest.get(index, searchRequest);
        } else {
            return null;
        }
    }

    /**
     * <p>
     * Builds search requests for globally accessed modules.
     * </p>
     *
     * @param presetSearchFilter {@link PresetSearchFilter}.
     * @param documentService    {@link ReadableDocumentService}.
     * @return {@link MultiSingleIndexRequest}.
     */
    private MultiSingleIndexRequest buildGloballyAccessibleSearchRequests(
            final PresetSearchFilter presetSearchFilter, final ReadableDocumentService documentService,
            final Collection<String> indexes, final Long orgId) {
        final Map<String, FilterClause> filterClauseMap = new LinkedHashMap<>();

        for (final String index : indexes) {

            switch (index) {
                case CommonSearchFields.DEAL -> filterClauseMap.put(index, presetSearchFilter.getDealFilter());
                case CommonSearchFields.CONTACT -> filterClauseMap.put(index, presetSearchFilter.getContactFilter());
                case CommonSearchFields.COMPANY -> filterClauseMap.put(index, presetSearchFilter.getCompanyFilter());
                case CommonSearchFields.ACTIVITY -> filterClauseMap.put(index, presetSearchFilter.getActivityFilter());
                case CommonSearchFields.PRODUCT -> {
                    if (isProductEnabled(orgId)) {
                        filterClauseMap.put(index, presetSearchFilter.getProductFilter());
                    }
                }
                case CommonSearchFields.FILE, CommonSearchFields.NOTE ->
                        filterClauseMap.put(index, presetSearchFilter.getNoteAndFileFilter());
            }
        }

        return buildGloballyAccessibleSearchRequests(filterClauseMap, documentService);
    }

    /**
     * <p>
     * Builds search requests for globally accessed modules.
     * </p>
     *
     * @param filterClauseMap Map holding index and it's filter.
     * @param documentService {@link ReadableDocumentService}.
     * @return {@link MultiSingleIndexRequest}.
     */
    private MultiSingleIndexRequest buildGloballyAccessibleSearchRequests(
            final Map<String, FilterClause> filterClauseMap, final ReadableDocumentService documentService) {
        final Collection<SingleIndexSearchRequest> singleIndexRequests = new ArrayList<>();
        final Map<String, Integer> countMap = getCountMap(filterClauseMap, documentService);

        for (final Map.Entry<String, Integer> countMapEntry : countMap.entrySet()) {
            final int count = countMapEntry.getValue();

            if (NumericalConstant.ZERO < count) {
                final String index = countMapEntry.getKey();

                final SearchRequest searchRequest = buildSearchRequest(new SearchContext(NumericalConstant.ZERO,
                        NumericalConstant.FOUR, count, index, filterClauseMap.get(index), documentService));

                searchRequestLoader.loadRequestForGlobalAccessibleModules(index, searchRequest);
                singleIndexRequests.add(SingleIndexSearchRequest.get(index, searchRequest));
            }
        }

        return MultiSingleIndexRequest.get(singleIndexRequests);
    }
}