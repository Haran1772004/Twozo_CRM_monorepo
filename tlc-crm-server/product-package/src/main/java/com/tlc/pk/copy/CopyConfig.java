package com.tlc.pk.copy;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * Represents the base configuration class for copy operations.
 * </p>
 *
 * @author pandiarajkumar G
 * @version 1.0
 */
@Getter
@SuperBuilder( builderMethodName = "copyConfig" )
public abstract class CopyConfig<I>
{
    @Builder.Default
    private final CopyHandler<I> copyHandler = new CopyHandler.DefaultHandler<>();

    /**
     * Retrieves the original object that needs to be copied.
     * Subclasses must implement this method to provide the source object for the copy operation.
     *
     * @return the original object to be copied
     */
    @NonNull
    protected abstract I getOriginalItem();
}
