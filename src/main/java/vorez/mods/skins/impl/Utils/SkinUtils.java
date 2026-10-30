package vorez.mods.skins.impl.Utils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import net.minecraft.resources.ResourceLocation;
import vorez.mods.skins.init.forge.ForgeOfflineSkinsReloaded;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

public class SkinUtils {

    private static final Function<GameProfile, ResourceLocation> SKIN = ForgeOfflineSkinsReloaded::getLocationSkin;
    private static final Function<GameProfile, ResourceLocation> CAPE = ForgeOfflineSkinsReloaded::getLocationCape;

    private static final Function<GameProfile, String> MODEL = profile -> {
        String type = ForgeOfflineSkinsReloaded.getSkinType(profile);
        if (type == null) {
            return "default";
        }
        return "slim".equalsIgnoreCase(type) ? "slim" : "default";
    };

    private record SkinData(
            ResourceLocation skin,
            ResourceLocation cape,
            String model
    ) {}

    private static final LoadingCache<GameProfile, Supplier<SkinData>>
            textureSuppliers = CacheBuilder
            .newBuilder()
            .expireAfterAccess(15, TimeUnit.SECONDS)
            .build(new CacheLoader<>() {
                @Override
                public Supplier<SkinData> load(GameProfile profile) {
                    AtomicReference<SkinData> holder = new AtomicReference<>();

                    return () -> {
                        SkinData textures = holder.get();

                        ResourceLocation skinTexture = SKIN.apply(profile);
                        ResourceLocation capeTexture = CAPE.apply(profile);
                        String model = MODEL.apply(profile);

                        if (textures == null) {
                            if (skinTexture != null) {
                                SkinData created = new SkinData(
                                        skinTexture,
                                        capeTexture,
                                        model
                                );

                                if (!holder.compareAndSet(null, created)) {
                                    textures = holder.get();
                                } else {
                                    textures = created;
                                }
                            }
                        } else if (skinTexture != null) {
                            ResourceLocation currentSkin = textures.skin();
                            ResourceLocation currentCape = textures.cape();
                            String currentModel = textures.model();

                            if (!skinTexture.equals(currentSkin) || !Objects.equals(capeTexture, currentCape) || !Objects.equals(model, currentModel)) {
                                SkinData created = new SkinData(
                                        skinTexture,
                                        capeTexture,
                                        model
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

    private static SkinData getSkinData(GameProfile profile) {
        return textureSuppliers.getUnchecked(profile).get();
    }

    public static ResourceLocation textures(GameProfile profile) {
        SkinData data = getSkinData(profile);
        return data == null ? null : data.skin();
    }

    public static ResourceLocation capeTexture(GameProfile profile) {
        SkinData data = getSkinData(profile);
        return data == null ? null : data.cape();
    }

    public static String model(GameProfile profile) {
        SkinData data = getSkinData(profile);
        return data == null ? null : data.model();
    }

    public static void clearPlayersTextureSuppliers() {
        textureSuppliers.invalidateAll();
        textureSuppliers.cleanUp();
    }

    public static void clearPlayerTextureSuppliers(String playerName) {
        for (GameProfile profile : textureSuppliers.asMap().keySet()) {
            if (playerName.equalsIgnoreCase(profile.getName())) {
                textureSuppliers.invalidate(profile);
            }
        }
    }
}