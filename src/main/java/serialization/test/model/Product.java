package serialization.test.model;

public class Product {
    private String id;
    private String name;
    private double price;
    private String category;
    private int stockQuantity;

    public Product() {
    }

    public Product(String id, String name, double price, String category, int stockQuantity) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.stockQuantity = stockQuantity;
    }

    // getters e setters
    public String getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public String getCategory() { return category; }
    public int getStockQuantity() { return stockQuantity; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setPrice(double price) { this.price = price; }
    public void setCategory(String category) { this.category = category; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }
}