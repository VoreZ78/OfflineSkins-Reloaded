package vorez.mods.skins.impl.Utils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import vorez.mods.skins.init.fabric.FabricOfflineSkinsReloaded;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

public class SkinUtils {

    private static final Function<GameProfile, Identifier> SKIN = FabricOfflineSkinsReloaded::getLocationSkin;
    private static final Function<GameProfile, Identifier> CAPE = FabricOfflineSkinsReloaded::getLocationCape;

    private static final Function<GameProfile, PlayerModelType> MODEL = profile -> {
        String type = FabricOfflineSkinsReloaded.getSkinType(profile);
        if (type == null) return PlayerModelType.WIDE;
        try {
            return PlayerModelType.valueOf(type.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return PlayerModelType.WIDE;
        }
    };

    private static ClientAsset.ResourceTexture texture(Identifier imageLocation) {
        return imageLocation == null ? null : new ClientAsset.ResourceTexture(imageLocation, imageLocation);
    };

    private static final LoadingCache<GameProfile, Supplier<PlayerSkin>> textureSuppliers = CacheBuilder
            .newBuilder()
            .expireAfterAccess(15, TimeUnit.SECONDS)
            .build(new CacheLoader<>() {
                @Override
                public Supplier<PlayerSkin> load(GameProfile profile) {
                    AtomicReference<PlayerSkin> holder = new AtomicReference<>();
                    return () -> {
                        PlayerSkin textures = holder.get();
                        Identifier skinTexture = SKIN.apply(profile);
                        Identifier capeTexture = CAPE.apply(profile);

                        PlayerModelType model = MODEL.apply(profile);

                        if (textures == null) {
                            if (skinTexture != null) {
                                PlayerSkin created = new PlayerSkin(
                                        texture(skinTexture),
                                        texture(capeTexture),
                                        null,
                                        model,
                                        true

                                );
                                if (!holder.compareAndSet(null, created)) {
                                    textures = holder.get();
                                } else {
                                    textures = created;
                                }
                            }
                        } else if (skinTexture != null) {
                            Identifier currentSkin = textures.body().id();
                            Identifier currentCape = textures.cape() != null ? textures.cape().id() : null;

                            if (!skinTexture.equals(currentSkin) || !Objects.equals(capeTexture, currentCape) || textures.model() != model) {
                                PlayerSkin created = new PlayerSkin(
                                        texture(skinTexture),
                                        texture(capeTexture),
                                        null,
                                        model,
                                        true
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

    public static PlayerSkin textures(GameProfile profile) {
        return textureSuppliers.getUnchecked(profile).get();
    }

    public static void clearPlayersTextureSuppliers() {
        textureSuppliers.invalidateAll();
        textureSuppliers.cleanUp();
    }

    public static void clearPlayerTextureSuppliers(String playerName) {
        for(GameProfile profile : textureSuppliers.asMap().keySet()) {
            if (playerName.equalsIgnoreCase(profile.name())) {
                textureSuppliers.invalidate(profile);
            }
        }
    }
}
