package com.tlc.mcp.resource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpResourceDef {

    String uri();

    String name();

    String title() default "";

    String mimeType() default "text/plain";

    String version() default "1";
}
