package vn.devpro.marketplace.database;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseSchemaConsistencyTest {

    @Test
    void baselineSchemaContainsColumnsRequiredByOrderEntities() throws IOException {
        String schema = Files.readString(Path.of("database/digital_marketplace.sql"))
            .toLowerCase(Locale.ROOT)
            .replaceAll("\\s+", " ");

        String orders = tableDefinition(schema, "orders");
        String orderItems = tableDefinition(schema, "order_items");

        assertTrue(orders.contains("vnpay_txn_ref varchar(50)"),
            "orders must contain vnpay_txn_ref");
        assertTrue(orders.contains("vnpay_transaction_id varchar(50)"),
            "orders must contain vnpay_transaction_id");
        assertTrue(orderItems.contains("variant_id int"),
            "order_items must contain variant_id");
    }

    private String tableDefinition(String schema, String tableName) {
        int start = schema.indexOf("create table " + tableName + " (");
        int end = schema.indexOf(") engine=innodb", start);
        assertTrue(start >= 0 && end > start, "Missing table " + tableName);
        return schema.substring(start, end);
    }
}
