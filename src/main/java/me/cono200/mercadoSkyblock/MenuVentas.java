package me.cono200.mercadoSkyblock;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.earth2me.essentials.Essentials;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.util.*;

public class MenuVentas {

    private final Essentials ess;

    public enum Categoria {
        AGRICULTURA, COMIDA, MOBS, MADERAS, MINERALES, PIEDRAS, CONSTRUCCION, VARIOS, NINGUNA
    }

    public MenuVentas() {
        this.ess = (Essentials) MercadoSkyblock.getInstance()
                .getServer()
                .getPluginManager()
                .getPlugin("Essentials");
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
    // BOTÓN DE CATEGORÍA (PERMISOS)
    // =========================
    private GuiItem crearBoton(Player player, Material icon, String nombre, Categoria cat) {
        String permiso = "mercado.categoria." + cat.name().toLowerCase();

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
                // 🔊 Sonido elegante (no molesto)
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.5f);

                // 📝 Mensaje bonito en chat
                player.sendMessage("");
                player.sendMessage("§c⛔ §lACCESO BLOQUEADO");
                player.sendMessage("§7Esta categoría es exclusiva para");
                player.sendMessage("§6★ jugadores con rango ★");
                player.sendMessage("");
                player.sendMessage("§eCompra un rango y vende más rápido");
                player.sendMessage("§e¡directamente desde tu isla!");
                player.sendMessage("");

                // 🟡 Título animado (muy vistoso)
                player.sendTitle(
                        "§6★ RANGO PREMIUM ★",
                        "§7Desbloquea más categorías",
                        10, 40, 10
                );
            });
        }


        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(nombre);
        meta.setLore(List.of("§aClic para acceder"));
        item.setItemMeta(meta);

        return new GuiItem(item, e -> abrirMenuCategoria(player, nombre, cat, false));
    }

    // =========================
    // NIVEL 2 - MENÚ CATEGORÍA
    // =========================
    private void abrirMenuCategoria(Player player, String titulo, Categoria categoria, boolean soloInventario) {
        ChestGui gui = new ChestGui(6, titulo);
        gui.setOnGlobalClick(e -> e.setCancelled(true));

        PaginatedPane paginas = new PaginatedPane(0, 0, 9, 5);

        Set<Material> materialesCategoria =
                getCategorias().getOrDefault(categoria, Collections.emptySet());

        if (materialesCategoria.isEmpty()) {
            player.sendMessage("§cEsta categoría no tiene ítems configurados.");
            return;
        }

        List<Material> materiales = new ArrayList<>(materialesCategoria);
        materiales.sort(Comparator.comparing(Enum::name));

        OutlinePane pagina = new OutlinePane(0, 0, 9, 5);
        int index = 0;
        int page = 0;

        for (Material mat : materiales) {
            if (soloInventario && !player.getInventory().contains(mat)) continue;

            double precioBase = 0;
            boolean tienePrecio = false;

            try {
                BigDecimal bd = ess.getWorth().getPrice(ess, new ItemStack(mat));
                if (bd != null) {
                    precioBase = bd.doubleValue();
                    tienePrecio = true;
                }
            } catch (Exception ignored) {}

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            List<String> lore = new ArrayList<>();

            double precioFinal = 0;
            if (tienePrecio) {
                precioFinal = MercadoSkyblock.getGestorPrecios()
                        .calcularPrecio(mat, precioBase);
                lore.add("§7Base: §8$" + precioBase);
                lore.add("§eActual: §6$" + String.format("%.2f", precioFinal));
                lore.add("");
                lore.add("§aClic para vender");
            } else {
                lore.add("§cSin precio en Essentials");
                lore.add("§7Usa /setworth");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);

            final double precio = precioFinal;
            final boolean puedeVender = tienePrecio;

            pagina.addItem(new GuiItem(item, e -> {
                if (puedeVender) {
                    abrirMenuDetalle(player, mat, precio);
                } else {
                    player.sendMessage("§cEste ítem no tiene precio.");
                }
            }));

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
        // NAVEGACIÓN
        // =========================
        StaticPane nav = new StaticPane(0, 5, 9, 1);

        nav.addItem(new GuiItem(crearItemSimple(Material.ARROW, "§eAnterior"),
                e -> {
                    if (paginas.getPage() > 0) {
                        paginas.setPage(paginas.getPage() - 1);
                        gui.update();
                    }
                }), 2, 0);

        nav.addItem(new GuiItem(
                crearItemSimple(Material.HOPPER,
                        soloInventario ? "§aSolo inventario: ON" : "§7Solo inventario: OFF"),
                e -> abrirMenuCategoria(player, titulo, categoria, !soloInventario)
        ), 4, 0);

        nav.addItem(new GuiItem(crearItemSimple(Material.ARROW, "§eSiguiente"),
                e -> {
                    if (paginas.getPage() < paginas.getPages() - 1) {
                        paginas.setPage(paginas.getPage() + 1);
                        gui.update();
                    }
                }), 6, 0);

        nav.addItem(new GuiItem(crearItemSimple(Material.BARRIER, "§cVolver"),
                e -> abrirMenuPrincipal(player)), 8, 0);

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

        player.sendMessage("§aVendiste x" + cantidad + " por §6$" + String.format("%.2f", total));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
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
