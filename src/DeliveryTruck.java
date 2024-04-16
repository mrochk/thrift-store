import java.util.*;

/* The delivery trucks responsible for bringing
 * new items to the thrift store.
 */
public class DeliveryTruck {
    /* Delivers nItems random items. */
    public Item[] randomDelivery(Random random, int nItems) {
        Item[] items = new Item[nItems];
        for (int i = 0; i < nItems; i++) {
            items[i] = new Item(Category.randomCategory(random));
        }
        return items;
    }
}
