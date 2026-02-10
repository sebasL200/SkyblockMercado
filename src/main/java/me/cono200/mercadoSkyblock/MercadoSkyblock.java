package me.cono200.mercadoSkyblock;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Logger;

public class MercadoSkyblock extends JavaPlugin {

    private static Economy econ = null;
    private static final Logger log = Logger.getLogger("Minecraft");

    private static MercadoSkyblock instance;
    private static GestorPrecios gestorPrecios;

    // Categorías generadas automáticamente desde "precios:"
    private Map<MenuVentas.Categoria, Set<Material>> categorias;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        if (!setupEconomy()) {
            log.severe("[" + getDescription().getName() + "] - Vault no encontrado. Plugin desactivado.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        gestorPrecios = new GestorPrecios(this);

        // 🔥 CLAVE: construir categorías desde PRECIOS
        categorias = cargarCategoriasDesdePrecios();

        getCommand("vender").setExecutor(new ComandoVenta());
        getCommand("madmin").setExecutor(new ComandoAdmin());

        getServer().getPluginManager().registerEvents(new ListenerCarteles(), this);

        getLogger().info("MercadoSkyblock activado correctamente");
    }

    @Override
    public void onDisable() {
        getLogger().info("MercadoSkyblock desactivado.");
    }

    // =========================
    // VAULT
    // =========================
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    // =========================
    // CATEGORÍAS DESDE "precios:"
    // =========================
    private Map<MenuVentas.Categoria, Set<Material>> cargarCategoriasDesdePrecios() {

        Map<MenuVentas.Categoria, Set<Material>> mapa =
                new EnumMap<>(MenuVentas.Categoria.class);

        // Inicializar sets vacíos
        for (MenuVentas.Categoria cat : MenuVentas.Categoria.values()) {
            mapa.put(cat, new HashSet<>());
        }

        ConfigurationSection precios = getConfig().getConfigurationSection("precios");
        if (precios == null) {
            getLogger().severe("No se encontró la sección 'precios' en config.yml");
            return mapa;
        }

        for (String key : precios.getKeys(false)) {
            Material mat = Material.matchMaterial(key);
            if (mat == null) {
                getLogger().warning("Material inválido en precios: " + key);
                continue;
            }

            MenuVentas.Categoria categoria = categorizarMaterial(mat);
            if (categoria != MenuVentas.Categoria.NINGUNA) {
                mapa.get(categoria).add(mat);
            }
        }

        // Log de debug (muy útil)
        for (MenuVentas.Categoria cat : mapa.keySet()) {
            getLogger().info("Categoría " + cat + ": " + mapa.get(cat).size() + " ítems");
        }

        return mapa;
    }

    // =========================
    // CLASIFICACIÓN AUTOMÁTICA
    // =========================
    private MenuVentas.Categoria categorizarMaterial(Material mat) {

        String name = mat.name();

        if (name.contains("WHEAT") || name.contains("SEEDS") ||
                name.contains("CARROT") || name.contains("POTATO") ||
                name.contains("BEETROOT") || name.contains("MELON") ||
                name.contains("PUMPKIN") || name.contains("CANE") ||
                name.contains("BAMBOO") || name.contains("CACTUS") ||
                name.contains("BERRY") || name.contains("WART") ||
                name.contains("MUSHROOM") || name.contains("FUNGUS") ||
                name.contains("VINE") || name.contains("AZALEA") ||
                name.contains("HAY")) {
            return MenuVentas.Categoria.AGRICULTURA;
        }

        if (name.contains("BEEF") || name.contains("PORK") ||
                name.contains("CHICKEN") || name.contains("MUTTON") ||
                name.contains("RABBIT") || name.contains("COD") ||
                name.contains("SALMON") || name.contains("FISH") ||
                name.contains("APPLE") || name.contains("HONEY")) {
            return MenuVentas.Categoria.COMIDA;
        }

        if (name.contains("ROTTEN") || name.contains("BONE") ||
                name.contains("STRING") || name.contains("SPIDER") ||
                name.contains("GUNPOWDER") || name.contains("ENDER") ||
                name.contains("BLAZE") || name.contains("GHAST") ||
                name.contains("SLIME") || name.contains("PHANTOM") ||
                name.contains("SHULKER") || name.contains("WITHER") ||
                name.contains("FEATHER") || name.contains("LEATHER") ||
                name.contains("INK") || name.contains("ARROW") ||
                name.contains("TOTEM") || name.contains("SCUTE")) {
            return MenuVentas.Categoria.MOBS;
        }

        if (name.contains("LOG") || name.contains("PLANK") ||
                name.contains("LEAVES") || name.contains("SAPLING") ||
                name.contains("STEM") || name.contains("WOOD") ||
                name.contains("STICK")) {
            return MenuVentas.Categoria.MADERAS;
        }

        if (name.contains("INGOT") || name.contains("ORE") ||
                name.contains("BLOCK") || name.contains("NUGGET") ||
                name.contains("DIAMOND") || name.contains("EMERALD") ||
                name.contains("GOLD") || name.contains("IRON") ||
                name.contains("COPPER") || name.contains("REDSTONE") ||
                name.contains("LAPIS") || name.contains("QUARTZ") ||
                name.contains("NETHERITE") || name.contains("AMETHYST") ||
                name.contains("FLINT")) {
            return MenuVentas.Categoria.MINERALES;
        }

        if (name.contains("STONE") || name.contains("COBBLE") ||
                name.contains("GRANITE") || name.contains("DIORITE") ||
                name.contains("ANDESITE") || name.contains("DEEPSLATE") ||
                name.contains("SAND") || name.contains("GRAVEL") ||
                name.contains("BASALT") || name.contains("BLACKSTONE") ||
                name.contains("NETHERRACK") || name.contains("END_STONE") ||
                name.contains("PURPUR")) {
            return MenuVentas.Categoria.PIEDRAS;
        }

        if (name.contains("DIRT") || name.contains("GRASS") ||
                name.contains("MUD") || name.contains("ICE") ||
                name.contains("GLASS") || name.contains("BRICK") ||
                name.contains("PRISMARINE") || name.contains("SEA_LANTERN") ||
                name.contains("SPONGE") || name.contains("BEACON") ||
                name.contains("CONDUIT")) {
            return MenuVentas.Categoria.CONSTRUCCION;
        }

        return MenuVentas.Categoria.NINGUNA;
    }

    // =========================
    // GETTERS
    // =========================
    public static Economy getEconomy() {
        return econ;
    }

    public static MercadoSkyblock getInstance() {
        return instance;
    }

    public static GestorPrecios getGestorPrecios() {
        return gestorPrecios;
    }

    public Map<MenuVentas.Categoria, Set<Material>> getCategorias() {
        return categorias;
    }
}
