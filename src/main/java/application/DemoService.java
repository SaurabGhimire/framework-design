package application;

import framework.annotations.Autowired;
import framework.annotations.Service;

@Service
public class DemoService {
    @Autowired
    DemoDAO demoDAO;

    @Autowired
    public DemoService(DemoDAO demoDAO){
        System.out.println("Inside constructor of DemoService — DemoDAO param");
    }

//    public DemoService(){
//        System.out.println("Inside constructor of DemoService");
//    }

    public void print(){
        System.out.println("Inside print method of DemoService");
        demoDAO.print();
    }
}
