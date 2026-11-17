package com.tlc.crm.workflow.v2.internal.execution;

import com.tlc.crm.core.workflow.internal.WorkflowEmailTracker;

import com.tlc.crm.workflow.api.model.CriteriaConnector;
import com.tlc.crm.workflow.api.model.WorkflowComparator;
import com.tlc.crm.workflow.api.model.WorkflowCriteria;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaField;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaSet;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaValue;
import com.tlc.crm.workflow.api.model.WorkflowField;
import com.tlc.crm.workflow.api.model.history.WorkflowExecutionStatus.WorkflowCriteriaExecutionStatus;
import com.tlc.crm.workflow.cache.service.WorkflowFieldService;
import com.tlc.crm.workflow.v2.api.model.ConditionConfiguration;

import java.util.Collection;
import java.util.Objects;

/**
 * The {@code WorkflowEmailConditionValidator} is responsible for validating the email condition.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0.0
 * @since 1.0.0
 */
public final class WorkflowEmailConditionValidator {

    private final WorkflowFieldService workflowFieldService;

    private WorkflowEmailConditionValidator() {
        this.workflowFieldService = WorkflowFieldService.getInstance();
    }

    private static final class Instance {
        private static final WorkflowEmailConditionValidator INSTANCE = new WorkflowEmailConditionValidator();
    }

    /**
     * Gets the instance of {@code WorkflowEmailConditionValidator}.
     *
     * @return The instance of {@code WorkflowEmailConditionValidator}.
     */
    public static WorkflowEmailConditionValidator getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Validates the email condition.
     *
     * @param context                The node execution context.
     * @param workflowEmailTracker   The workflow email tracker.
     * @param conditionConfiguration The condition configuration.
     * @return The workflow criteria execution status.
     */
    public WorkflowCriteriaExecutionStatus validate(final NodeExecutionContext context, final WorkflowEmailTracker workflowEmailTracker,
                                                    final ConditionConfiguration conditionConfiguration) {
        final boolean isClicked = workflowEmailTracker.isClicked();
        final boolean isReplied = !workflowEmailTracker.isInitialMessage();
        final int criteriaSetSize = conditionConfiguration.getCriteriaSets().size();

        Boolean expression = null;
        CriteriaConnector setConnector = null;
        int criteriaSetIndex = 1;

        for (final WorkflowCriteriaSet criteriaSet : conditionConfiguration.getCriteriaSets()) {
            final Collection<WorkflowCriteria> criteriaList = criteriaSet.getCriteriaList();
            final int criteriaSize = criteriaList.size();

            Boolean criteriaExpression = null;
            CriteriaConnector criteriaConnector = null;
            int criteriaIndex = 1;

            for (final WorkflowCriteria workflowCriteria : criteriaSet.getCriteriaList()) {
                final WorkflowCriteriaField workflowCriteriaField = workflowCriteria.getCriteriaField();
                final WorkflowField workflowField = workflowFieldService.get(workflowCriteriaField.getFieldId());
                final Collection<WorkflowCriteriaValue> criteriaValues = workflowCriteria.getCriteriaValues();
                final String aliasName = workflowField.getAliasName();
                final WorkflowComparator workflowComparator = workflowCriteria.getComparator();
                final Boolean value = criteriaValues.stream().findFirst().map(criteriaValue -> Boolean.valueOf((String) criteriaValue.getValue())).orElseGet(() -> null);

                final Boolean result = switch (aliasName) {
                    case "emailRepliedTo" -> switch (workflowComparator.getAliasName()) {
                        case "equal" -> Objects.equals(value, isReplied);
                        case "notEqual" -> !Objects.equals(value, isReplied);
                        default -> null;
                    };
                    case "emailLinkClicked" -> switch (workflowComparator.getAliasName()) {
                        case "equal" -> Objects.equals(value, isClicked);
                        case "notEqual" -> !Objects.equals(value, isClicked);
                        default -> null;
                    };
                    default -> null;
                };

                if (null == criteriaExpression) {
                    criteriaExpression = result;
                } else {
                    if (criteriaIndex++ < criteriaSize) {
                        criteriaExpression = CriteriaConnector.AND == criteriaConnector
                                ? criteriaExpression && result : criteriaExpression || result;
                    }
                }
                criteriaConnector = workflowCriteria.getConnector();
            }
            if (null == expression) {
                expression = criteriaExpression;
            } else {

                if (criteriaSetIndex++ < criteriaSetSize) {
                    expression = CriteriaConnector.AND == setConnector
                            ? expression && criteriaExpression : expression || criteriaExpression;
                }
            }
            setConnector = criteriaSet.getConnector();
        }
        return null == expression ? WorkflowCriteriaExecutionStatus.FAILED
                : expression ? WorkflowCriteriaExecutionStatus.PASSED : WorkflowCriteriaExecutionStatus.FAILED;
    }

}
