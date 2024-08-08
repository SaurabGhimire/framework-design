package framework;

import framework.annotations.Autowired;
import framework.annotations.Service;
import org.reflections.Reflections;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Framework {
    private static List<Object> serviceObjectList = new ArrayList<>();

    public Framework(){
        try{
            forwardContext();
        } catch (Exception e){
            e.printStackTrace();
        }
    }


    public void forwardContext(){
        try{
            Reflections reflections = new Reflections("application");
            Set<Class<?>> serviceTypes = reflections.getTypesAnnotatedWith(Service.class);
            for(Class<?> serviceClass: serviceTypes){
                serviceObjectList.add(serviceClass.getConstructor().newInstance());
            }
            performDI();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public void performDI(){
        try{
            for(Object serviceClass: serviceObjectList){
                for(Field field: serviceClass.getClass().getDeclaredFields()){
                    if(field.isAnnotationPresent(Autowired.class)){
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
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public Object getServiceBeanOfType(Class interfaceClass){
        Object service = null;
        try{
            for(Object theClass: serviceObjectList){
                Class<?>[] interfaces = theClass.getClass().getInterfaces();
                for(Class<?> theInterface: interfaces){
                    if(theInterface.getName().contentEquals(interfaceClass.getName()))
                        service = theClass;
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return service;
    }
}
