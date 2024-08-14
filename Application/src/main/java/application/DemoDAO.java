package application;

import application.observer.Observer;
import framework.annotations.Autowired;
import framework.annotations.Qualifier;
import framework.annotations.Service;
import framework.annotations.Value;
import lombok.Getter;

public interface DemoDAO {
    void print();
}
