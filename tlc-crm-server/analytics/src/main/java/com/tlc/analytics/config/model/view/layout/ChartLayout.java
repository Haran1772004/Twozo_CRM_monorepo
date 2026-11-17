package com.tlc.analytics.config.model.view.layout;

import lombok.experimental.SuperBuilder;

/**
 * {@link ChartLayout} is a specific implementation of {@link BaseViewLayout}, representing the layout details
 * for charts within a user interface. It inherits properties such as width, height, row index, column index,
 * sharable status, associated view, and favorite status from {@link BaseViewLayout}. Use this class to
 * customize and extend view layout behavior specifically for Charts.
 *
 * @author Aswath
 * @version 1.0
 */
@SuperBuilder
public class ChartLayout extends BaseViewLayout
{}
