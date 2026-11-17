package com.tlc.crm.core.api.analytics.permission.verifier.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.executor.CodeBlockExecutor;

import com.tlc.crm.core.api.analytics.entity.config.modules.ModuleType;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMChildViewKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMDashboardKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMViewPropertyKeys;
import com.tlc.crm.core.api.analytics.permission.handler.AnalyticsPermissionHandler;

import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.role.permission.verifier.AbstractPermissionsVerifier;

import com.tlc.crm.security.permission.annotation.authorizer.AnalyticsAuthorizer;
import com.tlc.commons.annotation.InstanceProvider;
import com.tlc.crm.security.permission.annotation.verifier.Verifier;
import com.tlc.crm.security.permission.verifier.request.VerificationRequest;
import com.tlc.crm.security.permission.verifier.response.VerificationResponse;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * <p>
 * The {@code AnalyticsPermissionsVerifier} class is represents for verifying permissions related to analytics operation.
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
@Verifier( authorizerClass = AnalyticsAuthorizer.class )
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class AnalyticsPermissionsVerifier extends AbstractPermissionsVerifier<AnalyticsAuthorizer>
{

    private static final class Instance
    {
        private static final AnalyticsPermissionsVerifier ANALYTICS_PERMISSIONS_VERIFIER = new AnalyticsPermissionsVerifier();
    }

    @InstanceProvider
    public static AnalyticsPermissionsVerifier getInstance()
    {
        return Instance.ANALYTICS_PERMISSIONS_VERIFIER;
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Holds the details of the request of verifier.
     * @param response Holds the details of the response of verifier.
     */
    @Override
    public void verify( final @NonNull VerificationRequest<AnalyticsAuthorizer> request,
                        final @NonNull VerificationResponse response )
    {
        final long orgId = request.orgId();
        final long roleId = request.roleId();
        final JsonObject requestJson = request.requestJson();
        final String apiType = request.apiDetails().type();

        if( Objects.nonNull( apiType ) )
        {
            switch ( apiType )
            {
                case "get" ->
                {
                    verifyAccess( orgId, roleId, apiType, requestJson.getJsonObject( FieldKeys.DATA.getName() ), request, response );
                }
                case "update" ->
                {
                    final String viewPropertiesKey = CRMChildViewKeys.VIEW_PROPERTIES;
                    final String metricsKey = CRMViewPropertyKeys.METRICS;
                    final JsonObject jsonObject = requestJson.getJsonObject( FieldKeys.DATA.getName() );
                    final JsonArray jsonArray = jsonObject.getJsonArray( CRMDashboardKeys.CHILD_VIEW );
                    final int totalViews = jsonArray.size();

                    for( int index = 0; index < totalViews; index++ )
                    {
                        final JsonObject viewPropertyJson = jsonArray.getJsonObject( index );
                        final JsonObject viewJson = viewPropertyJson.getJsonObject( viewPropertiesKey );
                        final JsonArray metricsJsonArray  = viewJson.getJsonArray( metricsKey );
                        final int totalMetrics = metricsJsonArray.size();

                        for( int iIndex = 0; iIndex < totalMetrics; iIndex++ )
                        {
                            verifyAccess( orgId, roleId, apiType, metricsJsonArray.getJsonObject( iIndex ), request, response );
                        }
                    }
                }
                case "apply" ->
                {
                    final JsonObject jsonObject = requestJson.getJsonObject(FieldKeys.DATA.getName());
                    final JsonObject viewJson = jsonObject.getJsonObject(CRMChildViewKeys.VIEW_PROPERTIES);
                    final JsonArray metricsJsonArray = viewJson.getJsonArray(CRMViewPropertyKeys.METRICS);
                    final int totalMetrics = metricsJsonArray.size();

                    for(int index = 0; index < totalMetrics; index++ )
                    {
                        verifyAccess( orgId, roleId, apiType, metricsJsonArray.getJsonObject( index ), request, response );
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Verifies whether the user has the necessary permissions to perform a specified operation on a given analytics module.
     * </p>
     *
     * @param orgId      Represents an organisation id
     * @param apiType    Represents the API type
     * @param jsonObject Holds the {@link JsonObject} details
     * @param request    Represents the {@link VerificationRequest} details
     * @param response   Represents the {@link VerificationResponse} details
     */
    private void verifyAccess( @NonNull final Long orgId,
                               @NonNull final Long roleId,
                               @NonNull final String apiType,
                               @NonNull final JsonObject jsonObject,
                               @NonNull final VerificationRequest<AnalyticsAuthorizer> request,
                               @NonNull final VerificationResponse response )
    {
        final AnalyticsPermissionHandler analyticsPermissionHandler = AnalyticsPermissionHandler.getInstance();
        final Collection<Integer> accessModuleIds = analyticsPermissionHandler.getAuthorizedModuleIds( orgId, roleId );

        if( jsonObject.containsKey( CRMReportKeys.MODULE_IDS ) )
        {
            final JsonArray jsonArray = jsonObject.getJsonArray( CRMReportKeys.MODULE_IDS );
            final Collection<Integer> moduleIds = new HashSet<>();
            final int totalModules = jsonArray.size();

            for( int index = 0; index < totalModules; index ++ )
            {
                moduleIds.add( jsonArray.getInt( index ) );
            }

            if( !accessModuleIds.containsAll( moduleIds ) )
            {
                try
                {
                    CodeBlockExecutor.forEachSafely( moduleIds,
                                                     moduleId ->
                                                     {
                                                         final ModuleType moduleType = ModuleType.getModuleType( moduleId );
                                                         response.addError( String.format( response.resolveI18nKey( "i18n.role.permission.error.subcategory.access.restricted"),
                                                                                            response.resolveI18nKey( moduleType.getI18nKey() ) ) );
                                                     } );
                }
                catch( final Exception exception )
                {
                    throw ErrorCode.get( AnalyticsErrorCodes.INVALID_MODULE,
                                 "i18n.analytics.config.invalid.module" );                }
            }
        }
    }
}
