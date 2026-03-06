package serialization.test.serializer;

import java.util.List;
import serialization.test.model.Product;

public interface Serializer {
    byte[] serialize(List<Product> products) throws Exception;
    List<Product> deserialize(byte[] data) throws Exception;
    String getFormatName();
}