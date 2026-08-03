package vorez.mods.skins.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.mojang.authlib.GameProfile;
import vorez.mods.skins.api.interfaces.IPlayerProfile;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class PlayerProfile implements IPlayerProfile {

    private static final PlayerProfile DUMMY = new PlayerProfile(Shared.DUMMY);

    private static final LoadingCache<GameProfile, PlayerProfile> profiles = CacheBuilder.newBuilder().weakKeys().refreshAfterWrite(10, TimeUnit.MINUTES).build(new CacheLoader<GameProfile, PlayerProfile>() {

        @Override
        public PlayerProfile load(GameProfile key) throws Exception {
            if (key == Shared.DUMMY || GameProfileCompat.properties(key) == null)
                return DUMMY;

            PlayerProfile profile = new PlayerProfile(key);
            if (Shared.isBlank(GameProfileCompat.name(key)))
            {
                if (GameProfileCompat.id(key) != null) {
                    Futures.addCallback(MojangService.fillProfile(key), new FutureCallback<GameProfile>() {

                        @Override
                        public void onFailure(Throwable t) {
                        }

                        @Override
                        public void onSuccess(GameProfile filled) {
                            if (filled == key)
                                return;
                            profile.set(filled);
                        }

                    }, Runnable::run);
                }
            } else if (Shared.isOfflinePlayer(GameProfileCompat.id(key), GameProfileCompat.name(key))) {
                Futures.addCallback(MojangService.getProfile(GameProfileCompat.name(key)), new FutureCallback<GameProfile>() {

                    @Override
                    public void onFailure(Throwable t) {
                    }

                    @Override
                    public void onSuccess(GameProfile resolved) {
                        if (resolved == Shared.DUMMY)
                            return;
                        profile.set(resolved);

                        Futures.addCallback(MojangService.fillProfile(resolved), new FutureCallback<GameProfile>() {

                            @Override
                            public void onFailure(Throwable t) {
                            }

                            @Override
                            public void onSuccess(GameProfile filled) {
                                if (filled == resolved)
                                    return;
                                profile.set(filled);
                            }

                        }, Runnable::run);
                    }

                }, Runnable::run);
            } else if (GameProfileCompat.properties(key).isEmpty()) {
                Futures.addCallback(MojangService.fillProfile(key), new FutureCallback<GameProfile>() {

                    @Override
                    public void onFailure(Throwable t) {
                    }

                    @Override
                    public void onSuccess(GameProfile filled) {
                        if (filled != key) {
                            profile.set(filled);
                            return;
                        }
                        Futures.addCallback(MojangService.getProfile(GameProfileCompat.name(key)), new FutureCallback<GameProfile>() {

                            @Override
                            public void onFailure(Throwable t) {
                            }

                            @Override
                            public void onSuccess(GameProfile resolved) {
                                if (resolved == Shared.DUMMY)
                                    return;
                                profile.set(resolved);

                                Futures.addCallback(MojangService.fillProfile(resolved), new FutureCallback<GameProfile>() {

                                    @Override
                                    public void onFailure(Throwable t) {
                                    }

                                    @Override
                                    public void onSuccess(GameProfile filled) {
                                        if (filled == resolved)
                                            return;
                                        profile.set(filled);
                                    }

                                }, Runnable::run);
                            }

                        }, Runnable::run);
                    }

                }, Runnable::run);
            }

            return profile;
        }

        @Override
        public ListenableFuture<PlayerProfile> reload(GameProfile key, PlayerProfile oldValue) throws Exception {
            if (oldValue == DUMMY)
                return Futures.immediateFuture(DUMMY);
            return Shared.submitTask(() -> {
                PlayerProfile newValue = load(key);
                if (oldValue.getOriginal() != newValue.getOriginal())
                    oldValue.set(newValue.getOriginal());
                return newValue;
            });
        }

    });
    private final Collection<Consumer<IPlayerProfile>> _listeners = new CopyOnWriteArrayList<>();
    private WeakReference<GameProfile> _profile;

    private PlayerProfile(GameProfile profile) {
        if (profile == null)
            throw new IllegalArgumentException("profile must not be null");
        _profile = new WeakReference<GameProfile>(profile);
    }

    public static PlayerProfile wrapGameProfile(GameProfile profile) {
        if (profile == null)
            return DUMMY;
        return profiles.getUnchecked(profile);
    }

    @Override
    public boolean equals(Object o) {
        GameProfile p;
        if ((p = _profile.get()) == null)
            return false;
        if (o instanceof PlayerProfile)
            return p.equals(((PlayerProfile) o)._profile.get());
        return false;
    }

    @Override
    public GameProfile getOriginal() {
        GameProfile p;
        if ((p = _profile.get()) == null)
            return Shared.DUMMY;
        return p;
    }

    @Override
    public UUID getPlayerID() {
        GameProfile p;
        if ((p = _profile.get()) == null)
            return GameProfileCompat.id(Shared.DUMMY);
        return GameProfileCompat.id(p);
    }

    @Override
    public String getPlayerName() {
        GameProfile p;
        if ((p = _profile.get()) == null)
            return GameProfileCompat.name(Shared.DUMMY);
        return GameProfileCompat.name(p);
    }

    @Override
    public int hashCode() {
        GameProfile p;
        if ((p = _profile.get()) == null)
            return 0;
        return p.hashCode();
    }

    private synchronized void set(GameProfile profile) {
        if (this == DUMMY)
            return;
        if (profile == null)
            throw new IllegalArgumentException("profile must not be null");
        _profile = new WeakReference<GameProfile>(profile);

        for (Consumer<IPlayerProfile> l : _listeners)
            l.accept(this);
    }

    @Override
    public boolean setUpdateListener(Consumer<IPlayerProfile> listener) {
        if (this == DUMMY)
            return false;
        if (listener == null || _listeners.contains(listener))
            return false;
        return _listeners.add(listener);
    }

}
