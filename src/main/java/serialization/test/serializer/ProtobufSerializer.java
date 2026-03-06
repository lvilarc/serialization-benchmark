package serialization.test.serializer;

import serialization.test.model.Product;
import serialization.test.proto.ProductProtos;
import java.util.ArrayList;
import java.util.List;

public class ProtobufSerializer implements Serializer {
    
    @Override
    public byte[] serialize(List<Product> products) throws Exception {
        ProductProtos.ProductList.Builder listBuilder = ProductProtos.ProductList.newBuilder();
        
        for (Product p : products) {
            ProductProtos.Product protoProduct = ProductProtos.Product.newBuilder()
                .setId(p.getId())
                .setName(p.getName())
                .setPrice(p.getPrice())
                .setCategory(p.getCategory())
                .setStockQuantity(p.getStockQuantity())
                .build();
            listBuilder.addProducts(protoProduct);
        }
        
        return listBuilder.build().toByteArray();
    }
    
    @Override
    public List<Product> deserialize(byte[] data) throws Exception {
        ProductProtos.ProductList protoList = ProductProtos.ProductList.parseFrom(data);
        List<Product> products = new ArrayList<>(protoList.getProductsCount());
        
        for (ProductProtos.Product protoProduct : protoList.getProductsList()) {
            products.add(new Product(
                protoProduct.getId(),
                protoProduct.getName(),
                protoProduct.getPrice(),
                protoProduct.getCategory(),
                protoProduct.getStockQuantity()
            ));
        }
        
        return products;
    }
    
    @Override
    public String getFormatName() {
        return "Protocol Buffers";
    }
}