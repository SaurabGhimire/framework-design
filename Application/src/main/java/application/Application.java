package application;

import framework.Framework;
import framework.annotations.Autowired;

public class Application implements Runnable {
    public static void main(String[] args) {
        try {
            Framework.run(Application.class, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public void run()  {
        System.out.println("Inside run method of Application");
    }
}
