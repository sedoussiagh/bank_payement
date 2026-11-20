package bankpayment.biller;

import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.MountableFile;

import java.util.ArrayList;

import static com.mongodb.client.model.Filters.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class MongoReplicaSetTest {
    private static final String DATABASE = "biller";
    private static final String SCRIPT = "/tmp/biller-indexes.js";

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer(
            "mongo@sha256:bdc216e9d5a6ec2cfdac789b5832fdab64cd32055f59885cae918a92de63c361")
            .withReplicaSet();

    @Test
    void commitsOnReplicaSetAndPreservesDataWhenReapplyingIndexes() throws Exception {
        MONGO.copyFileToContainer(MountableFile.forClasspathResource("mongo/V1__biller_indexes.js"), SCRIPT);
        applyIndexes();

        try (var client = MongoClients.create(MONGO.getReplicaSetUrl());
             var session = client.startSession()) {
            var database = client.getDatabase(DATABASE);
            var hello = database.runCommand(new Document("hello", 1));
            assertTrue(hello.getBoolean("isWritablePrimary"));
            assertNotNull(hello.getString("setName"));
            assertEquals("8.0.38", database.runCommand(new Document("buildInfo", 1)).getString("version"));

            var invoices = database.getCollection("invoices");
            var data = new Document("_id", "test-invoice").append("billerId", "test-biller")
                    .append("invoiceRef", "demo-1").append("amountMinor", 14025L);
            var options = TransactionOptions.builder().writeConcern(WriteConcern.MAJORITY).build();
            session.withTransaction(() -> {
                invoices.insertOne(session, data);
                return null;
            }, options);

            applyIndexes();

            assertEquals(data, invoices.find(eq("_id", "test-invoice")).first());
            assertEquals(1, invoices.countDocuments());
            var invoiceIndex = findIndex(invoices, "invoice_identity");
            assertTrue(invoiceIndex.getBoolean("unique"));
            var pendingIndex = findIndex(database.getCollection("biller_decisions"), "pending_decision_due");
            assertEquals(new Document("status", "PENDING"), pendingIndex.get("partialFilterExpression"));
        }
    }

    private static Document findIndex(MongoCollection<Document> collection, String name) {
        return collection.listIndexes().into(new ArrayList<>()).stream()
                .filter(index -> name.equals(index.getString("name")))
                .findFirst().orElseThrow();
    }

    private static void applyIndexes() throws Exception {
        var result = MONGO.execInContainer("mongosh", "--quiet", DATABASE, "--file", SCRIPT);
        assertEquals(0, result.getExitCode(), result.getStderr());
    }
}
