package application;

import application.observer.Observer;
import framework.annotations.Autowired;
import framework.annotations.Service;

@Service
public class DemoDAO {
    Observer observer;
    @Autowired
    public DemoDAO(Observer observer){
        this.observer = observer;
        System.out.println("Inside constructor of DemoDAO --- STRING param");
    }
//    public DemoDAO(){
//        System.out.println("Inside constructor of DemoDAO");
//    }
    public void print(){
        System.out.println("Inside print method of demoDAO");
    }
}
