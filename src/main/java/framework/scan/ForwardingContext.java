package framework.scan;

import framework.annotations.Service;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ForwardingContext {
    private static List<Object> serviceObjectList = new ArrayList<>();

    public void forwardContext(){
        try{
            Reflections reflections = new Reflections("");
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

    }
}
