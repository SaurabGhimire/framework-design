package application.events;

import framework.annotations.EventListener;
import framework.annotations.Service;

@Service
public class Listener2 {
    @EventListener
    public void onEvent(SubtractFeatureEvent event) {
        System.out.println("Received Event SubtractFeatureEvent:" + event.getMessage());;
    }
}
