package application;

import application.observer.Observer;
import framework.annotations.Autowired;
import framework.annotations.Qualifier;
import framework.annotations.Service;
import framework.annotations.Value;
import lombok.Getter;

@Service
public class DemoDAO {
    @Autowired
    @Qualifier("loggerObserver")
    @Getter
    Observer observer;

    @Value("env")
    String environment;

    @Autowired
    public DemoDAO() {
        System.out.println("Inside constructor of DemoDAO --- STRING param");
    }

    public void print() {
        System.out.println("Inside print method of demoDAO");
    }
}
