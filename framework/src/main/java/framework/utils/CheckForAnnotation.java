package framework.utils;

import framework.annotations.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class CheckForAnnotation {
    public static boolean hasScheduled(Method method) {
        return method.isAnnotationPresent(Scheduled.class);
    }

    public static boolean hasAsync(Method method) {
        return method.isAnnotationPresent(Async.class);
    }

    public static boolean hasServiceAnnotation(Class<?> clazz) {
        return clazz.isAnnotationPresent(Service.class);
    }

    public static boolean hasQualifier(Parameter param) {
        return param.isAnnotationPresent(Qualifier.class);
    }

    public static boolean hasAutowired(Field field) {
        return field.isAnnotationPresent(Autowired.class);
    }

    public static boolean hasAutowired(Method field) {
        return field.isAnnotationPresent(Autowired.class);
    }

    public static boolean hasQualifier(Field field) {
        return field.isAnnotationPresent(Qualifier.class);
    }
}
