package vorez.mods.skins.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.util.UUIDTypeAdapter;
import vorez.mods.skins.impl.Utils.MinecraftUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MojangService {

    private static final LoadingCache<String, Optional<GameProfile>> resolvedProfiles = CacheBuilder.newBuilder()
            .expireAfterAccess(3, TimeUnit.HOURS)
            .refreshAfterWrite(30, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {

                private final Gson gson = new GsonBuilder()
                        .registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
                        .create();

                @Override
                public Optional<GameProfile> load(String key) {
                    if (Shared.isBlank(key))
                        return Optional.of(Shared.DUMMY);

                    return Optional.ofNullable(Shared.call(() -> makeRequest(
                            String.format("https://api.mojang.com/users/profiles/minecraft/%s", key)
                    ), null, null));
                }

                private GameProfile makeRequest(String request) throws IOException {
                    HttpURLConnection conn = (HttpURLConnection) URI.create(request)
                            .toURL()
                            .openConnection(MinecraftUtils.getProxy());

                    conn.setConnectTimeout(30000);
                    conn.setReadTimeout(10000);
                    conn.setUseCaches(false);
                    conn.connect();

                    int code = conn.getResponseCode();

                    if (code == 204 || code == 404)
                        return Shared.DUMMY;

                    if (code / 100 == 2) {
                        try (InputStreamReader reader = new InputStreamReader(
                                conn.getInputStream(),
                                StandardCharsets.UTF_8
                        )) {
                            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();

                            UUID id = obj.has("id") && !obj.get("id").isJsonNull()
                                    ? gson.fromJson(obj.get("id"), UUID.class)
                                    : null;

                            String name = obj.has("name") && !obj.get("name").isJsonNull()
                                    ? obj.get("name").getAsString()
                                    : null;

                            if (id == null || Shared.isBlank(name))
                                return Shared.DUMMY;

                            if (Shared.isOfflinePlayer(id, name))
                                return Shared.DUMMY;

                            return new GameProfile(id, name);
                        }
                    }

                    return null;
                }

                @Override
                public ListenableFuture<Optional<GameProfile>> reload(
                        String key,
                        Optional<GameProfile> oldValue
                ) {
                    if (oldValue.isPresent()) {
                        if (oldValue.get() == Shared.DUMMY)
                            return Futures.immediateFuture(Optional.empty());

                        return Futures.immediateFuture(oldValue);
                    }

                    return Shared.submitTask(() -> load(key));
                }
            });

    /**
     * @param username the username to query about, requires non-blank to actually resolve.
     * @return a ListenableFuture of a resolved profile, otherwise {@link Shared#DUMMY DUMMY}.
     */
    public static ListenableFuture<GameProfile> getProfile(String username) {
        if (username == null)
            return Futures.immediateFailedFuture(new NullPointerException("username must not be null"));

        Optional<GameProfile> cachedResult;
        if ((cachedResult = resolvedProfiles.getIfPresent(username)) != null)
            return Futures.immediateFuture(cachedResult.orElse(Shared.DUMMY));

        return Shared.submitTask(() -> resolvedProfiles.getUnchecked(username).orElse(Shared.DUMMY));
    }
}