package framework.exceptions;

public class MultipleAutowiredConstructorsException extends Exception {
    public MultipleAutowiredConstructorsException(Class<?> serviceClass, int count) {
        super(generateErrorMessage(serviceClass, count));
    }

    private static String generateErrorMessage(Class<?> serviceClass, int count) {
        String serviceClassName = serviceClass.getName();
        return String.format("""
                \nError Message:
                    Only one constructor can have the @Autowired annotation in class %s. Found %d
                """, serviceClassName, count);
    }

}
