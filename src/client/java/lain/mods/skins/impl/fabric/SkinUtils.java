package lain.mods.skins.impl.fabric;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import lain.mods.skins.init.fabric.FabricOfflineSkins;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.SkinTextures.Model;
import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

public class SkinUtils {

    private static final Function<GameProfile, Identifier> SKIN = profile -> FabricOfflineSkins.getLocationSkin(profile, null);
    private static final Function<GameProfile, Identifier> CAPE = profile -> FabricOfflineSkins.getLocationCape(profile, null);
    private static final Function<GameProfile, Model> MODEL = profile -> Model.fromName(FabricOfflineSkins.getSkinType(profile, null));

    private static final LoadingCache<GameProfile, Supplier<SkinTextures>> textureSuppliers = CacheBuilder
            .newBuilder()
            .expireAfterAccess(15, TimeUnit.SECONDS)
            .build(new CacheLoader<GameProfile, Supplier<SkinTextures>>() {
                @Override
                public Supplier<SkinTextures> load(GameProfile profile) {
                    AtomicReference<SkinTextures> holder = new AtomicReference<>();
                    return () -> {
                        SkinTextures textures = holder.get();
                        Identifier skinTexture = SKIN.apply(profile);
                        Identifier capeTexture = CAPE.apply(profile);
                        Model model = MODEL.apply(profile);

                        if (textures == null) {
                            if (skinTexture != null) {
                                SkinTextures created = new SkinTextures(
                                        skinTexture,
                                        null,
                                        capeTexture,
                                        null,
                                        model,
                                        false
                                );
                                if (!holder.compareAndSet(null, created)) {
                                    textures = holder.get();
                                } else {
                                    textures = created;
                                }
                            }
                        } else if (skinTexture != null) {
                            Identifier currentSkin = textures.texture();
                            Identifier currentCape = textures.capeTexture();

                            if (!skinTexture.equals(currentSkin) || !Objects.equals(capeTexture, currentCape) || textures.model() != model) {
                                SkinTextures created = new SkinTextures(
                                        skinTexture,
                                        null,
                                        capeTexture,
                                        null,
                                        model,
                                        false
                                );
                                if (!holder.compareAndSet(textures, created)) {
                                    textures = holder.get();
                                } else {
                                    textures = created;
                                }
                            }
                        }

                        return textures;
                    };
                }
            });

    public static SkinTextures textures(GameProfile profile) {
        return textureSuppliers.getUnchecked(profile).get();
    }
}
