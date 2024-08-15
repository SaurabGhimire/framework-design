package application.events;

import framework.annotations.EventListener;
import framework.annotations.Service;

@Service
public class Listener {
    @EventListener
    public void onEvent(AddFeatureEvent event) {
        System.out.println("Received Event :" + event.getMessage());;
    }
}
