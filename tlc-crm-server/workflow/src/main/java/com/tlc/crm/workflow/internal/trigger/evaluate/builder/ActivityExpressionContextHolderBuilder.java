package com.tlc.crm.workflow.internal.trigger.evaluate.builder;

import com.tlc.commons.pair.Pair;

import com.tlc.l10n.temporal.resolver.DateTimeUtil;
import com.tlc.validator.expression.model.ExpressionContextHolder;

import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;

import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.features.api.activity.model.ActivityType;
import com.tlc.crm.entity.features.api.activity.model.Availability;
import com.tlc.crm.entity.features.api.activity.model.Priority;
import com.tlc.crm.entity.features.api.activity.model.Reminder;
import com.tlc.crm.entity.features.api.activity.model.Status;
import com.tlc.crm.entity.features.api.note.model.Note;

import com.tlc.crm.workflow.api.model.WorkflowField;
import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaSet;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * The builder that constructs the expression for {@link ActivityHolder}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ActivityExpressionContextHolderBuilder extends AbstractExpressionContextHolderBuilder<ActivityHolder> {

    private final DateTimeUtil dateTimeUtil;

    private ActivityExpressionContextHolderBuilder() {
        super(TriggerSource.ACTIVITY);
        dateTimeUtil = DateTimeUtil.getInstance();
    }

    private static final class Instance {
        private static final ActivityExpressionContextHolderBuilder INSTANCE = new ActivityExpressionContextHolderBuilder();
    }

    /**
     * Gets the instance of {@link ActivityExpressionContextHolderBuilder}.
     *
     * @return {@link ActivityExpressionContextHolderBuilder} instance.
     */
    public static ActivityExpressionContextHolderBuilder getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param activityHolderPair Refers to the {@link Pair} consisting of an old and current model of the same entity.
     * @param criteriaSets       Refers the {@link WorkflowCriteriaSet} to be checked.
     * @return The {@link ExpressionContextHolder}.
     */
    @Override
    public ExpressionContextHolder buildExpressionContextHolder(final Pair<ActivityHolder, ActivityHolder> activityHolderPair,
                                                                final Collection<WorkflowCriteriaSet> criteriaSets) {
        return build(activityHolderPair, criteriaSets);
    }

    /**
     * {@inheritDoc}
     *
     * @param activityHolder Refers the model from which the value is fetched.
     * @param fieldId        Refers the field id.
     * @return The field value.
     */
    @Override
    protected Object getValue(final ActivityHolder activityHolder, final Long fieldId) {
        final WorkflowField workflowField = workflowFieldService.get(fieldId);
        final Activity activity = activityHolder.getActivity();

        return switch (workflowField.getAliasName()) {
            case "title" -> TEXT_NORMALIZER.apply(activity.getTitle());
            case "activityType" -> {
                final ActivityType activityType = activity.getType();

                yield Objects.isNull(activityType) ? null : activityType.id();
            }
            case "availability" -> {
                final Availability availability = activity.getAvailability();

                yield Objects.isNull(availability) ? null : availability.getId();
            }
            case "description" -> TEXT_NORMALIZER.apply(activity.getDescription());
            case "startTime" -> dateTimeUtil.getTimeStamp(activity.getStartTime());
            case "endTime" -> dateTimeUtil.getTimeStamp(activity.getEndTime());
            case "assignee" -> activityHolder.getActivity().getAssigneeId();
            case "reminder" -> {
                final Reminder reminder = activity.getReminder();

                yield Objects.isNull(reminder) ? null : reminder.getId();
            }
            case "priority" -> {
                final Priority priority = activity.getPriority();

                yield Objects.isNull(priority) ? null : priority.getId();
            }
            case "location" -> TEXT_NORMALIZER.apply(activity.getLocation());
            case "contact" -> {
                final Collection<Contact> contacts = activityHolder.getContacts();

                yield contacts.isEmpty() ? Collections.emptyList() : contacts.stream().map(Contact::id).toList();
            }
            case "deal" -> {
                final Deal deal = activityHolder.getDeal();

                yield Objects.isNull(deal) ? null : deal.id();
            }
            case "company" -> {
                final Company company = activityHolder.getCompany();

                yield Objects.isNull(company) ? null : company.id();
            }
            case "note" -> {
                final Note note = activity.getNote();

                yield Objects.isNull(note) ? EMPTY_STRING : TEXT_NORMALIZER.apply(note.getContentBody());
            }
            case "status" -> {
                final Status status = activity.getStatus();

                yield Objects.isNull(status) ? null : status.getId();
            }
            default -> null;
        };
    }

}
