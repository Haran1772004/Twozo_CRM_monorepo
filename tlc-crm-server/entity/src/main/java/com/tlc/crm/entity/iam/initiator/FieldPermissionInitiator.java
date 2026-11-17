package com.tlc.crm.entity.iam.initiator;

import com.tlc.crm.common.iam.OrgInitiator;
import com.tlc.crm.common.iam.OrgInitiatorAction;
import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.internal.CrmUserEntity;

import com.tlc.crm.entity.builder.FieldInfoBuilder;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.manager.CrmFieldsManager;
import com.tlc.crm.entity.role.permission.processor.FieldPermissionProcessor;

import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.service.FieldSourceService;
import com.tlc.crm.security.permission.model.FieldPermissions;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the initializer to execute the creation of the default {@link FieldPermissions} for the organisation.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@OrgInitiator(name = "fieldPermission")
public final class FieldPermissionInitiator implements OrgInitiatorAction {

    private final CrmFieldsManager fieldsManager;
    private final FieldInfoBuilder fieldInfoBuilder;
    private final FieldSourceService fieldSourceService;
    private final FieldPermissionProcessor fieldPermissionProcessor;

    public FieldPermissionInitiator() {
        fieldsManager = CrmFieldsManager.getFieldsManager();
        fieldInfoBuilder = FieldInfoBuilder.getInstance();
        fieldSourceService = FieldSourceService.getInstance();
        fieldPermissionProcessor = FieldPermissionProcessor.getInstance();
    }

    /**
     * <p>
     * Executes the creation for {@link CrmOrg} and {@link CrmUser}
     * </p>
     *
     * @param crmOrg  Represents the {@link CrmOrg}
     * @param crmUser Represents the {@link CrmUserEntity}
     */
    @Override
    public void execute(final CrmOrg crmOrg, final CrmUser crmUser) {
        final Long orgId = crmOrg.id();
        final Collection<String> registeredSourceNames = Source.getPrimarySourceNames();
        final Collection<Long> sourceIds = fieldSourceService.getSources(registeredSourceNames).stream()
                .map(FieldSource::id).collect(Collectors.toSet());

        fieldPermissionProcessor.upsert(true, orgId, fieldInfoBuilder.build(fieldsManager.getFieldsBySource(orgId, sourceIds)));
    }
}
