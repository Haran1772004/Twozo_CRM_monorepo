package com.tlc.crm.auto.assignment.internal.listener;

import com.tlc.crm.auto.assignment.fields.AutoAssignmentAttributes;

import com.tlc.crm.entity.fieldaudit.manager.FieldAuditManager;
import com.tlc.crm.entity.fieldaudit.models.FieldAuditSource;

import com.tlc.scheduler.algorithm.roundrobin.manager.RoundRobinTrackerManager;
import com.tlc.scheduler.algorithm.roundrobin.service.RuleBasedRoundRobinStrategy;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.listeners.RowDeleteListener;
import com.tlc.sql.common.api.listeners.RowUpdateListener;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Listener that reacts to row-level delete events for Assignment Rule records.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class AssignmentRuleRowListener implements RowDeleteListener, RowUpdateListener {

    private final RoundRobinTrackerManager roundRobinTrackerManager;
    private final RuleBasedRoundRobinStrategy ruleBasedRoundRobinStrategy;
    private final FieldAuditManager fieldAuditManager;

    private AssignmentRuleRowListener() {
        this.roundRobinTrackerManager = RoundRobinTrackerManager.getInstance();
        this.ruleBasedRoundRobinStrategy = RuleBasedRoundRobinStrategy.getInstance();
        this.fieldAuditManager = FieldAuditManager.getInstance();
    }

    private final static class Instance {
        private static final AssignmentRuleRowListener INSTANCE = new AssignmentRuleRowListener();
    }

    public static AssignmentRuleRowListener getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param rows Refers the deleted rows.
     */
    @Override
    public void deleteRows(final Collection<Row> rows) {
        rows.forEach(row -> ruleBasedRoundRobinStrategy.delete(String.format("%s:%d:%d", AutoAssignmentAttributes.AUTO_ASSIGNMENT, row.getOrgId(), row.getPKValue())));

        roundRobinTrackerManager.deleteRoundRobinRule(rows.stream().map(Row::getPKValue).collect(Collectors.toUnmodifiableSet()));
        fieldAuditManager.deactivateAuditSource(rows.iterator().next().getOrgId(), FieldAuditSource.AUTO_ASSIGNMENT, rows.stream().map(Row::getPKValue).collect(Collectors.toSet()));
    }

    /**
     * {@inheritDoc}
     *
     * @param rows Refers the updated rows.
     */
    @Override
    public void updateRows(final Collection<Row> rows) {
        rows.forEach(row -> ruleBasedRoundRobinStrategy.resetIndex(String.format("%s:%d:%d", AutoAssignmentAttributes.AUTO_ASSIGNMENT, row.getOrgId(), row.getPKValue())));
    }

}
