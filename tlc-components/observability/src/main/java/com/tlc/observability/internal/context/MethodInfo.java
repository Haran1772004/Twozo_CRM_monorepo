package com.tlc.observability.internal.context;

/**
 * Represents metadata about a method.
 *
 * <p>
 * This record stores information about a method, such as its name, package, class, return type,
 * and its fully qualified name. It is used to capture and store key details of methods for
 * logging or tracking purposes.
 * </p>
 *
 * <b>Author:</b> Ashok<br>
 * <b>Version:</b> 1.0<br>
 * <b>Since:</b> 2024
 *
 * @param name The name of the method.
 * @param packageName The package to which the method belongs.
 * @param className The class in which the method is defined.
 * @param returnType The return type of the method.
 * @param fullyQualifiedName The fully qualified name of the method (including the package, class, and method name).
 */
public record MethodInfo(String name, String packageName, String className, String returnType, String fullyQualifiedName) {
}
