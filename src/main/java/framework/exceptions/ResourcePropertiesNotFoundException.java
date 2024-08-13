package framework.exceptions;

import java.util.Iterator;
import java.util.Set;

public class ResourcePropertiesNotFoundException extends Exception{
    public ResourcePropertiesNotFoundException() {
        super("""
                \nError Message:
                    Application.properties file not found in resources.
                """);
    }
}
