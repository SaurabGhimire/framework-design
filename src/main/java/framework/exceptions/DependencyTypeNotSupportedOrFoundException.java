package framework.exceptions;

import java.util.Set;
import java.util.stream.Collectors;

public class DependencyTypeNotSupportedOrFoundException extends Exception {
    public DependencyTypeNotSupportedOrFoundException(Class<?> serviceClass, Set<? extends Class<?>> notSupportDependencies, String type) {
        super(generateErrorMessage(serviceClass, notSupportDependencies, type));
    }

    private static String generateErrorMessage(Class<?> serviceClass, Set<? extends Class<?>> notSupportDependencies, String type) {
        String serviceClassName = serviceClass.getName();
        String dependencyClassNames = notSupportDependencies.stream().map(Class::getName).collect(Collectors.joining("\n"));
        return String.format("""
                \nError Message:
                    Parameter(s) of %s in %s are not known to the framework and can't be created
                    %s
                """, serviceClassName, type, dependencyClassNames);
    }
}
