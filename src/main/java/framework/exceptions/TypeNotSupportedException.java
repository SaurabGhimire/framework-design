package framework.exceptions;

public class TypeNotSupportedException extends Exception {
    public TypeNotSupportedException(Class<?> serviceClass) {
        super(generateErrorMessage(serviceClass));
    }

    private static String generateErrorMessage(Class<?> serviceClass) {
        return String.format("""
                Error Message:
                    Looks like the class: %s is marked for dependency injection (@Autowired)
                    but is not a supported type for autowiring.
                    Make sure the class is annotated with the correct annotation e.g. @Service
                """, serviceClass.getName());
    }
}
