package me.cono200.mercadoSkyblock;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class MercadoSkyblock extends JavaPlugin {

    private static Economy econ = null;
    private static final Logger log = Logger.getLogger("Minecraft");

    // Instancia del plugin
    private static MercadoSkyblock instance;

    // Gestor de precios dinámicos
    private static GestorPrecios gestorPrecios;

    // Categorías cargadas desde config.yml
    private Map<MenuVentas.Categoria, Set<Material>> categorias;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Conectar con Vault
        if (!setupEconomy()) {
            log.severe("[" + getDescription().getName() + "] - Vault no encontrado. Plugin desactivado.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Inicializar gestor de precios
        gestorPrecios = new GestorPrecios(this);

        // Cargar categorías DESDE config.yml (CLAVE)
        categorias = cargarCategorias();

        // Registrar comandos
        getCommand("vender").setExecutor(new ComandoVenta());
        getCommand("madmin").setExecutor(new ComandoAdmin());

        // Registrar eventos
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
    // CARGA DE CATEGORÍAS
    // =========================
    private Map<MenuVentas.Categoria, Set<Material>> cargarCategorias() {
        Map<MenuVentas.Categoria, Set<Material>> mapa =
                new EnumMap<>(MenuVentas.Categoria.class);

        ConfigurationSection section = getConfig().getConfigurationSection("categorias");
        if (section == null) {
            getLogger().warning("No se encontró la sección 'categorias' en config.yml");
            return mapa;
        }

        for (String categoriaKey : section.getKeys(false)) {
            try {
                MenuVentas.Categoria categoria =
                        MenuVentas.Categoria.valueOf(categoriaKey);

                Set<Material> materiales = new HashSet<>();

                for (String matName : section.getStringList(categoriaKey)) {
                    try {
                        Material mat = Material.valueOf(matName);
                        materiales.add(mat);
                    } catch (IllegalArgumentException e) {
                        getLogger().warning("Material inválido en config.yml: " + matName);
                    }
                }

                mapa.put(categoria, materiales);
                getLogger().info("Categoría cargada: " + categoria +
                        " (" + materiales.size() + " items)");

            } catch (IllegalArgumentException e) {
                getLogger().warning("Categoría inválida en config.yml: " + categoriaKey);
            }
        }

        return mapa;
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