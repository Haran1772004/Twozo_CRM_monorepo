package com.tlc.analytics.core.api.internal;

import com.tlc.analytics.config.model.view.ViewModel;
import com.tlc.analytics.core.api.ViewRequest;

import com.tlc.crm.common.action.CrmRequest;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * {@link DefaultViewRequest} represents a default implementation of a view request.
 * It implements the {@link ViewRequest} interface and specifies a generic type V that extends {@link ViewModel}.
 * This class contains information about the requested view and the {@link CrmRequest} associated with it.
 *
 * @author Aswath
 * @version 1.0
 */
@SuperBuilder
@Getter
public class DefaultViewRequest<V extends ViewModel> implements ViewRequest<V>
{
    // The view associated with the request
    private final V view;

    // The request associated with the view
    private final CrmRequest request;
    private final Map<String, Object> attributes = new HashMap<>();

    /**
     * Check the view ID associated with the request.
     *
     * @return true if the view id is null, false otherwise.
     */
    @Override
    public boolean isNewView()
    {
        return Objects.isNull( view.getViewId() );
    }
}
