package framework.async;

import framework.utils.CheckForAnnotation;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

public class AsyncProcessor {
    public static Object createProxyIfNeeded(Object instance) {
        return instance;
        /*
        if (noAsyncMethodsInClass(instance.getClass())) {
            return instance;
        }

        ClassLoader classLoader = instance.getClass().getClassLoader();
        Class<?>[] interfaces = instance.getClass().getInterfaces();

        AsyncProxy asyncProxy = new AsyncProxy(instance);

        return Proxy.newProxyInstance(classLoader, interfaces, asyncProxy);
        */
    }
//
//    public static Object createProxyIfNeeded(Object instance, List<? extends Class<?>> argTypes, Object[] args) {
//        if (noAsyncMethodsInClass(instance.getClass())) {
//            return instance;
//        }
//
//        NoInterfaceFoundAsyncProxy noInterfaceProxy = new NoInterfaceFoundAsyncProxy();
//        return noInterfaceProxy.createProxyWithArgs(instance, argTypes, args);
//    }

    private static boolean noAsyncMethodsInClass(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredMethods()).noneMatch(CheckForAnnotation::hasAsync);
    }
}
