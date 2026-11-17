package com.tlc.analytics.internal;

import com.tlc.analytics.config.model.table.TableDetail;
import com.tlc.analytics.resolver.table.TableDetailResolver;
import com.tlc.analytics.service.table.TableDetailService;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of {@link TableDetailService} backed by {@link TableDetailResolver}.
 *
 * @author Karthick V
 * @version 1.0
 */
public class TableDetailServiceImpl implements TableDetailService {

    private final TableDetailResolver tableDetailResolver;

    /**
     * <p>
     * Private constructor initializing the resolver dependency.
     * </p>
     */
    private TableDetailServiceImpl() {
        this.tableDetailResolver = TableDetailResolver.getInstance();
    }

    private static class Instance {
        private static final TableDetailServiceImpl INSTANCE = new TableDetailServiceImpl();
    }

    /**
     * <p>
     * Retrieves the singleton instance of {@link TableDetailServiceImpl}.
     * </p>
     *
     * @return {@link TableDetailServiceImpl}
     */
    public static TableDetailServiceImpl getInstance() {
        return Instance.INSTANCE;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<TableDetail> getBySourceId(final Integer moduleId) {
        if (Objects.isNull(moduleId)) {
            return Collections.emptyList();
        }
        final Collection<TableDetail> result = tableDetailResolver.getBySourceId(moduleId);

        return Objects.nonNull(result) ? result : Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TableDetail get(final Long tableId) {
        if (Objects.isNull(tableId)) {
            return null;
        }
        return tableDetailResolver.getById(tableId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<TableDetail> getAll(final Set<Long> tableIds) {
        if (Objects.isNull(tableIds) || tableIds.isEmpty()) {
            return Collections.emptyList();
        }
        return tableIds.stream().map(this::get).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
