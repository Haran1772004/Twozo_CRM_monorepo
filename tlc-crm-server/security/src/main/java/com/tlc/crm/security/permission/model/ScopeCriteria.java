package com.tlc.crm.security.permission.model;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.secure.JoinConfigs;

/**
 * <p>
 * A model that represents the criteria of the scope participant
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public class ScopeCriteria {

    private JoinConfigs joinConfigs;
    private ScopeParticipant scopeParticipant;
    private Column criteriaColumn;
    private boolean joinsRequired;

    public ScopeCriteria() {
    }

    public void setJoinConfigs(final JoinConfigs joinConfigs) {
        this.joinConfigs = joinConfigs;
    }

    public JoinConfigs getJoinConfigs() {
        return joinConfigs;
    }

    public ScopeParticipant getScopeParticipant() {
        return scopeParticipant;
    }

    public void setScopeParticipant(final ScopeParticipant scopeParticipant) {
        this.scopeParticipant = scopeParticipant;
    }

    public Column getCriteriaColumn() {
        return criteriaColumn;
    }

    public Column getCriteriaColumn(final Table table) {
        return table.getColumn(criteriaColumn.getColumnName());
    }

    public void setCriteriaColumn(final Column criteriaColumn) {
        this.criteriaColumn = criteriaColumn;
    }

    public boolean isJoinsRequired() {
        return joinsRequired;
    }

    public void setJoinsRequired(final boolean joinsRequired) {
        this.joinsRequired = joinsRequired;
    }
}