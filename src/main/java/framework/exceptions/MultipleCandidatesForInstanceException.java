package framework.exceptions;

import java.util.Iterator;
import java.util.Set;

public class MultipleCandidatesForInstanceException extends Exception {
    public MultipleCandidatesForInstanceException(Class<?> serviceClassType, Set<Object> instanceSet) {
        super(generateErrorMessage(serviceClassType, instanceSet));
    }

    private static String generateErrorMessage(Class<?> serviceClass, Set<Object> instances) {
        Iterator<Object> setIterator = instances.iterator();
        StringBuilder sb = new StringBuilder();

        while (setIterator.hasNext()) {
            sb.append("- ").append(setIterator.next().getClass().getName()).append("\n");
        }

        return String.format("""
                Error Message:
                    Ambiguous dependency autowiring for class type: %s.
                    Found multiple candidates that could be used. Please used @Qualifier to specify one.
                    Candidates:
                    %s
                """, serviceClass.getName(), sb);
    }
}
