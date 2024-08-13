package framework.utils;

import framework.exceptions.ResourcePropertiesNotFoundException;

import java.io.InputStream;
import java.util.Properties;

// Singleton Property Accessor class
public class PropertyAccessor {
    private static volatile Properties properties;
    private final String RESOURCE_NAME = "application.properties";
    private PropertyAccessor() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(RESOURCE_NAME)) {
            if (input == null) {
                throw new ResourcePropertiesNotFoundException();
            }
            properties = new Properties();
            properties.load(input);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static Object getValueOf(String key) {
        if (properties == null) {
            synchronized (PropertyAccessor.class) {
                if (properties == null) {
                    new PropertyAccessor();
                }
            }
        }
        if(!properties.containsKey(key)) return null;
        return properties.get(key);
    }
}