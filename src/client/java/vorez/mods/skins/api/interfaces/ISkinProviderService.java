package vorez.mods.skins.api.interfaces;

public interface ISkinProviderService extends ISkinProvider {

    /**
     * Clears all registered providers.
     */
    void clearProviders();

    /**
     * @param provider the provider to register.
     * @return true if successful.
     */
    boolean registerProvider(ISkinProvider provider);

    /**
     * Refreshes the cached skin for the specified profile.
     *
     * @param profile the profile to refresh.
     */

    ISkin getUnofficialSkin(IPlayerProfile profile);

    void clearFirst(IPlayerProfile profile);

    void recache(IPlayerProfile profile);
}
