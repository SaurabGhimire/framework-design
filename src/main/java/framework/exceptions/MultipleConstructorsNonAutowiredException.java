package framework.exceptions;

public class MultipleConstructorsNonAutowiredException extends Exception {
    public MultipleConstructorsNonAutowiredException(Class<?> serviceClass, int count) {
        super(generateErrorMessage(serviceClass, count));

    }

    private static String generateErrorMessage(Class<?> serviceClass, int count) {
        String serviceClassName = serviceClass.getName();
        return String.format("""
                Error Message:
                    Found %d constructors in class %s but none of them has @Autowired annotation.
                    Add the @Autowired  on one constructor which will be used for instance creation
                    and constructor injection.
                """, count, serviceClassName);
    }
}
