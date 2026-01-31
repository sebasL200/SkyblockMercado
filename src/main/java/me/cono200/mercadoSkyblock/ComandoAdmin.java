package me.cono200.mercadoSkyblock;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ComandoAdmin implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar permiso (Solo Admins)
        if (!sender.hasPermission("mercado.admin")) {
            sender.sendMessage(ChatColor.RED + "Sin permiso.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("recuperar")) {
            // Llamamos a la función pública que creamos antes
            MercadoSkyblock.getGestorPrecios().forzarRecuperacion("Manual por Admin");
            sender.sendMessage(ChatColor.GREEN + "¡Has forzado la recuperación de precios del mercado!");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Uso: /madmin recuperar");
        return true;
    }
}