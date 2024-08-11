package framework.exceptions;

public class DependencyInstanceMismatchException extends Exception {
    public DependencyInstanceMismatchException(Class<?> paramType, Object instance, Class<?> serviceClass) {
        super(generateErrorMessage(paramType, instance, serviceClass));
    }


    private static String generateErrorMessage(Class<?> paramType, Object instance, Class<?> serviceClass) {
        String instanceClassName = instance.getClass().getName();
        String paramTypeName = paramType.getName();
        String serviceClassName = serviceClass.getName();
        return String.format("""
                \nError Message:
                    Found a mismatch while trying to assign an instance (from the application context) of type: %s
                    to the Parameter Type: %s
                    in the class: %s.
                """, instanceClassName, paramTypeName, serviceClassName);
    }
}
