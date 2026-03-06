package serialization.test.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import serialization.test.model.Product;
import java.util.List;

public class JsonSerializer implements Serializer { 
    private final ObjectMapper objectMapper;
    
    public JsonSerializer() {
        this.objectMapper = new ObjectMapper();
        // Prevent errors when serializing classes with no fields (empty beans)
        this.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        // Disable pretty printing to produce compact JSON and reduce size
        this.objectMapper.configure(SerializationFeature.INDENT_OUTPUT, false);
    }
    
    @Override
    public byte[] serialize(List<Product> products) throws Exception {
        return objectMapper.writeValueAsBytes(products);
    }
    
    @Override
    public List<Product> deserialize(byte[] data) throws Exception {
        return objectMapper.readValue(data, 
            objectMapper.getTypeFactory().constructCollectionType(List.class, Product.class));
    }
    
    @Override
    public String getFormatName() {
        return "JSON (Jackson)";
    }
}