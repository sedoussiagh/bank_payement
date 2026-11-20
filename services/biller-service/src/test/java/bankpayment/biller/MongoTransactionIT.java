package bankpayment.biller;

import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.mongodb.client.model.Filters.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class MongoTransactionIT {
    @Inject
    MongoClient client;

    @Test
    void commitsAndReadsFromReplicaSet() {
        var database = client.getDatabase("biller");
        var name = "smoke_" + UUID.randomUUID().toString().replace("-", "");
        database.createCollection(name);
        var collection = database.getCollection(name);
        try (var session = client.startSession()) {
            var options = TransactionOptions.builder().writeConcern(WriteConcern.MAJORITY).build();
            session.withTransaction(() -> {
                collection.insertOne(session, new Document("_id", "transaction-smoke"));
                return null;
            }, options);
            assertEquals(1, collection.countDocuments(eq("_id", "transaction-smoke")));
        } finally {
            collection.drop();
        }
    }
}
