package framework.events;

import framework.annotations.Service;
import framework.utils.PropertyAccessor;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

@Service
public class FrameworkPublisher {
    private PropertyChangeSupport support;

    public FrameworkPublisher() {
        support = new PropertyChangeSupport(this);
    }

    public void publishEvent(Object object) {
        if (object == null) {
            System.out.println("Cannot publish event: Object is null");
            return;
        }
        support.firePropertyChange(object.getClass().getSimpleName(), null, object);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }
}
