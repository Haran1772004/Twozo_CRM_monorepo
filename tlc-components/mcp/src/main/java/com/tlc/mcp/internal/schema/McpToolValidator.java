package com.tlc.mcp.internal.schema;

import com.tlc.mcp.tool.SchemaDefinition;
import com.tlc.mcp.tool.SchemaDefinition.FieldDef;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class McpToolValidator {

    public void validate(Map<String, Object> args, SchemaDefinition schema) {
        final List<String> errors = new ArrayList<>();
        validateFields(args, schema.fields(), "", errors);
        if (!errors.isEmpty()) {
            throw new McpValidationException(errors);
        }
    }

    private void validateFields(Map<String, Object> args, List<FieldDef> fields,
                                String prefix, List<String> errors) {
        if (args == null) {
            args = Map.of();
        }
        for (FieldDef field : fields) {
            final String path = prefix.isEmpty() ? field.name() : prefix + "." + field.name();
            final Object value = args.get(field.name());

            if (value == null || "".equals(value)) {
                if (field.required() && (field.defaultValue() == null || field.defaultValue().isEmpty())) {
                    errors.add("'" + path + "' is required");
                }
                continue;
            }

            if ("array".equals(field.fieldType()) && field.nestedFields() != null && !field.nestedFields().isEmpty()) {
                if (!(value instanceof List<?> items)) {
                    errors.add("'" + path + "' must be an array");
                    continue;
                }

                for (int i = 0; i < items.size(); i++) {
                    final Object item = items.get(i);
                    final String itemPath = path + "[" + i + "]";

                    if (item instanceof Map<?, ?> nestedMap) {
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> nested = (Map<String, Object>) nestedMap;
                        validateFields(nested, field.nestedFields(), itemPath, errors);
                    } else {
                        errors.add("'" + itemPath + "' must be an object");
                    }
                }
                continue;
            }

            if (field.nestedFields() != null && !field.nestedFields().isEmpty()) {
                if (value instanceof Map<?, ?> nestedMap) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> nested = (Map<String, Object>) nestedMap;
                    validateFields(nested, field.nestedFields(), path, errors);
                } else {
                    errors.add("'" + path + "' must be an object");
                }
                continue;
            }

            if (field.multiSelect()) {
                if (!(value instanceof List)) {
                    errors.add("'" + path + "' must be an array");
                    continue;
                }
                if (field.options() != null && !field.options().isEmpty()) {
                    for (Object item : (List<?>) value) {
                        if (!field.options().contains(String.valueOf(item))) {
                            errors.add("'" + path + "' contains invalid option '" + item + "'");
                        }
                    }
                }
                continue;
            }

            if (field.options() != null && !field.options().isEmpty()) {
                if (!field.options().contains(String.valueOf(value))) {
                    errors.add("'" + path + "' must be one of " + field.options());
                }
            }

            final String strValue = String.valueOf(value);
            if (field.minLength() >= 0 && strValue.length() < field.minLength()) {
                errors.add("'" + path + "' length must be >= " + field.minLength());
            }
            if (field.maxLength() >= 0 && strValue.length() > field.maxLength()) {
                errors.add("'" + path + "' length must be <= " + field.maxLength());
            }
            if (field.pattern() != null && !field.pattern().isEmpty()) {
                if (!Pattern.matches(field.pattern(), strValue)) {
                    errors.add("'" + path + "' does not match pattern '" + field.pattern() + "'");
                }
            }

            if (!field.min().isEmpty() || !field.max().isEmpty()) {
                try {
                    final double numVal = Double.parseDouble(strValue);
                    if (!field.min().isEmpty() && numVal < Double.parseDouble(field.min())) {
                        errors.add("'" + path + "' must be >= " + field.min());
                    }
                    if (!field.max().isEmpty() && numVal > Double.parseDouble(field.max())) {
                        errors.add("'" + path + "' must be <= " + field.max());
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
    }
}
