package application;

import framework.annotations.Autowired;
import framework.annotations.Service;

@Service()
public class DemoService {
    @Autowired
    DemoDAO demoDAO;

    public DemoService(){
        System.out.println("Inside constructor of DemoService");
    }

    public void print(){
        System.out.println("Inside print method of DemoService");
        demoDAO.print();
    }
}
