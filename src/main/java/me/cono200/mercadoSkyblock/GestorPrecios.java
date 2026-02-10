package me.cono200.mercadoSkyblock;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
public class GestorPrecios {

    private final MercadoSkyblock plugin;
    private final FileConfiguration config;
    private int itemsVendidosGlobal = 0;


    private final Map<Material, Integer> oferta = new HashMap<>();
    private final Map<Material, Double> precios = new HashMap<>();

    // EL FACTOR DINÁMICO: Cada 5000 ítems vendidos en total, el mercado se ajusta.
    // Esto hace que si hay mucha gente, el mercado se mueva rápido.
    private final int UMBRAL_PARA_RECUPERACION = 5000;

    public GestorPrecios(MercadoSkyblock plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();

    }

    private void inicializarMercado() {
        for (Material mat : Material.values()) {
            if (!mat.isItem()) continue;

            // precio inicial configurable desde config.yml
            double precioBase = plugin.getConfig().getDouble("precios." + mat.name(), 1.0);

            precios.put(mat, precioBase);
            oferta.put(mat, 0);
        }
    }

    public double procesarVenta(Material material, int cantidad) {

        String matName = material.name();

        double precioBase = getPrecioBase(material);
        int ventasActuales = getVentas(material);

        // Precio dinámico (simple y estable)
        double precioActual = precioBase / (1 + (ventasActuales * 0.01));
        if (precioActual < 0.1) precioActual = 0.1;

        double total = precioActual * cantidad;

        // Guardar nuevas ventas
        setVentas(material, ventasActuales + cantidad);

        // Guardar precio recalculado (opcional)
        config.set("precios." + matName, precioBase);
        plugin.saveConfig();

        return total;
    }


    public double getPrecioBase(Material material) {
        return config.getDouble("precios." + material.name(), 0.0);
    }

    /**
     * Ventas acumuladas (oferta)
     */
    public int getVentas(Material material) {
        return config.getInt("ventas." + material.name(), 0);
    }

    /**
     * Guardar ventas
     */
    private void setVentas(Material material, int cantidad) {
        config.set("ventas." + material.name(), cantidad);
    }


    private void recalcularPrecio(Material material) {
        int ofertaActual = oferta.getOrDefault(material, 0);

        // fórmula simple (puedes ajustar en el futuro)
        double precioBase = plugin.getConfig().getDouble("precios." + material.name(), 1.0);
        double nuevoPrecio = precioBase / (1 + (ofertaActual * 0.01));

        if (nuevoPrecio < 0.1) nuevoPrecio = 0.1;

        precios.put(material, nuevoPrecio);
    }

    public double getPrecioActual(Material material) {
        double base = getPrecioBase(material);
        int ventas = getVentas(material);

        double precio = base / (1 + (ventas * 0.01));
        return Math.max(precio, 0.1);
    }


    public double calcularPrecio(Material material, double precioBase) {
        int itemsVendidos = obtenerVentasTotales(material);
        double factorDecaimiento = 0.0005;
        double multiplicador = 1.0 / (1.0 + (itemsVendidos * factorDecaimiento));
        return Math.max(0.01, precioBase * multiplicador);
    }

    public void registrarVenta(Material material, int cantidad) {
        // 1. Guardar la venta individual (para bajar el precio de ESTE ítem)
        int actuales = obtenerVentasTotales(material);
        plugin.getConfig().set("ventas." + material.name(), actuales + cantidad);

        // 2. Aumentar el contador global (Presión del Mercado)
        itemsVendidosGlobal += cantidad;

        // 3. Chequear si llegamos al tope dinámico
        if (itemsVendidosGlobal >= UMBRAL_PARA_RECUPERACION) {
            forzarRecuperacion("Automática por Volumen");
            itemsVendidosGlobal = 0; // Reiniciar contador
        }

        plugin.saveConfig();
    }

    public int obtenerVentasTotales(Material material) {
        return plugin.getConfig().getInt("ventas." + material.name(), 0);
    }

    // --- FUNCIÓN DE RECUPERACIÓN (Sirve para Automático y Manual) ---
    public void forzarRecuperacion(String motivo) {
        ConfigurationSection seccion = plugin.getConfig().getConfigurationSection("ventas");
        if (seccion == null) return;

        boolean huboCambios = false;
        double factorRecuperacion = 0.90; // Recupera un 10% del stock (reduce ventas un 10%)

        for (String key : seccion.getKeys(false)) {
            int ventasActuales = plugin.getConfig().getInt("ventas." + key);

            if (ventasActuales > 0) {
                // Matemáticas: Reducimos el conteo de ventas, lo que sube el precio
                int ventasNuevas = (int) (ventasActuales * factorRecuperacion);

                // Limpieza de números pequeños
                if (ventasNuevas < 5) ventasNuevas = 0;

                if (ventasNuevas != ventasActuales) {
                    plugin.getConfig().set("ventas." + key, ventasNuevas);
                    huboCambios = true;
                }
            }
        }

        if (huboCambios) {
            plugin.saveConfig();
            plugin.getLogger().info("⟳ Mercado Recuperado (" + motivo + "). Precios actualizados.");
        }
    }
}