package io.github.rephrasing.mongodbcacher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("all")
public final class MongoCacheHandler {

    private final GsonBuilder gsonBuilder;
    private final List<MongoCacher> cachers;

    public MongoCacheHandler() {
        this.gsonBuilder = new GsonBuilder();
        this.cachers = new ArrayList<>();
    }

    public Gson getGson() {
        for (MongoCacher cacher : cachers) {
            gsonBuilder.registerTypeAdapter(cacher.getType(), cacher);
        }
        return gsonBuilder.setPrettyPrinting().serializeNulls().create();
    }

    public void registerCachers(MongoCacher cacher, MongoCacher... cachers) {
        registerCacher(cacher);
        for (MongoCacher loop : cachers) {
            registerCacher(loop);
        }
    }

    public boolean registerCacher(MongoCacher cacher) {
        Optional<MongoCacher> optional = getCacher(cacher.getType());
        if (optional.isEmpty()) {
            this.cachers.add(cacher);
            return true;
        }
        return false;
    }

    public <T> Optional<MongoCacher<T>> getCacher(Class<T> clazz) {
        return cachers.stream().filter(cacher -> cacher.getType() == clazz).map(cacher -> (MongoCacher<T>) cacher).findFirst();
    }
}
