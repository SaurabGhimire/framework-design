package framework.async;

import framework.utils.CheckForAnnotation;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType.Unloaded;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NoInterfaceFoundAsyncProxy implements MethodInterceptor {
    private final ExecutorService executorService = Executors.newCachedThreadPool();

//    @SuppressWarnings("unchecked")
//    public <T> T createProxy(Object instance) {
//        return (T) Enhancer.create(instance.getClass(), this);
//    }

    @SuppressWarnings("unchecked")
    public <T> T createProxy(Object instance) {
        try {
            Class<?> dynamicType;

            try (Unloaded<?> unloadedType = new ByteBuddy()
                    .subclass(instance.getClass())
                    .method(ElementMatchers.isDeclaredBy(instance.getClass()).and(ElementMatchers.not(ElementMatchers.isDeclaredBy(Object.class))))
//                    .method(ElementMatchers.not(ElementMatchers.isDeclaredBy(Object.class)))
//                    .method(ElementMatchers.any())
                    .intercept(MethodDelegation.to(this))
                    .make()) {

                dynamicType = unloadedType
                        .load(instance.getClass().getClassLoader())
                        .getLoaded();
            }

            return (T) dynamicType.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create proxy with ByteBuddy", e);
        }
    }

    @Override
    public Object intercept(Object classObj, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        if (CheckForAnnotation.hasAsync(method)) {
            executorService.submit(() -> {
                try {
                    return methodProxy.invokeSuper(classObj, args);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            });
            return null;
        } else {
            return methodProxy.invokeSuper(classObj, args);
        }
    }

//    @SuppressWarnings("unchecked")
//    public <T> T createProxyWithArgs(Object instance, List<? extends Class<?>> argTypes, Object[] args) {
//        Enhancer e = new Enhancer();
//
//        e.setSuperclass(instance.getClass());
//        e.setCallback(this);
//        Class<?>[] types = new Class[argTypes.size()];
//        for (int i = 0; i < argTypes.size(); i++) {
//            types[i] = argTypes.get(i);
//        }
//
//        return (T) e.create(types, args);
//    }

    @SuppressWarnings("unchecked")
    public <T> T createProxyWithArgs(Object instance, List<? extends Class<?>> argTypes, Object[] args) {
        try {
            // Create a dynamic subclass of the target class
            Class<?> dynamicType;
            try (Unloaded<?> unloadedType = new ByteBuddy()
                    .subclass(instance.getClass())
                    .method(ElementMatchers.isDeclaredBy(instance.getClass()).and(ElementMatchers.not(ElementMatchers.isDeclaredBy(Object.class))))
//                    .method(ElementMatchers.not(ElementMatchers.isDeclaredBy(Object.class)))
//                    .method(ElementMatchers.any())
                    .intercept(MethodDelegation.to(this))
                    .make()) {

                // Load the dynamic subclass
                dynamicType = unloadedType
                        .load(instance.getClass().getClassLoader())
                        .getLoaded();
            }

            Constructor<?> constructor = dynamicType.getConstructor(argTypes.toArray(new Class[0]));

            return (T) constructor.newInstance(args);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create proxy with ByteBuddy", e);
        }
    }
}
