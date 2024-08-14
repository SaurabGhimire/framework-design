package application;

import application.observer.Observer;
import framework.annotations.Autowired;
import framework.annotations.Qualifier;
import framework.annotations.Service;

@Service
public class DemoService {

    DemoDAO demoDAO;

    @Autowired
    private MailProperties mailProperties;

    Observer observer;

    @Autowired
    public DemoService(DemoDAO demoDAO) {
        this.demoDAO = demoDAO;
        System.out.println("Inside constructor of DemoService — DemoDAO param");
    }

    public void print() {
        System.out.println("Inside print method of DemoService");
        demoDAO.print();
    }

    @Autowired
    public void setObserver(@Qualifier("emailSenderObserver") Observer observer) {
        this.observer = observer;
        System.out.println("Setter injection of observer instance: " + observer);
    }
}
