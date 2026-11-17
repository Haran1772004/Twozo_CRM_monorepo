package com.tlc.crm.core.features.internal.dataimport;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The information that includes the field mapping and header mappings of a source
 *
 * @author sundar
 * @version 1.0
 */
public class SourceFieldHeaders {

    private final Source source;
    private final Map<FieldInfo, Collection<FieldHeader>> fieldHeadersMap;
    private final Map<FieldInfo, FieldHeader> associatedFieldHeaderMap;

    public SourceFieldHeaders(final Source source) {
        this.source = source;
        this.fieldHeadersMap = new HashMap<>();
        this.associatedFieldHeaderMap = new HashMap<>();
    }

    /**
     * Adds the {@link FieldHeader} into the {@link SourceFieldHeaders}
     *
     * @param fieldHeader the {@link FieldHeader} to be added
     */
    public void add(final FieldHeader fieldHeader) {
        fieldHeadersMap.computeIfAbsent(fieldHeader.getImportField(), importField -> new HashSet<>()).add(fieldHeader);
    }

    /**
     * Adds the collection of field headers into the {@link SourceFieldHeaders}
     *
     * @param fieldHeaders Which contains the field headers to be added
     */
    public void addAll(final Collection<FieldHeader> fieldHeaders) {

        for (final FieldHeader fieldHeader : fieldHeaders) {
            add(fieldHeader);
        }
    }

    /**
     * Gets the source
     *
     * @return the source
     */
    public Source getSource() {
        return source;
    }

    /**
     * Gets the {@link Collection} of import fields mapped with the headers
     *
     * @return the import fields mapped with the headers
     */
    public Collection<FieldInfo> getFields() {
        return fieldHeadersMap.keySet();
    }

    /**
     * Gets the {@link FieldHeader} associated with the specified {@link FieldInfo}
     *
     * @param importField the import field to retrieve the header
     * @return the {@link FieldHeader} associated with the specified {@link FieldInfo} optionally
     */
    public Optional<FieldHeader> getFieldHeader(final FieldInfo importField) {
        return fieldHeadersMap.containsKey(importField) ? fieldHeadersMap.get(importField).stream().findAny() : Optional.empty();
    }

    /**
     * Gets the {@link Collection} of field headers associated with the given {@link FieldInfo}
     *
     * @param importField Represents the {@link FieldInfo}
     * @return The {@link Collection} of field headers associated with the given {@link FieldInfo}
     */
    public Collection<FieldHeader> getFieldHeaders(final FieldInfo importField) {
        return fieldHeadersMap.getOrDefault(importField, Collections.emptyList());
    }

    /**
     * Gets all the {@link Collection} of field headers associated with the current source
     *
     * @return The {@link Collection} of field headers associated with the current source
     */
    public Collection<FieldHeader> getFieldHeaders() {
        return fieldHeadersMap.values().stream().flatMap(Collection::stream).toList();
    }

    /**
     * Gets the {@link Collection} of field headers associated with the given {@link FieldInfo}
     *
     * @return The {@link Collection} of field headers associated with the given {@link FieldInfo}
     */
    public Collection<FieldHeader> getFieldHeaders(final Collection<FieldInfo> importFields) {
        return fieldHeadersMap.entrySet().stream().filter(entry -> importFields.contains(entry.getKey())).map(Map.Entry::getValue).flatMap(Collection::stream).toList();
    }

    /**
     * Retrieves the collection of field headers associated with the current source,
     * sorted in a specified order.
     *
     * @return The sorted collection of field headers.
     */
    public Collection<FieldHeader> getSortedFieldHeaders(final boolean includeAssociatedHeaders) {
        final Collection<FieldHeader> fieldHeaders;

        if (includeAssociatedHeaders) {
            fieldHeaders = new HashSet<>(getFieldHeaders());
            fieldHeaders.addAll(associatedFieldHeaderMap.values());
        } else {
             fieldHeaders = getFieldHeaders();
        }
        final Stream<FieldHeader> fieldHeaderStream = fieldHeaders.stream();

        if (Source.isPrimarySource(source)) {
            return fieldHeaderStream.sorted(Comparator.comparing(FieldHeader::getImportField, Comparator.nullsLast(Comparator.comparing(field -> field.presetConfig().order()))).thenComparing(fieldHeader ->
                    Objects.requireNonNullElse(fieldHeader.getImportField().aliasName(), ""))).toList();
        } else {
            return fieldHeaderStream.sorted(Comparator.comparing(FieldHeader::getImportField, Comparator.comparing(importField ->
                    Objects.requireNonNullElse(importField.aliasName(), "")))).toList();
        }
    }

    /**
     * Checks whether the given {@link FieldInfo} mapped with any header or not
     *
     * @param importField Which contains importable field info
     * @return true if the {@link FieldInfo} linked with any header
     */
    public boolean contains(final FieldInfo importField) {
        return fieldHeadersMap.containsKey(importField);
    }

    /**
     * Checks if the subfields of the provided field are mapped with headers
     *
     * @param importField Represents the field to check
     * @return true if the subfield(s) of the field are mapped with headers, otherwise false
     */
    public boolean hasSubFields(final FieldInfo importField) {
        return fieldHeadersMap.containsKey(importField) && fieldHeadersMap.get(importField).stream().anyMatch(FieldHeader::isSubFieldHeader);
    }

    /**
     * Adds the associated fields with this field header
     *
     * @param associatedFieldHeader Represents the field header to be associated
     */
    public void addAssociatedField(final FieldInfo field, final FieldHeader associatedFieldHeader) {

        if (!fieldHeadersMap.containsKey(field)) {
            associatedFieldHeaderMap.put(field, new FieldHeader(field, associatedFieldHeader.getHeaderIndex(), true));
        }
    }
}