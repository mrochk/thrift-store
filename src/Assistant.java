import java.util.*;

public class Assistant extends Util implements Runnable  {
    private static final int TIME_TO_SECTION       = 10; // time to go to a certain section
    private static final int TIME_TO_DELIVERY_AREA = 10; // time to walk back to delivery area when in a section

    private final int MAX_SECTIONS;  // maximum number of category of items the assistant can carry
    private final int MAX_ITEMS; // maximum number of items the assistant can carry
    private final int BREAK_INTERVAL; // 1 / BREAK_INTERVAL = P("assistant takes a break at each tick")
    private final int BREAK_TIME; // duration of a break

    private final Random randgen = new Random();
    private final Store store; // store in which the assistant works

    private Queue<Item> carriedItems; // items carried by the assistant 
    private int lastTick; 

    public Assistant(Store store) {
        String configFile = configFilename();
        Properties config = loadConfigFile(configFile);

        this.store   = store;
        carriedItems = new LinkedList<>();

        MAX_ITEMS      = Integer.parseInt(config.getProperty("MAX_CARRIED_ITEMS"));
        MAX_SECTIONS   = Integer.parseInt(config.getProperty("MAX_CARRIED_SECTIONS"));
        BREAK_INTERVAL = Integer.parseInt(config.getProperty("BREAK_INTERVAL"));
        BREAK_TIME     = Integer.parseInt(config.getProperty("BREAK_TIME"));
    }

    private boolean canCarryMoreItems() { return carriedItems.size() < MAX_ITEMS; }

    private boolean isCarryingItems() { return !carriedItems.isEmpty(); }

    private boolean wantsToTakeBreak() { return randgen.nextInt(BREAK_INTERVAL) == 0; }

    /* Sleep the required amount of time to go to section "section". */
    private void walkToSection(Section section) {
        log("is walking to section " + section.category + "...", store.ticks);
        sleep(store.TICK_TIME * (TIME_TO_SECTION + carriedItems.size()));
    }

    /* Sleep the required amount of time to go back to delivery area. */
    private void walkToDeliveryArea() {
        log("is walking back to delivery area...", store.ticks);
        sleep(store.TICK_TIME * TIME_TO_DELIVERY_AREA);
    }

    /* True if there is at least one customer waiting in some section. */
    private boolean customersWaiting() {
        for (Category category : Category.values()) {
            if (store.sections.get(category).waitingCustomers > 0) { 
                return true; 
            }
        }
        return false;
    }

    /* Returns a set containing all the empty sections. */
    private Set<Category> sectionsWith0Items() {
        Set<Category> result = new HashSet<>();

        for (Category category : Category.values()) {
            if (store.deliveryBox.numberOfItems(category) == 0) {
                result.add(category);
            }
        }
        return result;
    }

    /* Takes as much items as possible of category "category",
     * returns the number of items taken. 
     */
    private int takeItems(Category category) {
        int items = 0; // number of items taken 

        // take items from delivery box
        for (Item item : store.deliveryBox.getItems(category)) {
            // if we can't carry more items, we stop taking them
            if (carriedItems.size() == MAX_ITEMS) { break; }
            carriedItems.add(item);
            items++;
        }

        // remove the items from the delivery box
        for (int i = 0; i < items; i++) {
             store.deliveryBox.removeItem(category); 
        }

        log("took " + items + " items from section " + category, store.ticks);

        return items;
    }

    /* Take items from the delivery box if not empty. */
    private void takeItemsFromDeliveryBox() {

        // ensuring no two assistants can 
        // take items from the box at the same time
        store.deliveryBox.requestAccess();

        try {
            // Check if there are items that need to be 
            // put into sections, else do nothing
            if (store.emptyDeliveryBox()) { return; }

            log("saw that delivery box is not empty", store.ticks);

            int items    = 0; // number of items taken
            int sections = 0; // number of different sections taken

            while (sections < MAX_SECTIONS && !store.emptyDeliveryBox() && 
                    canCarryMoreItems()) {

                // to optimize the process, we first take the items from
                // the section where there are the most customers waiting,
                // if no customer is waiting, we take the items from the sections
                // of which there are the most items in the delivery box

                Category category = null; // the category from which we will select the items

                if (customersWaiting()) { // if there are customers waiting we prioritize items from these sections
                    int max = 0;

                    Set<Category> zeroItems = sectionsWith0Items();

                    // selecting the category that has the most customers waiting in
                    // AND that the delivery box contains items
                    for (Category cat : Category.values()) {
                        int customers = store.sections.get(cat).waitingCustomers;
                        if (customers > max && !zeroItems.contains(cat)) { 
                            max = customers; category = cat; 
                        }
                    }

                    // it is possible that between the moment we checked if there 
                    // was at least one customer waiting and now, an assistant filled the section
                    // and thus there are no customers waiting anymore, in this case we just go to the
                    // second part of if statement [if (category == null)]

                    // -> also, it is possible that while the assistant will be walking
                    // to the section (where the customer is waiting), an assistant fills the 
                    // section and therefore the customer leaves, in this case it is not 
                    // optimal -> but it doesnt break anything 

                    if (category != null) {
                        log("saw that there are customers waiting in " + category, store.ticks);
                    }
                } 

                // if (there are 0 customers waiting) || (no items to take for these sections)
                if (category == null) {
                    int max = 0;

                    // selecting the category that has the most items
                    for (Category cat : Category.values()) {
                        int nItems = store.deliveryBox.numberOfItems(cat);
                        if (nItems > max) { max = nItems; category = cat; }
                    }
                }

                int itemsTaken = takeItems(category);
                items += itemsTaken;
                store.itemsInBox -= itemsTaken; // update the number of items in the delivery box
                sections++;
            }
            log("took " + items + " items from delivery box (current number of carried items: " + carriedItems.size() + ")", store.ticks);

        } finally { store.deliveryBox.freeAccess(); } 
        // finally we let other assistants access the delivery box
    }

    /* Add items to a section while the peek item of the stack
     * is of the same category than the one of the section.
     */
    private void addItemsToSection(Section section) {
        // lock ensuring that no two assistants can put items in a section
        // at the same time, and no customer can buy at the same time
        section.requestAccess(); 
        log("starts adding items to section " + section.category + "...", store.ticks);

        try {
            int addedItems = 0; // to keep track of the number of items we added

            while (isCarryingItems() && 
                    carriedItems.peek().getCategory() == section.category) {

                Item peekItem = carriedItems.poll(); // remove the item from the assistant queue of items
                section.addItem(peekItem); // add the item to the section
                addedItems++;

                log("added 1 item to section " + section.category, store.ticks);
                sleep(store.TICK_TIME); // it takes 1 tick to add an item to a section 
            }

            log("finished adding " + addedItems + " items to section " + section.category, store.ticks);

        } finally { section.freeAccess(); }
        // finally we let other actors access the section
    }
    
    /* Put the items carried by the assistant in their
     * respective corresponding sections.
     */
    private void putCarriedItemsInSections() {
        // we know it will visit at most "MAX_CARRIED_SECTIONS" sections
        while (isCarryingItems()) {
            Category category = carriedItems.peek().getCategory(); // checking in what section we need to go
            Section section   = store.sections.get(category); // getting the section in which we need to go

            walkToSection(section);

            addItemsToSection(section);
        }
    }

    @Override
    public void run() {
        log("assistant started", store.ticks);

        for (;;) {

            if (lastTick != store.ticks) {
                lastTick = store.ticks;

                if (wantsToTakeBreak()) {
                    log("is taking a break...", store.ticks);
                    sleep(store.TICK_TIME * BREAK_TIME);
                }
            }

            if (canCarryMoreItems()) { 
                takeItemsFromDeliveryBox(); 
            }

            if (isCarryingItems()) {
                putCarriedItemsInSections();
                walkToDeliveryArea();
            }

            sleep(1);
        }
    }
}
