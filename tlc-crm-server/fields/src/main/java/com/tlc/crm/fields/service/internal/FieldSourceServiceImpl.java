package com.tlc.crm.fields.service.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.fields.internal.status.FieldErrorCodes;
import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.models.settings.SourceSettings;
import com.tlc.crm.fields.resolver.FieldSourceResolver;
import com.tlc.crm.fields.service.FieldSourceService;
import com.tlc.crm.fields.service.settings.SourceSettingsService;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Implements the {@link FieldSourceService} provides functionality for accessing {@link FieldSource}.
 *
 * @author Kishore J
 * @version 1.0
 * @see FieldSourceService
 */
public class FieldSourceServiceImpl implements FieldSourceService {

    private final SourceSettingsService sourceSettingsService;
    private final FieldSourceResolver fieldSourceResolver;

    private FieldSourceServiceImpl() {
        sourceSettingsService = SourceSettingsService.getInstance();
        fieldSourceResolver = FieldSourceResolver.getInstance();
    }

    private static class Instance {
        private static final FieldSourceServiceImpl INSTANCE = new FieldSourceServiceImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link FieldSourceServiceImpl}.
     * </p>
     *
     * @return {@link FieldSourceServiceImpl}.
     */
    public static FieldSourceServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param id Field source id
     * @return The {@link FieldSource}
     */
    @Override
    public FieldSource getSource(final Long id) {
        final FieldSource fieldSource = fieldSourceResolver.get(id);

        if (Objects.isNull(fieldSource)) {
            throw ErrorCode.get(FieldErrorCodes.FIELD_SOURCE_NOT_FOUND);
        } else {
            return fieldSource;
        }
    }

    /**
     * <p>
     * Fetches the collection of field source entities for the given collection of field source ids.
     * </p>
     *
     * @param ids Collection of field source ids
     * @return Collection of field source entities
     */
    @Override
    public Collection<FieldSource> getFieldSourcesByIds(final Collection<Long> ids) {
        return fieldSourceResolver.getAll().stream().filter(Objects::nonNull)
                .filter(fieldSource -> ids.contains(fieldSource.id())).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceName Represents source name.
     * @return {@link FieldSource}
     */
    @Override
    public FieldSource getSource(final String sourceName) {
        final Optional<FieldSource> entitySource = fieldSourceResolver.getAll().stream()
                .filter(source -> sourceName.equalsIgnoreCase(source.getName())).findAny();

        if (entitySource.isPresent()) {
            return entitySource.get();
        } else {
            throw ErrorCode.get(FieldErrorCodes.FIELD_SOURCE_NOT_FOUND);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Collection} of FieldSource.
     */
    @Override
    public Collection<FieldSource> getSources() {
        return fieldSourceResolver.getAll();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Refers the organization id.
     * @param sourceName Refers the source name.
     * @return The field source.
     */
    @Override
    public FieldSource retrieveSourceWithSettings(final Long orgId, final String sourceName) {
        final FieldSource fieldSource = getSource(sourceName);
        final SourceSettings sourceSettings = sourceSettingsService.getSourceSettings(orgId, fieldSource.id());

        if (Objects.nonNull(sourceSettings)) {
            fieldSource.setSettings(sourceSettings);
        }

        return fieldSource;
    }

    /**
     * <p>
     * Fetches the collection of field source entities.
     * </p>
     *
     * @return Collection of field source entities
     */
    public Collection<FieldSource> getSources(final Collection<String> sourceNames) {
        return fieldSourceResolver.getAll().stream().filter(fieldSource -> sourceNames.contains(fieldSource.getName())).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Map} containing key as field source id value as {@link FieldSource}.
     */
    public Map<Long, FieldSource> groupSources() {
        return fieldSourceResolver.groupSources();
    }
}
