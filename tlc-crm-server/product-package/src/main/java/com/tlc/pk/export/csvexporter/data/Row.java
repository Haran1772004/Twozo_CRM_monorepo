package com.tlc.pk.export.csvexporter.data;

import jakarta.validation.constraints.NotNull;

import lombok.Builder;
import lombok.Getter;

import java.util.Collection;

/**
 * Represents a single row of data in a CSV file.
 *
 * <p>A row consists of multiple {@code Cell} objects, each representing a value
 * in the CSV output.</p>
 */
@Getter
@Builder
public class Row
{

    private final @NotNull Collection<Cell<?>> cells;

}
