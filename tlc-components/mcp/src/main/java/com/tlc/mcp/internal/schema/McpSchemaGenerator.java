package com.tlc.mcp.internal.schema;

import com.tlc.mcp.internal.registry.ParameterSpec;
import com.tlc.mcp.tool.OptionsProvider;
import com.tlc.mcp.tool.SchemaDefinition;
import com.tlc.mcp.tool.SchemaDefinition.FieldDef;
import com.tlc.mcp.tool.ToolParam;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class McpSchemaGenerator {

    public SchemaDefinition generate(Method executeMethod, List<ParameterSpec> specs) {
        final List<FieldDef> fields = new ArrayList<>();
        final Parameter[] params = executeMethod.getParameters();

        for (int i = 0; i < specs.size(); i++) {
            final ParameterSpec spec = specs.get(i);
            if (spec.isMcpToolContext() || spec.isCatchAll()) {
                continue;
            }
            final Parameter param = params[i];
            final ToolParam ann    = param.getAnnotation(ToolParam.class);
            fields.add(buildField(spec.name(), spec.type(), param.getParameterizedType(),
                    ann, new LinkedHashSet<>()));
        }

        return SchemaDefinition.of(fields);
    }

    private FieldDef buildField(String name, Class<?> type, Type genericType,
                                 ToolParam ann, Set<Class<?>> visitedTypes) {
        final FieldDef.Builder b = FieldDef.builder(name);

        if (ann != null) {
            b.description(ann.description())
             .required(ann.required())
             .defaultValue(ann.defaultValue())
             .min(ann.min())
             .max(ann.max())
             .minLength(ann.minLength())
             .maxLength(ann.maxLength())
             .pattern(ann.pattern())
             .multiSelect(ann.multiSelect());

            final List<String> opts = resolveOptions(ann, type);
            if (!opts.isEmpty()) {
                b.options(opts);
            }
        }

        if (type.isEnum()) {
            b.type("string");
            final List<String> enumVals = new ArrayList<>();
            for (Object constant : type.getEnumConstants()) {
                enumVals.add(((Enum<?>) constant).name());
            }
            if (ann == null || ann.options().length == 0) {
                b.options(enumVals);
            }
            return b.build();
        }

        if (type.isArray()) {
            b.type("array");
            final Class<?> componentType = type.getComponentType();
            b.itemType(javaTypeToSchema(componentType));
            return b.build();
        }
        if (Collection.class.isAssignableFrom(type)) {
            b.type("array");
            final String itemSchemaType = resolveGenericItemType(genericType);
            b.itemType(itemSchemaType);
            return b.build();
        }

        if (isComplexPojo(type)) {
            if (visitedTypes.contains(type)) {
                throw new IllegalArgumentException(
                        "Circular reference detected in tool schema at type: " + type.getName());
            }
            visitedTypes.add(type);
            b.type("object").nestedFields(reflectPojoFields(type, visitedTypes));
            visitedTypes.remove(type);
            return b.build();
        }

        b.type(javaTypeToSchema(type));
        return b.build();
    }

    private String resolveGenericItemType(Type genericType) {
        if (genericType instanceof ParameterizedType pt) {
            final Type[] args = pt.getActualTypeArguments();
            if (args.length > 0 && args[0] instanceof Class<?> argClass) {
                return javaTypeToSchema(argClass);
            }
        }
        return "string";
    }

    private List<String> resolveOptions(ToolParam ann, Class<?> type) {
        if (ann.options().length > 0) {
            return List.of(ann.options());
        }
        if (ann.dynamicOptions() != OptionsProvider.None.class) {
            try {
                final OptionsProvider provider = ann.dynamicOptions()
                        .getDeclaredConstructor().newInstance();
                return provider.options();
            } catch (Exception e) {
                return List.of();
            }
        }
        return List.of();
    }

    private List<FieldDef> reflectPojoFields(Class<?> type, Set<Class<?>> visitedTypes) {
        final List<FieldDef> nested = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            final ToolParam ann = field.getAnnotation(ToolParam.class);
            if (ann == null) {
                continue;
            }
            final String name = ann.name().isEmpty() ? field.getName() : ann.name();
            nested.add(buildField(name, field.getType(), field.getGenericType(), ann,
                    new LinkedHashSet<>(visitedTypes)));
        }
        return nested;
    }

    private boolean isComplexPojo(Class<?> type) {
        return !type.isPrimitive()
            && !type.isEnum()
            && !type.isArray()
            && !type.getName().startsWith("java.")
            && !type.getName().startsWith("javax.")
            && !Collection.class.isAssignableFrom(type)
            && !Map.class.isAssignableFrom(type);
    }

    private String javaTypeToSchema(Class<?> type) {
        if (type == null) return "string";
        if (type == boolean.class || type == Boolean.class) return "boolean";
        if (type == int.class    || type == Integer.class
         || type == long.class   || type == Long.class
         || type == short.class  || type == Short.class
         || type == byte.class   || type == Byte.class)  return "integer";
        if (type == float.class  || type == Float.class
         || type == double.class || type == Double.class) return "number";
        if (type.isArray() || Collection.class.isAssignableFrom(type)) return "array";
        if (isComplexPojo(type)) return "object";
        return "string";
    }
}
