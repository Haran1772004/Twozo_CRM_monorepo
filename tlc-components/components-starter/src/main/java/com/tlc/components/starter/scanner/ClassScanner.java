package com.tlc.components.starter.scanner;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Selvakumar
 * @version 1.0
 */
@Component
public class ClassScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassScanner.class);

    /**
     * Finds all classes with a specified annotation type in the default package.
     *
     * @param annotationType The class object representing the annotation type to search for.
     * @return A set of classes that have the specified annotation.
     */
    public Set<Class<?>> findClassesWithAnnotation(final Class<? extends Annotation> annotationType) {
        return findClassesWithAnnotation("", annotationType);
    }

    /**
     * Finds all classes with a specified annotation type within the specified base package.
     *
     * @param basePackage    The base package to search within.
     * @param annotationType The class object representing the annotation type to search for.
     * @return A set of classes that have the specified annotation within the given base package.
     */
    public Set<Class<?>> findClassesWithAnnotation(final String basePackage, final Class<? extends Annotation> annotationType) {
        final ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);

        scanner.addIncludeFilter(new AnnotationTypeFilter(annotationType));

        return findClasses(basePackage, scanner);
    }

    /**
     * Finds all classes that implement a specified interface type in the default package.
     *
     * @param interfaceType The class object representing the interface type to search for.
     * @param <T>           The type of the interface.
     * @return A set of classes that implement the specified interface.
     */
    public <T> Set<Class<T>> findClassesImplementingInterface(final Class<T> interfaceType) {
        return findClassesImplementingInterface("", interfaceType);
    }

    /**
     * Finds all classes within the specified base package that implement a specified interface type.
     *
     * @param basePackage   The base package to search within.
     * @param interfaceType The class object representing the interface type to search for.
     * @param <T>           The type of the interface.
     * @return A set of classes within the given base package that implement the specified interface.
     */
    public <T> Set<Class<T>> findClassesImplementingInterface(final String basePackage, final Class<T> interfaceType) {
        final ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);

        scanner.addIncludeFilter(new AssignableTypeFilter(interfaceType));

        return castToSetOfClass(findClasses(basePackage, scanner));
    }

    /**
     * Casts a set of raw classes to a set of classes of a specific type.
     *
     * @param rawClasses The raw set of classes to be casted.
     * @param <T>        The type of the classes to cast to.
     * @return A set of classes of the specified type.
     */
    @SuppressWarnings("unchecked")
    private static <T> Set<Class<T>> castToSetOfClass(final Set<?> rawClasses) {
        return (Set<Class<T>>) rawClasses;
    }

    /**
     * Finds classes within the specified base package using the given classpath scanning candidate component provider.
     *
     * @param basePackage The base package to search within.
     * @param scanner     The ClassPathScanningCandidateComponentProvider to use for scanning.
     * @return A set of classes found within the given base package.
     */
    private Set<Class<?>> findClasses(final String basePackage, final ClassPathScanningCandidateComponentProvider scanner) {
        final Set<Class<?>> classes = new HashSet<>();

        for (final BeanDefinition beanDefinition : scanner.findCandidateComponents(basePackage)) {
            try {
                final Class<?> clazz = Class.forName(beanDefinition.getBeanClassName());
                if (!clazz.isInterface() && !clazz.isAnnotation()) {
                    classes.add(clazz.asSubclass(clazz));
                }
            } catch (ClassNotFoundException e) {
                LOGGER.debug("Failed to locate classes for the provided base package: {}. Stack trace: ", basePackage, e);
            }
        }

        return classes;
    }
}