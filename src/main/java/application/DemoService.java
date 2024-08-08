package application;

import framework.annotations.Service;

@Service()
public class DemoService {
    public DemoService(){
        System.out.println("Inside demo service");
    }
}
