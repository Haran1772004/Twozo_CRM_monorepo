package com.tlc.crm.core.action.transformer;

import com.tlc.crm.common.list.transformer.annotation.Transformer;

import com.tlc.crm.common.list.transformer.model.TransformerRequest;
import com.tlc.crm.common.list.transformer.model.TransformerResponse;

import com.tlc.crm.entity.fields.transformer.TransformerFields;

import lombok.NonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * <p>
 * It transforms the contact entities into required forms.
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
@Transformer( name = "reports", type = Transformer.Type.ENTITY )
public class ReportTransformer extends AbstractModelTransformer
{

    private static final String CREATED_BY  = "createdBy";
    private static final String MODIFIED_BY = "modifiedBy";
    private static final String USER_ID     = "userId";

    /**
     * {@inheritDoc}
     *
     * @param transformerRequest Represents the {@link TransformerRequest}
     * @return the {@link TransformerResponse} which holds the transform details
     */
    @Override
    public TransformerResponse transform( final @NonNull TransformerRequest transformerRequest )
    {
        final Collection<Long>  userIds = new HashSet<>();
        final TransformerResponse transformerResponse = new TransformerResponse();
        final Collection<Map<String, Object>> records = transformerRequest.records();
        final Map<Long, Map<String, Boolean>> dashboardPermissionAccess = getDashboardPermissionAccess( transformerRequest.orgId(),
                                                                                                  false, records );

        for( final Map<String, Object> reportsRow : records )
        {
            final Map<String, Object> users = new HashMap<>();
            final Long pkValue = (Long) reportsRow.get( TransformerFields.ID.getName() );

            if ( dashboardPermissionAccess.containsKey( pkValue ) )
            {
                reportsRow.putAll( dashboardPermissionAccess.get( pkValue ) );
            }

            collectUserIds( CREATED_BY, reportsRow, userIds, users );
            collectUserIds( MODIFIED_BY, reportsRow, userIds, users );

            if( !userIds.isEmpty() )
            {
                reportsRow.put( USER_ID, users );
            }
        }

        transformerResponse.setResults( records );
        transformerResponse.setUserIds( userIds );

        return transformerResponse;
    }
}
