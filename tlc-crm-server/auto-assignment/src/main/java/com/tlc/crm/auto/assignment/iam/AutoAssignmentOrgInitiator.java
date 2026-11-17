package com.tlc.crm.auto.assignment.iam;

import com.tlc.commons.Counter;

import com.tlc.crm.common.iam.OrgInitiator;
import com.tlc.crm.common.iam.OrgInitiatorAction;
import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.auto.assignment.api.manager.AutoAssignmentManager;
import com.tlc.crm.auto.assignment.api.manager.AutoAssignmentTemplateManager;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentRule;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentComparator;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentCriteria;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentCriteriaSet;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentCriteriaValue;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentTemplate;
import com.tlc.crm.auto.assignment.api.model.AssignmentRuleUser;
import com.tlc.crm.auto.assignment.resolver.AutoAssignmentCriteriaResolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

/**
 * An action to perform the initialization of default {@code AutoAssignment} while org is being initialized.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@OrgInitiator(name = "autoAssignment")
public class AutoAssignmentOrgInitiator implements OrgInitiatorAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoAssignmentOrgInitiator.class);

    private final AutoAssignmentTemplateManager assignmentTemplateManager;

    public AutoAssignmentOrgInitiator() {
        this.assignmentTemplateManager = AutoAssignmentTemplateManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmOrg  Represents the {@code CrmOrg}
     * @param crmUser Represents the {@code CrmUserEntity}
     */
    @Override
    public void execute(final CrmOrg crmOrg, final CrmUser crmUser) {
        final Long orgId = crmOrg.id();
        final Long userId = crmUser.id();
        final Collection<AutoAssignmentTemplate> templates = assignmentTemplateManager.getAllTemplates();
        final Collection<AutoAssignmentRule> autoAssignmentRuleList = new LinkedList<>();
        final Counter counter = new Counter(1);

        for (final AutoAssignmentTemplate template : templates) {
            final AutoAssignmentRule autoAssignmentRule = new AutoAssignmentRule();

            autoAssignmentRule.setOrgId(orgId);
            autoAssignmentRule.setName(template.name().getKey());
            autoAssignmentRule.setDescription(template.description().getKey());
            autoAssignmentRule.setCreatedBy(crmUser);
            autoAssignmentRule.setUpdatedBy(crmUser);
            autoAssignmentRule.setActive(false);
            autoAssignmentRule.setAssignmentModule(template.assignmentModule());
            autoAssignmentRule.setTemplateId(template.id());
            autoAssignmentRule.setOrder((short) counter.getAndIncrement());

            for (final AutoAssignmentCriteriaSet criteriaSet : template.criteriaSets()) {
                final AutoAssignmentCriteriaSet assignmentCriteriaSet = new AutoAssignmentCriteriaSet();

                assignmentCriteriaSet.setConnector(criteriaSet.getConnector());
                assignmentCriteriaSet.setOrder(criteriaSet.getOrder());
                assignmentCriteriaSet.setCriteriaList(resolveCriteria(orgId, criteriaSet.getCriteriaList()));
                autoAssignmentRule.addCriteriaSet(assignmentCriteriaSet);
            }
            final AssignmentRuleUser assignmentRuleUser = new AssignmentRuleUser();

            assignmentRuleUser.setUserId(userId);
            autoAssignmentRule.addAssignmentRuleUser(assignmentRuleUser);
            autoAssignmentRuleList.add(autoAssignmentRule);
        }

        AutoAssignmentManager.getInstance().create(orgId, autoAssignmentRuleList);
        LOGGER.info("Org Init Action: Auto-assignment initialization completed. Org ID: {}, User ID: {}", orgId, userId);
    }

    /**
     * Resolve the collection of auto assignment criteria for the specified organization.
     *
     * @param orgId        The organization id.
     * @param criteriaList The collection of AutoAssignmentCriteria to resolve.
     * @return A collection of resolved AutoAssignmentCriteria objects.
     */
    private Collection<AutoAssignmentCriteria> resolveCriteria(final long orgId, final Collection<AutoAssignmentCriteria> criteriaList) {
        final AutoAssignmentCriteriaResolver assignmentCriteriaResolver = AutoAssignmentCriteriaResolver.getInstance();
        final Collection<AutoAssignmentCriteria> assignmentCriteriaList = new ArrayList<>();

        for (final AutoAssignmentCriteria templateCriteria : criteriaList) {
            final AutoAssignmentComparator templateCriteriaComparator = templateCriteria.getComparator();
            final Collection<AutoAssignmentCriteriaValue> templateCriteriaValues = templateCriteria.getCriteriaValues();
            final AutoAssignmentCriteria assignmentCriteria = new AutoAssignmentCriteria();

            assignmentCriteria.setFieldId(templateCriteria.getFieldId());
            assignmentCriteria.setComparator(templateCriteriaComparator);
            assignmentCriteria.setConnector(templateCriteria.getConnector());
            assignmentCriteria.setOrder(templateCriteria.getOrder());

            if (templateCriteriaComparator.hasValue()) {

                for (final AutoAssignmentCriteriaValue templateCriteriaValue : templateCriteriaValues) {
                    assignmentCriteriaResolver.resolveTemplateCriteria(orgId, templateCriteria, templateCriteriaValue);
                }
            }
            assignmentCriteria.setCriteriaValues(templateCriteriaValues);
            assignmentCriteriaList.add(assignmentCriteria);
        }
        return assignmentCriteriaList;
    }
}
