package com.tlc.mcp.tool;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface McpTool {

    String name();

    String title() default "";

    String description() default "";

    String version() default "1";

    boolean deprecated() default false;

    boolean readOnly() default false;

    boolean destructive() default false;

    boolean idempotent() default false;

    String executor() default "default";

    int timeoutSeconds() default -1;

    ExecutionMode mode() default ExecutionMode.SYNC;
}
