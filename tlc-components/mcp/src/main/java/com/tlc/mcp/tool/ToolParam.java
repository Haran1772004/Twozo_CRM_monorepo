package com.tlc.mcp.tool;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Documented
public @interface ToolParam {

    String name() default "";

    String description() default "";

    boolean required() default true;

    String defaultValue() default "";

    String min() default "";

    String max() default "";

    int minLength() default -1;

    int maxLength() default -1;

    String pattern() default "";

    String[] options() default {};

    boolean multiSelect() default false;

    Class<? extends OptionsProvider> dynamicOptions() default OptionsProvider.None.class;
}
