package me.cono200.mercadoSkyblock;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
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

    private final String HEADER = "§9[Mercado]";

    // =================================================
    // CREAR CARTEL
    // =================================================
    @EventHandler
    public void alCrearCartel(SignChangeEvent event) {

        if (!event.getLine(0).equalsIgnoreCase("[Vender]")) return;

        Player player = event.getPlayer();

        if (!player.hasPermission("mercado.admin")) {
            player.sendMessage(ChatColor.RED + "No tienes permiso.");
            event.setCancelled(true);
            return;
        }

        Material material = Material.matchMaterial(
                event.getLine(1).toUpperCase().replace(" ", "_")
        );

        if (material == null) {
            event.setLine(0, "§cERROR");
            player.sendMessage(ChatColor.RED + "Material inválido.");
            return;
        }

        String maxStr = event.getLine(2);
        if (maxStr.isEmpty()) maxStr = "64";

        event.setLine(0, HEADER);                       // Azul
        event.setLine(1, "§f" + material.name());      // Blanco
        event.setLine(2, "§a$...");                    // Verde (precio pendiente)
        event.setLine(3, "§7Max §e" + maxStr);         // Gris + amarillo

        player.sendMessage(ChatColor.GREEN + "Cartel de mercado creado.");
    }

    // =================================================
    // USAR CARTEL
    // =================================================
    @EventHandler
    public void alUsarCartel(PlayerInteractEvent event) {

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign)) return;

        Sign cartel = (Sign) block.getState();
        if (!cartel.getLine(0).equals(HEADER)) return;

        event.setCancelled(true);

        Player player = event.getPlayer();

        Material material = Material.matchMaterial(
                ChatColor.stripColor(cartel.getLine(1))
        );
        if (material == null) return;

        int maxCantidad;
        try {
            maxCantidad = Integer.parseInt(
                    ChatColor.stripColor(cartel.getLine(3)).replace("Max ", "")
            );
        } catch (NumberFormatException e) {
            return;
        }

        int disponible = contarItems(player, material);
        if (disponible <= 0) {
            player.sendMessage(ChatColor.RED + "No tienes " + material.name());
            return;
        }

        int cantidadAVender = Math.min(disponible, maxCantidad);

        // 📉 Precio ANTES de vender
        double precioAntes = MercadoSkyblock.getGestorPrecios()
                .getPrecioActual(material);

        if (precioAntes <= 0) {
            player.sendMessage(ChatColor.RED + "Este ítem no se puede vender.");
            return;
        }

        double total = precioAntes * cantidadAVender;

        // Ejecutar venta
        quitarItems(player, material, cantidadAVender);
        MercadoSkyblock.getEconomy().depositPlayer(player, total);
        MercadoSkyblock.getGestorPrecios().registrarVenta(material, cantidadAVender);

        // 📉 Precio DESPUÉS de vender
        double precioDespues = MercadoSkyblock.getGestorPrecios()
                .getPrecioActual(material);

        // Flecha de tendencia
        String flecha;
        if (precioDespues < precioAntes) {
            flecha = "§c↓";
        } else if (precioDespues > precioAntes) {
            flecha = "§a↑";
        } else {
            flecha = "§7=";
        }

        // 🔄 Actualizar cartel
        cartel.setLine(2, "§a$" + String.format("%.2f", precioDespues) + flecha);
        cartel.update();

        // 🔊 Sonido
        player.playSound(player.getLocation(),
                Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);

        // ✨ Partículas
        player.getWorld().spawnParticle(
                Particle.HAPPY_VILLAGER,
                player.getLocation().add(0, 1, 0),
                12, 0.4, 0.6, 0.4, 0
        );

        // 💬 Mensaje
        player.sendMessage(ChatColor.GREEN +
                "Vendiste x" + cantidadAVender + " " + material.name() +
                " por §6$" + String.format("%.2f", total));
    }

    // =================================================
    // UTILIDADES
    // =================================================
    private int contarItems(Player player, Material material) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }
        return total;
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
