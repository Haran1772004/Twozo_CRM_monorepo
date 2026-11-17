package com.tlc.pk.export.csvexporter.data;

import jakarta.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

/**
 * Represents a single cell in a CSV row.
 *
 * <p>A cell holds a value of type {@code E}, which represents the data to be
 * written in a CSV field.</p>
 *
 * @param <E> the type of the value stored in the cell
 */
@Builder
public class Cell<E>
{

    @Getter(AccessLevel.PUBLIC)
    private final @NotNull E value;

}
