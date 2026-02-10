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

        // Solo clic derecho a un bloque
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block bloque = event.getClickedBlock();
        if (bloque == null) return;

        // Solo si es un cartel
        if (!(bloque.getState() instanceof Sign)) return;

        Sign cartel = (Sign) bloque.getState();

        // Solo si es NUESTRO cartel
        if (!cartel.getLine(0).equals(HEADER)) return;

        // 🔒 AHORA SÍ: cancelamos, porque sabemos que es nuestro cartel
        event.setCancelled(true);

        Player player = event.getPlayer();

        // -----------------------------
        // 1. Leer datos del cartel
        // -----------------------------
        Material material = Material.matchMaterial(cartel.getLine(1));
        if (material == null) {
            player.sendMessage(ChatColor.RED + "Material inválido en el cartel.");
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(cartel.getLine(2));
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Cantidad inválida en el cartel.");
            return;
        }

        // -----------------------------
        // 2. Verificar inventario del jugador
        // -----------------------------
        ItemStack requerido = new ItemStack(material, 1);
        if (!player.getInventory().containsAtLeast(requerido, cantidad)) {
            player.sendMessage(ChatColor.RED + "No tienes " + cantidad + " de " + material.name());
            return;
        }

        // -----------------------------
        // 3. Obtener precio dinámico
        // -----------------------------
        double precioUnitario = MercadoSkyblock
                .getGestorPrecios()
                .getPrecioActual(material);

        if (precioUnitario <= 0) {
            player.sendMessage(ChatColor.RED + "Este ítem no se puede vender.");
            return;
        }

        double total = precioUnitario * cantidad;

        // -----------------------------
        // 4. Ejecutar transacción
        // -----------------------------
        quitarItems(player, material, cantidad);
        MercadoSkyblock.getEconomy().depositPlayer(player, total);
        MercadoSkyblock.getGestorPrecios().registrarVenta(material, cantidad);

        // -----------------------------
        // 5. Mensaje final
        // -----------------------------
        player.sendMessage(ChatColor.GREEN +
                "Vendiste x" + cantidad + " " + material.name() +
                " por §6$" + String.format("%.2f", total));
    }


    private void quitarItems(Player player, Material material, int cantidad) {
        int restante = cantidad;

        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != material) continue;

            int stackAmount = item.getAmount();

            if (stackAmount <= restante) {
                contents[i] = null;
                restante -= stackAmount;
            } else {
                item.setAmount(stackAmount - restante);
                restante = 0;
            }

            if (restante <= 0) break;
        }

        player.getInventory().setContents(contents);
    }

}