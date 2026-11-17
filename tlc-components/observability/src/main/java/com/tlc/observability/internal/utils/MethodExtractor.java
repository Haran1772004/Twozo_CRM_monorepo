package com.tlc.observability.internal.utils;

import com.tlc.observability.internal.context.MethodInfo;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * Utility class for extracting method metadata from a join point.
 * <p>
 * This class provides functionality to extract detailed method information such as the method name,
 * class name, return type, package name, and fully qualified method name from the context of a method
 * execution intercepted by AspectJ.
 * </p>
 *
 * <b>Author:</b> Ashok<br>
 * <b>Version:</b> 1.0<br>
 * <b>Since:</b> 2024
 */
public class MethodExtractor {

    /**
     * Extracts method information from the given {@link ProceedingJoinPoint}.
     * <p>
     * This method retrieves details about the method being intercepted, including the method's name,
     * declaring class, return type, package name, and the fully qualified name (package + class + method name).
     * </p>
     *
     * @param joinPoint The join point representing the intercepted method execution.
     * @return A {@link MethodInfo} object containing the extracted method details.
     */
    public static MethodInfo extractMethodInfo(ProceedingJoinPoint joinPoint) {
        final MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        final String methodName = signature.getName();
        final String className = signature.getDeclaringTypeName();
        final String returnType = signature.getReturnType().getTypeName();
        final Package classPackage = signature.getDeclaringType().getPackage();
        final String packageName = (classPackage != null) ? classPackage.getName() : "";
        final String fullyQualifiedName = packageName + "." + className + "." + methodName;

        return new MethodInfo(methodName, packageName, className, returnType, fullyQualifiedName);
    }
}
