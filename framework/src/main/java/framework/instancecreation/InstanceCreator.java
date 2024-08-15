package framework.instancecreation;

import framework.Framework;
import framework.async.AsyncProcessor;
import framework.exceptions.DependencyInstanceMismatchException;
import framework.exceptions.InstanceCreationWrapperException;
import framework.exceptions.InstanceNotFoundInAppContextException;
import framework.exceptions.MultipleCandidatesForInstanceException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.List;

import static framework.utils.CheckForAnnotation.hasQualifier;

public class InstanceCreator {

    public static Object createInstanceWithHasArgsConstructor(Class<?> serviceClass, Constructor<?> constructor,
                                                              List<? extends Class<?>> paramTypes)
            throws InstanceCreationWrapperException {
        try {
            Object[] dependencies = new Object[paramTypes.size()];
            Parameter[] params = constructor.getParameters();
            for (int i = 0; i < paramTypes.size(); i++) {
                Object instance;

                if (hasQualifier(params[i])) {
                    String instanceId = Framework.getServiceInstanceId(paramTypes.get(i));
                    instance = Framework.getInstanceFromContextUsingId(instanceId, paramTypes.get(i), true);
                } else {
                    instance = Framework.getInstanceFromContextUsingType(paramTypes.get(i), true);
                }

                validateIsAssignable(paramTypes.get(i), instance, serviceClass);

                dependencies[i] = instance;
            }

            return AsyncProcessor.createProxyIfNeeded(constructor.newInstance(dependencies));

        } catch (InstantiationException | InvocationTargetException |
                 IllegalAccessException | InstanceNotFoundInAppContextException |
                 DependencyInstanceMismatchException | MultipleCandidatesForInstanceException e) {
            throw new InstanceCreationWrapperException(e.getMessage(), e);
        }
    }

    private static void validateIsAssignable(Class<?> serviceClass, Object instance, Class<?> parentClass)
            throws DependencyInstanceMismatchException {
        if (!serviceClass.isAssignableFrom(instance.getClass())) {
            throw new DependencyInstanceMismatchException(serviceClass, instance, parentClass);
        }
    }

    public static Object createInstanceWithNoArgsConstructor(Class<?> serviceClass) throws InstanceCreationWrapperException {
        try {
            return AsyncProcessor.createProxyIfNeeded(serviceClass.getConstructor().newInstance());
        } catch (NoSuchMethodException | InstantiationException | InvocationTargetException |
                 IllegalAccessException e) {
            throw new InstanceCreationWrapperException(e.getMessage(), e);
        }
    }

}
