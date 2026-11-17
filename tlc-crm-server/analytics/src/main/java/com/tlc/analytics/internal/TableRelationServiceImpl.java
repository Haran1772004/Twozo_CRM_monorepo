package com.tlc.analytics.internal;

import com.tlc.analytics.config.model.table.TableRelation;
import com.tlc.analytics.resolver.table.TableRelationResolver;
import com.tlc.analytics.service.table.TableRelationService;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of {@link TableRelationService} backed by {@link TableRelationResolver}.
 *
 * @author Karthick V
 * @version 1.0
 */
public class TableRelationServiceImpl implements TableRelationService {

    private final TableRelationResolver resolver;

    /**
     * <p>
     * Private constructor initializing the resolver dependency.
     * </p>
     */
    private TableRelationServiceImpl() {
        this.resolver = TableRelationResolver.getInstance();
    }

    private static class Instance {
        private static final TableRelationServiceImpl INSTANCE = new TableRelationServiceImpl();
    }

    /**
     * <p>
     * Retrieves the singleton instance of {@link TableRelationServiceImpl}.
     * </p>
     *
     * @return {@link TableRelationServiceImpl}
     */
    public static TableRelationServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TableRelation get(final Long id) {
        if (Objects.isNull(id)) {
            return null;
        }
        return resolver.getById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<TableRelation> getAll(final Set<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return ids.stream().map(this::get).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
