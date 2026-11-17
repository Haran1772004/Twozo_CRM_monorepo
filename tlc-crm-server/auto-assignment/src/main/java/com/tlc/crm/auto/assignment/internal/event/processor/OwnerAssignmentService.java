package com.tlc.crm.auto.assignment.internal.event.processor;

import com.tlc.commons.json.Json;
import com.tlc.crm.auto.assignment.api.manager.AutoAssignmentManager;
import com.tlc.crm.auto.assignment.api.model.AssignmentRuleUser;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentRule;
import com.tlc.crm.auto.assignment.fields.AutoAssignmentAttributes;

import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.internal.event.index.publisher.IndexDataPublisher;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.fieldaudit.models.FieldAuditInfo;
import com.tlc.crm.entity.fieldaudit.models.FieldAuditSource;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.timeline.field.TimelineActionFields;
import com.tlc.crm.entity.timeline.field.TimelineFields;
import com.tlc.crm.entity.timeline.model.FieldTimeline;
import com.tlc.crm.entity.timeline.model.Timeline;
import com.tlc.crm.entity.timeline.model.TimelineType;

import com.tlc.iam.common.models.User;
import com.tlc.l10n.temporal.model.DateTime;

import com.tlc.scheduler.algorithm.roundrobin.manager.RoundRobinGroupManager;
import com.tlc.scheduler.algorithm.roundrobin.manager.RoundRobinTrackerManager;
import com.tlc.scheduler.algorithm.roundrobin.model.RoundRobinRule;
import com.tlc.scheduler.algorithm.roundrobin.model.RoundRobinTracker;
import com.tlc.scheduler.algorithm.roundrobin.service.RoundRobinService;

import com.tlc.validator.TlcModel;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import java.time.Instant;

/**
 * Handles the owner assignment of entities by round-robin scheduling services.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 * @see RoundRobinService
 */
final class OwnerAssignmentService extends RoundRobinService {

    private final ContactManager contactManager;
    private final CompanyManager companyManager;
    private final DealManager dealManager;
    private final FieldService fieldService;
    private final RoundRobinTrackerManager roundRobinTrackerManager;
    private final RoundRobinGroupManager roundRobinGroupManager;
    private final IndexDataPublisher indexDataPublisher;
    private final UserService crmUserService;
    private final AutoAssignmentManager autoAssignmentManager;

    private OwnerAssignmentService() {
        this.contactManager = ContactManager.getInstance();
        this.companyManager = CompanyManager.getInstance();
        this.dealManager = DealManager.getInstance();
        this.fieldService = FieldService.getInstance();
        this.roundRobinTrackerManager = RoundRobinTrackerManager.getInstance();
        this.roundRobinGroupManager = RoundRobinGroupManager.getInstance();
        this.indexDataPublisher = IndexDataPublisher.getInstance();
        this.crmUserService = UserService.getInstance();
        this.autoAssignmentManager = AutoAssignmentManager.getInstance();
    }

    private static final class Instance {
        private static final OwnerAssignmentService ASSIGNMENT_SERVICE = new OwnerAssignmentService();
    }

    static OwnerAssignmentService getInstance() {
        return Instance.ASSIGNMENT_SERVICE;
    }

    /**
     * Performs the auto-assignment operation for a single model within the given organization.
     * and determines the appropriate user or owner based on the rule configuration, and applies
     * the assignment to the provided {@link TlcModel}.
     *
     * @param orgId              The ID of the organization in which the assignment is executed.
     * @param model              The model/entity to be assigned (e.g., lead, deal, note).
     * @param autoAssignmentRule The assignment rule that defines the assignment strategy.
     * @param source             The origin of the assignment request.
     */
    void assign(final long orgId, final TlcModel model, final AutoAssignmentRule autoAssignmentRule, final Source source) {
        final List<AssignmentRuleUser> sortedUsers = autoAssignmentRule.getAssignmentRuleUsers().stream()
                .filter(assignmentRuleUser -> User.State.ACTIVE == assignmentRuleUser.getCrmUser().getState())
                .sorted(Comparator.comparing(assignmentRuleUser -> assignmentRuleUser.getCrmUser().getFullName())).toList();
        final Consumer<RoundRobinTracker> assigner = roundRobinTracker -> {
            roundRobinTracker.setTotalCount(sortedUsers.size());

            final int index = getIndex(roundRobinTracker);
            final AssignmentRuleUser assignmentRuleUser = sortedUsers.get(index);

            assignSalesOwner(orgId, model, assignmentRuleUser, source, autoAssignmentRule.id());
            roundRobinTrackerManager.updateLastAssignedIndex(orgId, roundRobinTracker.id(), autoAssignmentRule.id(), index); // TODO #3300
        };

        final Optional<RoundRobinTracker> optionalRoundRobinTracker = roundRobinTrackerManager.getByRuleEntityId(orgId, autoAssignmentRule.id());

        optionalRoundRobinTracker.ifPresentOrElse(assigner, () -> {
            final RoundRobinTracker roundRobinTracker = new RoundRobinTracker();
            final RoundRobinRule roundRobinRule = new RoundRobinRule();

            roundRobinTracker.setOrgId(orgId);
            roundRobinTracker.setRoundRobinGroup(roundRobinGroupManager.getByName("autoAssignment"));
            roundRobinTracker.setCurrentIndex(0);
            roundRobinTracker.setTotalCount(sortedUsers.size());

            roundRobinRule.setOrgId(orgId);
            roundRobinRule.setRuleEntityId(autoAssignmentRule.id());
            roundRobinTracker.setRoundRobinRule(roundRobinRule);

            roundRobinTrackerManager.create(roundRobinTracker);
            assigner.accept(roundRobinTracker);
        });
    }

    /**
     * Assigns the sales owner to the given model based on the resolved
     * {@link AssignmentRuleUser} and executes the necessary update logic.
     *
     * @param orgId              The ID of the organization in which the assignment is performed.
     * @param model              The model/entity whose sales owner is being set.
     * @param assignmentRuleUser The resolved user to whom the model should be assigned.
     * @param source             The source of the model.
     */
    private void assignSalesOwner(final long orgId, final TlcModel model, final AssignmentRuleUser assignmentRuleUser,
                                  final Source source, final Long ruleId) {
        switch (source) {
            case COMPANY -> {
                final Company company = (Company) model;
                final Long oldOwnerId = company.getOwnerId();
                final Long userId = assignmentRuleUser.getUserId();
                final Timeline timeline = buildTimeline(orgId, oldOwnerId, source, String.valueOf(oldOwnerId), String.valueOf(userId), ruleId);

                companyManager.updateOwnerAndTerritory(orgId, model.id(), timeline, userId, assignmentRuleUser.getTerritory());
                company.setOwnerId(userId);
                company.setSalesOwner(crmUserService.getUser(orgId, userId));
            }
            case CONTACT -> {
                final Contact contact = (Contact) model;
                final Long oldOwnerId = contact.getOwnerId();
                final Long userId = assignmentRuleUser.getUserId();
                final Timeline timeline = buildTimeline(orgId, oldOwnerId, source, String.valueOf(oldOwnerId), String.valueOf(userId), ruleId);

                contactManager.updateOwnerAndTerritory(orgId, model.id(), timeline, userId, assignmentRuleUser.getTerritory());
                contact.setOwnerId(userId);
                contact.setSalesOwner(crmUserService.getUser(orgId, userId));
            }
            case DEAL -> {
                final Deal deal = (Deal) model;
                final Long oldOwnerId = deal.getOwnerId();
                final Long userId = assignmentRuleUser.getUserId();
                final Timeline timeline = buildTimeline(orgId, oldOwnerId, source, String.valueOf(oldOwnerId), String.valueOf(userId), ruleId);

                dealManager.updateOwnerAndTerritory(orgId, model.id(), timeline, userId, assignmentRuleUser.getTerritory());
                deal.setOwnerId(userId);
                deal.setSalesOwner(crmUserService.getUser(orgId, userId));
            }
        }
        indexDataPublisher.publishIndexData(orgId, List.of(model), Action.UPDATE);
    }

    /**
     * Builds a {@link Timeline} entry representing a sales-owner assignment
     * update for the specified model.
     *
     * @param orgId    The organization ID under which the timeline event is recorded.
     * @param ownerId  The ID of the user who becomes the new owner of the model.
     * @param source   The origin of the update (e.g., API, auto-assignment, migration).
     * @param oldValue The previous owner value recorded before assignment.
     * @param newValue The new owner value after assignment.
     * @return A fully populated {@link Timeline} object representing the update event.
     */
    private Timeline buildTimeline(final long orgId, final long ownerId, final Source source,
                                   final String oldValue, final String newValue, final Long autoAssignmentRuleId) {
        if (Objects.equals(oldValue, newValue)) {
            return null;
        }
        final Timeline timeline = new Timeline();
        final FieldInfo salesOwnerField = fieldService.groupDefaultFieldsByAliasName(orgId, source.getName(),
                List.of(AutoAssignmentAttributes.SALES_OWNER)).get(AutoAssignmentAttributes.SALES_OWNER);

        timeline.setAction(TimelineActionFields.REASSIGNED);
        timeline.setCategory(switch (source) {
            case COMPANY -> Timeline.Category.COMPANY;
            case CONTACT -> Timeline.Category.CONTACT;
            case DEAL -> Timeline.Category.DEAL;
            default -> null;
        });
        timeline.setOrgId(orgId);
        timeline.setOwnerId(ownerId);
        timeline.setTimelineType(TimelineType.OWNER_ASSIGNED_BY_RULE);
        timeline.setAuditedTime(new DateTime(Instant.now().toEpochMilli()));
        timeline.setIconType(TimelineFields.USER.getName());

        final FieldTimeline fieldTimeline = new FieldTimeline(List.of(oldValue), newValue, salesOwnerField.i18nName());

        timeline.setFieldTimeline(fieldTimeline);
        timeline.setFieldAuditInfo(new FieldAuditInfo(FieldAuditSource.AUTO_ASSIGNMENT, autoAssignmentRuleId, Json.object()
                .put(AutoAssignmentAttributes.NAME, autoAssignmentManager.getName(orgId, autoAssignmentRuleId))
                .put(AutoAssignmentAttributes.IS_ACTIVE, true)
        ));
        return timeline;
    }
}
