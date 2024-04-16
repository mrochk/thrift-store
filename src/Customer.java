import java.util.*;

public class Customer extends Util implements Runnable {
    private static final int TIME_TO_BUY = 1;  // the time it takes for a customer to buy an item
    private        final int BUY_INTERVAL; // (1 / BUY_INTERVAL) = P("customer buys an item at each tick")

    private final Random randgen; // random number generator
    private final Store store;    // the store in which the customer buys items

    private int lastTick; // to be able to log the current tick in messages

    public Customer(Store store) {
        String configFile = configFilename();
        Properties config = loadConfigFile(configFile);

        this.store = store;
        randgen    = new Random();

        BUY_INTERVAL = Integer.parseInt(config.getProperty("BUY_INTERVAL"));
    }

    /* Buy an item from a randomly selected section. */
    private void buyItem(Category itemSection) {
        Section section = store.sections.get(itemSection); // the section we will buy an item from
        boolean signaledWaiting = false; // so that we signal that we are waiting only once
        int waitedTicks = 0; // number of ticks waited before the customer could buy the item

        for (;;) { // waiting while there are no items in section

            // here we request the access, and we check if there 
            // are items that can be bought, if not, we free the 
            // access, allowing an assistant to fill the section with items
            section.requestAccess();
            if (section.numberOfItems() > 0) { break; }
            else { section.freeAccess(); }

            // we signal that we are waiting in this section 
            if (!signaledWaiting) {
                signaledWaiting = true;
                section.addWaitingCustomer();
                log("is waiting for an item in section " + section.category + "...", store.ticks);
            }

            sleep(store.TICK_TIME);
            waitedTicks++;
        }

        // => there is an item in the section

        // we signal that we stopped waiting
        if (signaledWaiting) { section.removeWaitingCustomer(); }

        log("is buying 1 item in section " + section.category, store.ticks);
        sleep(TIME_TO_BUY); 
        section.removeItem(); // actually taking the item
        log("bought 1 item in section " + section.category + ", waited ticks: " + waitedTicks, store.ticks);

        // at this point the access is not yet given back to the other threads
        section.freeAccess();
    }

    private boolean buysItem() { return randgen.nextInt(BUY_INTERVAL) == 0; }

    @Override
    public void run() {
        log("customer started", store.ticks);

        for (;;) {
            if (lastTick != store.ticks) {
                lastTick = store.ticks; // updating the tick number

                // if the customer decides to buy an item 
                if (buysItem()) {
                    // selecting a random item category
                    Category itemSection = Category.randomCategory(randgen);

                    log("decided to buy an item in " + itemSection, store.ticks);

                    buyItem(itemSection);
                }
            }

            sleep(1); // small pause between updates
        }
    }
}
