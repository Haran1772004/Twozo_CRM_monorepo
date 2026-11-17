package com.tlc.crm.entity.internal.security.search.impl;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.service.RoleService;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.internal.security.search.SearchAccessController;
import com.tlc.crm.entity.internal.security.search.filter.PresetSearchFilter;
import com.tlc.crm.entity.role.permission.util.SubCategoryTypeUtils;
import com.tlc.crm.entity.search.IndexRangeCalculator;
import com.tlc.crm.entity.search.api.model.Index;
import com.tlc.crm.entity.search.api.model.RecentlyViewedEntriesContext;
import com.tlc.crm.entity.search.fields.CommonSearchFields;
import com.tlc.crm.entity.search.loader.SearchRequestLoader;
import com.tlc.crm.fields.service.settings.SourceSettingsService;
import com.tlc.crm.security.iam.BaseCrmUser;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.Scope;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.service.RolePermissionService;
import com.tlc.search.SearchService;
import com.tlc.search.model.Criteria;
import com.tlc.search.model.FilterClause;
import com.tlc.search.model.Comparator;
import com.tlc.search.model.RangeClause;
import com.tlc.search.model.SearchRequest;
import com.tlc.search.model.CountRequest;
import com.tlc.search.request.index.multi.read.MultiSingleIndexRequest;
import com.tlc.search.request.index.single.read.SingleIndexSearchRequest;
import com.tlc.search.request.index.single.read.count.SingleIndexCountRequest;
import com.tlc.search.service.ReadableDocumentService;
import com.tlc.search.service.provider.SearchServiceProvider;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * <p>
 * Based on user's access to different modules, requests are controlled and build accordingly.
 * </p>
 *
 * @author Ajay.
 * @version 1.0
 */
public abstract class AbstractSearchAccessController implements SearchAccessController {

    protected final SearchService service;
    protected final IndexRangeCalculator indexRangeCalculator;

    private final RoleService roleService;
    private final RolePermissionService rolePermissionService;
    private final SourceSettingsService sourceSettingsService;

    protected AbstractSearchAccessController() {
        service = SearchServiceProvider.get();
        sourceSettingsService = SourceSettingsService.getInstance();
        rolePermissionService = RolePermissionService.getInstance();
        roleService = RoleService.getInstance();
        indexRangeCalculator = IndexRangeCalculator.getInstance();
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
    protected abstract SingleIndexSearchRequest buildActivityIndexRequest(final int start, final int fetchCount, final String index,
                                                                          final PresetSearchFilter presetSearchFilter);


    /**
     * <p>
     * Checks whether the index is accessible.
     * </p>
     *
     * @param orgId Represents an organization's id.
     * @param index Represents {@link Index} name.
     * @return whether the index is accessible.
     */
    @Override
    public boolean isAccessible(final Long orgId, final String index) {
        return rolePermissionService.getPermission(orgId, LoggedInUserInfo.getUser().getRoleId(), Category.Type.SOURCES,
                SubCategoryTypeUtils.getBySourceName(index), Action.Type.ACCESS).isGranted();
    }

    /**
     * <p>
     * Checks whether the user is admin.
     * </p>
     *
     * @param orgId  Represents an organization's id.
     * @param userId A user's id.
     * @return whether the user is admin.
     */
    protected boolean isAdmin(final Long orgId, final Long userId) {
        return roleService.getRoleByUser(orgId, userId).getConfig().isAdmin();
    }

    /**
     * <p>
     * Gets the {@link RolePermission} for sources.
     * </p>
     *
     * @param orgId  Represents an organization's id.
     * @param roleId A role's id.
     * @param type   Represents {@link SubCategory}
     * @return {@link RolePermission}.
     */
    protected RolePermission getSourcesPermission(final Long orgId, final Long roleId, final SubCategory.Type type) {
        return rolePermissionService.getPermission(orgId, roleId, Category.Type.SOURCES, type, Action.Type.ACCESS);
    }

    /**
     * <p>
     * Gets the {@link RolePermission} for followers.
     * </p>
     *
     * @param orgId  Represents an organization's id.
     * @param roleId A role's id.
     * @param type   Represents {@link SubCategory}
     * @return {@link RolePermission}.
     */
    protected RolePermission getFollowersPermission(final Long orgId, final Long roleId, final SubCategory.Type type) {
        return rolePermissionService.getPermission(orgId, roleId, Category.Type.FOLLOWERS, type, Action.Type.ACCESS);
    }

    /**
     * <p>
     * Whether the product is enabled.
     * </p>
     *
     * @param orgId Represents an organization's id.
     * @return product is enabled.
     */
    protected boolean isProductEnabled(final Long orgId) {
        return sourceSettingsService.isSourceEnabled(orgId, Source.PRODUCT.getName());
    }

    /**
     * <p>
     * Gets the search request.
     * </p>
     *
     * @param wrapper              {@link SearchContext}.
     * @param dealPermissionMap    Key value pair referring deal related permissions.
     * @param contactPermissionMap Key value pair referring contact related permissions.
     * @param companyPermissionMap Key value pair referring company related permissions.
     * @return {@link SingleIndexSearchRequest}.
     */
    protected SingleIndexSearchRequest getSearchRequest(final SearchContext wrapper,
                                                        final Map<String, Map<String, Object>> dealPermissionMap,
                                                        final Map<String, Map<String, Object>> contactPermissionMap,
                                                        final Map<String, Map<String, Object>> companyPermissionMap,
                                                        final SearchRequestLoader searchRequestLoader) {
        final SearchRequest searchRequest = buildSearchRequest(wrapper);
        final String index = wrapper.index();

        switch (index) {
            case CommonSearchFields.CONTACT -> searchRequestLoader.loadContactIndexRequest(
                    searchRequest, companyPermissionMap);
            case CommonSearchFields.DEAL -> searchRequestLoader.loadDealIndexRequest(
                    searchRequest, contactPermissionMap, companyPermissionMap);
            case CommonSearchFields.COMPANY -> searchRequestLoader.loadCompanyIndexRequest(searchRequest);
            case CommonSearchFields.ACTIVITY -> searchRequestLoader.loadActivityIndexRequest(
                    searchRequest, dealPermissionMap, contactPermissionMap, companyPermissionMap);
            case CommonSearchFields.PRODUCT -> searchRequestLoader.loadProductIndexRequest(searchRequest);
            case CommonSearchFields.NOTE, CommonSearchFields.FILE -> searchRequestLoader.loadNoteAndFileIndexRequest(
                    searchRequest, contactPermissionMap, companyPermissionMap, index);
        }
        return SingleIndexSearchRequest.get(index, searchRequest);
    }

    /**
     * <p>
     * Builds {@link SingleIndexSearchRequest}.
     * </p>
     *
     * @param start               Start point.
     * @param fetchCount          Doc to be fetched.
     * @param index               Index name.
     * @param presetSearchFilter  {@link PresetSearchFilter}.
     * @param searchRequestLoader {@link SearchRequestLoader}.
     * @return {@link SingleIndexSearchRequest}.
     */
    protected SingleIndexSearchRequest buildSingleIndexRequest(final int start, final int fetchCount, final String index,
                                                               final PresetSearchFilter presetSearchFilter,
                                                               final SearchRequestLoader searchRequestLoader) {
        final int from = start - NumericalConstant.ONE;

        return switch (index) {
            case CommonSearchFields.CONTACT ->
                    buildContactRequest(from, fetchCount, index, presetSearchFilter, searchRequestLoader);
            case CommonSearchFields.COMPANY ->
                    buildCompanyRequest(from, fetchCount, index, presetSearchFilter, searchRequestLoader);
            case CommonSearchFields.DEAL ->
                    buildDealRequest(from, fetchCount, index, presetSearchFilter, searchRequestLoader);
            case CommonSearchFields.PRODUCT ->
                    buildProductRequest(from, fetchCount, index, presetSearchFilter, searchRequestLoader);
            case CommonSearchFields.ACTIVITY -> buildActivityIndexRequest(from, fetchCount, index, presetSearchFilter);
            case CommonSearchFields.FILE, CommonSearchFields.NOTE ->
                    buildNoteAndFileIndexRequest(from, fetchCount, index, presetSearchFilter, searchRequestLoader);
            default -> null;
        };
    }

    /**
     * <p>
     * Builds {@link SearchRequest}.
     * </p>
     *
     * @param searchContext {@link SearchContext}.
     * @return {@link SearchRequest}.
     */
    protected SearchRequest buildSearchRequest(final SearchContext searchContext) {
        final SearchRequest searchRequest = new SearchRequest();

        searchRequest.setFilter(searchContext.filterClause());
        searchRequest.setRangeClause(new RangeClause(searchContext.start(), searchContext.limit()));
        searchRequest.setTotalDocCount(searchContext.totalCount());

        return searchRequest;
    }

    /**
     * <p>
     * Gets count.
     * </p>
     *
     * @param filter          Represents {@link FilterClause}.
     * @param index           Represents {@link Index}.
     * @param documentService {@link ReadableDocumentService}.
     * @return count.
     */
    protected int getCount(final FilterClause filter, final String index, final ReadableDocumentService documentService) {

        if (Objects.nonNull(filter)) {
            return documentService.count(SingleIndexCountRequest.get(index, new CountRequest(filter))).intValue();
        }
        return NumericalConstant.ZERO;
    }

    /**
     * <p>
     * Gets the entities permission appended filter for Note/File index.
     * NOTE: This method's triggered only when one of the entity has valid permission.
     * </p>
     *
     * @param searchTextFilter     {@link FilterClause}.
     * @param dealPermissionMap    A map containing details pertaining to deal permission.
     * @param contactPermissionMap A map containing details pertaining to contact permission.
     * @param companyPermissionMap A map containing details pertaining to company permission.
     * @return {@link FilterClause}.
     */
    protected FilterClause getEntitiesScopeAppendedFilter(final FilterClause searchTextFilter,
                                                          final Map<String, Map<String, Object>> dealPermissionMap,
                                                          final Map<String, Map<String, Object>> contactPermissionMap,
                                                          final Map<String, Map<String, Object>> companyPermissionMap) {
        final FilterClause dealFilter = getEntityFilter(dealPermissionMap, CommonSearchFields.DEAL);
        final FilterClause contactFilter = getEntityFilter(contactPermissionMap, CommonSearchFields.CONTACT);
        final FilterClause companyFilter = getEntityFilter(companyPermissionMap, CommonSearchFields.COMPANY);
        FilterClause entitiesFilter;

        if (Objects.isNull(dealFilter)) {
            if (Objects.isNull(contactFilter)) {
                entitiesFilter = companyFilter;
            } else {
                entitiesFilter = contactFilter;

                if (Objects.nonNull(companyFilter)) {
                    entitiesFilter = entitiesFilter.or(companyFilter);
                }
            }
        } else {
            entitiesFilter = dealFilter;

            if (Objects.nonNull(contactFilter)) {
                entitiesFilter = entitiesFilter.or(contactFilter);
            }

            if (Objects.nonNull(companyFilter)) {
                entitiesFilter = entitiesFilter.or(companyFilter);
            }
        }
        return Objects.isNull(entitiesFilter) ? null : searchTextFilter.and(entitiesFilter);
    }

    /**
     * <p>
     * Builds a map script params
     * </p>
     *
     * @param scope        {@link Scope}
     * @param ownerId      A user's id.
     * @param territoryIds Collection of user ids.
     * @return map containing script params.
     */
    protected Map<String, Map<String, Object>> buildParamsMap(final String source, final Scope scope, final Long ownerId,
                                                              final boolean isRecordsFollower, final Collection<Long> territoryIds) {
        final Map<String, Map<String, Object>> paramsMap = new HashMap<>();
        final Map<String, Object> paramsValueMap = new HashMap<>();

        switch (scope) {
            case TERRITORY_ACCESS -> {
                if (territoryIds.isEmpty()) {
                    loadUserParamsValue(source, ownerId, isRecordsFollower, paramsValueMap);
                    paramsMap.put(CommonSearchFields.RESTRICTED, paramsValueMap);
                } else {
                    loadUserParamsValue(source, ownerId, isRecordsFollower, paramsValueMap);
                    paramsValueMap.put(CommonSearchFields.TERRITORY_IDS, territoryIds);
                    paramsMap.put(CommonSearchFields.TERRITORY, paramsValueMap);
                }
            }
            case RESTRICTED_ACCESS -> {
                loadUserParamsValue(source, ownerId, isRecordsFollower, paramsValueMap);
                paramsMap.put(CommonSearchFields.RESTRICTED, paramsValueMap);
            }
            default -> {
            }
        }
        return paramsMap;
    }

    /**
     * <p>
     * Builds search requests for globally accessed modules.
     * </p>
     *
     * @param viewedEntities Holds relevant index and it's ids.
     * @return {@link MultiSingleIndexRequest}.
     */
    protected RecentlyViewedEntriesContext buildGloballyAccessibleSearchRequests(final Map<Index, Collection<Long>> viewedEntities,
                                                                                 final SearchRequestLoader searchRequestLoader,
                                                                                 final Long orgId) {
        final Collection<SingleIndexSearchRequest> singleIndexSearchRequests = new ArrayList<>();
        final Map<String, Boolean> permissibleByIndex = new HashMap<>();
        final boolean isProductEnabled = isProductEnabled(orgId);

        for (final Map.Entry<Index, Collection<Long>> recentlyViewedEntity : viewedEntities.entrySet()) {
            final Index index = recentlyViewedEntity.getKey();
            final Collection<Long> entryIds = recentlyViewedEntity.getValue();
            final String indexName = index.name();
            final SearchRequest searchRequest = new SearchRequest();
            final FilterClause filterClause = new FilterClause(new Criteria(entryIds, Comparator.EQUAL, CommonSearchFields.ID));

            searchRequest.setFilter(filterClause);

            if (Objects.equals(CommonSearchFields.PRODUCT, indexName) && !isProductEnabled) {
                setFieldsForNonAccessibleDocs(searchRequest);
                permissibleByIndex.put(indexName, false);
            } else {
                searchRequestLoader.loadRequestForGlobalAccessibleModules(indexName, searchRequest);
                permissibleByIndex.put(indexName, true);
            }
            singleIndexSearchRequests.add(SingleIndexSearchRequest.get(indexName, searchRequest));
        }

        final MultiSingleIndexRequest multiSearchRequest = singleIndexSearchRequests.isEmpty()
                ? null
                : MultiSingleIndexRequest.get(singleIndexSearchRequests);

        return RecentlyViewedEntriesContext.get(multiSearchRequest, permissibleByIndex);
    }

    /**
     * <p>
     * Builds the permission map.
     * </p>
     *
     * @param context {@link SearchPermissionContext}.
     * @return permission map.
     */
    protected Map<String, Map<String, Object>> buildPermissionMap(final SearchPermissionContext context, final String source) {
        final Map<String, Map<String, Object>> permissionMap;
        final boolean isFollower = getFollowersPermission(context.orgId(), context.roleId(),
                context.followerSubType()).isGranted();

        if (context.isAccessible()) {
            permissionMap = context.isGloballyAccessible() ? Collections.emptyMap() : buildParamsMap(source,
                    context.sourceScope(), context.ownerId(), isFollower, context.territoryIds());
        } else {
            permissionMap = null;
        }
        return permissionMap;
    }

    /**
     * <p>
     * Appends follower criteria to the filter.
     * </p>
     *
     * @param userId A user's id.
     * @return {@link FilterClause}.
     */
    protected FilterClause appendFollowerCriteria(final Long userId, final FilterClause filterClause,
                                                  final boolean isFollower) {

        return isFollower ? filterClause.and(new Criteria(
                userId, Comparator.CONTAINS, CommonSearchFields.FOLLOWER_IDS)) : null;
    }

    /**
     * <p>
     * Gets the necessary permissions appended filter.
     * </p>
     *
     * @param userId           A user's id.
     * @param territoryIds     Collection of territory's id.
     * @param searchTextFilter Represents {@link FilterClause}.
     * @param scope            Represents {@link Scope}.
     * @return {@link FilterClause}.
     */
    protected FilterClause getPermissionBasedFilter(final Long userId, final Collection<Long> territoryIds,
                                                    final FilterClause searchTextFilter, final Scope scope,
                                                    final boolean isFollowersFilterRequired) {

        return switch (scope) {
            case RESTRICTED_ACCESS -> getOwnerAndFollowersFilter(userId, searchTextFilter, isFollowersFilterRequired);
            case TERRITORY_ACCESS -> {

                if (territoryIds.isEmpty()) {
                    yield getOwnerAndFollowersFilter(userId, searchTextFilter, isFollowersFilterRequired);
                }
                final FilterClause ownerIdFilter = getOwnerIdFilter(userId);
                final FilterClause territoryFilter = getTerritoryIdFilter(territoryIds);
                final FilterClause territoryOrOwnerFilter = ownerIdFilter.or(territoryFilter);
                final FilterClause permissionsFilter;

                if (isFollowersFilterRequired) {
                    permissionsFilter = territoryOrOwnerFilter.or(getFollowerIdsFilter(userId));
                } else {
                    permissionsFilter = territoryOrOwnerFilter;
                }
                yield searchTextFilter.and(permissionsFilter);
            }
            default -> searchTextFilter;
        };
    }

    /**
     * <p>
     * Gets the owner & territory ids filter.
     * </p>
     *
     * @param userId       A user's id.
     * @param territoryIds Territory Ids.
     * @return {@link FilterClause}.
     */
    protected FilterClause getOwnerAndTerritoryIdsFilter(final Long userId, final Collection<Long> territoryIds) {
        final FilterClause ownerIdsFilter = getOwnerIdsFilter(userId);

        if (territoryIds.isEmpty()) {
            return ownerIdsFilter;
        }

        return getTerritoryIdsFilter(territoryIds).or(ownerIdsFilter);
    }

    /**
     * <p>
     * Map containing index name & total doc count for the input passed.
     * </p>
     *
     * @param filterMap       {@link FilterClause}.
     * @param documentService {@link ReadableDocumentService}.
     * @return Map containing index and it's count.
     */
    protected Map<String, Integer> getCountMap(final Map<String, FilterClause> filterMap,
                                               final ReadableDocumentService documentService) {
        final Map<String, Integer> countMap = new LinkedHashMap<>();

        for (final Map.Entry<String, FilterClause> filterMapEntry : filterMap.entrySet()) {
            final String index = filterMapEntry.getKey();

            countMap.put(index, documentService.count(SingleIndexCountRequest.get(
                    index, new CountRequest(filterMapEntry.getValue()))).intValue());
        }
        return countMap;
    }

    /**
     * <p>
     * Set fields for non accessible fields.
     * </p>
     *
     * @param searchRequest {@link SearchRequest}.
     */
    protected void setFieldsForNonAccessibleDocs(final SearchRequest searchRequest) {
        searchRequest.includeField(CommonSearchFields.ID);
        searchRequest.includeField(CommonSearchFields.NAME);
        searchRequest.includeField(CommonSearchFields.OWNER_ID);
        searchRequest.includeField(CommonSearchFields.ORG_ID);
    }

    /**
     * <p>
     * Loads file and note filter.
     * </p>
     *
     * @param noteAndFileFilter {@link FilterClause}.
     * @param filterMap         file and note filter map.
     */
    protected void loadNoteAndFileFilter(final FilterClause noteAndFileFilter, final Map<String, FilterClause> filterMap) {
        filterMap.put(CommonSearchFields.FILE, noteAndFileFilter);
        filterMap.put(CommonSearchFields.NOTE, noteAndFileFilter);
    }

    /**
     * <p>
     * Gets {@link SingleIndexSearchRequest}.
     * </p>
     *
     * @param wrapper      {@link SearchContext}.
     * @param orgId        An organization's id.
     * @param roleId       User's role id.
     * @param userId       User's id
     * @param territoryIds User's territories ids.
     * @return {@link SingleIndexSearchRequest}.
     */
    private SingleIndexSearchRequest getSearchRequest(final SearchContext wrapper, final long orgId, final long roleId,
                                                      final long userId, final Collection<Long> territoryIds,
                                                      final SearchRequestLoader searchRequestLoader) {
        final FilterClause filterClause = wrapper.filterClause();

        if (Objects.nonNull(filterClause)) {
            final String index = wrapper.index();
            final int from = wrapper.start();
            final ReadableDocumentService documentService = wrapper.docService();
            final int count = getCount(filterClause, index, documentService);

            if (from < count) {
                final SearchRequest searchRequest = buildSearchRequest(new SearchContext(
                        from, wrapper.limit(), count, index, filterClause, documentService));

                switch (index) {
                    case CommonSearchFields.CONTACT ->
                            loadContactIndexRequest(searchRequest, orgId, roleId, userId, territoryIds, searchRequestLoader);
                    case CommonSearchFields.DEAL ->
                            loadDealIndexRequest(searchRequest, orgId, roleId, userId, territoryIds, searchRequestLoader);
                    case CommonSearchFields.COMPANY -> searchRequestLoader.loadCompanyIndexRequest(searchRequest);
                    case CommonSearchFields.PRODUCT -> searchRequestLoader.loadProductIndexRequest(searchRequest);
                }

                return SingleIndexSearchRequest.get(index, searchRequest);
            }
        }

        return null;
    }

    /**
     * <p>
     * Builds contact index request.
     * </p>
     *
     * @param start               Start point.
     * @param fetchCount          Doc to be fetched.
     * @param index               Index name.
     * @param presetSearchFilter  {@link PresetSearchFilter}.
     * @param searchRequestLoader {@link SearchRequestLoader}.
     * @return {@link SingleIndexSearchRequest}.
     */
    private SingleIndexSearchRequest buildContactRequest(final int start, final int fetchCount, final String index,
                                                         final PresetSearchFilter presetSearchFilter,
                                                         final SearchRequestLoader searchRequestLoader) {
        final BaseCrmUser user = LoggedInUserInfo.getUser();
        final Long orgId = user.orgId();
        final Long userId = user.id();
        final Long roleId = user.getRoleId();
        final Collection<Long> territoryIds = user.getTerritoryIds();
        final ReadableDocumentService readableDocumentService = service.getReadableService(orgId);
        final RolePermission contactPermission = getSourcesPermission(orgId, roleId, SubCategory.Type.CONTACTS);
        final boolean isContactRecordsFollower = getFollowersPermission(orgId, roleId, SubCategory.Type.CONTACT_FOLLOWERS_RECORDS).isGranted();
        final FilterClause scopeBasedFilter = contactPermission.isGranted() ? getPermissionBasedFilter(userId, territoryIds,
                presetSearchFilter.getContactFilter(), contactPermission.getScope(), isContactRecordsFollower) : null;

        return getSearchRequest(new SearchContext(start, fetchCount, NumericalConstant.ZERO, index,
                scopeBasedFilter, readableDocumentService), orgId, roleId, userId, territoryIds, searchRequestLoader);
    }

    /**
     * <p>
     * Builds company index request.
     * </p>
     *
     * @param start               Start point.
     * @param fetchCount          Doc to be fetched.
     * @param index               Index name.
     * @param presetSearchFilter  {@link PresetSearchFilter}.
     * @param searchRequestLoader {@link SearchRequestLoader}.
     * @return {@link SingleIndexSearchRequest}.
     */
    private SingleIndexSearchRequest buildCompanyRequest(final int start, final int fetchCount, final String index,
                                                         final PresetSearchFilter presetSearchFilter,
                                                         final SearchRequestLoader searchRequestLoader) {
        final BaseCrmUser user = LoggedInUserInfo.getUser();
        final Long orgId = user.orgId();
        final Long userId = user.id();
        final Long roleId = user.getRoleId();
        final Collection<Long> territoryIds = user.getTerritoryIds();
        final ReadableDocumentService readableDocumentService = service.getReadableService(orgId);
        final RolePermission companyPermission = getSourcesPermission(orgId, roleId, SubCategory.Type.COMPANIES);
        final boolean isCompanyRecordsFollower = getFollowersPermission(orgId, roleId, SubCategory.Type.COMPANY_FOLLOWERS_RECORDS).isGranted();
        final FilterClause scopeBasedFilter = companyPermission.isGranted() ? getPermissionBasedFilter(userId, territoryIds,
                presetSearchFilter.getCompanyFilter(), companyPermission.getScope(), isCompanyRecordsFollower) :
                appendFollowerCriteria(userId, presetSearchFilter.getCompanyFilter(), isCompanyRecordsFollower);

        return getSearchRequest(new SearchContext(start, fetchCount, NumericalConstant.ZERO, index,
                scopeBasedFilter, readableDocumentService), orgId, roleId, userId, territoryIds, searchRequestLoader);
    }

    /**
     * <p>
     * Builds deal index request.
     * </p>
     *
     * @param start               Start point.
     * @param fetchCount          Doc to be fetched.
     * @param index               Index name.
     * @param presetSearchFilter  {@link PresetSearchFilter}.
     * @param searchRequestLoader {@link SearchRequestLoader}.
     * @return {@link SingleIndexSearchRequest}.
     */
    private SingleIndexSearchRequest buildDealRequest(final int start, final int fetchCount, final String index,
                                                      final PresetSearchFilter presetSearchFilter,
                                                      final SearchRequestLoader searchRequestLoader) {
        final BaseCrmUser user = LoggedInUserInfo.getUser();
        final Long orgId = user.orgId();
        final Long userId = user.id();
        final Long roleId = user.getRoleId();
        final Collection<Long> territoryIds = user.getTerritoryIds();
        final ReadableDocumentService readableDocumentService = service.getReadableService(orgId);
        final RolePermission dealPermission = getSourcesPermission(orgId, roleId, SubCategory.Type.DEALS);
        final boolean isDealRecordsFollower = getFollowersPermission(orgId, roleId, SubCategory.Type.DEAL_FOLLOWERS_RECORDS).isGranted();
        final FilterClause scopeBasedFilter = dealPermission.isGranted() ? getPermissionBasedFilter(userId, territoryIds,
                presetSearchFilter.getDealFilter(), dealPermission.getScope(), isDealRecordsFollower) :
                appendFollowerCriteria(userId, presetSearchFilter.getDealFilter(), isDealRecordsFollower);

        return getSearchRequest(new SearchContext(start, fetchCount, NumericalConstant.ZERO, index,
                scopeBasedFilter, readableDocumentService), orgId, roleId, userId, territoryIds, searchRequestLoader);
    }

    /**
     * <p>
     * Builds product index request.
     * </p>
     *
     * @param start               Start point.
     * @param fetchCount          Doc to be fetched.
     * @param index               Index name.
     * @param presetSearchFilter  {@link PresetSearchFilter}.
     * @param searchRequestLoader {@link SearchRequestLoader}.
     * @return {@link SingleIndexSearchRequest}.
     */
    private SingleIndexSearchRequest buildProductRequest(final int start, final int fetchCount, final String index,
                                                         final PresetSearchFilter presetSearchFilter,
                                                         final SearchRequestLoader searchRequestLoader) {
        final BaseCrmUser user = LoggedInUserInfo.getUser();
        final Long orgId = user.orgId();

        if (!isProductEnabled(orgId)) {
            return null;
        }
        final Long userId = user.id();
        final Long roleId = user.getRoleId();
        final Collection<Long> territoryIds = user.getTerritoryIds();
        final ReadableDocumentService readableDocumentService = service.getReadableService(orgId);
        final RolePermission productPermission = getSourcesPermission(orgId, roleId, SubCategory.Type.PRODUCTS);

        if (!productPermission.isGranted()) {
            return null;
        }
        final FilterClause scopeBasedFilter = getPermissionBasedFilter(userId, territoryIds,
                presetSearchFilter.getProductFilter(), productPermission.getScope(), false);

        return getSearchRequest(new SearchContext(start, fetchCount, NumericalConstant.ZERO, index,
                scopeBasedFilter, readableDocumentService), orgId, roleId, userId, territoryIds, searchRequestLoader);
    }

    /**
     * <p>
     * Builds note/file index request.
     * </p>
     *
     * @param start               Start point.
     * @param fetchCount          Doc to be fetched.
     * @param index               Index name.
     * @param presetSearchFilter  {@link PresetSearchFilter}.
     * @param searchRequestLoader {@link SearchRequestLoader}.
     * @return {@link SingleIndexSearchRequest}.
     */
    private SingleIndexSearchRequest buildNoteAndFileIndexRequest(final int start, final int fetchCount, final String index,
                                                                  final PresetSearchFilter presetSearchFilter,
                                                                  final SearchRequestLoader searchRequestLoader) {
        final BaseCrmUser user = LoggedInUserInfo.getUser();
        final Long orgId = user.orgId();
        final Long userId = user.id();
        final Long roleId = user.getRoleId();
        final Collection<Long> territoryIds = user.getTerritoryIds();
        final ReadableDocumentService documentService = service.getReadableService(orgId);

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
        final FilterClause noteAndFileFilter = presetSearchFilter.getNoteAndFileFilter();

        if (areContactsGloballyAccessible && areCompaniesGloballyAccessible && areDealsGloballyAccessible) {
            final int docCount = getCount(noteAndFileFilter, index, documentService);

            if (start < docCount) {
                final SearchRequest searchRequest = buildSearchRequest(new SearchContext(start,
                        fetchCount, docCount, index, noteAndFileFilter, documentService));

                searchRequestLoader.loadRequestForGlobalAccessibleModules(index, searchRequest);

                return SingleIndexSearchRequest.get(index, searchRequest);
            }
        } else {
            final boolean isContactRecordsFollower = getFollowersPermission(orgId, roleId, SubCategory.Type.CONTACT_FOLLOWERS_RECORDS).isGranted();
            final boolean isCompanyRecordsFollower = getFollowersPermission(orgId, roleId, SubCategory.Type.COMPANY_FOLLOWERS_RECORDS).isGranted();
            final boolean isDealRecordsFollower = getFollowersPermission(orgId, roleId, SubCategory.Type.DEAL_FOLLOWERS_RECORDS).isGranted();

            final Map<String, Map<String, Object>> emptyMap = Collections.emptyMap();
            final Map<String, Map<String, Object>> contactPermissionMap;
            final Map<String, Map<String, Object>> companyPermissionMap;
            final Map<String, Map<String, Object>> dealPermissionMap;

            if (areContactsAccessible) {
                contactPermissionMap = areContactsGloballyAccessible ? emptyMap : buildParamsMap(
                        CommonSearchFields.CONTACT, contactScope, userId, isContactRecordsFollower, territoryIds);
            } else {
                contactPermissionMap = null;
            }

            if (areCompaniesAccessible) {
                companyPermissionMap = areCompaniesGloballyAccessible ? emptyMap : buildParamsMap(
                        CommonSearchFields.COMPANY, companyScope, userId, isCompanyRecordsFollower, territoryIds);
            } else {
                companyPermissionMap = null;
            }

            if (areDealsAccessible) {
                dealPermissionMap = areDealsGloballyAccessible ? emptyMap : buildParamsMap(
                        CommonSearchFields.DEAL, dealScope, userId, isDealRecordsFollower, territoryIds);
            } else {
                dealPermissionMap = null;
            }
            final FilterClause entityAppendedFilter = getEntitiesScopeAppendedFilter(
                    noteAndFileFilter, dealPermissionMap, contactPermissionMap, companyPermissionMap);
            final int docCount = getCount(entityAppendedFilter, index, documentService);

            if (start < docCount) {
                final SearchRequest searchRequest = buildSearchRequest(new SearchContext(start,
                        fetchCount, docCount, index, entityAppendedFilter, documentService));

                searchRequestLoader.loadNoteAndFileIndexRequest(searchRequest, contactPermissionMap, companyPermissionMap, index);

                return SingleIndexSearchRequest.get(index, searchRequest);
            }
        }
        return null;
    }

    /**
     * <p>
     * Loads contact index request.
     * </p>
     *
     * @param searchRequest {@link SearchRequest}.
     * @param orgId         An organization's id.
     * @param roleId        User's role id.
     * @param userId        User's id
     * @param territoryIds  User's territories ids.
     */
    private void loadContactIndexRequest(final SearchRequest searchRequest, final long orgId, final long roleId,
                                         final long userId, final Collection<Long> territoryIds, final SearchRequestLoader searchRequestLoader) {
        final RolePermission companyPermission = getSourcesPermission(orgId, roleId, SubCategory.Type.COMPANIES);
        final Scope companyScope = companyPermission.getScope();
        final SearchPermissionContext context = new SearchPermissionContext(orgId, roleId, userId, territoryIds,
                companyScope, SubCategory.Type.COMPANY_FOLLOWERS_RECORDS,
                true, false);

        if (companyPermission.isGranted() && Scope.GLOBAL_ACCESS == companyScope) {
            searchRequestLoader.loadRequestForGlobalAccessibleModules(CommonSearchFields.CONTACT, searchRequest);
        } else {
            searchRequestLoader.loadContactIndexRequest(searchRequest, buildPermissionMap(context, CommonSearchFields.COMPANY));
        }
    }

    /**
     * <p>
     * Loads deal index's request.
     * </p>
     *
     * @param searchRequest {@link SearchRequest}.
     * @param orgId         An organization's id.
     * @param roleId        User's role id.
     * @param userId        User's id
     * @param territoryIds  User's territories ids.
     */
    private void loadDealIndexRequest(final SearchRequest searchRequest, final long orgId, final long roleId,
                                      final long userId, final Collection<Long> territoryIds,
                                      final SearchRequestLoader loader) {
        final RolePermission companyPermission = getSourcesPermission(orgId, roleId, SubCategory.Type.COMPANIES);
        final RolePermission contactPermission = getSourcesPermission(orgId, roleId, SubCategory.Type.CONTACTS);
        final Scope contactScope = contactPermission.getScope();
        final Scope companyScope = companyPermission.getScope();
        final boolean areContactsAccessible = contactPermission.isGranted();
        final boolean areCompaniesAccessible = companyPermission.isGranted();
        final boolean areContactsGloballyAccessible = areContactsAccessible && Scope.GLOBAL_ACCESS == contactScope;
        final boolean areCompaniesGloballyAccessible = areCompaniesAccessible && Scope.GLOBAL_ACCESS == companyScope;

        if (areContactsGloballyAccessible && areCompaniesGloballyAccessible) {
            loader.loadRequestForGlobalAccessibleModules(CommonSearchFields.DEAL, searchRequest);
        } else {
            final Map<String, Map<String, Object>> contactPermissionMap = buildPermissionMap(new SearchPermissionContext(
                    orgId, roleId, userId, territoryIds, contactScope, SubCategory.Type.CONTACT_FOLLOWERS_RECORDS,
                    areContactsAccessible, areContactsGloballyAccessible), CommonSearchFields.CONTACT);
            final Map<String, Map<String, Object>> companyPermissionMap = buildPermissionMap(new SearchPermissionContext(
                    orgId, roleId, userId, territoryIds, companyScope, SubCategory.Type.COMPANY_FOLLOWERS_RECORDS,
                    areCompaniesAccessible, areCompaniesGloballyAccessible), CommonSearchFields.COMPANY);

            loader.loadDealIndexRequest(searchRequest, contactPermissionMap, companyPermissionMap);
        }
    }

    /**
     * <p>
     * Gets the entity filter based on it's permission.
     * </p>
     * NOTE - (main map iterates only once).
     *
     * @param permissionMap A map containing details pertaining to entity permission.
     * @param entity        Entity's name.
     * @return {@link FilterClause}.
     */
    private FilterClause getEntityFilter(final Map<String, Map<String, Object>> permissionMap, final String entity) {

        if (Objects.nonNull(permissionMap)) {
            FilterClause filter = new FilterClause(new Criteria(entity, Comparator.EQUAL, CommonSearchFields.ENTITY_SOURCE));

            for (final Map.Entry<String, Map<String, Object>> permissionEntry : permissionMap.entrySet()) {
                final Map<String, Object> paramsMap = permissionEntry.getValue();
                final boolean isFollower = isFollower(paramsMap, entity);

                switch (permissionEntry.getKey()) {
                    case CommonSearchFields.TERRITORY -> {
                        final Long ownerId = (Long) paramsMap.get(CommonSearchFields.OWNER_ID);
                        final FilterClause territoryFilter = new FilterClause(new Criteria(paramsMap.get(
                                CommonSearchFields.TERRITORY_IDS), Comparator.CONTAINS, CommonSearchFields.TERRITORY_ID));
                        final FilterClause ownerFilter = new FilterClause(new Criteria(ownerId, Comparator.EQUAL, CommonSearchFields.OWNER_ID));
                        final FilterClause territoryAndOwnerFilter = territoryFilter.or(ownerFilter);
                        final FilterClause entityFilter;

                        if (isFollower) {
                            entityFilter = territoryAndOwnerFilter.or(new Criteria(ownerId, Comparator.CONTAINS, CommonSearchFields.FOLLOWER_IDS));
                        } else {
                            entityFilter = territoryAndOwnerFilter;
                        }
                        filter = filter.and(entityFilter);
                    }
                    case CommonSearchFields.RESTRICTED -> {
                        final Long ownerId = (Long) paramsMap.get(CommonSearchFields.OWNER_ID);
                        final FilterClause ownerFilter = new FilterClause(new Criteria(ownerId, Comparator.EQUAL, CommonSearchFields.OWNER_ID));
                        final FilterClause entityFilter;

                        if (isFollower) {
                            entityFilter = ownerFilter.or(new Criteria(ownerId, Comparator.CONTAINS, CommonSearchFields.FOLLOWER_IDS));
                        } else {
                            entityFilter = ownerFilter;
                        }
                        filter = filter.and(entityFilter);
                    }
                    case CommonSearchFields.FOLLOWER -> {
                        if (isFollower) {
                            filter = filter.and(new Criteria(LoggedInUserInfo.getUser().id(), Comparator.CONTAINS, CommonSearchFields.FOLLOWER_IDS));
                        } else {
                            return null;
                        }
                    }
                    default -> {
                    }
                }
            }
            return filter;
        }
        return null;
    }

    /**
     * <p>
     * Whether follower.
     * </p>
     *
     * @param paramsMap A map containing details pertaining to entity permission.
     * @param entity    Entity name.
     * @return whether follower.
     */
    private boolean isFollower(final Map<String, Object> paramsMap, final String entity) {
        return switch (entity) {
            case CommonSearchFields.DEAL -> (boolean) paramsMap.get(CommonSearchFields.DEAL_FOLLOWER);
            case CommonSearchFields.CONTACT -> (boolean) paramsMap.get(CommonSearchFields.CONTACT_FOLLOWER);
            case CommonSearchFields.COMPANY -> (boolean) paramsMap.get(CommonSearchFields.COMPANY_FOLLOWER);
            default -> false;
        };
    }

    /**
     * <p>
     * Loads owner/followers value into params map.
     * </p>
     *
     * @param ownerId        Owner's id.
     * @param isFollower     Whether the user is follower.
     * @param paramsValueMap Map of params value.
     */
    private void loadUserParamsValue(final String source, final Long ownerId, final boolean isFollower,
                                     final Map<String, Object> paramsValueMap) {

        final String followerType = switch (source) {
            case CommonSearchFields.DEAL -> CommonSearchFields.DEAL_FOLLOWER;
            case CommonSearchFields.CONTACT -> CommonSearchFields.CONTACT_FOLLOWER;
            case CommonSearchFields.COMPANY -> CommonSearchFields.COMPANY_FOLLOWER;
            default -> null;
        };

        if (Objects.nonNull(followerType)) {
            paramsValueMap.put(followerType, isFollower);
        }
        paramsValueMap.put(CommonSearchFields.OWNER_ID, ownerId);
    }

    /**
     * <p>
     * Gets the sales owner and followers filter.
     * </p>
     *
     * @param userId                    A user's id.
     * @param searchTextFilter          {@link FilterClause}.
     * @param isFollowersFilterRequired Whether the follower filters are required
     * @return {@link FilterClause}.
     */
    private FilterClause getOwnerAndFollowersFilter(final Long userId, final FilterClause searchTextFilter,
                                                    final boolean isFollowersFilterRequired) {
        final FilterClause ownerIdFilter = getOwnerIdFilter(userId);
        final FilterClause permissionsFilter;

        if (isFollowersFilterRequired) {
            permissionsFilter = ownerIdFilter.or(getFollowerIdsFilter(userId));
        } else {
            permissionsFilter = ownerIdFilter;
        }

        return searchTextFilter.and(permissionsFilter);
    }

    /**
     * <p>
     * Gets the owner id filter.
     * </p>
     *
     * @param ownerId Owner's id.
     * @return {@link FilterClause}.
     */
    private FilterClause getOwnerIdFilter(final Long ownerId) {
        return new FilterClause(new Criteria(ownerId, Comparator.EQUAL, CommonSearchFields.OWNER_ID));
    }

    /**
     * <p>
     * Gets the owner ids field filter.
     * </p>
     *
     * @param ownerId Owner's id.
     * @return {@link FilterClause}.
     */
    private FilterClause getOwnerIdsFilter(final Long ownerId) {
        return new FilterClause(new Criteria(ownerId, Comparator.CONTAINS, CommonSearchFields.OWNER_IDS));
    }

    /**
     * <p>
     * Gets the territory ids field filter.
     * </p>
     *
     * @param territoryIds Territory ids.
     * @return {@link FilterClause}.
     */
    private FilterClause getTerritoryIdsFilter(final Collection<Long> territoryIds) {
        return new FilterClause(new Criteria(territoryIds, Comparator.CONTAINS, CommonSearchFields.TERRITORY_IDS));
    }

    /**
     * <p>
     * Gets territory appended filter.
     * </p>
     *
     * @param territoryIds Collection of territory ids.
     * @return {@link FilterClause}.
     */
    private FilterClause getTerritoryIdFilter(final Collection<Long> territoryIds) {
        return new FilterClause(new Criteria(territoryIds, Comparator.CONTAINS, CommonSearchFields.TERRITORY_ID));
    }

    /**
     * <p>
     * Gets the follower ids filter.
     * </p>
     *
     * @param userId A user's id.
     * @return {@link FilterClause}.
     */
    private FilterClause getFollowerIdsFilter(final Long userId) {
        return new FilterClause(new Criteria(userId, Comparator.CONTAINS, CommonSearchFields.FOLLOWER_IDS));
    }
}