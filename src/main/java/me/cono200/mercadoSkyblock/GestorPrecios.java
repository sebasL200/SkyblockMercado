package me.cono200.mercadoSkyblock;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public class GestorPrecios {

    private final MercadoSkyblock plugin;

    // Contador global: ¿Cuántos ítems se han vendido en todo el server desde la última recuperación?
    private int itemsVendidosGlobal = 0;

    // EL FACTOR DINÁMICO: Cada 5000 ítems vendidos en total, el mercado se ajusta.
    // Esto hace que si hay mucha gente, el mercado se mueva rápido.
    private final int UMBRAL_PARA_RECUPERACION = 5000;

    public GestorPrecios(MercadoSkyblock plugin) {
        this.plugin = plugin;
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