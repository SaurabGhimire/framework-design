package application;

import application.observer.Observer;
import framework.annotations.Autowired;
import framework.annotations.Service;

@Service
public class DemoDAO {
    @Autowired
    Observer observer;

    @Autowired
    public DemoDAO() {
        System.out.println("Inside constructor of DemoDAO --- STRING param");
        System.out.println("Observer instance: " + observer);
    }

    public void print() {
        System.out.println("Inside print method of demoDAO");
    }
}
