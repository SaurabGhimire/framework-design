package application.observer;

import framework.annotations.Service;

@Service("emailSenderObserver")
public class EmailSender implements Observer {
    @Override
    public void update(String message) {
        System.out.println("EmailSender" +
                " received message: " + message);
    }
}
