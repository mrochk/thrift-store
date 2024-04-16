import java.util.*;

public class DeliveryBox extends SharedResource {
    private Map<Category, Queue<Item>> box; // the box where the items get delivered

    public Item removeItem(Category category) {
        assert !box.get(category).isEmpty();
        return box.get(category).remove();
    }

    public int numberOfItems(Category category) { return box.get(category).size(); }

    public Queue<Item> getItems(Category category) { return box.get(category); }

    public void addItemToBox(Item item) { box.get(item.getCategory()).add(item); }

    public DeliveryBox() {
        box = new HashMap<>();
        for(Category category : Category.values()) {
            box.put(category, new LinkedList<>());
        }
    }
}
