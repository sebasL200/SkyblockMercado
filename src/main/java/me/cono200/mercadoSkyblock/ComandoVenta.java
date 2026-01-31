package me.cono200.mercadoSkyblock;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ComandoVenta implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando solo es para jugadores.");
            return true;
        }

        Player player = (Player) sender;

        // CORRECCIÓN: El constructor ahora va vacío ()
        // Antes: new MenuVentas(MercadoSkyblock.getInstance().getCategorias())...
        // Ahora:
        new MenuVentas().abrirMenuPrincipal(player);

        return true;
    }
}