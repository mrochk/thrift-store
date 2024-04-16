import java.util.*;

/* Section containing items waiting to be bought. */
public class Section extends SharedResource {
    private Set<Item> items; // the items in the section

    public final Category category; // the category of the items in this section

    public volatile int waitingCustomers; // the number of customer waiting in this section,
                                          // any read of this variable must be always loaded from main memory

    public Section(Category category) {
        this.category = category;
        items = new HashSet<>();
    }

    /* Used by customers to signal that they are waiting. */
    public synchronized void addWaitingCustomer() { waitingCustomers++; }

    /* Used by customers to signal that they finished waiting. */
    public synchronized void removeWaitingCustomer() { waitingCustomers--; }

    /* Get the current number of items in the section/ */
    public final int numberOfItems() { return items.size(); }

    /* Used by assistants to add an item to the section. */
    public void addItem(Item item) { 
        assert item.getCategory() == this.category;
        items.add(item); 
    }

    /* Used by customers to buy an item from the section. */
    public void removeItem() { 
        assert !items.isEmpty();

        int before = items.size();
        items.remove(items.toArray()[0]); // -> maybe I should use another data structure to store the items
        int after = items.size();

        assert after < before;
    }

}
