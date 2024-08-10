package application.observer;

import framework.annotations.Service;

@Service("loggerObserver")
public class Logger implements Observer {
    @Override
    public void update(String message) {
        System.out.println("Logger received message: " + message);
    }
}
