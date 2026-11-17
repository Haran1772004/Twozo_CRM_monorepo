package com.tlc.crm.entity.list.filter.appender;

import com.tlc.crm.entity.list.filter.appender.model.CriteriaAppenderRequisites;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds and manages a collection of {@link CriteriaAppender} instances to build a composite {@link WhereClause}.
 *
 * <p>This class acts as a registry and executor for criteria-based SQL filtering logic. It maintains
 * an ordered map of appender types to their respective appender implementations, and allows
 * sequential composition of filters during query building.</p>
 *
 * @author Sathish
 * @version 1.0
 */
public final class CriteriaAppenderHolder {

    /**
     * Stores criteria appenders mapped by their type. Preserves insertion order.
     */
    private final Map<CriteriaAppender.Type, CriteriaAppender<?>> criteriaAppenderMap;

    /**
     * Constructs an empty holder ready to collect {@link CriteriaAppender} instances.
     */
    public CriteriaAppenderHolder() {
        criteriaAppenderMap = new LinkedHashMap<>();
    }

    /**
     * Adds a new criteria appender to the holder.
     *
     * @param criteriaAppender the appender to register
     * @param <R>              the type of {@link CriteriaAppenderRequisites} associated with this appender
     */
    public <R extends CriteriaAppenderRequisites> void add(final CriteriaAppender<R> criteriaAppender) {
        criteriaAppenderMap.put(criteriaAppender.getType(), criteriaAppender);
    }

    /**
     * Retrieves a specific {@link CriteriaAppender} by its type.
     *
     * @param criteriaAppenderType the type identifier
     * @param <R>                  the expected type of {@link CriteriaAppenderRequisites}
     * @return the corresponding {@link CriteriaAppender}, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <R extends CriteriaAppenderRequisites> CriteriaAppender<R> get(final CriteriaAppender.Type criteriaAppenderType) {
        return (CriteriaAppender<R>) criteriaAppenderMap.get(criteriaAppenderType);
    }

    /**
     * Sequentially applies all registered {@link CriteriaAppender} instances to the provided {@link WhereClause}.
     *
     * <p>Only appenders that have a column (i.e., `hasColumn() == true`) will be applied.</p>
     *
     * @param existingCriteria the initial where clause to build upon
     * @return the composed {@link WhereClause} after all applicable criteria are appended
     */
    public WhereClause doAppend(final WhereClause existingCriteria) {
        WhereClause whereClause = existingCriteria;

        for (final CriteriaAppender<?> criteriaAppender : criteriaAppenderMap.values()) {
            if (criteriaAppender.hasColumn()) {
                whereClause = criteriaAppender.append(whereClause);
            }
        }

        return whereClause;
    }
}