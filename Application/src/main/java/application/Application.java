package application;

import framework.Framework;
import framework.annotations.Autowired;

public class Application implements Runnable {
    @Autowired
    private DemoService demoService;
    public static void main(String[] args) {
        try {
            Framework.run(Application.class, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public void run()  {
        demoService.print();
    }
}
