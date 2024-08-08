package application;

import framework.annotations.Service;

@Service
public class DemoDAO {
    public DemoDAO(){
        System.out.println("Inside constructor of DemoDAO");
    }
    public void print(){
        System.out.println("Inside print method of demoDAO");
    }
}
