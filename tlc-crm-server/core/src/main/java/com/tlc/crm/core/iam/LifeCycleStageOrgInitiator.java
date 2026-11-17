package com.tlc.crm.core.iam;

import com.tlc.commons.pair.Pair;
import com.tlc.crm.common.iam.OrgInitiator;
import com.tlc.crm.common.iam.OrgInitiatorAction;
import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.contact.model.LifeCycleRuleDetails;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.dropdown.contact.ContactLifecycleStage;
import com.tlc.crm.core.dropdown.contact.ContactLifecycleStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;

/**
 * An action performed by the life cycle stage org initializer to initialize organizational fields
 * with predefined values upon the creation of a new organization.
 *
 * @author Muthu kumar V
 * @version 1.0
 */
@OrgInitiator(name = "lifeCycleStage")
public class LifeCycleStageOrgInitiator implements OrgInitiatorAction {

    private final LifeCycleStageManager lifeCycleStageManager;

    public LifeCycleStageOrgInitiator() {
        lifeCycleStageManager = LifeCycleStageManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmOrg  Represents the {@link CrmOrg}
     * @param crmUser Represents the {@link CrmUser}
     */
    @Override
    public void execute(final CrmOrg crmOrg, final CrmUser crmUser) {
        final Long orgId = crmOrg.id();
        final Map<ContactLifecycleStage, Pair<LifeCycleStage, LifeCycleStatus>> lifeCycleStageMap = new EnumMap<>(ContactLifecycleStage.class);

        for (final ContactLifecycleStage contactLifecycleStage : ContactLifecycleStage.values()) {
            final LifeCycleStage lifecycleStage = new LifeCycleStage();

            lifecycleStage.setOrgId(orgId);
            lifecycleStage.setName(contactLifecycleStage.getName());
            lifecycleStage.setOrder(contactLifecycleStage.getOrder());
            lifecycleStage.setSystemStage(true);
            lifecycleStage.setLastStage(contactLifecycleStage.isLastStage());
            lifecycleStage.setStatuses(ContactLifecycleStatus.get(contactLifecycleStage).stream().map(lifecycleStatus ->
                    buildLifecycleStatus(orgId, lifecycleStatus)).sorted(Comparator.comparing(LifeCycleStatus::getOrder)).toList());

            lifeCycleStageManager.create(lifecycleStage);

            lifecycleStage.getStatuses().stream().filter(LifeCycleStatus::isDefault).forEach(lifeCycleStatus ->
                lifeCycleStageMap.put(contactLifecycleStage, Pair.of(lifecycleStage, lifeCycleStatus))
            );
        }
        final Collection<LifeCycleRuleDetails> lifeCycleRuleDetailsCollection = new ArrayList<>();

        for (final LifeCycleRuleDetails.LifeCycleRuleType lifeCycleRuleType : LifeCycleRuleDetails.LifeCycleRuleType.values()) {
            final LifeCycleRuleDetails lifeCycleRuleDetails = new LifeCycleRuleDetails(lifeCycleRuleType);
            final Pair<LifeCycleStage, LifeCycleStatus> lifeCyclePair = lifeCycleStageMap.get(lifeCycleRuleType.getDefaultContactLifecycleStage());

            lifeCycleRuleDetails.setOrgId(orgId);
            lifeCycleRuleDetails.setStage(lifeCyclePair.getKey());
            lifeCycleRuleDetails.setStatus(lifeCyclePair.getValue());
            lifeCycleRuleDetailsCollection.add(lifeCycleRuleDetails);
        }
        lifeCycleStageManager.updateRules(orgId, lifeCycleRuleDetailsCollection);
    }

    /**
     * <p>
     * Builds the {@link LifeCycleStatus}
     * </p>
     *
     * @param orgId                  Represents the organisation id.
     * @param contactLifecycleStatus Represents the lifecycle status.
     * @return {@link LifeCycleStatus}.
     */
    private LifeCycleStatus buildLifecycleStatus(final Long orgId, final ContactLifecycleStatus contactLifecycleStatus) {
        final LifeCycleStatus lifeCycleStatus = new LifeCycleStatus();

        lifeCycleStatus.setOrgId(orgId);
        lifeCycleStatus.setName(contactLifecycleStatus.getName());
        lifeCycleStatus.setOrder(contactLifecycleStatus.getOrder());
        lifeCycleStatus.setLabel(contactLifecycleStatus.getLabel());
        lifeCycleStatus.setDefault(contactLifecycleStatus.isDefault());
        lifeCycleStatus.setSystemStatus(true);

        return lifeCycleStatus;
    }
}