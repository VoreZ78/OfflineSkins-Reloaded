package vorez.mods.skins.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import vorez.mods.skins.api.interfaces.IPlayerProfile;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class PlayerProfile implements IPlayerProfile {
    private static final Logger LOG = LogUtils.getLogger();

    private static final PlayerProfile DUMMY = new PlayerProfile(Shared.DUMMY, stableKey(Shared.DUMMY));

    private static final LoadingCache<GameProfile, PlayerProfile> profiles =
            CacheBuilder.newBuilder()
                    .weakKeys()
                    .build(new CacheLoader<>() {

                        @Override
                        public PlayerProfile load(GameProfile key) {
                            if (key == Shared.DUMMY) {
                                return DUMMY;
                            }

                            if (GameProfileCompat.properties(key) == null) {
                                return DUMMY;
                            }

                            String keyName = GameProfileCompat.name(key);
                            PlayerProfile profile = new PlayerProfile(key, stableKey(key));

                            if (!Shared.isBlank(keyName)) {
                                UUID resolvedUuid = mojangUUIDCache.get(keyName.toLowerCase(Locale.ROOT));

                                if (resolvedUuid != null) {
                                    profile.mojangUUID = resolvedUuid;
                                }
                            }

                            return profile;
                        }
                    });

    private static final ConcurrentMap<String, UUID> mojangUUIDCache = new ConcurrentHashMap<>();

    private static final ConcurrentMap<String, Boolean> mojangUUIDAttempts = new ConcurrentHashMap<>();

    private final Collection<Consumer<IPlayerProfile>> listeners = new CopyOnWriteArrayList<>();

    private final String stableIdentity;

    private final AtomicBoolean mojangLookupStarted = new AtomicBoolean(false);

    private final WeakReference<GameProfile> profile;

    private volatile UUID mojangUUID;

    private PlayerProfile(GameProfile profile, String stableIdentity) {
        if (profile == null) {
            throw new IllegalArgumentException("profile must not be null");
        }

        this.profile = new WeakReference<>(profile);
        this.stableIdentity = stableIdentity;
    }

    public static PlayerProfile wrapGameProfile(GameProfile profile) {
        if (profile == null) {
            return DUMMY;
        }

        return profiles.getUnchecked(profile);
    }

    private static void scheduleMojangUuidLookup(PlayerProfile localProfile, GameProfile original) {
        if (localProfile == DUMMY || original == Shared.DUMMY) {
            return;
        }

        String name = GameProfileCompat.name(original);
        UUID currentUuid = GameProfileCompat.id(original);

        if (!setMojangUUID(currentUuid, name)) {
            return;
        }

        if (Shared.isBlank(name)) {
            return;
        }

        String attemptKey = name.toLowerCase(Locale.ROOT);

        UUID cachedUuid = mojangUUIDCache.get(attemptKey);

        if (cachedUuid != null) {
            localProfile.mojangUUID = cachedUuid;
            return;
        }

        if (mojangUUIDAttempts.putIfAbsent(attemptKey, Boolean.TRUE) != null) {
            return;
        }

        Futures.addCallback(
                MojangService.getProfile(name),
                new FutureCallback<>() {
                    @Override
                    public void onFailure(Throwable throwable) {
                        LOG.error("[OfflineSkins-Reloaded] Failed to load profile for {}", name, throwable);
                    }

                    @Override
                    public void onSuccess(GameProfile resolved) {
                        if (resolved == null || resolved == Shared.DUMMY) {
                            return;
                        }

                        UUID resolvedUuid = GameProfileCompat.id(resolved);
                        if (resolvedUuid == null) {
                            return;
                        }

                        mojangUUIDCache.put(attemptKey, resolvedUuid);
                        localProfile.mojangUUID = resolvedUuid;
                    }
                },
                Runnable::run
        );

    }

    private static boolean setMojangUUID(UUID uuid, String name) {
        if (Shared.isBlank(name)) {
            return false;
        }

        if (uuid == null) {
            return true;
        }

        return Shared.isOfflinePlayer(uuid, name);
    }

    private static String stableKey(GameProfile profile) {
        if (profile == null || profile == Shared.DUMMY) {
            return "dummy";
        }

        String name = GameProfileCompat.name(profile);
        if (!Shared.isBlank(name)) {
            return "name: " + name.toLowerCase(Locale.ROOT);
        }

        UUID uuid = GameProfileCompat.id(profile);
        if (uuid != null) {
            return "uuid: " + uuid;
        }

        return "anonymous";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof PlayerProfile other)) {
            return false;
        }

        return stableIdentity.equals(other.stableIdentity);
    }

    @Override
    public GameProfile getOriginal() {
        GameProfile current =
                profile.get();

        if (current == null) {
            return Shared.DUMMY;
        }

        return current;
    }

    @Override
    public UUID getPlayerUUID() {
        UUID resolved = mojangUUID;
        if (resolved != null) {
            return resolved;
        }
        GameProfile current = profile.get();
        return GameProfileCompat.id(Objects.requireNonNullElse(current, Shared.DUMMY));

    }

    @Override
    public String getPlayerName() {
        GameProfile current = profile.get();
        return GameProfileCompat.name(Objects.requireNonNullElse(current, Shared.DUMMY));
    }

    @Override
    public int hashCode() {
        return stableIdentity.hashCode();
    }

    @Override
    public boolean setUpdateListener(Consumer<IPlayerProfile> listener) {
        if (this == DUMMY) {
            return false;
        }
        if (listener == null || listeners.contains(listener)) {
            return false;
        }
        boolean added = listeners.add(listener);
        if (added && mojangLookupStarted.compareAndSet(
                false,
                true)) {
            scheduleMojangUuidLookup(
                    this,
                    getOriginal()
            );
        }
        return added;
    }
}