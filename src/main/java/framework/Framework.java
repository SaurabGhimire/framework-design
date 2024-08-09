package framework;

import framework.annotations.Autowired;
import framework.annotations.Service;
import framework.exceptions.*;
import org.apache.logging.log4j.util.Strings;
import org.reflections.Reflections;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class Framework {
    private static final Map<String, Object> SERVICE_SINGLETON_INSTANCES = new HashMap<>();
    private static final Set<String> KNOWN_SERVICE_BEAN_IDENTIFIERS = new HashSet<>();
    private static final List<Object> SERVICE_OBJECT_LIST = new ArrayList<>();

    public Framework() {
        try {
            forwardContext();
        } catch (Exception e) {
//            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static void createInstancesRecursively(Collection<? extends Class<?>> serviceTypes) throws
            InstanceCreationWrapperException {
        /*
        - try_to_create_instance: A -> B -> C
            - task requires B. try_to_create_instance: B -> C
                - task requires C. try_to_create_instance: C
                    - no args constructor — created. return nothing.
                    - at this point, call stack returns to B creation, and at this instance all of B's dependency
                - B checks for instance of C in application context, if 404 -> BeanInstanceNotFoundException
                - else: B gets the required instances and injects them into constructor creation
                - return to A
            - A does the same thing, loop over constructor types, get the id, get instance from context,
            inject into @Autowired constructor for A.
        - Proceed i.e. exit recursive loop
        */

        /*
        - we need to create service instances regardless — automatically create for those with no-args constructor
        - however, if constructor has args,
            1. check if class has multiple constructors,
                a. if no, use the single constructor to create instance (annotated or not)
                b. else prefer @autowired constructor. if multiple, throw error. if single -> use that (need a method)
            2. if constructor has params, validate that they are beans only - else throw error
            3. if beans only, recursively create and add instances to app context
            4. after creating dependency beans, try creating parent bean and inject dependencies using the instance id from context
        */

        for (Class<?> serviceClass : serviceTypes) {
            String serviceClassId = getClassInstanceIdentifier(serviceClass);
            if (!Framework.SERVICE_SINGLETON_INSTANCES.containsKey(getServiceBeanId(serviceClass))) {
                continue;
            }

            Constructor<?> constructor = getPreferredConstructor(serviceClass);

            List<? extends Class<?>> paramClasses = validateParameterTypes(serviceClass,
                    constructor.getParameterTypes(), "constructor");

            if (!paramClasses.isEmpty()) {
                createInstancesRecursively(paramClasses);
            }

            // classes at this point have no-args constructors
            // OR their dependencies are already in the app context
            Object instance = createOrGetServiceInstance(serviceClass, constructor, paramClasses);

            SERVICE_OBJECT_LIST.add(instance);
            Framework.SERVICE_SINGLETON_INSTANCES.put(serviceClassId, instance);
        }

    }

    /**
     * @param serviceClass - the parent or containing class for the parameters we're validating
     * @param parameters   - array of parameter classes
     * @param paramType    - can be constructor or method i.e. params from a constructor or a method
     * @return - list of classes that are valid/managed beans in the framework i.e. service instances for now
     * @throws InstanceCreationWrapperException - wrapper exception to hold any exceptions that might have occurred
     *                                          during instance creation
     */
    private static List<? extends Class<?>> validateParameterTypes(Class<?> serviceClass, Class<?>[] parameters,
                                                                   String paramType)
            throws InstanceCreationWrapperException {
        try {
            List<? extends Class<?>> paramClasses = Arrays.stream(parameters)
                    .filter(Framework::isAManagedServiceBean).toList();

            Set<? extends Class<?>> notSupportDependencies = Arrays.stream(parameters)
                    .filter(type -> !isAManagedServiceBean(type)).collect(Collectors.toSet());

            if (!notSupportDependencies.isEmpty()) {
                throw new DependencyTypeNotSupportedOrFoundException(serviceClass, notSupportDependencies, paramType);
            }

            return paramClasses;
        } catch (DependencyTypeNotSupportedOrFoundException e) {
            throw new InstanceCreationWrapperException(e.getMessage(), e);
        }
    }

    private static Constructor<?> getPreferredConstructor(Class<?> serviceClass) throws InstanceCreationWrapperException {
        try {

            if (getAutowiredConstructor(serviceClass) != null) {
                return getAutowiredConstructor(serviceClass);
            }

            Constructor<?>[] constructors = serviceClass.getConstructors();
            if (constructors.length > 1) {
                // if class has multiple constructors and no @autowired, error out
                // and require adding it to one constructor
                throw new MultipleConstructorsNonAutowiredException(serviceClass, constructors.length);
            }

            return constructors[0];

        } catch (MultipleConstructorsNonAutowiredException e) {
            throw new InstanceCreationWrapperException(e.getMessage(), e);
        }
    }

    private static @Nullable Constructor<?> getAutowiredConstructor(Class<?> serviceClass) throws InstanceCreationWrapperException {
        try {

            List<Constructor<?>> autowiredConstructors = Arrays.stream(serviceClass.getConstructors())
                    .filter(c -> c.getAnnotation(Autowired.class) != null)
                    .toList();
            if (autowiredConstructors.size() > 1) {
                throw new MultipleAutowiredConstructorsException(serviceClass, autowiredConstructors.size());
            }
            return autowiredConstructors.isEmpty() ? null : autowiredConstructors.getFirst();

        } catch (MultipleAutowiredConstructorsException e) {
            throw new InstanceCreationWrapperException(e.getMessage(), e);
        }
    }

    /**
     * This method should only be called once all dependencies are added to app context else
     * it will throw an InstanceNotFoundInAppContextException
     *
     * @param serviceClass - class for which we're creating an instance
     * @param paramTypes   - parameter classes that need to be injected
     * @return newly created instance
     * @throws InstanceCreationWrapperException - a wrapper exception that helps keep the number of exceptions in the signature low
     */
    private static Object createOrGetServiceInstance(Class<?> serviceClass,
                                                     Constructor<?> constructor,
                                                     List<? extends Class<?>> paramTypes)
            throws InstanceCreationWrapperException {

        String serviceId = Framework.getServiceBeanId(serviceClass);
        if (instanceIsAvailableInContext(serviceId)) {
            return Framework.SERVICE_SINGLETON_INSTANCES.get(serviceId);
        }

        if (paramTypes.isEmpty()) {
            return createInstanceWithNoArgsConstructor(serviceClass);
        }

        return createInstanceWithHasArgsConstructor(serviceClass, constructor, paramTypes);
    }

    private static boolean instanceIsAvailableInContext(String serviceId) {
        return Framework.SERVICE_SINGLETON_INSTANCES.containsKey(serviceId)
                && Framework.SERVICE_SINGLETON_INSTANCES.get(serviceId) != null;
    }

    private static Object createInstanceWithHasArgsConstructor(Class<?> serviceClass, Constructor<?> constructor,
                                                               List<? extends Class<?>> paramTypes)
            throws InstanceCreationWrapperException {
        try {
            Object[] dependencies = new Object[paramTypes.size()];

            for (int i = 0; i < paramTypes.size(); i++) {
                String instanceId = Framework.getServiceBeanId(paramTypes.get(i));

                if (!instanceIsAvailableInContext(instanceId))
                    throw new InstanceNotFoundInAppContextException(paramTypes.get(i));

                Object instance = Framework.SERVICE_SINGLETON_INSTANCES.get(instanceId);

                if (paramTypes.get(i).isAssignableFrom(instance.getClass())) {
                    dependencies[i] = instance;
                    throw new DependencyInstanceMismatchException(paramTypes.get(i), instance, serviceClass);
                }
            }

            return constructor.newInstance(dependencies);

        } catch (InstantiationException | InvocationTargetException |
                 IllegalAccessException | InstanceNotFoundInAppContextException |
                 DependencyInstanceMismatchException e) {
            throw new InstanceCreationWrapperException(e.getMessage(), e);
        }
    }

    private static Object createInstanceWithNoArgsConstructor(Class<?> serviceClass) throws InstanceCreationWrapperException {
        try {
            return serviceClass.getConstructor().newInstance();
        } catch (NoSuchMethodException | InstantiationException | InvocationTargetException |
                 IllegalAccessException e) {
            throw new InstanceCreationWrapperException(e.getMessage(), e);
        }
    }

    private static String getClassInstanceIdentifier(Class<?> serviceClass) {
        String className = serviceClass.getSimpleName();
        String[] name = className.split("");
        name[0] = name[0].toLowerCase();
        className = String.join("", name);
        return className;
    }

    private static boolean isAManagedServiceBean(Class<?> type) {
        return KNOWN_SERVICE_BEAN_IDENTIFIERS.contains(getServiceBeanId(type));
    }

    private static String getServiceBeanId(Class<?> clazz) {
        String serviceIdentifier = clazz.getAnnotation(Service.class).value();
        if (Strings.isEmpty(serviceIdentifier)) {
            serviceIdentifier = clazz.getSimpleName();
        }
        return serviceIdentifier;
    }

    public void forwardContext() {
        try {
            Reflections reflections = new Reflections("application"); // should figure out how to bring this in as an option if needed (or leave blank)

            Set<Class<?>> serviceTypes = reflections.getTypesAnnotatedWith(Service.class);
            KNOWN_SERVICE_BEAN_IDENTIFIERS.addAll(
                    serviceTypes.stream().map(Framework::getServiceBeanId).collect(Collectors.toSet())
            );

            createInstancesRecursively(serviceTypes);

            performDI();
        } catch (Exception e) {
//            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void performDI() {

        try {
            for (Object serviceClass : SERVICE_OBJECT_LIST) {
                for (Field field : serviceClass.getClass().getDeclaredFields()) {
                    if (field.isAnnotationPresent(Autowired.class)) {
                        // get type
                        Class<?> fieldType = field.getType();
                        // get object of type
                        Object instance = getServiceBeanOfType(fieldType);
                        // autowire
                        field.setAccessible(true);
                        field.set(serviceClass, instance);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Object getServiceBeanOfType(Class<?> type) {
        Object service = null;
        try {
            for (Object theClass : SERVICE_OBJECT_LIST) {
                Class<?>[] interfaces = theClass.getClass().getInterfaces();
                for (Class<?> theInterface : interfaces) {
                    if (theInterface.getSimpleName().contentEquals(type.getSimpleName())) service = theClass;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return service;
    }
}
