package vorez.mods.skins.api;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import vorez.lib.SharedPool;
import vorez.mods.skins.api.interfaces.IPlayerProfile;
import vorez.mods.skins.api.interfaces.ISkin;
import vorez.mods.skins.api.interfaces.ISkinProvider;
import vorez.mods.skins.api.interfaces.ISkinProviderService;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ManagedBlocker;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class SkinProviderAPI {

    public static final ISkin DUMMY = new ISkin() {

        @Override
        public ByteBuffer getData() {
            return null;
        }

        @Override
        public String getSkinType() {
            return null;
        }

        @Override
        public boolean isDataReady() {
            return false;
        }

        @Override
        public void onRemoval() {
        }

        @Override
        public boolean setRemovalListener(Consumer<ISkin> listener) {
            return false;
        }

        @Override
        public boolean setSkinFilter(Function<ByteBuffer, ByteBuffer> filter) {
            return false;
        }
    };

    public static final ISkinProviderService SKIN = create();
    public static final ISkinProviderService CAPE = create();

    public static ISkinProviderService create() {
        return new ISkinProviderService() {

            private final LoadingCache<SkinBundle, AtomicReference<Object>> reloading;
            private final LoadingCache<IPlayerProfile, SkinBundle> cache;
            private final List<ISkinProvider> providers;
            private final Consumer<IPlayerProfile> profileChangeListener;

            {
                reloading = CacheBuilder.newBuilder().weakKeys().build(new CacheLoader<>() {

                    @Override
                    public AtomicReference<Object> load(SkinBundle key) throws Exception {
                        return new AtomicReference<>();
                    }

                });

                cache = CacheBuilder.newBuilder().expireAfterAccess(15, TimeUnit.SECONDS).removalListener((RemovalListener<IPlayerProfile, SkinBundle>) notification -> {
                    SkinBundle skin = notification.getValue();
                    if (skin != null)
                        skin.onRemoval();

                }).build(new CacheLoader<>() {

                    @Override
                    public SkinBundle load(IPlayerProfile key) throws Exception {
                        key.setUpdateListener(profileChangeListener);

                        List<ISkin> skins = new ArrayList<>();
                        List<ISkin> unofficialSkins = new ArrayList<>();

                        for (ISkinProvider provider : providers) {
                            ISkin skin = provider.getSkin(key);

                            if (skin != null) {
                                skins.add(skin);

                                if (provider instanceof vorez.mods.skins.providers.CachedSkinProvider
                                        || provider instanceof vorez.mods.skins.providers.CustomServerSkinProvider
                                        || provider instanceof  vorez.mods.skins.providers.CrafatarSkinProvider
                                ) {
                                    unofficialSkins.add(skin);
                                }
                            }
                        }

                        return new SkinBundle().set(skins).setUnofficial(unofficialSkins);
                    }

                    @Override
                    public ListenableFuture<SkinBundle> reload(
                            IPlayerProfile key,
                            SkinBundle oldValue
                    ) throws Exception {
                        Collection<ISkin> skins = new ArrayList<>();
                        Collection<ISkin> unofficialSkins = new ArrayList<>();

                        for (ISkinProvider provider : providers) {
                            ISkin skin = provider.getSkin(key);

                            if (skin != null) {
                                skins.add(skin);

                                if (provider instanceof vorez.mods.skins.providers.CachedSkinProvider
                                        || provider instanceof vorez.mods.skins.providers.CustomServerSkinProvider
                                        || provider instanceof vorez.mods.skins.providers.CrafatarSkinProvider) {
                                    unofficialSkins.add(skin);
                                }
                            }
                        }

                        Object token;
                        reloading.getUnchecked(oldValue).set(token = new Object());

                        long deadline = System.currentTimeMillis() + 10000;

                        Supplier<Boolean> ready = () ->
                                System.currentTimeMillis() - deadline > 0L
                                        || reloading.getUnchecked(oldValue).get() != token
                                        || skins.stream().anyMatch(ISkin::isDataReady);

                        Runnable update = () -> {
                            if (reloading.getUnchecked(oldValue).compareAndSet(token, null)) {
                                oldValue.set(skins);
                                oldValue.setUnofficial(unofficialSkins);
                            }
                        };

                        if (skins.isEmpty()) {
                            update.run();
                        } else {
                            ManagedBlocker blocker = new ManagedBlocker() {

                                @Override
                                public boolean block() throws InterruptedException {
                                    Thread.sleep(1000);
                                    return ready.get();
                                }

                                @Override
                                public boolean isReleasable() {
                                    return ready.get();
                                }

                            };

                            SharedPool.execute(() -> {
                                try {
                                    ForkJoinPool.managedBlock(blocker);
                                } catch (InterruptedException e) {
                                } finally {
                                    update.run();
                                }
                            });
                        }

                        return Futures.immediateFuture(oldValue);
                    }

                });

                providers = new CopyOnWriteArrayList<>();

                profileChangeListener = profile -> {
                    if (cache.getIfPresent(profile) != null)
                        cache.refresh(profile);
                };
            }

            @Override
            public void clearProviders() {
                providers.clear();
                cache.invalidateAll();
            }

            @Override
            public ISkin getSkin(IPlayerProfile profile) {
                if (profile == null)
                    return DUMMY;

                return cache.getUnchecked(profile);
            }

            @Override
            public ISkin getUnofficialSkin(IPlayerProfile profile) {
                if (profile == null)
                    return DUMMY;

                return cache.getUnchecked(profile).getUnofficialSkin();
            }

            @Override
            public boolean registerProvider(ISkinProvider provider) {
                if (provider == null || provider == this)
                    return false;

                return providers.add(provider);
            }

            @Override
            public void clearFirst(IPlayerProfile profile) {
                if (profile == null) {
                    return;
                }
                SkinBundle bundle = cache.getIfPresent(profile);

                if (bundle == null) {
                    return;
                }
                AtomicReference<Object> state = reloading.getIfPresent(bundle);

                if (state != null) {
                    state.set(new Object());
                }
                bundle.clearFirst();
            }

            @Override
            public void recache(IPlayerProfile profile) {
                if (profile == null)
                    return;

                SkinBundle old = cache.getIfPresent(profile);

                if (old != null) {
                    AtomicReference<Object> state = reloading.getIfPresent(old);

                    if (state != null)
                        state.set(new Object());

                    cache.invalidate(profile);
                }

                cache.getUnchecked(profile);
            }

        };
    }
}