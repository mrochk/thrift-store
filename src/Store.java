import java.util.*;

/* Our thrift store. */
public class Store extends Util {
    public final int ITEMS_PER_DELIVERY; // number of items delivered by the delivery truck
    public final int DELIVERY_INTERVAL;  // 1 / DELIVERY_INTERVAL = P("delivery at tick t")
    public final int INITIAL_ITEMS;      // initial number of items in each section
    public final int TICK_TIME;          // time (in milliseconds) of each tick
    public final int ASSISTANTS;         // initial number of items in each section
    public final int CUSTOMERS;          // time (in milliseconds) of each tick

    public HashMap<Category, Section> sections; // sections of the shop containing items
    public DeliveryBox deliveryBox;             // box where the items get delivered

    private DeliveryTruck truck; // delivers items with probability 1% each tick
    private Random random;       // random number generator

    public volatile int itemsInBox; // number of items waiting to be put in sections
    public volatile int ticks;      // number of ticks since shop started

    public Store() {

        String configFile = configFilename();
        Properties config = loadConfigFile(configFile);

        INITIAL_ITEMS      = Integer.parseInt(config.getProperty("INITIAL_ITEMS"));
        TICK_TIME          = Integer.parseInt(config.getProperty("TICK_TIME"));
        DELIVERY_INTERVAL  = Integer.parseInt(config.getProperty("DELIVERY_INTERVAL"));
        ITEMS_PER_DELIVERY = Integer.parseInt(config.getProperty("ITEMS_PER_DELIVERY"));
        ASSISTANTS         = Integer.parseInt(config.getProperty("ASSISTANTS"));
        CUSTOMERS          = Integer.parseInt(config.getProperty("CUSTOMERS"));

        random      = new Random();
        sections    = new HashMap<>();
        truck       = new DeliveryTruck();
        deliveryBox = new DeliveryBox();

        // instantiating each section with required initial number of items 
        for(Category category : Category.values()) {
            sections.put(category, new Section(category));
            for (int i = 0; i < INITIAL_ITEMS; i++) {
                sections.get(category).addItem(new Item(category));
            }
        }
    }

    public boolean emptyDeliveryBox() { return itemsInBox == 0; }

    private boolean deliveryArrived() { return random.nextInt(DELIVERY_INTERVAL) == 0; }

    /* Receive a delivery from the delivry truck
     * and put the items in the store delivery box.
     */
    private void receiveDelivery() {

        // ensuring no two assistants can put or take items from
        // the delivery box simultaneously
        deliveryBox.requestAccess();

        try {
            // receive delivery
            Item[] itemsDelivered = truck.randomDelivery(random, ITEMS_PER_DELIVERY);

            Map<Category, Integer> count = new HashMap<>(); // used to display the number of items in each section
            for (Category category : Category.values()) { count.put(category, 0); }

            // put the items in the delivery box
            for (Item item : itemsDelivered) { 
                deliveryBox.addItemToBox(item); 
                count.put(item.getCategory(), count.get(item.getCategory()) + 1);
                itemsInBox++;
            }

            // logging the delivery
            StringBuilder deliveryMsg = new StringBuilder();
            deliveryMsg.append("=> Delivery Received:");
            for (Category category : Category.values()) {
                deliveryMsg.append(" " + category.toString() + ":" + count.get(category));
            }
            deliveryMsg.append("\n\n");
            print(deliveryMsg.toString());

        } finally { 
            deliveryBox.freeAccess(); // allow the assistants to access the delivery box
        }
    }

    /* Logging the state of the shop at each tick. */
    private void tickLog() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("\n | sections: ");

        for (Category category : Category.values()) {
            Section section = sections.get(category);
            stringBuilder.append(category + ": " + section.numberOfItems() + " (");
            stringBuilder.append("" + section.waitingCustomers);
            stringBuilder.append(") ");
        }
        stringBuilder.append("\n");

        stringBuilder.append(" | delivery box: ");
        for (Category category : Category.values()) {
            stringBuilder.append(category + ": " + deliveryBox.numberOfItems(category) + " ");
        }
        stringBuilder.append("\n");

        log(stringBuilder.toString(), ticks);
    }

    public void tick() {
        ticks++; // increment number of ticks

        tickLog(); // print the state of the shop

        // receive delivery with probability 1%.
        if (deliveryArrived()) { receiveDelivery(); }
    }

    public void start() {
        log("starting the store...\n", ticks); sleep(100);

        // starting the assistants threads
        for (int i = 0; i < ASSISTANTS; i++) { new Thread(new Assistant(this)).start(); sleep(100); } print("\n");

        // starting the customers threads
        for (int i = 0; i < CUSTOMERS; i++) { new Thread(new Customer(this)).start(); sleep(100); } print("\n");

        for (;;) { 
            sleep(TICK_TIME);
            tick(); 
        }
    }
}
