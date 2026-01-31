package me.cono200.mercadoSkyblock;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ComandoVenta implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar que quien escribe es un jugador, no la consola
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando solo es para jugadores.");
            return true;
        }

        Player player = (Player) sender;

        // Aquí verificamos permisos o rangos en el futuro
        player.sendMessage(ChatColor.GREEN + "¡Hola " + player.getName() + "! El sistema de ventas funciona.");
        player.sendMessage(ChatColor.YELLOW + "Tu dinero actual: $" + MercadoSkyblock.getEconomy().getBalance(player));

        // MÁS ADELANTE: Aquí pondremos "new ShopMenu().abrir(player);"

      //  new MenuVentas().abrirMenuPrincipal(player);

        new MenuVentas(
                MercadoSkyblock.getInstance().getCategorias()
        ).abrirMenuPrincipal(player);

        return true;
    }
}