package com.tlc.pk.export.csvexporter.data;

import jakarta.validation.constraints.NotNull;

import lombok.Builder;
import lombok.Getter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Represents a collection of data to be written in CSV format.
 *
 * <p>This class encapsulates the headers and rows that make up the CSV content.</p>
 */
@Builder
@Getter
public class Data
{

    private final @NotNull Optional<List<String>> headers;

    private final @NotNull Collection<Row> rows;

    private final @NotNull Function<Cell<?>, String> stringConverter;

}
