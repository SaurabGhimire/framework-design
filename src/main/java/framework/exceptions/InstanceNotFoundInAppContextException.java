package framework.exceptions;

public class InstanceNotFoundInAppContextException extends Exception {
    public InstanceNotFoundInAppContextException(Class<?> serviceClass) {
        super(generateErrorMessage(serviceClass));

    }

    private static String generateErrorMessage(Class<?> serviceClass) {
        String serviceClassName = serviceClass.getName();
        return String.format("""
                Error Message:
                    Failed to find instance of class: %s in the Application Context.
                    Please make sure it's configured correctly.
                """, serviceClassName);
    }
}
