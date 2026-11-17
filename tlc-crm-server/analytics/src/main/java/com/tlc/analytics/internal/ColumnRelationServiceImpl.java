package com.tlc.analytics.internal;

import com.tlc.analytics.config.model.table.ColumnRelation;
import com.tlc.analytics.resolver.column.ColumnRelationResolver;
import com.tlc.analytics.service.column.ColumnRelationService;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Implementation of {@link ColumnRelationService} backed by {@link ColumnRelationResolver}.
 *
 * @author Karthick V
 * @version 1.0
 */
public class ColumnRelationServiceImpl implements ColumnRelationService {

    private final ColumnRelationResolver resolver;

    /**
     * <p>
     * Private constructor initializing the resolver dependency.
     * </p>
     */
    private ColumnRelationServiceImpl() {
        this.resolver = ColumnRelationResolver.getInstance();
    }

    private static class Instance {
        private static final ColumnRelationServiceImpl INSTANCE = new ColumnRelationServiceImpl();
    }

    /**
     * <p>
     * Retrieves the singleton instance of {@link ColumnRelationServiceImpl}.
     * </p>
     *
     * @return {@link ColumnRelationServiceImpl}
     */
    public static ColumnRelationServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ColumnRelation> getColumnRelationsList(final Long relationId) {
        if (Objects.isNull(relationId)) {
            return Collections.emptyList();
        }
        final Collection<ColumnRelation> result = resolver.getBySourceId(relationId);

        return Objects.nonNull(result) ? result : Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ColumnRelation getColumnRelation(final Long id) {
        if (Objects.isNull(id)) {
            return null;
        }
        return resolver.getById(id);
    }
}
