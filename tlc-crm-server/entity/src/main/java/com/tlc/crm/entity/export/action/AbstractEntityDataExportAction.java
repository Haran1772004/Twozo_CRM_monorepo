package com.tlc.crm.entity.export.action;

import com.tlc.attachment.io.Extension;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.export.action.DataExportAction;
import com.tlc.crm.common.export.model.ExportProviderType;
import com.tlc.crm.common.export.req.DataExportRequest;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.handler.EventRequestHandler;
import com.tlc.crm.common.iam.api.manager.RoleManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.list.JoinLoader;
import com.tlc.crm.common.list.ListInterpreter;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.common.list.model.ListDataContainer;
import com.tlc.crm.common.list.model.ListRequestData;

import com.tlc.crm.common.resolver.I18NCRMResolutionAdapter;
import com.tlc.crm.common.service.EventRequestService;
import com.tlc.crm.entity.export.DataExportHandler;
import com.tlc.crm.entity.export.internal.EntityDataExportHandler;
import com.tlc.crm.entity.export.fields.ExportFields;
import com.tlc.crm.entity.export.internal.model.FileExportRequest;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.list.EntityListAction;
import com.tlc.crm.entity.list.EntityListInterpreter;
import com.tlc.crm.entity.list.EntityStructureHandler;
import com.tlc.crm.entity.list.export.DataExportSQLListInterpreter;
import com.tlc.crm.entity.list.model.Provider;

import com.tlc.crm.security.permission.PermissionLoader;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.service.RolePermissionService;

import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.I18nService;
import com.tlc.i18n.service.provider.I18nServiceProvider;

import com.tlc.iam.common.models.User;

import com.tlc.sql.common.api.dml.JoinClause;

import com.tlc.web.client.event.EventCallback;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Provides an abstract way to export the required data based on the request parameters.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public abstract class AbstractEntityDataExportAction extends EntityListAction implements DataExportAction {

    private final RoleManager roleManager;
    private final I18nService i18nService;
    private final DataExportHandler dataExportHandler;
    private final UserService userService;
    private final com.tlc.iam.client.UserService iamUserService;
    private final EventRequestHandler eventRequestHandler;

    protected final RolePermissionService rolePermissionService;

    protected AbstractEntityDataExportAction() {
        roleManager = RoleManager.getInstance();
        i18nService = I18nServiceProvider.get();
        rolePermissionService = RolePermissionService.getInstance();
        dataExportHandler = EntityDataExportHandler.getInstance();
        userService = UserService.getInstance();
        iamUserService = com.tlc.iam.client.UserService.getInstance();
        eventRequestHandler = EventRequestService.get().getEventHandler();
    }

    /**
     * {@inheritDoc}
     *
     * @param dataExportRequest Holds the data export request parameters
     * @return true, if the data export is successful.
     */
    @Override
    public boolean doExport(final DataExportRequest dataExportRequest) {
        final long orgId = dataExportRequest.orgId();
        final Long userId = dataExportRequest.userId();
        final I18nResolver resolver = i18nService.getResolver(dataExportRequest.locale());

        if (checkPermission(orgId, userId)) {
            addAdditionalDataToRequestJson(dataExportRequest);
            final ListRequestData listRequestData = new ListRequestData();

            listRequestData.setUserId(userId);
            listRequestData.setOrgId(orgId);
            listRequestData.setI18nPlaceholderResolver(new I18NCRMResolutionAdapter(resolver, orgId));
            listRequestData.setI18nResolver(resolver);
            listRequestData.setRequestJson(dataExportRequest.parameters());
            processListAction(listRequestData, null);

            return true;
        }
        return false;
    }

    /**
     * Adds the additional information to the {@link DataExportRequest} json fields.
     *
     * @param dataExportRequest Holds the data export request parameters.
     */
    private void addAdditionalDataToRequestJson(final DataExportRequest dataExportRequest) {
        final JsonObject jsonObject = dataExportRequest.parameters();

        if (!jsonObject.containsKey(ExportFields.FILE_TYPE)) {
            jsonObject.put(ExportFields.FILE_TYPE, dataExportRequest.type());
        }
        jsonObject.put(ExportFields.EXPORT_ID, dataExportRequest.exportId());
    }

    /**
     * <p>
     * Checks the permission of the data export.
     * </p>
     *
     * @param orgId  Represents the organization id.
     * @param userId Represents the user id.
     * @return A flag that indicates whether the action has permission.
     */
    private boolean checkPermission(final Long orgId, final Long userId) {
        final Long roleId = roleManager.getRoleByUser(orgId, userId).id();
        final RolePermission permission = getPermission(orgId, roleId);

        PermissionLoader.getInstance().loadPermissionIntoContext(permission);
        return permission.isGranted();
    }

    /**
     * <p>
     * Fetches the permission of the data export.
     * </p>
     *
     * @param orgId  Represents the organization id.
     * @param roleId Represents the role id.
     * @return Holds the permission details of the data export.
     */
    protected abstract RolePermission getPermission(final Long orgId, final Long roleId);

    /**
     * <p>
     * Gets the export sql {@link EntityListInterpreter} for the given request json and {@link Provider}.
     * </p>
     *
     * @param orgId                   Represents an organization id
     * @param userId                  Represents the user id
     * @param request                 Represents the request {@link JsonObject}
     * @param provider                Represents the {@link Provider}
     * @param joinLoader              Represents the {@link JoinLoader} to obtain {@link JoinClause} for the dependent
     * @param i18NResolutionAdapter Represents the {@link I18nResolutionAdapter}.
     * @return export sql {@link EntityListInterpreter}
     */
    @Override
    public EntityListInterpreter getEntityListInterpreter(final long orgId, final Long userId, final JsonObject request,
                                                                final Provider provider, final JoinLoader joinLoader, final I18nResolutionAdapter i18NResolutionAdapter) {
        final DataExportSQLListInterpreter dataExportSQLListInterpreter = new DataExportSQLListInterpreter(userId, orgId,
                provider, joinLoader, i18NResolutionAdapter, ExportProviderType.get(request.getString(ExportFields.EXPORT_PROVIDER_TYPE)),
                maxFieldSelectionLimit, maxFilterCriteriaLimit, getFieldNames(orgId, userId, request, provider), false);
        final boolean isAllDataSelected = request.optBoolean(ExportFields.IS_ALL_SELECTED);

        if (!isAllDataSelected) {
            final JsonArray entityIdsArray = request.getJsonArray(ExportFields.IDS);
            final Collection<Long> entityIds = new HashSet<>();

            entityIdsArray.forEach(id -> entityIds.add((Long) id));
            dataExportSQLListInterpreter.applyPkCriteria(entityIds, false);
        }
        final JsonArray excludedIdsArray = request.optJsonArray(ExportFields.EXCLUDED_IDS);
        final Collection<Long> excludedIds = new HashSet<>();

        if (Objects.nonNull(excludedIdsArray)) {
            excludedIdsArray.forEach(excludedId -> excludedIds.add((Long) excludedId));
        }

        if (!excludedIds.isEmpty()) {
            dataExportSQLListInterpreter.applyPkCriteria(excludedIds, true);
        }

        return dataExportSQLListInterpreter;
    }

    /**
     * <p>
     * Gets the field names from the request for export action
     * </p>
     *
     * @param orgId   Represents the org id
     * @param request Represents the request json
     * @return the field names
     */
    protected Set<String> getFieldNames(final Long orgId, final Long userId, final JsonObject request, final Provider provider) {
        final Set<String> fieldNames;

        if (request.optBoolean(ExportFields.IS_ALL_FIELDS_SELECTED)) {
            fieldNames = EntityStructureHandler.get().fetchConfiguredFields(userId, orgId, provider.name())
                    .stream().map(configHolder -> configHolder.fieldConfig().getName()).collect(Collectors.toUnmodifiableSet());
        } else if (request.containsKey(ExportFields.FIELD_NAMES)) {
            final Set<String> selectedFieldNames = new HashSet<>();

            request.getJsonArray(ExportFields.FIELD_NAMES).forEach(fieldName -> selectedFieldNames.add((String) fieldName));
            fieldNames = Collections.unmodifiableSet(selectedFieldNames);
        } else {
            fieldNames = Collections.emptySet();
        }

        return fieldNames;
    }

    /**
     * <p>
     * Loads and processes the response based on the provided organization ID, list interpreter,
     * internationalization resolver, and request JSON.
     * </p>
     *
     * @param listRequestData Represents the {@link ListRequestData}
     * @param listInterpreter Represents the {@link EntityListInterpreter}.
     * @param response        Holds the response {@link JsonObject}.
     * @param crmResponse     Holds the {@link CrmResponse}.
     * @param provider        Refers the {@code Provider}.
     */
    @Override
    protected void loadResponse(final ListRequestData listRequestData, final EntityListInterpreter listInterpreter,
                                final JsonObject response, final CrmResponse crmResponse, final Provider provider) {
        final ListDataContainer listDataContainer = fetchCrmListResponse(listRequestData, listInterpreter);
        final Collection<Long> userIds = listDataContainer.getUserIds();
        final Map<String, Map<String, Object>> rows = listDataContainer.groupRowsByPk(listInterpreter, getRowListener());
        final Collection<Map<String, Object>> entityRows = rows.values();

        loadDynamicFields(listInterpreter, rows);
        final JsonObject requestJson = listRequestData.getRequestJson();
        final ExportProviderType exportProviderType = ExportProviderType.get(requestJson.getString(ExportFields.EXPORT_PROVIDER_TYPE));
        final Collection<FieldConfig> fields = listInterpreter.getListStructure().getFields();
        final Collection<FieldConfig> fieldConfigs;

        if (Objects.equals(ExportProviderType.DETAIL, exportProviderType)) {
            fieldConfigs = fields;
        } else {
            fieldConfigs = getConfiguredFieldConfigs((DataExportSQLListInterpreter) listInterpreter);
        }
        preProcess(entityRows);
        attachAdditionalEntityData(listRequestData.getOrgId(), exportProviderType, entityRows);
        final FileExportRequest fileExportRequest = buildFileExportRequest(listRequestData, exportProviderType, entityRows, fieldConfigs);

        if (userIds.isEmpty()) {
            dataExportHandler.exportData(fileExportRequest);
        } else {
            dispatchEntities(userIds, fileExportRequest);
        }
    }

    /**
     * Excludes pagination from export without modifying the persisted pagination state.
     *
     * @param orgId       Represents the organization ID.
     * @param userId      Represents the user ID.
     * @param requestJson Represents the request JSON object.
     * @return {@code null}, as export should not be paginated
     */
    @Override
    protected JsonObject resolveLimitJson(final long orgId, final long userId,
                                          final JsonObject requestJson) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param requestJson     Represents the request json
     * @param listInterpreter Represents the {@link ListInterpreter}
     * @param limitInfo       Holds the information about the limit for pagination
     * @param response        Represents the response json
     */
    @Override
    protected void applyLimitsAndLoadPageSummary(final long orgId, final long userId, final JsonObject requestJson, final ListInterpreter listInterpreter, final JsonObject limitInfo, final JsonObject response) {
        if (Objects.nonNull(limitInfo)) {
            listInterpreter.setLimits(limitInfo.getInt(ListActionFields.START), limitInfo.getInt(ListActionFields.COUNT));
        }
    }

    /**
     * <p>
     * Builds the {@link FileExportRequest}
     * </p>
     *
     * @param listRequestData    Represents the {@link ListRequestData}
     * @param exportProviderType Represents the {@link ExportProviderType}
     * @param entityRows         Which contains the collection of entity rows
     * @return The {@link FileExportRequest}
     */
    private FileExportRequest buildFileExportRequest(final ListRequestData listRequestData, final ExportProviderType exportProviderType,
                                                     final Collection<Map<String, Object>> entityRows, final Collection<FieldConfig> fieldConfigs) {
        final JsonObject requestJson = listRequestData.getRequestJson();
        final Source source = getProvider().getSource();
        final long orgId = listRequestData.getOrgId();
        final FileExportRequest fileExportRequest = new FileExportRequest();

        fileExportRequest.setOrgId(orgId);
        fileExportRequest.setUserId(listRequestData.getUserId());
        fileExportRequest.setId(requestJson.getLong(ExportFields.EXPORT_ID));
        fileExportRequest.setExtension(Extension.get(requestJson.getString(ExportFields.FILE_TYPE)));
        fileExportRequest.setExportProvider(exportProviderType);
        fileExportRequest.setI18nPlaceholderResolver(listRequestData.getI18nPlaceholderResolver());
        fileExportRequest.setSource(source);
        fileExportRequest.setFieldConfigs(fieldConfigs);
        fileExportRequest.setHeaders(fieldConfigs.stream().map(
                fieldConfig -> listRequestData.getI18nPlaceholderResolver().get(fieldConfig.getDisplayName())).collect(Collectors.toCollection(LinkedHashSet::new)));
        fileExportRequest.setEntityRows(entityRows);

        return fileExportRequest;
    }

    /**
     * <p>
     * Fetches the user details by using {@link EventCallback} and then dispatch the response.
     * </p>
     *
     * @param userIds           Represents the {@link Collection} of user ids
     * @param fileExportRequest Represents the {@link FileExportRequest}
     */
    private void dispatchEntities(final Collection<Long> userIds, final FileExportRequest fileExportRequest) {
        final long orgId = fileExportRequest.getOrgId();
        final Collection<Map<String, Object>> entityRows = fileExportRequest.getEntityRows();
        final Collection<Long> iamUserIds = userService.getUsers(orgId, userIds).stream().map(CrmUser::getIamId).collect(Collectors.toSet());
        final OrganizationService organizationService = OrganizationService.getInstance();

        final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

            @Override
            public void onSuccess(final Collection<User> users) {
                eventRequestHandler.handle(() -> {
                    final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                    processUserDetails(orgId, iamUserIds, userMap, entityRows);
                    fileExportRequest.setEntityRows(entityRows);
                    dataExportHandler.exportData(fileExportRequest);
                });
            }

            @Override
            public void onFailure(final Throwable throwable) {
            }
        };

        iamUserService.fetchUsers(organizationService.getIamOrgId(orgId), iamUserIds, usersFetchCallBack);
    }


    /**
     * <p>
     * Pre-process the entity rows
     * </p>
     *
     * @param entityRows A {@link Map} contains the collection of entity rows
     */
    protected void preProcess(final Collection<Map<String, Object>> entityRows) {

    }

    /**
     * <p>
     * Attaches additional data to the entity rows
     * </p>
     *
     * @param orgId              Represents an organization id
     * @param exportProviderType Represents the {@link ExportProviderType}
     * @param entityRows         Which contains the collection of entity rows
     */
    protected void attachAdditionalEntityData(final long orgId, final ExportProviderType exportProviderType,
                                              final Collection<Map<String, Object>> entityRows) {
    }

    /**
     * <p>
     * Fetches additional headers that should be included in the export
     * </p>
     *
     * @param exportProviderType Represents the {@link ExportProviderType}
     * @return A collection of additional headers as strings
     */
    protected Collection<String> getAdditionalHeaders(final ExportProviderType exportProviderType) {
        return Collections.emptySet();
    }

    /**
     * <p>
     * Determines whether the given field configuration is enabled
     * </p>
     *
     * @param config the field configuration settings
     * @return {@code true} if the configuration is enabled for web, otherwise {@code false}
     */
    protected boolean isEnabled(final FieldConfig.Config config) {
        return config.isEnabledForWeb();
    }

    /**
     * <p>
     * Fetches the list of field configurations that are enabled for usage.
     * </p>
     *
     * @param listInterpreter the interpreter containing field configurations
     * @return a collection of enabled {@link FieldConfig} objects
     */
    private Collection<FieldConfig> getConfiguredFieldConfigs(final DataExportSQLListInterpreter listInterpreter) {
        final Stream<FieldConfig> stream = listInterpreter.getFieldConfigs().stream();
        final Set<String> fieldNames = listInterpreter.getFieldNames();

        return Objects.isNull(fieldNames) || fieldNames.isEmpty()
                ? stream.filter(fieldConfig -> {
                    final FieldConfig.Config config = fieldConfig.getConfig();

                    return Objects.nonNull(config) && isEnabled(config);
                }).toList()
                : stream.filter(fieldConfig -> fieldNames.contains(fieldConfig.getName())).toList();
    }
}