package application.events;

public class SubtractFeatureEvent {
    private String message;
    public SubtractFeatureEvent(String message) {
        this.message = message;
    }
    public String getMessage() {
        return message;
    }
}
