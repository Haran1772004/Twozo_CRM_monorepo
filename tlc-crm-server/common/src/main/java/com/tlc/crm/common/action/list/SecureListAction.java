package com.tlc.crm.common.action.list;

import com.tlc.attachment.model.profilePicture.DisplayPicture;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.RandomIdGenerator;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.list.JoinLoader;
import com.tlc.crm.common.list.ListInterpreter;
import com.tlc.crm.common.list.ListRowListener;
import com.tlc.crm.common.list.SQLListInterpreter;
import com.tlc.crm.common.list.model.ListDataContainer;
import com.tlc.crm.common.list.model.ListRequestData;
import com.tlc.crm.common.list.transformer.ModelTransformer;
import com.tlc.crm.common.list.transformer.model.TransformerRequest;
import com.tlc.crm.common.list.transformer.model.TransformerResponse;

import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.i18n.I18nResolver;

import com.tlc.iam.common.models.User;

import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.WhereClause;

import com.tlc.web.client.event.EventCallback;

import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * That involve listing or retrieving data
 * </p>
 *
 * @author Abishek
 * @version 1.0
 * @see AuthorizationController
 */
public abstract class SecureListAction extends AuthorizationController {

    protected static final long DEFAULT_LIMIT = 25;

    protected final UserService userService;
    protected final com.tlc.iam.client.UserService iamUserService;
    protected final RandomIdGenerator randomIdGenerator;
    protected final int maxFieldSelectionLimit;
    protected final int maxFilterCriteriaLimit;

    protected final String profilePicRedirectionUrl;

    public SecureListAction() {
        userService = UserService.getInstance();
        iamUserService = com.tlc.iam.client.UserService.getInstance();
        randomIdGenerator = RandomIdGenerator.getInstance();
        final Configurations configurations = ConfHolder.getInstance().getProperties("profile-picture").getNestedConfigs("redirectUrl");
        this.profilePicRedirectionUrl = configurations.getConfig("user");

        final Configurations listActionConfigurations = ConfHolder.getInstance().getProperties("list-action");
        this.maxFieldSelectionLimit = listActionConfigurations.getInt("field.selection.limit");
        this.maxFilterCriteriaLimit = listActionConfigurations.getInt("filter.limit");
    }

    /**
     * {@inheritDoc}
     *
     * @param request     Holds the {@link CrmRequest} details
     * @param crmResponse Holds the {@link CrmResponse} details
     */
    @Override
    protected final void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) {
        processListAction(buildListRequestData(request), crmResponse);
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    @Override
    protected final void handleMobileRequest(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        processMobileListAction(buildListRequestData(crmRequest), crmResponse);
    }

    /**
     * Loads the list of {@link JoinClause} into the {@link JoinLoader}
     *
     * @param preAction {@link JoinLoader}
     */
    protected void loadJoins(final JoinLoader preAction) {

    }

    /**
     * <p>
     * Processes a list action based on the provided JSON request, applying sorting, filtering,
     * and pagination as specified in the request, and prepares the response accordingly.
     * </p>
     *
     * @param listRequestData Represents the {@link ListRequestData}
     * @param crmResponse     Represents the {@link CrmResponse}
     */
    protected void processListAction(final ListRequestData listRequestData, final CrmResponse crmResponse) {
        final String providerName = getProviderName();
        final JoinLoader joinLoader = getJoinLoader();
        final JsonObject requestJson = listRequestData.getRequestJson();
        final long orgId = listRequestData.getOrgId();
        final long userId = listRequestData.getUserId();
        final I18nResolver i18nResolver = listRequestData.getI18nResolver();

        loadJoins(joinLoader);

        final ListInterpreter listInterpreter = new SQLListInterpreter(userId, orgId, providerName, i18nResolver, joinLoader);

        preProcess(userId, listInterpreter);
        final JsonArray fields = requestJson.optJsonArray(ListActionFields.FIELDS);
        final JsonArray criteria = requestJson.optJsonArray(ListActionFields.CRITERIA);
        final JsonArray sortFields = requestJson.optJsonArray(ListActionFields.SORT);
        final JsonObject filterCriteria = requestJson.optJsonObject(ListActionFields.FILTER_CRITERIA);

        if (Objects.isNull(fields)) {
            listInterpreter.applyDefaultSelect();
        } else {
            final int size = fields.size();

            for (int index = 0; index < size; index++) {
                JsonObject selectJson = fields.getJsonObject(index);
                listInterpreter.selectClause(selectJson.getString(ListActionFields.FIELD));
            }
        }

        if (Objects.isNull(sortFields) || 0 == sortFields.size()) {
            listInterpreter.applyDefaultOrderBy();
        } else {
            final int size = sortFields.size();

            for (int index = 0; index < size; index++) {
                final JsonObject fieldInfo = sortFields.getJsonObject(index);
                final String field = fieldInfo.getString(ListActionFields.FIELD);
                final Object sortType = fieldInfo.get(ListActionFields.TYPE);

                listInterpreter.orderByClause(field, sortType == null || !sortType.toString().equalsIgnoreCase(ListActionFields.SORT_DESC));
            }
        }

        if (Objects.nonNull(criteria)) {
            final int size = criteria.size();

            for (int index = 0; index < size; index++) {
                final JsonObject criteriaJson = criteria.getJsonObject(index);
                final String field = criteriaJson.getString(ListActionFields.FIELD);
                final Object value = criteriaJson.get(ListActionFields.VALUE);
                final String comparator = criteriaJson.getString(ListActionFields.COMPARATOR);

                if (value instanceof JsonArray) {
                    listInterpreter.criteria(field, ((JsonArray) value).toList(), comparator);
                } else {
                    listInterpreter.criteria(field, value, comparator);
                }
            }
        }

        if (Objects.nonNull(filterCriteria)) {
            applyCustomCriteria(listInterpreter, filterCriteria);
        }
        final JsonObject response = Json.object();

        applyLimitsAndLoadPageSummary(orgId, userId, requestJson, listInterpreter, requestJson.optJsonObject(ListActionFields.LIMIT), response);
        loadResponse(listInterpreter, response, crmResponse, listRequestData);
        crmResponse.put(ListActionFields.TABLE, response);
    }

    /**
     * Gets the {@link JoinLoader}
     *
     * @return {@link JoinLoader}
     */
    protected JoinLoader getJoinLoader() {
        return new JoinLoader() {
            private List<JoinClause> joinClauseList;
            private boolean joinExist;

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isJoinExist() {
                return joinExist;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public List<JoinClause> getJoins() {
                return joinClauseList;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void addJoinClause(final List<JoinClause> joinClauses) {
                joinClauseList = List.copyOf(joinClauses);
                joinExist = true;
            }
        };
    }

    public ListRowListener getRowListener() {
        return null;
    }

    /**
     * <p>
     * Fetches the {@link CrmResponse}
     * </p>
     *
     * @param listRequestData Represents the {@link ListRequestData}
     * @param listInterpreter Represents the {@link ListInterpreter} details
     * @return A map containing converted content data
     */
    protected ListDataContainer fetchCrmListResponse(final ListRequestData listRequestData, final ListInterpreter listInterpreter) {
        final ModelTransformer modelTransformer = listInterpreter.structure().getProviderTransformer();
        final Collection<Map<String, Object>> result = listInterpreter.getResult();
        final long orgId = listRequestData.getOrgId();
        final I18nResolutionAdapter i18NResolutionAdapter = listRequestData.getI18nPlaceholderResolver();

        if (Objects.isNull(modelTransformer) || result.isEmpty()) {
            return new ListDataContainer(orgId, i18NResolutionAdapter, result);
        } else {
            final TransformerResponse transformerResponse = transform(listRequestData.getOrgId(), result, modelTransformer, listRequestData.getTimezoneInfo());

            return new ListDataContainer(orgId, i18NResolutionAdapter, transformerResponse.getResults(), transformerResponse.getUserIds());
        }
    }

    /**
     * <p>
     * Groups the array of fields and transforms the entities into the required forms.
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param result           Represents the results to be converted
     * @param modelTransformer Represents the {@link ModelTransformer}
     * @param timezoneInfo     Represents the {@link TimezoneInfo}
     * @return {@link TransformerResponse}
     */
    protected TransformerResponse transform(final long orgId, final Collection<Map<String, Object>> result,
                                            final ModelTransformer modelTransformer, final TimezoneInfo timezoneInfo) {
        return modelTransformer.transform(new TransformerRequest(orgId, result, timezoneInfo));
    }

    /**
     * <p>
     * Applies custom criteria.
     * </p>
     *
     * @param listInterpreter Represents {@link ListInterpreter}.
     * @param filterCriteria  Hold's details about criteria to filter.
     */
    protected void applyCustomCriteria(final ListInterpreter listInterpreter, final JsonObject filterCriteria) {
    }

    /**
     * <p>
     * Populates the provided {@link JsonObject} response with record-related information.
     * This includes setting the total number of records and indicating whether there are
     * more records available.
     * </p>
     *
     * @param totalHits The total number of records that match the criteria.
     * @param hasMore   A boolean indicating whether there are more records available.
     * @param response  The {@link JsonObject} to be populated with record information.
     */
    protected void loadPageSummary(final long totalHits, final boolean hasMore, final JsonObject response) {
        response.put(ListActionFields.TOTAL_RECORDS, totalHits);
        response.put(ListActionFields.HAS_MORE, hasMore);
    }

    /**
     * <p>
     * Loads the response as asynchronously to the client.
     * </p>
     *
     * @param listInterpreter Represents the {@link ListInterpreter}.
     * @param response        Holds the response {@link JsonObject}.
     * @param crmResponse     Holds the {@link CrmResponse}.
     * @param listRequestData Represents the {@link ListRequestData}
     */
    private void loadResponse(final ListInterpreter listInterpreter, final JsonObject response, final CrmResponse crmResponse, final ListRequestData listRequestData) {
        final ListDataContainer listDataContainer = fetchCrmListResponse(listRequestData, listInterpreter);
        final Collection<Long> userIds = listDataContainer.getUserIds();
        final Collection<Map<String, Object>> entityRows = listDataContainer.getData(listInterpreter, getRowListener());

        if (Objects.isNull(userIds) || userIds.isEmpty()) {
            response.put(ListActionFields.ROWS, entityRows);
            crmResponse.put(ListActionFields.TABLE, response);
        } else {
            dispatchEntities(listRequestData.getOrgId(), userIds, entityRows, response, crmResponse);
        }
    }

    /**
     * <p>
     * Fetches the user details by using {@link EventCallback} and then dispatch the response.
     * </p>
     *
     * @param orgId       Represents the organisation id.
     * @param userIds     Represents the user ids.
     * @param rows        Represents the details of the entities.
     * @param response    Holds the details of the response.
     * @param crmResponse Holds the details of the {@link CrmResponse}.
     */
    protected void dispatchEntities(final Long orgId, final Collection<Long> userIds, final Collection<Map<String, Object>> rows,
                                  final JsonObject response, final CrmResponse crmResponse) {
        final Map<Long, Map<String, Object>> crmUsers = new HashMap<>();
        final Collection<Long> iamUserIds = userService.getUsers(orgId, userIds).stream().map(CrmUser::getIamId).collect(Collectors.toSet());
        final OrganizationService organizationService = OrganizationService.getInstance();

        if (Objects.nonNull(crmResponse)) {
            crmResponse.convertIntoAsync();
        }
        final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {
            @Override
            public void onSuccess(final Collection<User> users) {
                final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                for (final Long iamUserId : iamUserIds) {
                    final Map<String, Object> crmUser = new HashMap<>();
                    final User user = userMap.get(iamUserId);
                    final Long crmUserId = userService.getUserId(orgId, iamUserId);
                    final String firstName = user.getFirstName();
                    final String lastName = user.getLastName();
                    final DisplayPicture thumbnail = user.getThumbnail();
                    final User.State userState = user.getUserState();

                    crmUser.put(ListActionFields.ID, crmUserId);
                    crmUser.put(ListActionFields.NAME, Objects.isNull(lastName) ? firstName : String.format("%s %s", firstName, lastName));
                    crmUser.put(ListActionFields.IS_ACTIVE, Objects.nonNull(userState) && userState.isActive());

                    if (Objects.nonNull(thumbnail)) {
                        final Map<String, Object> thumbnailValues = new HashMap<>();
                        final JsonObject userJson = Json.object();
                        final String extension = thumbnail.extension().getName();
                        final String imageType = thumbnail.type().getName();

                        userJson.put(ListActionFields.USER_ID, user.id());
                        userJson.put(ListActionFields.ID, randomIdGenerator.generateUniqueId());
                        thumbnailValues.put(ListActionFields.NAME, imageType);
                        thumbnailValues.put(ListActionFields.SIZE, thumbnail.size());
                        thumbnailValues.put(ListActionFields.EXTENSION, extension);
                        thumbnailValues.put(ListActionFields.URL, String.format("%s%s%s%s%s%s", profilePicRedirectionUrl,
                                Base64.getEncoder().encodeToString(userJson.getBytes()), "/", imageType, ".", extension));
                        crmUser.put(ListActionFields.THUMBNAIL, thumbnailValues);
                    }

                    crmUsers.put(crmUserId, crmUser);
                }

                for (final Map<String, Object> row : rows) {
                    final Object userIdValue = row.remove(ListActionFields.USER_ID);

                    if (userIdValue instanceof final Map<?, ?> userData) {

                        for (final Map.Entry<?, ?> userEntry : userData.entrySet()) {
                            final String field = String.valueOf(userEntry.getKey());
                            final Object resolvedValue = resolveUserValue(userEntry.getValue(), crmUsers);
                            final Object existingValue = row.get(field);

                            if (existingValue instanceof Map && resolvedValue instanceof Map) {
                                ((Map<String, Object>) existingValue).putAll((Map<String, Object>) resolvedValue);
                            } else {
                                row.put(field, resolvedValue);
                            }
                        }
                    }
                }

                response.put(ListActionFields.ROWS, rows);
                crmResponse.put(ListActionFields.TABLE, response);
                crmResponse.send();
            }

            @Override
            public void onFailure(final Throwable throwable) {
                sendError(crmResponse, null, ErrorCode.get(CommonErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
            }
        };
        iamUserService.fetchUsers(organizationService.getIamOrgId(orgId), iamUserIds, usersFetchCallBack);
    }

    /**
     * <p>
     * Resolves user-related values by mapping user identifiers to their corresponding
     * </p>
     *
     *
     * @param value The value to resolve; may be a user ID, a collection of user IDs,
     *              or a nested map containing user-related values
     * @param crmUsers A map of user IDs to their corresponding CRM user data
     * @return The resolved value with user identifiers replaced by CRM user objects;
     *         preserves the original structure for maps and collections
     */
    private Object resolveUserValue(final Object value, final Map<Long, Map<String, Object>> crmUsers) {

        if (value instanceof final Map<?, ?> userMap) {
            final Map<String, Object> resolvedMap = new HashMap<>();

            for (final Map.Entry<?, ?> userEntry : userMap.entrySet()) {
                resolvedMap.put(String.valueOf(userEntry.getKey()), resolveUserValue(userEntry.getValue(), crmUsers));
            }
            return resolvedMap;
        } else if (value instanceof final Collection<?> userIds) {
            return userIds.stream().map(crmUsers::get).toList();
        }
        return crmUsers.get(value);
    }

    private void preProcess(final Long userId, final ListInterpreter listInterpreter) {
        final WhereClause additionalWhereClause = getAdditionalWhereClause(userId);

        if (Objects.nonNull(additionalWhereClause)) {
            listInterpreter.setWhereClause(additionalWhereClause);
        }
    }

    /**
     * <p>
     * Retrieves an additional {@link WhereClause} to filter
     * </p>
     *
     * @return The additional {@link WhereClause}
     */
    protected WhereClause getAdditionalWhereClause(final Long userId) {
        return null;
    }

    /**
     * Gets the name of the provider for the list action.
     *
     * @return The provider name
     */
    protected String getProviderName() {
        return " ";
    }

    /**
     * <p>
     * Builds a {@link ListRequestData} object using the given {@link CrmRequest}
     * </p>
     *
     * @param crmRequest Represents the {@link CrmRequest}
     * @return the {@link ListRequestData}
     */
    private ListRequestData buildListRequestData(final CrmRequest crmRequest) {
        final ListRequestData listRequestData = new ListRequestData();

        listRequestData.setOrgId(crmRequest.orgId());
        listRequestData.setUserId(crmRequest.userId());
        listRequestData.setRequestJson(crmRequest.getRequestJson());
        listRequestData.setI18nPlaceholderResolver(crmRequest.getI18NCRMResolutionAdapter());
        listRequestData.setI18nResolver(crmRequest.i18nResolver());
        listRequestData.setTimezoneInfo(crmRequest.timeZone());
        listRequestData.setCrmRequest(crmRequest);

        return listRequestData;
    }

    /**
     * <p>
     * Performs the mobile list action process
     * </p>
     *
     * @param listRequestData Represents the {@link ListRequestData}
     * @param crmResponse     Refers the {@link CrmResponse} object
     */
    protected void processMobileListAction(final ListRequestData listRequestData, final CrmResponse crmResponse) {
    }

    /**
     * <p>
     * Applies limits and loads the pagination summary details into response
     * </p>
     *
     * @param orgId           Refers the organisation ID.
     * @param userId          Refers the user ID.
     * @param requestJson     Represents the request json
     * @param listInterpreter Represents the {@link ListInterpreter}
     * @param limitInfo       Holds the information about the limit for pagination
     * @param response        Represents the response json
     */
    protected void applyLimitsAndLoadPageSummary(final long orgId, final long userId, final JsonObject requestJson, final ListInterpreter listInterpreter, final JsonObject limitInfo, final JsonObject response) {
        final long endPosition;

        if (Objects.isNull(limitInfo)) {
            listInterpreter.applyDefaultLimit();
            endPosition = DEFAULT_LIMIT;
        } else {
            final int start = limitInfo.getInt(ListActionFields.START);
            final int count = limitInfo.getInt(ListActionFields.COUNT);
            endPosition = start + count;

            listInterpreter.setLimits(start, count);
        }

        if (requestJson.optBoolean(ListActionFields.FETCH_HITS)) {
            final long totalHits = listInterpreter.fetchTotalHits();

            loadPageSummary(totalHits, endPosition <= totalHits, response);
        }
    }
}
