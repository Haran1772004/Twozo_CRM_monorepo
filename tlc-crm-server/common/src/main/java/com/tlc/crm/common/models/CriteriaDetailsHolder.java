package com.tlc.crm.common.models;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Holds the details of the criteria and provides the respective {@link WhereClause}.
 *
 * @author Sathish
 * @version 1.0
 */
public class CriteriaDetailsHolder {

    private final Collection<CriteriaDetails> criteriaDetailsList;

    public CriteriaDetailsHolder() {
        criteriaDetailsList = new ArrayList<>();
    }

    public void addCriteria(final CriteriaDetails criteriaDetails) {
        criteriaDetailsList.add(criteriaDetails);
    }

    /**
     * Builds a composite {@link WhereClause} from a collection of criteria configurations.
     *
     * @param columnProvider Represents the column based on the field.
     * @return an {@link Optional} containing the built {@link WhereClause}, if any
     */
    public Optional<WhereClause> getCriteria(final Function<String, Column> columnProvider) {
        if (criteriaDetailsList.isEmpty()) {
            return Optional.empty();
        }
        WhereClause mandatoryCriteria = null;
        WhereClause optionalCriteria = null;

        for (final CriteriaDetails criteriaDetails : criteriaDetailsList) {
            final BiFunction<WhereClause, Boolean, WhereClause> criteriaLoader = getWhereClauseAdder(criteriaDetails, columnProvider);

            if (criteriaDetails.isMandatory()) {
                mandatoryCriteria = criteriaLoader.apply(mandatoryCriteria, true);
            } else {
                optionalCriteria = criteriaLoader.apply(optionalCriteria, false);
            }
        }
        return Optional.ofNullable(WhereClauseUtil.connect(mandatoryCriteria, optionalCriteria, Connector.AND));
    }

    /**
     * Creates a function to add a {@link Criteria} to a {@link WhereClause} based on field and operator.
     *
     * @param criteriaDetails the criteria configuration to build.
     * @param columnProvider Represents the column based on the field.
     * @return a function that adds the criteria to a {@link WhereClause}
     */
    private BiFunction<WhereClause, Boolean, WhereClause> getWhereClauseAdder(final CriteriaDetails criteriaDetails,
                                                                              final Function<String, Column> columnProvider) {
        final Column column = columnProvider.apply(criteriaDetails.field());
        final Criteria criteria = new Criteria(column, criteriaDetails.value(), criteriaDetails.operator());

        return (whereClause, isMandatory) -> {
            if (Objects.isNull(whereClause)) {
                whereClause = new WhereClause(criteria);
            } else if (isMandatory) {
                whereClause.and(criteria);
            } else {
                whereClause.or(criteria);
            }
            return whereClause;
        };
    }
}
