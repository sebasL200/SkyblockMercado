package me.cono200.mercadoSkyblock;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import com.earth2me.essentials.Essentials;

import java.math.BigDecimal;

public class ListenerCarteles implements Listener {

    private final String HEADER = "§9[Mercado]"; // Título azul para identificar nuestros carteles

    // ---------------------------------------------------------
    // EVENTO 1: CUANDO UN ADMIN CREA EL CARTEL
    // ---------------------------------------------------------
    @EventHandler
    public void alCrearCartel(SignChangeEvent event) {
        // Detectamos si escribió "[Vender]" en la primera línea
        if (event.getLine(0).equalsIgnoreCase("[Vender]")) {
            Player player = event.getPlayer();

            // Seguridad: Solo admins pueden crear tiendas
            if (!player.hasPermission("mercado.admin")) {
                player.sendMessage(ChatColor.RED + "No tienes permiso para crear tiendas de mercado.");
                event.setCancelled(true);
                return;
            }

            // Leemos el material de la línea 2 (Ej: "Diamond")
            String nombreMaterial = event.getLine(1).toUpperCase().replace(" ", "_");
            Material material = Material.matchMaterial(nombreMaterial);

            if (material == null) {
                player.sendMessage(ChatColor.RED + "Material inválido: " + event.getLine(1));
                event.setLine(0, "§cERROR");
                return;
            }

            // Leemos la cantidad de la línea 3 (Si está vacía, ponemos 1)
            String cantidadStr = event.getLine(2);
            if (cantidadStr.isEmpty()) cantidadStr = "1";

            // Formateamos el cartel para que se vea profesional
            event.setLine(0, HEADER);           // Título Azul
            event.setLine(1, material.name());  // Nombre correcto del ítem
            event.setLine(2, cantidadStr);      // Cantidad
            event.setLine(3, "§8Clic para vender"); // Instrucción

            player.sendMessage(ChatColor.GREEN + "¡Tienda creada exitosamente!");
        }
    }

    // ---------------------------------------------------------
    // EVENTO 2: CUANDO UN JUGADOR USA EL CARTEL
    // ---------------------------------------------------------
    @EventHandler
    public void alUsarCartel(PlayerInteractEvent event) {
        // Solo nos interesa el Clic Derecho en un Bloque
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block bloque = event.getClickedBlock();
        if (bloque == null || !(bloque.getState() instanceof Sign)) return;

        Sign cartel = (Sign) bloque.getState();

        // Verificamos si es un cartel de NUESTRO plugin
        if (!cartel.getLine(0).equals(HEADER)) return;

        Player player = event.getPlayer();

        // --- 1. Leer datos del cartel ---
        Material material = Material.matchMaterial(cartel.getLine(1));
        int cantidad;
        try {
            cantidad = Integer.parseInt(cartel.getLine(2));
        } catch (NumberFormatException e) {
            return; // Si el cartel está mal escrito, ignoramos
        }

        // --- 2. Verificar inventario ---
        ItemStack itemRequerido = new ItemStack(material, 1);
        if (!player.getInventory().containsAtLeast(itemRequerido, cantidad)) {
            player.sendMessage(ChatColor.RED + "No tienes " + cantidad + " de " + material.name());
            return;
        }

        // --- 3. Obtener Precio Base (Essentials) ---
        Essentials ess = (Essentials) MercadoSkyblock.getInstance().getServer().getPluginManager().getPlugin("Essentials");
        if (ess == null) return;

        BigDecimal precioBaseBD = ess.getWorth().getPrice(ess, itemRequerido);
        if (precioBaseBD == null) {
            player.sendMessage(ChatColor.RED + "Este ítem no tiene precio base en Essentials.");
            return;
        }

        // --- 4. CALCULAR PRECIO DINÁMICO ---
        // Aquí ocurre la magia: Preguntamos a tu GestorPrecios cuánto vale REALMENTE según la oferta
        double precioUnitario = MercadoSkyblock.getGestorPrecios().calcularPrecio(material, precioBaseBD.doubleValue());
        double total = precioUnitario * cantidad;

        // --- 5. Ejecutar Transacción ---

        // A) Quitar ítems
        player.getInventory().removeItem(new ItemStack(material, cantidad));

        // B) Pagar dinero
        MercadoSkyblock.getEconomy().depositPlayer(player, total);

        // C) REGISTRAR VENTA (¡Importante!)
        // Esto aumenta el contador global y baja el precio para el siguiente
        MercadoSkyblock.getGestorPrecios().registrarVenta(material, cantidad);

        // Mensaje bonito
        player.sendMessage(ChatColor.GREEN + "Vendiste x" + cantidad + " " + material.name() +
                " por §6$" + String.format("%.2f", total));
    }
}