package framework.exceptions;

public class InstanceNotFoundInAppContextException extends Exception {
    public InstanceNotFoundInAppContextException(Class<?> serviceClass, String instanceId) {
        super(generateErrorMessageForInstanceId(serviceClass, instanceId));
    }

    public InstanceNotFoundInAppContextException(Class<?> serviceClass) {
        super(generateErrorMessageForType(serviceClass));
    }

    private static String generateErrorMessageForInstanceId(Class<?> serviceClass, String instanceId) {
        String serviceClassName = serviceClass.getName();
        return String.format("""
                \nError Message:
                    Failed to find an instance of class: %s named: %s in the Application Context.
                    Please make sure your @Service("%s") is matched to some @Qualifier("%s") so it is configured correctly.
                """, serviceClassName, instanceId, instanceId, instanceId);
    }

    private static String generateErrorMessageForType(Class<?> serviceClass) {
        String serviceClassName = serviceClass.getName();
        return String.format("""
                \nError Message:
                    Failed to find instance of class type : %s in the Application Context.
                    Please make sure it's configured correctly.
                """, serviceClassName);
    }
}
