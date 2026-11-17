package com.tlc.crm.entity.internal.security.search.impl;

import com.tlc.commons.Counter;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.entity.internal.security.search.filter.PresetSearchFilter;
import com.tlc.crm.entity.internal.security.search.filter.PresetSearchFilterWeb;
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

import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * <p>
 * Controller for search api web client requests.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class SearchWebClientController extends AbstractSearchAccessController {

    private final SearchRequestLoader searchRequestLoader;

    private SearchWebClientController() {
        searchRequestLoader = SearchRequestLoader.getWebClientRequestLoader();
    }

    private static final class Instance {
        private static final SearchWebClientController INSTANCE = new SearchWebClientController();
    }

    /**
     * Gets the {@link SearchWebClientController} instance.
     *
     * @return {@link SearchWebClientController}.
     */
    public static SearchWebClientController getInstance() {
        return Instance.INSTANCE;
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
        final Collection<SingleIndexSearchRequest> singleIndexSearchRequests = new ArrayList<>();
        final Map<String, Boolean> permissibleByIndex = new HashMap<>();
        final BaseCrmUser user = LoggedInUserInfo.getUser();
        final Long orgId = user.orgId();
        final Long userId = user.id();

        if (isAdmin(orgId, userId)) {
            return buildGloballyAccessibleSearchRequests(viewedEntities, searchRequestLoader, orgId);
        }
        final ReadableDocumentService readableDocumentService = service.getReadableService(orgId);
        final Collection<Long> territoryIds = user.getTerritoryIds();
        final Long roleId = user.getRoleId();
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
            boolean isPermissibleFetch = false;

            switch (indexName) {
                case CommonSearchFields.CONTACT -> {
                    if (!areContactsAccessible || NumericalConstant.ZERO == getCount(getPermissionBasedFilter(
                                    userId, territoryIds, filterClause, contactScope, isContactRecordsFollower),
                            indexName, readableDocumentService)) {
                        setFieldsForNonAccessibleDocs(searchRequest);
                    } else {
                        searchRequestLoader.loadContactIndexRequest(searchRequest, companyPermissionMap);
                        isPermissibleFetch = true;
                    }
                }
                case CommonSearchFields.COMPANY -> {
                    if (!areCompaniesAccessible || NumericalConstant.ZERO == getCount(getPermissionBasedFilter(
                                    userId, territoryIds, filterClause, companyScope, isCompanyRecordsFollower),
                            indexName, readableDocumentService)) {
                        setFieldsForNonAccessibleDocs(searchRequest);
                    } else {
                        searchRequestLoader.loadCompanyIndexRequest(searchRequest);
                        isPermissibleFetch = true;
                    }
                }
                case CommonSearchFields.DEAL -> {
                    if (!areDealsAccessible || NumericalConstant.ZERO == getCount(getPermissionBasedFilter(
                                    userId, territoryIds, filterClause, dealScope, isDealRecordsFollower),
                            indexName, readableDocumentService)) {
                        setFieldsForNonAccessibleDocs(searchRequest);
                    } else {
                        searchRequestLoader.loadDealIndexRequest(searchRequest, contactPermissionMap, companyPermissionMap);
                        isPermissibleFetch = true;
                    }
                }
                case CommonSearchFields.ACTIVITY -> {
                    final FilterClause activityFilter;

                    activityFilter = filterClause.and(getOwnerAndTerritoryIdsFilter(userId, territoryIds));

                    if (NumericalConstant.ZERO == getCount(activityFilter, indexName, readableDocumentService)) {
                        setFieldsForNonAccessibleDocs(searchRequest);
                        searchRequest.includeField(CommonSearchFields.ACTIVITY_TYPE);
                    } else {
                        searchRequestLoader.loadActivityIndexRequest(searchRequest, dealPermissionMap, contactPermissionMap, companyPermissionMap);
                        isPermissibleFetch = true;
                    }
                }
                case CommonSearchFields.PRODUCT -> {
                    if (!areProductsAccessible || NumericalConstant.ZERO == getCount(getPermissionBasedFilter(
                            userId, territoryIds, filterClause, productScope, false), indexName, readableDocumentService)) {
                        setFieldsForNonAccessibleDocs(searchRequest);
                    } else {
                        searchRequestLoader.loadProductIndexRequest(searchRequest);
                        isPermissibleFetch = true;
                    }
                }
                case CommonSearchFields.FILE, CommonSearchFields.NOTE -> {
                    if (NumericalConstant.ZERO == getCount(getEntitiesScopeAppendedFilter(filterClause, dealPermissionMap,
                            contactPermissionMap, companyPermissionMap), indexName, readableDocumentService)) {
                        searchRequest.includeField(CommonSearchFields.ID);
                        searchRequest.includeField(CommonSearchFields.NAME);
                    } else if (areAllEntitiesGloballyAccessible) {
                        searchRequestLoader.loadRequestForGlobalAccessibleModules(indexName, searchRequest);
                        isPermissibleFetch = true;
                    } else {
                        searchRequestLoader.loadNoteAndFileIndexRequest(searchRequest, contactPermissionMap, companyPermissionMap, indexName);
                        isPermissibleFetch = true;
                    }
                }
            }
            permissibleByIndex.put(indexName, isPermissibleFetch);
            singleIndexSearchRequests.add(SingleIndexSearchRequest.get(indexName, searchRequest));
        }

        final MultiSingleIndexRequest multiSearchRequest = singleIndexSearchRequests.isEmpty() ?
                null : MultiSingleIndexRequest.get(singleIndexSearchRequests);

        return RecentlyViewedEntriesContext.get(multiSearchRequest, permissibleByIndex);
    }

    /**
     * <p>
     * Builds search requests.
     * </p>
     *
     * @param indexes    Index names.
     * @param searchText Searched text.
     * @param start      Indicates the point from where doc should be fetched.
     * @param limit      Total amount of docs to be fetched.
     * @return {@link MultiSingleIndexRequest}.
     */
    public MultiSingleIndexRequest build(final Collection<String> indexes, final String searchText,
                                         final int start, final int limit, final Counter totalDocCounter) {
        final BaseCrmUser user = LoggedInUserInfo.getUser();
        final Long orgId = user.orgId();
        final Long userId = user.id();
        final PresetSearchFilterWeb presetSearchFilterWeb = PresetSearchFilterWeb.getInstance(searchText);
        final ReadableDocumentService docService = service.getReadableService(orgId);

        if (isAdmin(orgId, userId)) {
            return buildGloballyAccessibleSearchRequests(
                    start, limit, presetSearchFilterWeb, docService, totalDocCounter, orgId);
        }
        final Long roleId = user.getRoleId();
        final Collection<Long> territoryIds = user.getTerritoryIds();
        final Map<String, FilterClause> filterMap = new LinkedHashMap<>();

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
                    userId, territoryIds, presetSearchFilterWeb.getDealFilter(), dealScope, isDealRecordsFollower));
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
                    userId, territoryIds, presetSearchFilterWeb.getContactFilter(), contactScope, isContactRecordsFollower));
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
                    userId, territoryIds, presetSearchFilterWeb.getCompanyFilter(), companyScope, isCompanyRecordsFollower));
        } else {
            companyPermissionMap = null;
            areCompaniesGloballyAccessible = false;
        }

        filterMap.put(CommonSearchFields.ACTIVITY, presetSearchFilterWeb.getActivityFilter().and(
                getOwnerAndTerritoryIdsFilter(userId, territoryIds)));

        if (isProductEnabled(orgId)) {
            final RolePermission productPermission = getSourcesPermission(orgId, roleId, SubCategory.Type.PRODUCTS);

            if (productPermission.isGranted()) {
                filterMap.put(CommonSearchFields.PRODUCT, getPermissionBasedFilter(userId, territoryIds,
                        presetSearchFilterWeb.getProductFilter(), productPermission.getScope(), false));
            }
        }

        if (areContactsGloballyAccessible && areCompaniesGloballyAccessible && areDealsGloballyAccessible) {
            loadNoteAndFileFilter(presetSearchFilterWeb.getNoteAndFileFilter(), filterMap);
        } else {
            final FilterClause entitiesFilter = getEntitiesScopeAppendedFilter(presetSearchFilterWeb.getNoteAndFileFilter(),
                    dealPermissionMap, contactPermissionMap, companyPermissionMap);

            if (Objects.nonNull(entitiesFilter)) {
                loadNoteAndFileFilter(entitiesFilter, filterMap);
            }
        }

        final Map<String, Integer> countMap = getCountMap(filterMap, docService);
        final Map<String, Map<Integer, Integer>> rangeMap = indexRangeCalculator.calculate(countMap, start, limit);
        final Collection<SingleIndexSearchRequest> singleIndexSearchRequests = new ArrayList<>();

        for (final Map.Entry<String, Map<Integer, Integer>> rangeMapEntry : rangeMap.entrySet()) {
            final String index = rangeMapEntry.getKey();
            final Map<Integer, Integer> range = rangeMapEntry.getValue();
            final Map.Entry<Integer, Integer> rangeEntry = range.entrySet().iterator().next();
            final int indexDocFetchStart = rangeEntry.getKey();
            final int indexDocFetchCount = rangeEntry.getValue();

            singleIndexSearchRequests.add(getSearchRequest(new SearchContext(indexDocFetchStart, indexDocFetchCount,
                            countMap.get(index), index, filterMap.get(index), docService), dealPermissionMap,
                    contactPermissionMap, companyPermissionMap, searchRequestLoader));
        }
        totalDocCounter.setCount(countMap.values().stream().mapToInt(Integer::intValue).sum());

        return singleIndexSearchRequests.isEmpty() ? null : MultiSingleIndexRequest.get(singleIndexSearchRequests);
    }

    /**
     * <p>
     * Builds search requests for web.
     * </p>
     *
     * @param index      Represents the index name.
     * @param searchText Searched text.
     * @param start      Indicates the point start from doc should be fetched.
     * @param fetchCount Total amount of docs to be fetched.
     * @return {@link SingleIndexSearchRequest}.
     */
    public SingleIndexSearchRequest build(final String index, final String searchText, final int start,
                                          final int fetchCount) {

       return buildSingleIndexRequest(start, fetchCount, index, PresetSearchFilterWeb.getInstance(searchText), searchRequestLoader);
    }

    /**
     * <p>
     * Gets activity search request.
     * </p>
     *
     * @param start                 Start point.
     * @param fetchCount            Docs to be fetched.
     * @param index                 Index name.
     * @param presetSearchFilterWeb {@link PresetSearchFilterWeb}.
     * @return {@link SingleIndexSearchRequest}.
     */
    protected SingleIndexSearchRequest buildActivityIndexRequest(final int start, final int fetchCount, final String index,
                                                                 final PresetSearchFilter presetSearchFilterWeb) {
        final BaseCrmUser user = LoggedInUserInfo.getUser();
        final Long orgId = user.orgId();
        final Long userId = user.id();
        final Long roleId = user.getRoleId();
        final ReadableDocumentService documentService = service.getReadableService(orgId);
        final FilterClause activityFilter = presetSearchFilterWeb.getActivityFilter();

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
        final RolePermission contactPermission = getSourcesPermission(orgId, roleId, SubCategory.Type.CONTACTS);
        final RolePermission companyPermission = getSourcesPermission(orgId, roleId, SubCategory.Type.COMPANIES);
        final RolePermission dealPermission = getSourcesPermission(orgId, roleId, SubCategory.Type.DEALS);
        final Scope contactScope = contactPermission.getScope();
        final Scope companyScope = companyPermission.getScope();
        final Scope dealScope = dealPermission.getScope();

        final boolean areContactsAccessible = contactPermission.isGranted();
        final boolean areCompaniesAccessible = companyPermission.isGranted();
        final boolean areDealsAccessible = dealPermission.isGranted();
        final boolean areContactsGloballyAccessible = areContactsAccessible && Scope.GLOBAL_ACCESS == contactScope;
        final boolean areCompaniesGloballyAccessible = areCompaniesAccessible && Scope.GLOBAL_ACCESS == companyScope;
        final boolean areDealsGloballyAccessible = areDealsAccessible && Scope.GLOBAL_ACCESS == dealScope;

        final FilterClause activityPermissionFilter = activityFilter.and(getOwnerAndTerritoryIdsFilter(userId, territoryIds));
        final int totalCount = getCount(activityPermissionFilter, CommonSearchFields.ACTIVITY, documentService);

        if (start < totalCount) {
            final SearchRequest searchRequest = buildSearchRequest(new SearchContext(start, fetchCount, totalCount,
                    CommonSearchFields.ACTIVITY, activityFilter, documentService));
            final Map<String, Map<String, Object>> emptyMap = Collections.emptyMap();

            if (areCompaniesGloballyAccessible && areContactsGloballyAccessible && areDealsGloballyAccessible) {
                searchRequestLoader.loadActivityIndexRequest(searchRequest, emptyMap, emptyMap, emptyMap);

                return SingleIndexSearchRequest.get(index, searchRequest);
            }
            final Map<String, Map<String, Object>> contactPermissionMap = buildPermissionMap(new SearchPermissionContext(
                    orgId, roleId, userId, territoryIds, contactScope, SubCategory.Type.CONTACT_FOLLOWERS_RECORDS,
                    areContactsAccessible, areContactsGloballyAccessible), CommonSearchFields.CONTACT);
            final Map<String, Map<String, Object>> companyPermissionMap = buildPermissionMap(new SearchPermissionContext(
                    orgId, roleId, userId, territoryIds, companyScope, SubCategory.Type.COMPANY_FOLLOWERS_RECORDS,
                    areCompaniesAccessible, areCompaniesGloballyAccessible), CommonSearchFields.COMPANY);
            final Map<String, Map<String, Object>> dealPermissionMap = buildPermissionMap(new SearchPermissionContext(
                    orgId, roleId, userId, territoryIds, dealScope, SubCategory.Type.DEAL_FOLLOWERS_RECORDS,
                    areDealsAccessible, areDealsGloballyAccessible), CommonSearchFields.DEAL);

            searchRequestLoader.loadActivityIndexRequest(searchRequest, dealPermissionMap, contactPermissionMap, companyPermissionMap);

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
     * @param start              Request's range start point.
     * @param limit              Request's total docs to fetch.
     * @param presetSearchFilter {@link PresetSearchFilter}.
     * @param docService    {@link ReadableDocumentService}.
     * @return {@link MultiSingleIndexRequest}.
     */
    private MultiSingleIndexRequest buildGloballyAccessibleSearchRequests(
            final int start, final int limit, final PresetSearchFilter presetSearchFilter,
            final ReadableDocumentService docService, final Counter docCounter, final Long orgId) {
        final Map<String, FilterClause> filterClauseMap = new LinkedHashMap<>();
        final FilterClause noteAndFileFilter = presetSearchFilter.getNoteAndFileFilter();

        filterClauseMap.put(CommonSearchFields.DEAL, presetSearchFilter.getDealFilter());
        filterClauseMap.put(CommonSearchFields.CONTACT, presetSearchFilter.getContactFilter());
        filterClauseMap.put(CommonSearchFields.COMPANY, presetSearchFilter.getCompanyFilter());
        filterClauseMap.put(CommonSearchFields.ACTIVITY, presetSearchFilter.getActivityFilter());

        if (isProductEnabled(orgId)) {
            filterClauseMap.put(CommonSearchFields.PRODUCT, presetSearchFilter.getProductFilter());
        }
        filterClauseMap.put(CommonSearchFields.FILE, noteAndFileFilter);
        filterClauseMap.put(CommonSearchFields.NOTE, noteAndFileFilter);

        return buildGloballyAccessibleSearchRequests(start, limit, filterClauseMap, docService, docCounter);
    }

    /**
     * <p>
     * Builds search requests for globally accessed modules.
     * </p>
     *
     * @param start           Request's range start point.
     * @param limit           Request's total docs to fetch.
     * @param filterClauseMap Map holding index and it's filter.
     * @param docService {@link ReadableDocumentService}.
     * @return {@link MultiSingleIndexRequest}.
     */
    private MultiSingleIndexRequest buildGloballyAccessibleSearchRequests(
            final int start, final int limit, final Map<String, FilterClause> filterClauseMap,
            final ReadableDocumentService docService, final Counter totalDocCounter) {
        final Collection<SingleIndexSearchRequest> singleIndexRequests = new ArrayList<>();
        final Map<String, Integer> countMap = getCountMap(filterClauseMap, docService);
        final Map<String, Map<Integer, Integer>> rangeMap = indexRangeCalculator.calculate(countMap, start, limit);

        for (final Map.Entry<String, Map<Integer, Integer>> rangeMapEntry : rangeMap.entrySet()) {
            final String index = rangeMapEntry.getKey();
            final Map.Entry<Integer, Integer> rangeEntry = rangeMapEntry.getValue().entrySet().iterator().next();
            final int indexDocFetchStart = rangeEntry.getKey();
            final int indexDocFetchCount = rangeEntry.getValue();
            final SearchRequest searchRequest = buildSearchRequest(new SearchContext(indexDocFetchStart,
                    indexDocFetchCount, countMap.get(index), index, filterClauseMap.get(index), docService));

            searchRequestLoader.loadRequestForGlobalAccessibleModules(index, searchRequest);
            singleIndexRequests.add(SingleIndexSearchRequest.get(index, searchRequest));
        }
        totalDocCounter.setCount(countMap.values().stream().mapToInt(Integer::intValue).sum());

        return MultiSingleIndexRequest.get(singleIndexRequests);
    }
}