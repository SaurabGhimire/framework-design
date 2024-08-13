package framework;

import framework.annotations.Autowired;
import framework.annotations.Qualifier;
import framework.annotations.Service;
import framework.annotations.Value;
import framework.exceptions.*;
import framework.utils.PropertyAccessor;
import org.apache.logging.log4j.util.Strings;
import org.reflections.Reflections;

import javax.annotation.Nullable;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

public class Framework {
    private static final Map<String, Object> INSTANCES_MAPPED_BY_NAME = new HashMap<>();
    private static final Map<Class<?>, Set<Object>> INSTANCES_MAPPED_BY_TYPE = new HashMap<>();
    private static final Set<Class<?>> ANNOTATED_SERVICE_CLASS_TYPES = new HashSet<>();

    public static void run(Class<?> mainClass, String... args) throws Exception {
        // Scan and initialize components
        // Here, for simplicity, we're assuming you have a basic DI setup
        forwardContext();
        // Retrieve and run the Application instance
        Object appInstance = INSTANCES_MAPPED_BY_NAME.get(mainClass);
        if (appInstance instanceof Runnable) {
            ((Runnable) appInstance).run();
        }
    }

    /**
     * @param serviceTypes - Class<?>[] of classes that have @Service or abstractions of a
     *                     class annotated with it
     * @throws InstanceCreationWrapperException - wrapper exception to track all potential
     *                                          exceptions and to keep method signature clean
     * @implSpec -> steps:
     * - try_to_create_instance: A -> B -> C
     * - task requires B. try_to_create_instance: B -> C
     * - task requires C. try_to_create_instance: C
     * - no args constructor — created. return nothing.
     * - at this point, call stack returns to B creation, and at this instance all of B's dependency
     * - B checks for instance of C in application context, if 404 -> BeanInstanceNotFoundException
     * - else: B gets the required instances and injects them into constructor creation
     * - return to A
     * - A does the same thing, loop over constructor types, get the id, get instance from context,
     * inject into @Autowired constructor for A.
     * - Proceed i.e. exit recursive loop
     */
    private static void createInstancesRecursively(Collection<? extends Class<?>> serviceTypes)
            throws InstanceCreationWrapperException {

        for (Class<?> serviceClassType : serviceTypes) {
            String serviceClassId = getServiceInstanceId(serviceClassType);

            if (Framework.instanceIsAvailableInContextCombined(serviceClassType, serviceClassId)) {
                continue;
            }

            Constructor<?> constructor = getPreferredConstructor(serviceClassType);
            // if no constructor, skip potential interface
            if (constructor == null) {
                // todo: might need to create instances of inheritors/implementors — interfaces are being left null instances
                continue;
            }


            List<? extends Class<?>> paramClasses = validateParameterTypes(serviceClassType,
                    constructor.getParameterTypes(), "constructor");

            if (!paramClasses.isEmpty()) {
                createInstancesRecursively(paramClasses);
            }

            // classes at this point have no-args constructors
            // OR their dependencies are already in the app context
            Object instance = createServiceInstance(serviceClassType, constructor, paramClasses);

            addInstanceToApplicationContext(serviceClassType, instance, serviceClassId);

        }
    }

    private static void addInstanceToApplicationContext(Class<?> serviceClassType, Object instance, String serviceClassId) {

        registerInstanceByNameIdentifier(instance, serviceClassId);

        registerInstanceByType(serviceClassType, instance);

        // map instance to superclass and interface types
        getSuperClasses(serviceClassType).forEach(superclassType -> {
            registerInstanceByType(superclassType, instance);
        });
        Arrays.stream(serviceClassType.getInterfaces()).forEach(interfaceType -> {
            registerInstanceByType(interfaceType, instance);
        });

    }

    private static void registerInstanceByNameIdentifier(Object instance, String serviceClassId) {
        if (!Framework.INSTANCES_MAPPED_BY_NAME.containsKey(serviceClassId)) {
            Framework.INSTANCES_MAPPED_BY_NAME.put(serviceClassId, instance);
        }
    }

    private static void registerInstanceByType(Class<?> serviceClassType, Object instance) {
        if (Framework.INSTANCES_MAPPED_BY_TYPE.containsKey(serviceClassType)) {
            Framework.INSTANCES_MAPPED_BY_TYPE.get(serviceClassType).add(instance);
        } else {
            HashSet<Object> instanceSet = new HashSet<>();
            instanceSet.add(instance);
            Framework.INSTANCES_MAPPED_BY_TYPE.put(serviceClassType, instanceSet);
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
                    .filter(Framework::isAManagedServiceClassType).toList();

            Set<? extends Class<?>> notSupportDependencies = Arrays.stream(parameters)
                    .filter(type -> !isAManagedServiceClassType(type)).collect(Collectors.toSet());

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

            if (constructors.length == 0) return null;

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
     * @param serviceClassType - class for which we're creating an instance
     * @param paramTypes       - parameter classes that need to be injected
     * @return newly created instance
     * @throws InstanceCreationWrapperException - a wrapper exception that helps keep the number of exceptions in the signature low
     */
    private static Object createServiceInstance(Class<?> serviceClassType,
                                                Constructor<?> constructor,
                                                List<? extends Class<?>> paramTypes)
            throws InstanceCreationWrapperException {

        String serviceId = Framework.getServiceInstanceId(serviceClassType);

        if (instanceIsAvailableInContextCombined(serviceClassType, serviceId)) {
            return Framework.INSTANCES_MAPPED_BY_TYPE.get(serviceClassType);
        }


        if (paramTypes.isEmpty()) {
            return createInstanceWithNoArgsConstructor(serviceClassType);
        }

        return createInstanceWithHasArgsConstructor(serviceClassType, constructor, paramTypes);
    }

    private static boolean instanceIsAvailableInContextByName(String serviceId) {
        return Framework.INSTANCES_MAPPED_BY_NAME.containsKey(serviceId)
                && Framework.INSTANCES_MAPPED_BY_NAME.get(serviceId) != null;
    }

    private static boolean instanceIsAvailableInContextByType(Class<?> serviceClassType) {
        return Framework.INSTANCES_MAPPED_BY_TYPE.containsKey(serviceClassType)
                && Framework.INSTANCES_MAPPED_BY_TYPE.get(serviceClassType) != null;
    }

    private static boolean instanceIsAvailableInContextCombined(Class<?> serviceClassType, String instanceId) {
        return instanceIsAvailableInContextByType(serviceClassType)
                || instanceIsAvailableInContextByName(instanceId);
    }

    private static Object createInstanceWithHasArgsConstructor(Class<?> serviceClass, Constructor<?> constructor,
                                                               List<? extends Class<?>> paramTypes)
            throws InstanceCreationWrapperException {
        try {
            Object[] dependencies = new Object[paramTypes.size()];
            Parameter[] params = constructor.getParameters();
            for (int i = 0; i < paramTypes.size(); i++) {
                Object instance;

                if (hasQualifier(params[i])) {
                    String instanceId = Framework.getServiceInstanceId(paramTypes.get(i));
                    instance = Framework.getInstanceFromContextUsingId(instanceId, paramTypes.get(i));
                } else {
                    instance = getInstanceFromContextUsingType(paramTypes.get(i));
                }

                validateIsAssignable(paramTypes.get(i), instance, serviceClass);

                dependencies[i] = instance;
            }

            return constructor.newInstance(dependencies);

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

    private static boolean isAManagedServiceClassType(Class<?> type) {
        return ANNOTATED_SERVICE_CLASS_TYPES.contains(type);
    }

    private static String getServiceInstanceId(Class<?> clazz) {
        String serviceIdentifier;

        if (hasServiceAnnotationWithValue(clazz)) {
            serviceIdentifier = clazz.getAnnotation(Service.class).value();
        } else {
            serviceIdentifier = clazz.getSimpleName();
        }

        return serviceIdentifier;
    }

    private static void performFieldInjection(Object parentClassInstance, Field[] fields)
            throws InstanceCreationWrapperException {
        try {
            for (Field field : fields) {
                Class<?> fieldType = field.getType();

                if (hasAutowired(field)) {
                    Object instance;
                    if (hasQualifier(field)) {
                        String typeInstanceId = field.getAnnotation(Qualifier.class).value();
                        instance = getInstanceFromContextUsingId(typeInstanceId, fieldType);
                    } else {
                        instance = getInstanceFromContextUsingType(fieldType);
                    }
                    field.setAccessible(true);
                    field.set(parentClassInstance, fieldType.cast(instance));
                }
                // Inject Value if the field has @Value annotation
                if (hasValueAnnotation(field)) {
                    String key = field.getAnnotation(Value.class).value();
                    Object propertyValue = PropertyAccessor.getValueOf(key);
                    field.setAccessible(true);
                    field.set(parentClassInstance, fieldType.cast(propertyValue));
                }

            }
        } catch (IllegalArgumentException | IllegalAccessException |
                 InstanceNotFoundInAppContextException | MultipleCandidatesForInstanceException e) {
            throw new InstanceCreationWrapperException(e.getMessage(), e);
        }
    }


    private static void performSetterInjection(Object parentClassInstance, Class<?> serviceClass, Method[] methods)
            throws InstanceCreationWrapperException {
        try {
            for (Method method : methods) {

                if (hasAutowired(method)) {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    // validator will throw error if any types aren't autowire enabled
                    List<? extends Class<?>> validatedParamTypes =
                            validateParameterTypes(serviceClass, paramTypes, "method");

                    Parameter[] params = method.getParameters();
                    Object[] argumentInstances = new Object[validatedParamTypes.size()];

                    for (int i = 0; i < validatedParamTypes.size(); i++) {
                        Object instance;

                        if (hasQualifier(params[i])) {
                            String argumentTypeId = params[i].getAnnotation(Qualifier.class).value();
                            instance = Framework.getInstanceFromContextUsingId(argumentTypeId, validatedParamTypes.get(i));
                        } else {
                            instance = Framework.getInstanceFromContextUsingType(validatedParamTypes.get(i));
                        }

                        argumentInstances[i] = instance;
                    }

                    method.setAccessible(true);
                    method.invoke(parentClassInstance, argumentInstances);
                }
            }
        } catch (IllegalArgumentException | IllegalAccessException |
                 InstanceNotFoundInAppContextException | InvocationTargetException |
                 MultipleCandidatesForInstanceException e) {
            throw new InstanceCreationWrapperException(e.getMessage(), e);
        }

    }


    private static Object getInstanceFromContextUsingId(String instanceId, Class<?> serviceClass)
            throws InstanceNotFoundInAppContextException {

        if (!Framework.instanceIsAvailableInContextByName(instanceId)) {
            throw new InstanceNotFoundInAppContextException(serviceClass, instanceId);
        }

        return Framework.INSTANCES_MAPPED_BY_NAME.get(instanceId);
    }

    private static Object getInstanceFromContextUsingType(Class<?> serviceClassType)
            throws InstanceNotFoundInAppContextException, MultipleCandidatesForInstanceException {

        if (Framework.INSTANCES_MAPPED_BY_TYPE.get(serviceClassType).isEmpty()) {
            throw new InstanceNotFoundInAppContextException(serviceClassType);
        }

        Set<Object> instanceSet = Framework.INSTANCES_MAPPED_BY_TYPE.get(serviceClassType);

        if (instanceSet.size() > 1) {
            throw new MultipleCandidatesForInstanceException(serviceClassType, instanceSet);
        }

        return instanceSet.toArray()[0];
    }

    private static boolean hasServiceAnnotation(Class<?> clazz) {
        return clazz.isAnnotationPresent(Service.class);
    }

    private static boolean hasQualifier(Parameter param) {
        return param.isAnnotationPresent(Qualifier.class);
    }

    private static boolean hasAutowired(Field field) {
        return field.isAnnotationPresent(Autowired.class);
    }

    private static boolean hasAutowired(Method field) {
        return field.isAnnotationPresent(Autowired.class);
    }

    private static boolean hasQualifier(Field field) {
        return field.isAnnotationPresent(Qualifier.class);
    }

    private static boolean hasValueAnnotation(Field field) {
        return field.isAnnotationPresent(Value.class);
    }

    private static void registerAnnotatedServiceClassTypes(Set<Class<?>> serviceTypes) {
        Framework.ANNOTATED_SERVICE_CLASS_TYPES.addAll(serviceTypes);

        serviceTypes.forEach(type -> {
            Framework.ANNOTATED_SERVICE_CLASS_TYPES.addAll(getSuperClasses(type));
            Framework.ANNOTATED_SERVICE_CLASS_TYPES.addAll(Arrays.asList(type.getInterfaces()));
        });
    }

    private static Collection<Class<?>> getSuperClasses(Class<?> type) {
        Collection<Class<?>> superClasses = new HashSet<>();
        Class<?> superclass = type.getSuperclass();
        while (superclass != null) {
            superClasses.add(superclass);
            superclass = superclass.getSuperclass();
        }
        return superClasses;
    }

    public static Object getInstanceFromAppContext(Class<?> serviceClassType)
            throws InstanceCreationWrapperException {
        try {
            Object instance;

            if (hasServiceAnnotationWithValue(serviceClassType)) {
                String instanceId = serviceClassType.getAnnotation(Service.class).value();
                instance = Framework.getInstanceFromContextUsingId(instanceId, serviceClassType);
            } else {
                instance = Framework.getInstanceFromContextUsingType(serviceClassType);
            }

            return instance;
        } catch (InstanceNotFoundInAppContextException | MultipleCandidatesForInstanceException e) {
            throw new InstanceCreationWrapperException(e.getMessage(), e);
        }
    }

    private static boolean hasServiceAnnotationWithValue(Class<?> serviceClassType) {
        return serviceClassType.isAnnotationPresent(Service.class) &&
                !Strings.isEmpty(serviceClassType.getAnnotation(Service.class).value());
    }

    public static void forwardContext() {
        try {
            Reflections reflections = new Reflections("application"); // should figure out how to bring this in as an option if needed (or leave blank)

            Set<Class<?>> serviceTypes = reflections.getTypesAnnotatedWith(Service.class);

            registerAnnotatedServiceClassTypes(serviceTypes);

            createInstancesRecursively(serviceTypes);

            performDI();
        } catch (Exception e) {
//            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static void performDI() throws InstanceCreationWrapperException {
        List<Class<?>> serviceAnnotatedClasses = Framework.ANNOTATED_SERVICE_CLASS_TYPES
                .stream()
                .filter(Framework::hasServiceAnnotation)
                .toList();

        for (Class<?> serviceClassType : serviceAnnotatedClasses) {

            Object parentClassInstance = Framework.getInstanceFromAppContext(serviceClassType);

            Framework.performFieldInjection(parentClassInstance,
                    serviceClassType.getDeclaredFields());

            Framework.performSetterInjection(parentClassInstance, serviceClassType,
                    serviceClassType.getDeclaredMethods());
        }
    }

}
