/* An item in the store. */
public class Item {
    private final Category category;

    Item(Category category) { this.category = category; }

    public Category getCategory() { return category; }
}