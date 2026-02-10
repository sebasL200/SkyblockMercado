package me.cono200.mercadoSkyblock;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class MenuVentas {

    public enum Categoria {
        AGRICULTURA, COMIDA, MOBS, MADERAS, MINERALES, PIEDRAS, CONSTRUCCION, NINGUNA
    }

    //PARA ORDENAR EN EL COMANDO
    private enum Orden {
        A_Z,
        Z_A
    }


    // =========================
    // FUENTE ÚNICA DE CATEGORÍAS
    // =========================
    private Map<Categoria, Set<Material>> getCategorias() {
        return MercadoSkyblock.getInstance().getCategorias();
    }

    // =========================
    // NIVEL 1 - MENÚ PRINCIPAL
    // =========================
    public void abrirMenuPrincipal(Player player) {

        ChestGui gui = new ChestGui(3, "§1Mercado: Categorías");
        gui.setOnGlobalClick(e -> e.setCancelled(true));

        StaticPane pane = new StaticPane(0, 0, 9, 3);

        pane.addItem(crearBoton(player, Material.WHEAT, "§aAgricultura", Categoria.AGRICULTURA), 1, 1);
        pane.addItem(crearBoton(player, Material.COOKED_BEEF, "§6Comida", Categoria.COMIDA), 2, 1);
        pane.addItem(crearBoton(player, Material.ROTTEN_FLESH, "§cMobs", Categoria.MOBS), 3, 1);
        pane.addItem(crearBoton(player, Material.OAK_LOG, "§6Maderas", Categoria.MADERAS), 4, 1);
        pane.addItem(crearBoton(player, Material.IRON_INGOT, "§fMinerales", Categoria.MINERALES), 5, 1);
        pane.addItem(crearBoton(player, Material.COBBLESTONE, "§7Piedras", Categoria.PIEDRAS), 6, 1);
        pane.addItem(crearBoton(player, Material.BRICKS, "§eConstrucción", Categoria.CONSTRUCCION), 7, 1);

        gui.addPane(pane);
        gui.show(player);
    }

    // =========================
    // BOTÓN DE CATEGORÍA (CON PERMISOS)
    // =========================
    private GuiItem crearBoton(Player player, Material icon, String nombre, Categoria cat) {

        String permiso = "mercado.categoria." + cat.name().toLowerCase();

        // 🔒 CATEGORÍA BLOQUEADA
        if (!player.hasPermission(permiso)) {

            ItemStack locked = new ItemStack(Material.BARRIER);
            ItemMeta meta = locked.getItemMeta();

            meta.setDisplayName("§c✖ Categoría bloqueada");
            meta.setLore(List.of(
                    "§7Esta categoría está disponible",
                    "§7exclusivamente para jugadores",
                    "§6★ con rango premium ★",
                    "",
                    "§eHaz clic para más información"
            ));

            locked.setItemMeta(meta);

            return new GuiItem(locked, e -> {
                player.playSound(player.getLocation(),
                        Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.5f);

                player.sendMessage("");
                player.sendMessage("§c⛔ §lACCESO BLOQUEADO");
                player.sendMessage("§7Esta categoría es exclusiva para");
                player.sendMessage("§6★ jugadores con rango ★");
                player.sendMessage("");
                player.sendMessage("§eCompra un rango y vende más rápido");
                player.sendMessage("§e¡directamente desde tu isla!");
                player.sendMessage("");

                player.sendTitle(
                        "§6★ RANGO PREMIUM ★",
                        "§7Desbloquea más categorías",
                        10, 40, 10
                );
            });
        }

        // ✅ CATEGORÍA DESBLOQUEADA
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(nombre);
        meta.setLore(List.of("§aClic para acceder"));

        item.setItemMeta(meta);

        //return new GuiItem(item, e -> abrirMenuCategoria(player, nombre, cat));
        return new GuiItem(item,
                e -> abrirMenuCategoria(
                        player,
                        nombre,
                        cat,
                        Orden.A_Z,
                        false
                ));

    }

    // =========================
    // NIVEL 2 - MENÚ CATEGORÍA
    // =========================
    private void abrirMenuCategoria(
            Player player,
            String titulo,
            Categoria categoria,
            Orden orden,
            boolean soloInventario
    ) {

        ChestGui gui = new ChestGui(6, titulo);
        gui.setOnGlobalClick(e -> e.setCancelled(true));

        PaginatedPane paginas = new PaginatedPane(0, 0, 9, 5);

        Set<Material> base = MercadoSkyblock.getInstance()
                .getCategorias()
                .getOrDefault(categoria, Collections.emptySet());

        List<Material> materiales = new ArrayList<>();

        for (Material mat : base) {
            if (soloInventario && !player.getInventory().contains(mat)) continue;
            if (MercadoSkyblock.getGestorPrecios().getPrecioActual(mat) <= 0) continue;
            materiales.add(mat);
        }

        if (materiales.isEmpty()) {
            player.sendMessage("§cNo hay ítems disponibles con estos filtros.");
            return;
        }

        // 🔤 ORDENAR
        materiales.sort(Comparator.comparing(Enum::name));
        if (orden == Orden.Z_A) Collections.reverse(materiales);

        OutlinePane pagina = new OutlinePane(0, 0, 9, 5);
        int index = 0;
        int page = 0;

        for (Material mat : materiales) {

            double precio = MercadoSkyblock.getGestorPrecios().getPrecioActual(mat);

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();

            meta.setDisplayName("§f" + mat.name());
            meta.setLore(List.of(
                    "§7Precio:",
                    "§6$" + String.format("%.2f", precio),
                    "",
                    "§aClic para vender"
            ));

            item.setItemMeta(meta);

            pagina.addItem(new GuiItem(item,
                    e -> abrirMenuDetalle(player, mat, precio)));

            index++;
            if (index >= 45) {
                paginas.addPane(page++, pagina);
                pagina = new OutlinePane(0, 0, 9, 5);
                index = 0;
            }
        }

        paginas.addPane(page, pagina);
        gui.addPane(paginas);

        // =========================
        // 🔽 BARRA DE FILTROS
        // =========================
        StaticPane nav = new StaticPane(0, 5, 9, 1);

        nav.addItem(new GuiItem(
                crearItemSimple(Material.ARROW, "§e⬅ Anterior"),
                e -> {
                    if (paginas.getPage() > 0) {
                        paginas.setPage(paginas.getPage() - 1);
                        gui.update();
                    }
                }), 0, 0);

        nav.addItem(new GuiItem(
                crearItemSimple(Material.PAPER, "§bOrden: A–Z"),
                e -> abrirMenuCategoria(player, titulo, categoria, Orden.A_Z, soloInventario)
        ), 2, 0);

        nav.addItem(new GuiItem(
                crearItemSimple(
                        Material.CHEST,
                        soloInventario ? "§a🎒 Inventario: ON" : "§7🎒 Inventario: OFF"
                ),
                e -> abrirMenuCategoria(player, titulo, categoria, orden, !soloInventario)
        ), 4, 0);

        nav.addItem(new GuiItem(
                crearItemSimple(Material.PAPER, "§bOrden: Z–A"),
                e -> abrirMenuCategoria(player, titulo, categoria, Orden.Z_A, soloInventario)
        ), 6, 0);

        nav.addItem(new GuiItem(
                crearItemSimple(Material.ARROW, "§eSiguiente ➡"),
                e -> {
                    if (paginas.getPage() < paginas.getPages() - 1) {
                        paginas.setPage(paginas.getPage() + 1);
                        gui.update();
                    }
                }), 8, 0);

        gui.addPane(nav);
        gui.show(player);
    }


    // =========================
    // NIVEL 3 - DETALLE
    // =========================
    private void abrirMenuDetalle(Player player, Material material, double precioUnitario) {

        ChestGui gui = new ChestGui(3, "Vender: " + material.name());
        gui.setOnGlobalClick(e -> e.setCancelled(true));

        StaticPane pane = new StaticPane(0, 0, 9, 3);

        pane.addItem(new GuiItem(crearItemSimple(Material.GOLD_NUGGET, "§eVender 1"),
                e -> vender(player, material, 1, precioUnitario)), 2, 1);

        pane.addItem(new GuiItem(crearItemSimple(Material.GOLD_INGOT, "§6Vender 64"),
                e -> vender(player, material, 64, precioUnitario)), 4, 1);

        pane.addItem(new GuiItem(crearItemSimple(Material.GOLD_BLOCK, "§aVender Todo"),
                e -> vender(player, material, contarItems(player, material), precioUnitario)), 6, 1);

        pane.addItem(new GuiItem(crearItemSimple(Material.BARRIER, "§cCancelar"),
                e -> abrirMenuPrincipal(player)), 8, 1);

        gui.addPane(pane);
        gui.show(player);
    }

    // =========================
    // UTILIDADES
    // =========================
    private void vender(Player player, Material material, int cantidad, double precioUnitario) {

        if (cantidad <= 0) return;
        if (!player.getInventory().containsAtLeast(new ItemStack(material), cantidad)) return;

        player.getInventory().removeItem(new ItemStack(material, cantidad));

        double total = cantidad * precioUnitario;

        MercadoSkyblock.getEconomy().depositPlayer(player, total);
        MercadoSkyblock.getGestorPrecios().registrarVenta(material, cantidad);

        player.sendMessage("§aVendiste x" + cantidad +
                " por §6$" + String.format("%.2f", total));

        player.playSound(player.getLocation(),
                Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

        player.closeInventory();
    }

    private ItemStack crearItemSimple(Material mat, String nombre) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(nombre);
        item.setItemMeta(meta);
        return item;
    }

    private int contarItems(Player player, Material material) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }
        return total;
    }
}
