import java.util.concurrent.locks.ReentrantLock;

/* This class represents a ressource shared in the shop,
 * accessed by multiple threads, these threads must begin
 * any interactions with a "SharedRessource" by asking the 
 * access to the ressource with "requestAccess" and end it 
 * with "freeAccess".
 */
public abstract class SharedResource {
    private final ReentrantLock lock = new ReentrantLock(true); // lock wit fairness set to true

    /* Wrapper around lock.lock(). */
    public void requestAccess() { lock.lock(); }

    /* Wrapper around lock.unlock(). */
    public void freeAccess() { lock.unlock(); }
}
