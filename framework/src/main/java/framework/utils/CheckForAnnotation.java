package framework.utils;

import framework.annotations.Scheduled;

import java.lang.reflect.Method;

public class CheckForAnnotation {
    public static boolean hasScheduled(Method method) {
        return method.isAnnotationPresent(Scheduled.class);
    }
}
