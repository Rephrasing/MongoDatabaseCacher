package io.github.rephrasing.mongodbcacher;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.mongodb.client.MongoCollection;
import lombok.Getter;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Getter
public abstract class MongoCacher<T> implements JsonSerializer<T>, JsonDeserializer<T> {

    private List<T> cache;
    private final String databaseName;
    private final String collectionName;

    public MongoCacher(String databaseName, String collectionName) {
        this.cache = new ArrayList<>();
        this.databaseName = databaseName;
        this.collectionName = collectionName;
    }

    abstract public Class<T> getType();

    abstract public boolean compare(T object1, T object2);

    public void fetch(MongoDatabaseHandler databaseHandler, MongoCacheHandler cacheHandler) {
        if (!databaseHandler.isConnected()) {
            throw new RuntimeException("Provided MongoDatabaseHandler is not connected to mongodb");
        }
        MongoCollection<Document> coll = databaseHandler.getCollection(databaseName, collectionName);
        List<T> fetched = new ArrayList<>();
        for (Document doc : coll.find()) {
            String json = doc.toJson();
            T object = cacheHandler.getGson().fromJson(json, getType());
            fetched.add(object);
        }

        this.cache = fetched;
    }

    public void push(MongoDatabaseHandler databaseHandler, MongoCacheHandler cacheHandler) {
        if (!databaseHandler.isConnected()) {
            throw new RuntimeException("Provided MongoDatabaseHandler is not connected to mongodb");
        }
        MongoCollection<Document> coll = databaseHandler.getCollection(databaseName, collectionName);
        for (T object : cache) {
            for (Document doc : coll.find()) {
                T docItem = cacheHandler.getGson().fromJson(doc.toJson(), getType());
                if (compare(object, docItem)) {
                    coll.deleteOne(doc);
                }
            }
        }
        coll.insertMany(cache.stream().map(t -> Document.parse(cacheHandler.getGson().toJson(t))).collect(Collectors.toList()));
    }

    public void cache(T object) {
        cache.stream().filter(loop -> compare(object, loop)).findFirst().ifPresent(cache::remove);
        cache.add(object);
    }

    public Optional<T> get(Predicate<T> search) {
        return cache.stream().filter(search).findFirst();
    }

    public boolean drop(Predicate<T> search) {
        return cache.removeIf(search);
    }

    public boolean ifPresent(Predicate<T> search, Consumer<T> ifPresent) {
        Optional<T> optional = get(search);
        if (optional.isEmpty()) return false;
        ifPresent.accept(optional.get());
        cache(optional.get());
        return true;
    }

    public void ifPresentOrElse(Predicate<T> search, Consumer<T> present, Runnable ifNotPresent) {
        Optional<T> optional = get(search);
        if (optional.isEmpty()) {
            ifNotPresent.run();
            return;
        }
        present.accept(optional.get());
        cache(optional.get());
    }

    public <S> Optional<S> getNestedIfPresent(Predicate<T> search, Function<T, S> function) {
        Optional<T> optional = get(search);
        if (optional.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(function.apply(optional.get()));
    }
}
