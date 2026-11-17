package com.tlc.crm.core.api.analytics.permission.handler;

import com.tlc.analytics.config.action.AnalyticsRequest;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.commons.util.executor.CodeBlockExecutor;

import com.tlc.crm.core.api.analytics.entity.config.modules.ModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModuleType;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.manager.CRMReportManagerAPI;

import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;

import com.tlc.crm.fields.service.settings.SourceSettingsService;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.Scope;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.model.Action.Type;
import com.tlc.crm.security.permission.service.RolePermissionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.NonNull;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * The {@link  AnalyticsPermissionHandler} class is responsible for handling permission checks for analytics-related actions
 * within the crm.
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
public class AnalyticsPermissionHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsPermissionHandler.class);

    private final RolePermissionService rolePermissionService;
    private final CRMViewConfigManager<ReportModularField> reportModularFieldManager;
    private final SourceSettingsService sourceSettingsService;

    private AnalyticsPermissionHandler()
    {
        rolePermissionService = RolePermissionService.getInstance();
        reportModularFieldManager = CRMReportManagerAPI.INSTANCE().getReportFieldManager();
        sourceSettingsService = SourceSettingsService.getInstance();
    }

    private static class Instance
    {
        private static final AnalyticsPermissionHandler ANALYTICS_PERMISSION_HANDLER = new AnalyticsPermissionHandler();
    }

    public static AnalyticsPermissionHandler getInstance()
    {
        return Instance.ANALYTICS_PERMISSION_HANDLER;
    }

    /**
     * <p>
     * Handles the permission verification for analytics modules based on the role
     * </p>
     *
     * @param orgId  Represents the organisation id
     * @param roleId Represents the role id
     * @return The {@link Collection} of authorized module ids based on the permission
     */
    public Collection<Integer> getAuthorizedModuleIds( final @NonNull Long orgId, final @NonNull Long roleId )
    {
        final Collection<RolePermission> sourceRolePermissions = rolePermissionService.getPermissions( orgId, roleId, Category.Type.SOURCES );
        final Collection<SubCategory.Type> sourceSubCategoryTypes = new HashSet<>();
        final Collection<Integer> authorizedModuleIds = new HashSet<>();

        try
        {
            CodeBlockExecutor.forEachSafely( sourceRolePermissions,
                                             sourceRolePermission ->
                                             {
                                                 if( sourceRolePermission.isGranted() && Type.ACCESS == sourceRolePermission.getAction()
                                                                                                                            .getType() )
                                                 {
                                                     final SubCategory.Type sourceSubcategoryType = sourceRolePermission.getSubCategory()
                                                                                                                        .getType();
                                                     if( SubCategory.Type.PRODUCTS == sourceSubcategoryType && sourceSettingsService.isSourceEnabled(orgId, SubCategory.Type.PRODUCTS.getAliasName()) )
                                                     {
                                                         authorizedModuleIds.add(ModuleType.getModuleTypeByName(sourceSubcategoryType.getAliasName()).getTypeCode());
                                                     }
                                                     sourceSubCategoryTypes.add( sourceSubcategoryType );
                                                 }
                                             } );

            if( !sourceSubCategoryTypes.isEmpty() )
            {
                final Collection<RolePermission> analyticsRolePermissions = rolePermissionService.getPermissions( orgId, roleId, Category.Type.ANALYTICS );
                final Set<SubCategory.Type> grantedAnalyticsTypes = analyticsRolePermissions.stream()
                                                                                             .filter( RolePermission::isGranted )
                                                                                             .map( rolePermission -> rolePermission.getSubCategory().getType() )
                                                                                             .collect( Collectors.toSet() );

                CodeBlockExecutor.forEachSafely( analyticsRolePermissions,
                                                 analyticsRolePermission ->
                                                 {
                                                     if( analyticsRolePermission.isGranted() )
                                                     {
                                                         final SubCategory.Type analyticsSubcategoryType = analyticsRolePermission.getSubCategory()
                                                                                                                                  .getType();

                                                         if( isAuthorizedSubcategory( analyticsSubcategoryType, sourceSubCategoryTypes, grantedAnalyticsTypes, orgId ) || Objects.equals( SubCategory.Type.ACTIVITIES, analyticsSubcategoryType ) )
                                                         {
                                                             authorizedModuleIds.add( ModuleType.getModuleTypeByName( analyticsSubcategoryType.getAliasName()).getTypeCode());
                                                         }
                                                     }
                                                 } );
            }
        }
        catch( final Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_MODULE,
                         "i18n.analytics.config.invalid.module" );
        }

        return authorizedModuleIds;
    }

    /**
     * Determines whether all the provided report fields belong to modules
     * that are clonable for the given role.
     *
     * @param orgId          Represents the organisation id
     * @param roleId         Represents the role id
     * @param reportFieldIds the collection of report field IDs to verify
     * @return {@code true} if all fields belong to clonable modules; {@code false} otherwise
     */
    public boolean isClonable(final Long orgId, final Long roleId, final Collection<Long> reportFieldIds) {
        final Map<Scope, Set<Integer>> modulesByScope = getAuthorizedModulesByScope(orgId, roleId);
        final Set<Integer> clonableModules = new HashSet<>();

        clonableModules.addAll(modulesByScope.getOrDefault(Scope.VIEW_AND_EDIT, Set.of()));
        clonableModules.addAll(modulesByScope.getOrDefault(Scope.VIEW_EDIT_AND_MANAGE, Set.of()));

        return hasAllModules(orgId, reportFieldIds, clonableModules);
    }

    /**
     * Determines whether the given analytics request is exportable based on
     * the role's module access permissions.
     *
     * @param reportFieldIds the collection of report field IDs involved in the export
     * @param request        the analytics request containing organization and role context
     * @return {@code true} if all fields belong to exportable modules; {@code false} otherwise
     */
    public boolean isExportable(final Collection<Long> reportFieldIds, final AnalyticsRequest request) {
        final Long orgId = request.orgId();
        final Map<Scope, Set<Integer>> modulesByScope = getAuthorizedModulesByScope(orgId, request.roleId());
        final Set<Integer> exportableModules = modulesByScope.getOrDefault(Scope.VIEW_EDIT_AND_MANAGE, Set.of());

        return hasAllModules(orgId, reportFieldIds, exportableModules);
    }

    /**
     * Determines whether the given user has permission to view a chart based on the provided field IDs.
     *
     * @param reportFieldIds     the collection of report field IDs to validate
     * @param analyticsRequest   the {@link AnalyticsRequest} containing user and role information
     * @return {@code true} if the user has view access to all modules required by the fields;
     *         {@code false} otherwise
     */
    public boolean isViewAccessible(final Collection<Long> reportFieldIds, final AnalyticsRequest analyticsRequest) {
        final Long orgId = analyticsRequest.orgId();

        try {
            final Collection<Integer> accessModuleIds = getAuthorizedModuleIds(orgId, analyticsRequest.roleId());
            final Collection<ReportModularField> reportModularFields = reportModularFieldManager.get(orgId, reportFieldIds);

            if (!reportModularFields.isEmpty()) {
                final Collection<Integer> moduleIds = reportModularFields.stream()
                        .map(ReportModularField::getModularField)
                        .filter(Objects::nonNull)
                        .map(ModularField::getModuleId)
                        .collect(Collectors.toSet());

                final boolean isAnyRelatedModuleUnauthorized = reportModularFields.stream()
                        .map(ReportModularField::getModularField)
                        .filter(Objects::nonNull)
                        .map(modularField -> modularField.getModularFieldConfig().getRelatedModuleId())
                        .filter(relatedModuleId -> Objects.nonNull(relatedModuleId) && !Objects.equals(relatedModuleId, NumericalConstant.ZERO))
                        .map(relatedModuleId -> {
                            final ModuleType moduleType = ModuleType.getModuleType(relatedModuleId);
                            return Objects.isNull(moduleType) ? null : moduleType.getTypeCode();
                        })
                        .filter(Objects::nonNull)
                        .anyMatch(resolvedModuleId -> !accessModuleIds.contains(resolvedModuleId));

                if (!isAnyRelatedModuleUnauthorized
                        && accessModuleIds.containsAll(moduleIds)
                        && reportModularFields.stream().allMatch(ReportModularField::getIsActive)) {
                    return true;
                }
            }
        } catch (final Exception exception) {
            LOGGER.error("Analytics Permission: Error occurred while checking view access for orgId {}, reportFieldIds {}, roleId {}, reason {}",
                    orgId, reportFieldIds, analyticsRequest.roleId(), exception.getMessage());
            throw ErrorCode.get(AnalyticsErrorCodes.INVALID_PERMISSION);
        }

        return false;
    }

    /**
     * Determines whether the current user has permission to edit the specified chart.
     *
     * @param reportFieldIds    the collection of report field IDs to check for edit permission
     * @param analyticsRequest  the {@link AnalyticsRequest} containing organization and role details
     * @return {@code true} if the user can edit all modules associated with the given fields;
     *         {@code false} otherwise
     */
    public boolean isEditable(final Collection<Long> reportFieldIds, final AnalyticsRequest analyticsRequest) {
        final Long orgId = analyticsRequest.orgId();

        try {
            final Map<Scope, Set<Integer>> modulesByScope = getAuthorizedModulesByScope(orgId, analyticsRequest.roleId());
            final Set<Integer> editableModules = new HashSet<>();

            editableModules.addAll(modulesByScope.getOrDefault(Scope.VIEW_AND_EDIT, Set.of()));
            editableModules.addAll(modulesByScope.getOrDefault(Scope.VIEW_EDIT_AND_MANAGE, Set.of()));

            return hasAllModules(orgId, reportFieldIds, editableModules);
        } catch (Exception exception) {
            LOGGER.error("Analytics Permission: Error occurred while checking editable permission for orgId {}, reportFieldIds {}, roleId {}, reason {}",
                    orgId, reportFieldIds, analyticsRequest.roleId(), exception.getMessage());
            throw ErrorCode.get(AnalyticsErrorCodes.INVALID_PERMISSION);
        }
    }

    /**
     * Retrieves a mapping of authorized modules grouped by their access scope
     * for a given organization and role.
     *
     * @param orgId  Represents the organisation id
     * @param roleId Represents the role id
     * @return a map where each key represents a {@link Scope} and the value is a set of
     * module IDs the role has access to under that scope
     */
    private Map<Scope, Set<Integer>> getAuthorizedModulesByScope(final Long orgId, final Long roleId) {
        final Map<Scope, Set<Integer>> modulesByScope = new EnumMap<>(Scope.class);
        final Collection<SubCategory.Type> nonSourceAnalyticsSubCategories = SubCategory.Type.getNonSourceAnalyticsSubCategories();
        final Set<SubCategory.Type> authorizedSourceSubCategories = rolePermissionService.getPermissions(orgId, roleId, Category.Type.SOURCES)
                .stream().filter(rolePermission -> rolePermission.isGranted() && Objects.equals(Type.ACCESS, rolePermission.getAction().getType()))
                .map(rolePermission -> rolePermission.getSubCategory().getType()).collect(Collectors.toSet());

        if (authorizedSourceSubCategories.isEmpty()) {
            return modulesByScope;
        }

        final Collection<RolePermission> analyticsRolePermissions = rolePermissionService.getPermissions(orgId, roleId, Category.Type.ANALYTICS);
        final Set<SubCategory.Type> grantedAnalyticsTypes = analyticsRolePermissions.stream()
                .filter(RolePermission::isGranted)
                .map(rolePermission -> rolePermission.getSubCategory().getType())
                .collect(Collectors.toSet());

        for (final RolePermission analyticsPermission : analyticsRolePermissions) {

            if (!analyticsPermission.isGranted()) {
                continue;
            }

            final SubCategory subCategory = analyticsPermission.getSubCategory();
            final SubCategory.Type analyticsType = subCategory.getType();
            final boolean isNonSourceAnalytics = nonSourceAnalyticsSubCategories.contains(analyticsType)
                    && !Objects.equals(SubCategory.Type.DEAL_LINE_ITEMS, analyticsType);

            if (isAuthorizedSubcategory(analyticsType, authorizedSourceSubCategories, grantedAnalyticsTypes, orgId)
                    || isNonSourceAnalytics) {
                final ModuleType moduleTypeByName = ModuleType.getModuleTypeByName(analyticsType.getAliasName());

                modulesByScope.computeIfAbsent(subCategory.getScope(), scope -> new HashSet<>())
                        .add(Objects.isNull(moduleTypeByName) ? null : moduleTypeByName.getTypeCode());
            }
        }

        if (authorizedSourceSubCategories.contains(SubCategory.Type.PRODUCTS)) {

            final int productModuleId = ModuleType.PRODUCT.getTypeCode();

            for (final Set<Integer> moduleIds : modulesByScope.values()) {
                moduleIds.add(productModuleId);
            }
        }

        return modulesByScope;
    }

    /**
     * Checks whether all the given field IDs correspond to modules
     * that exist within a provided set of allowed module IDs.
     *
     * @param orgId            Represents the organisation id
     * @param fieldIds         the collection of field IDs to check
     * @param allowedModuleIds the set of module IDs that are permitted
     * @return {@code true} if all fields belong to allowed modules; {@code false} otherwise
     */
    private boolean hasAllModules(final Long orgId, final Collection<Long> fieldIds, final Set<Integer> allowedModuleIds) {
        try {
            final Collection<ReportModularField> reportModularFields = reportModularFieldManager.get(orgId, fieldIds);
            final Set<Integer> fieldModuleIds = reportModularFields.stream()
                    .map(ReportModularField::getModularField)
                    .filter(Objects::nonNull)
                    .map(ModularField::getModuleId)
                    .collect(Collectors.toSet());

            final boolean isAnyRelatedModuleUnauthorized = reportModularFields.stream()
                    .map(ReportModularField::getModularField)
                    .filter(Objects::nonNull)
                    .map(modularField -> modularField.getModularFieldConfig().getRelatedModuleId())
                    .filter(relatedModuleId -> Objects.nonNull(relatedModuleId) && !Objects.equals(relatedModuleId, NumericalConstant.ZERO))
                    .map(relatedModuleId -> {
                        final ModuleType moduleType = ModuleType.getModuleType(relatedModuleId);
                        return Objects.isNull(moduleType) ? null : moduleType.getTypeCode();
                    })
                    .filter(Objects::nonNull)
                    .anyMatch(resolvedModuleId -> !allowedModuleIds.contains(resolvedModuleId));

            return !isAnyRelatedModuleUnauthorized && allowedModuleIds.containsAll(fieldModuleIds);
        } catch (Exception exception) {
            LOGGER.error("Analytics Permission: Error occurred while validating modules for orgId: {}, fieldIds: {}, allowedModuleIds: {}, reason {}",
                    orgId, fieldIds, allowedModuleIds, exception.getMessage());
            throw ErrorCode.get(AnalyticsErrorCodes.INVALID_PERMISSION);
        }
    }

    /**
     * Checks if the given analytics subcategory type is authorized based on the provided source types
     *
     * @param analyticsType the subcategory type from the analytics role permission
     * @param sourceTypes   a set of subcategory types from the source role permissions
     * @return {@code true} if the analytics subcategory type is authorized based on the source types
     */
    private boolean isAuthorizedSubcategory( final SubCategory.Type analyticsType,
                                             final Collection<SubCategory.Type> sourceTypes,
                                             final Collection<SubCategory.Type> grantedAnalyticsTypes,
                                             final Long orgId )
    {
        return switch( analyticsType )
        {
            case DEALS, CONTACTS, COMPANIES -> sourceTypes.contains( analyticsType );
            case DEAL_LINE_ITEMS -> sourceTypes.contains( SubCategory.Type.DEALS )
                                    && sourceTypes.contains( SubCategory.Type.PRODUCTS )
                                    && grantedAnalyticsTypes.contains( SubCategory.Type.DEALS )
                                    && sourceSettingsService.isSourceEnabled(orgId, SubCategory.Type.PRODUCTS.getAliasName());
            default -> false;
        };
    }
}
