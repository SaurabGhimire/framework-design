package application;

import application.observer.Observer;
import framework.annotations.*;
import lombok.Getter;

@Service
@Profile("production")
public class DemoDAOImpl implements DemoDAO {
    @Autowired
    @Qualifier("loggerObserver")
    @Getter
    Observer observer;

    @Value("env")
    String environment;

    @Autowired
    public DemoDAOImpl() {
        System.out.println("Inside constructor of DemoDAOImpl --- STRING param");
    }

    public void print() {
        System.out.println("Inside print method of DemoDAOImpl");
    }
}
