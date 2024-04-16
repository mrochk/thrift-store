import java.util.Random;

/* Enum representing the different categories of items in our shop. */
public enum Category {
    ELECTRONICS, CLOTHING, FURNITURE, TOYS, SPORTING_GOODS, BOOKS;

    /* Returns a random category. */
    public static Category randomCategory(Random random) {
        Category[] arr  = Category.values();
        int randomIndex = random.nextInt(Category.values().length);
        return arr[randomIndex];
    }
}
