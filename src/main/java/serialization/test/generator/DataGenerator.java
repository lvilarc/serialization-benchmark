package serialization.test.generator;

import serialization.test.model.Product;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class DataGenerator {

    private static Random rand = new Random();
    private static String[] categories = {"Electronics", "Books", "Clothing", "Food", "Toys", "Sports", "Home", "Beauty"};
    private static String[] productNames = {
        "Laptop", "Smartphone", "Headphones", "Mouse", "Keyboard", 
        "Monitor", "Tablet", "Camera", "Printer", "Speaker",
        "Book", "Notebook", "Pen", "Backpack", "T-shirt",
        "Jeans", "Shoes", "Watch", "Glasses", "Hat"
    };

    public static List<Product> generateSmallPayload() {
        List<Product> list = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            list.add(new Product(
                generateId(),
                productNames[rand.nextInt(productNames.length)] + " " + (i + 1),
                10 + rand.nextDouble() * 990,  
                categories[rand.nextInt(categories.length)],
                1 + rand.nextInt(100)  
            ));
        }
        return list;
    }

    public static List<Product> generateMediumPayload() {
        List<Product> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            list.add(new Product(
                generateId(),
                productNames[rand.nextInt(productNames.length)] + " " + (i + 1),
                10 + rand.nextDouble() * 990,
                categories[rand.nextInt(categories.length)],
                1 + rand.nextInt(500) 
            ));
        }
        return list;
    }

    public static List<Product> generateLargePayload() {
        List<Product> list = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            list.add(new Product(
                generateId(),
                productNames[rand.nextInt(productNames.length)] + " " + (i + 1),
                10 + rand.nextDouble() * 1990,  
                categories[rand.nextInt(categories.length)],
                1 + rand.nextInt(1000)  
            ));
        }
        return list;
    }
    
    private static String generateId() {
        return UUID.randomUUID().toString();  
    }
}