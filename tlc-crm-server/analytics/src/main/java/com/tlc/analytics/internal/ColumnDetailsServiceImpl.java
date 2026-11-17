package com.tlc.analytics.internal;

import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.resolver.column.ColumnDetailsResolver;
import com.tlc.analytics.service.column.ColumnDetailsService;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of {@link ColumnDetailsService} backed by {@link ColumnDetailsResolver}.
 *
 * @author Karthick V
 * @version 1.0
 */
public class ColumnDetailsServiceImpl implements ColumnDetailsService {

    private final ColumnDetailsResolver columnDetailsResolver;

    /**
     * <p>
     * Private constructor initializing the resolver dependency.
     * </p>
     */
    private ColumnDetailsServiceImpl() {
        this.columnDetailsResolver = ColumnDetailsResolver.getInstance();
    }

    private static class Instance {
        private static final ColumnDetailsServiceImpl INSTANCE = new ColumnDetailsServiceImpl();
    }

    /**
     * <p>
     * Retrieves the singleton instance of {@link ColumnDetailsServiceImpl}.
     * </p>
     *
     * @return {@link ColumnDetailsServiceImpl}
     */
    public static ColumnDetailsServiceImpl getInstance() {
        return Instance.INSTANCE;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ColumnDetail> getByTableId(final Long tableId) {
        if (Objects.isNull(tableId)) {
            return Collections.emptyList();
        }
        final Collection<ColumnDetail> result = columnDetailsResolver.getBySourceId(tableId);

        return Objects.nonNull(result) ? result : Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ColumnDetail get(final Long columnId) {
        if (Objects.isNull(columnId)) {
            return null;
        }
        return columnDetailsResolver.getById(columnId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ColumnDetail> getAll(final Set<Long> columnIds) {
        if (Objects.isNull(columnIds) || columnIds.isEmpty()) {
            return Collections.emptyList();
        }
        return columnIds.stream().map(this::get).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
