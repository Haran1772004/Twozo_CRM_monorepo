package com.tlc.crm.workflow.resolver;

import com.tlc.crm.workflow.api.model.WorkflowCriteria;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaField;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaValue;

import com.tlc.i18n.I18nResolutionAdapter;

/**
 * <p>
 * A resolver that resolves the {@link WorkflowCriteria} related details.
 * </p>
 *
 * @author karthick V
 * @version 1.0
 */
public interface CriteriaResolver {

    /**
     * <p>
     * Configures the value and entity id for a given {@link WorkflowCriteriaValue}
     * </p>
     *
     * @param orgId                   Represents the organization id
     * @param workflowCriteriaField   It holds the {@link WorkflowCriteriaField} details
     * @param workflowCriteriaValue   It holds the {@link WorkflowCriteriaValue} details
     * @param i18NResolutionAdapter Represents the resolver of the I18N strings
     */
    void resolveCriteriaDetails(final Long orgId, final WorkflowCriteriaField workflowCriteriaField,
                                final WorkflowCriteriaValue workflowCriteriaValue, final I18nResolutionAdapter i18NResolutionAdapter);
}
