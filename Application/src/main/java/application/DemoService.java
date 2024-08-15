package application;

import application.events.AddFeatureEvent;
import application.events.SubtractFeatureEvent;
import application.observer.Observer;
import framework.events.FrameworkPublisher;
import framework.annotations.*;

@Service
public class DemoService {

    DemoDAO demoDAO;
    Observer observer;
    @Autowired
    private MailProperties mailProperties;

    @Autowired
    private FrameworkPublisher frameworkPublisher;

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

        // async testing
        asyncMethod();
        normalMethod();
    }

    @Async
    public void asyncMethod() {
        System.out.println("Async method running in thread: " + Thread.currentThread().getName());
    }

    public void normalMethod() {
        System.out.println("Normal method running in thread: " + Thread.currentThread().getName());
    }

   @Scheduled(fixedRate = 5000)
    public void scheduledMethod() {
        System.out.println("Printing from inside scheduledMethod - every 5s");
    }

   @Scheduled(cron = "5 1")
    public void scheduledByCron() {
        System.out.println("Printing from inside scheduledByCron - every 1m, 5s => 65s");
    }

    public void publishEvent() {
        frameworkPublisher.publishEvent(new SubtractFeatureEvent("New Feature for event was added"));
    }
}
