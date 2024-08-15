package application.events;

public class AddFeatureEvent {
    private String message;
    public AddFeatureEvent(String message) {
        this.message = message;
    }
    public String getMessage() {
        return message;
    }
}
