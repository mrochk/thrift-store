import java.util.*;
import java.io.*;

public abstract class Util {

    public final String configFilename() {
        return "config/" + this.getClass().getName() + ".config";
    }

    public final static Properties loadConfigFile(String fileName) {
        Properties properties  = new Properties();
        try (InputStream input = new FileInputStream(fileName)) {
            properties.load(input);
        } catch (IOException e) { e.printStackTrace(); }
        return properties;
    }

    public static final Object loggingLock = new Object();

    public static enum Color { ORANGE, RED, GREEN }

    public static void sleep(int ms) {
        try { Thread.sleep(ms); } 
        catch(Exception e) { e.printStackTrace(); }
    }

    public void log(String message, int ticks) {
        synchronized (loggingLock) {
            switch (getClass().getName()) {

                case "Store": // white
                    System.out.println("<tick "+ ticks + "> <thread " + Thread.currentThread().getId()+ "> " + message); 
                    break;

                case "Customer": // orange
                    System.out.println("\033[38;5;208m<tick " + ticks + "> <thread " + Thread.currentThread().getId() + "> " + message + "\033[0m");
                    break;

                case "Assistant": // red
                    System.out.println("\033[0;31m<tick "+ ticks + "> <thread " + Thread.currentThread().getId() +"> " + message + "\033[0m");
                    break;
        
                default: 
                    break;
            }
        }
    }

    public void print(String message) {
        synchronized (loggingLock) {
            switch (getClass().getName()) {

                case "Store": // green
                    System.out.print("\033[0;32m" + message + "\033[0m"); 
                    break;

                case "Customer": // orange
                    System.out.print("\033[38;5;208m" + message + "\033[0m");
                    break;

                case "Assistant": // red
                    System.out.println("\033[0;31m" + message + "\033[0m");
                    break;
        
                default: break;
            }
        }
    }
}
