package com.tlc.mcp.internal.registrar;

import com.tlc.mcp.internal.registry.McpToolVersionRegistry;
import com.tlc.mcp.internal.registry.ParameterSpec;
import com.tlc.mcp.internal.registry.ToolRegistration;
import com.tlc.mcp.internal.schema.McpSchemaGenerator;
import com.tlc.mcp.tool.AbstractMcpTool;
import com.tlc.mcp.tool.McpTool;
import com.tlc.mcp.tool.McpToolContext;
import com.tlc.mcp.tool.SchemaDefinition;
import com.tlc.mcp.tool.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class McpToolRegistrar {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpToolRegistrar.class);

    private final McpToolVersionRegistry registry;
    private final McpSchemaGenerator schemaGenerator;

    public McpToolRegistrar(McpToolVersionRegistry registry, McpSchemaGenerator schemaGenerator) {
        this.registry = registry;
        this.schemaGenerator = schemaGenerator;
    }

    public void register(String bundleSymbolicName, Class<?> toolClass) {
        final McpTool annotation = toolClass.getAnnotation(McpTool.class);
        if (annotation == null) {
            return;
        }
        if (!AbstractMcpTool.class.isAssignableFrom(toolClass)) {
            LOGGER.warn("Tool class {} must extend AbstractMcpTool — skipped", toolClass.getName());
            return;
        }

        final Method executeMethod = findExecuteMethod(toolClass);
        if (executeMethod == null) {
            LOGGER.warn("Tool class {} has @McpTool but no public execute() method — skipped",
                    toolClass.getName());
            return;
        }

        final List<ParameterSpec> params = buildParameterSpecs(executeMethod);
        final SchemaDefinition baseSchema = schemaGenerator.generate(executeMethod, params);

        final AbstractMcpTool<?> tool;
        try {
            tool = (AbstractMcpTool<?>) toolClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            LOGGER.error("Cannot instantiate tool class {} — must have no-arg constructor",
                    toolClass.getName(), e);
            return;
        }

        final McpToolContext<Object> nullCtx = new McpToolContext<>(annotation.name(), annotation.version(), null);
        final String inputSchemaJson = tool.resolveSchema(baseSchema, nullCtx).toJsonSchema().toString();

        final ToolRegistration registration = new ToolRegistration(
                annotation.name(),
                annotation.version(),
                annotation.title().isEmpty() ? annotation.name() : annotation.title(),
                annotation.description(),
                annotation.readOnly(),
                annotation.destructive(),
                annotation.idempotent(),
                annotation.deprecated(),
                annotation.executor(),
                annotation.timeoutSeconds(),
                annotation.mode(),
                tool,
                executeMethod,
                params,
                baseSchema,
                inputSchemaJson
        );

        registry.register(bundleSymbolicName, registration);
    }

    public void unregisterBundle(String bundleSymbolicName) {
        registry.unregisterBundle(bundleSymbolicName);
    }

    private Method findExecuteMethod(Class<?> toolClass) {
        for (Method method : toolClass.getMethods()) {
            if ("execute".equals(method.getName())) {
                return method;
            }
        }
        return null;
    }

    private List<ParameterSpec> buildParameterSpecs(Method executeMethod) {
        final List<ParameterSpec> specs = new ArrayList<>();
        for (Parameter param : executeMethod.getParameters()) {
            final boolean isContext = McpToolContext.class.isAssignableFrom(param.getType());
            final boolean isCatchAll = Map.class.isAssignableFrom(param.getType())
                    && param.getAnnotation(ToolParam.class) == null;

            String name;
            if (isContext) {
                name = param.getName();
            } else if (param.isAnnotationPresent(ToolParam.class)) {
                final ToolParam ann = param.getAnnotation(ToolParam.class);
                name = ann.name().isEmpty() ? param.getName() : ann.name();
            } else {
                name = param.getName();
            }

            specs.add(new ParameterSpec(name, param.getType(), isContext, isCatchAll));
        }
        return specs;
    }
}
