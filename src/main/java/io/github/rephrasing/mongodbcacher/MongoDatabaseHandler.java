package io.github.rephrasing.mongodbcacher;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

@SuppressWarnings("all")
public final class MongoDatabaseHandler {

    private MongoClient client;

    public MongoDatabaseHandler() {}

    public void connect(String connectionString) {
        if (client != null) throw new RuntimeException("Already connected to mongodb");
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .build();
        this.client = MongoClients.create(settings);
    }

    protected MongoCollection<Document> getCollection(String databaseName, String collectionName) {
        return client.getDatabase(databaseName).getCollection(collectionName);
    }

    public boolean isConnected() {
        return client != null;
    }

    public void disconnect() {
        if (client == null) throw new RuntimeException("Not connected to mongodb");
        client.close();
    }
}
