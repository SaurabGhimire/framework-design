package application;

import application.observer.Observer;
import framework.annotations.*;
import lombok.Getter;

@Service
@Profile("testing")
public class DemoDAOMock implements DemoDAO{
    @Autowired
    @Qualifier("loggerObserver")
    @Getter
    Observer observer;

    @Value("devenv")
    String environment;

    @Autowired
    public DemoDAOMock() {
        System.out.println("Inside constructor of DemoDAOMock --- STRING param");
    }
    @Override
    public void print() {
        System.out.println("Inside print method of DemoDAOMock");
    }
}
