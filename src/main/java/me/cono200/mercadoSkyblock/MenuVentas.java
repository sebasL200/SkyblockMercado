package me.cono200.mercadoSkyblock;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.earth2me.essentials.Essentials;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.logging.log4j.LogManager.getLogger;

public class MenuVentas {

    private final Essentials ess;
    private final Map<Categoria, Set<Material>> categorias;

    // =========================
    // ENUM DE CATEGORÍAS
    // =========================
    public enum Categoria {
        AGRICULTURA,
        COMIDA,
        MOBS,
        MADERAS,
        MINERALES,
        PIEDRAS,
        CONSTRUCCION,
        NINGUNA
    }

    // =========================
    // CONSTRUCTOR
    // =========================
    // =========================
// CONSTRUCTOR VACÍO (FIX)
// =========================
    public MenuVentas(Map<Categoria, Set<Material>> categorias) {
        this.ess = (Essentials) MercadoSkyblock.getInstance()
                .getServer()
                .getPluginManager()
                .getPlugin("Essentials");

        // Cargar categorías directamente desde config
        this.categorias = MercadoSkyblock.getInstance()
                .getConfig()
                .getConfigurationSection("categorias")
                .getKeys(false)
                .stream()
                .collect(
                        java.util.stream.Collectors.toMap(
                                k -> Categoria.valueOf(k),
                                k -> {
                                    java.util.Set<Material> set = new java.util.HashSet<>();
                                    for (String mat : MercadoSkyblock.getInstance()
                                            .getConfig()
                                            .getStringList("categorias." + k)) {
                                        try {
                                            set.add(Material.valueOf(mat));
                                        } catch (IllegalArgumentException ignored) {}
                                    }
                                    return set;
                                }
                        )
                );
        //getLogger().info("Categorias cargadas: " + this.categorias);
    }

    // =========================
    // NIVEL 1: MENÚ PRINCIPAL
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

    private GuiItem crearBoton(Player player, Material icon, String nombre, Categoria categoria) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(nombre);
        item.setItemMeta(meta);

        return new GuiItem(item, e -> abrirMenuCategoria(player, nombre, categoria));
    }

    // =========================
    // NIVEL 2: MENÚ DE CATEGORÍA
    // =========================
    private void abrirMenuCategoria(Player player, String titulo, Categoria categoriaDeseada) {

        ChestGui gui = new ChestGui(6, titulo);
        gui.setOnGlobalClick(e -> e.setCancelled(true));

        // ===== PAGINACIÓN =====
        PaginatedPane paginas = new PaginatedPane(0, 0, 9, 5);
        OutlinePane items = new OutlinePane(0, 0, 9, 5);

        Set<Material> materiales = categorias.getOrDefault(categoriaDeseada, Set.of());

        for (Material mat : materiales) {

            BigDecimal precio = ess.getWorth().getPrice(ess, new ItemStack(mat));
            if (precio == null) {
                precio = BigDecimal.valueOf(1.0); // fallback seguro
            }

            ItemStack displayItem = new ItemStack(mat);
            ItemMeta meta = displayItem.getItemMeta();

            double precioReal = MercadoSkyblock.getGestorPrecios()
                    .calcularPrecio(mat, precio.doubleValue());

            List<String> lore = new ArrayList<>();
            lore.add("§7Base: §8$" + precio);
            lore.add("§eActual: §6$" + String.format("%.2f", precioReal));
            lore.add("");
            lore.add("§aClic para vender");

            meta.setLore(lore);
            displayItem.setItemMeta(meta);

            items.addItem(new GuiItem(displayItem,
                    e -> abrirMenuDetalle(player, mat, precioReal)));
        }

        paginas.addPane(0, items);
        gui.addPane(paginas);

        // ===== BARRA DE NAVEGACIÓN =====
        StaticPane nav = new StaticPane(0, 5, 9, 1);

        // Página anterior
        nav.addItem(new GuiItem(crearItemSimple(Material.ARROW, "§e◀ Página anterior"),
                e -> {
                    int page = paginas.getPage();
                    if (page > 0) {
                        paginas.setPage(page - 1);
                        gui.update();
                    }
                }), 2, 0);

        // Volver
        nav.addItem(new GuiItem(crearItemSimple(Material.BARRIER, "§cVolver"),
                e -> abrirMenuPrincipal(player)), 4, 0);

        // Página siguiente
        nav.addItem(new GuiItem(crearItemSimple(Material.ARROW, "§ePágina siguiente ▶"),
                e -> {
                    paginas.setPage(paginas.getPage() + 1);
                    gui.update();
                }), 6, 0);

        gui.addPane(nav);
        gui.show(player);
    }

    // =========================
    // IDENTIFICAR CATEGORÍA
    // =========================
    private Categoria identificarCategoria(Material mat) {
        for (Map.Entry<Categoria, Set<Material>> entry : categorias.entrySet()) {
            if (entry.getValue().contains(mat)) {
                return entry.getKey();
            }
        }
        return Categoria.NINGUNA;
    }

    // =========================
    // AGREGAR ITEM AL MENÚ
    // =========================
    private void agregarItemAlMenu(OutlinePane pane, Player player, Material mat, double precioBase) {
        ItemStack displayItem = new ItemStack(mat);
        ItemMeta meta = displayItem.getItemMeta();

        double precioReal = MercadoSkyblock.getGestorPrecios()
                .calcularPrecio(mat, precioBase);

        List<String> lore = new ArrayList<>();
        lore.add("§7Base: §8$" + precioBase);
        lore.add("§eActual: §6$" + String.format("%.2f", precioReal));
        lore.add("");
        lore.add("§aClic para vender");

        meta.setLore(lore);
        displayItem.setItemMeta(meta);

        pane.addItem(new GuiItem(displayItem,
                e -> abrirMenuDetalle(player, mat, precioReal)));
    }

    // =========================
    // NIVEL 3: MENÚ DE VENTA
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
                e -> vender(player, material,
                        contarItems(player, material), precioUnitario)), 6, 1);

        gui.addPane(pane);
        gui.show(player);
    }

    // =========================
    // LÓGICA DE VENTA
    // =========================
    private void vender(Player player, Material material, int cantidad, double precioUnitario) {
        if (cantidad <= 0) {
            player.sendMessage("§cNo tienes ese ítem.");
            return;
        }

        ItemStack base = new ItemStack(material);
        if (!player.getInventory().containsAtLeast(base, cantidad)) {
            player.sendMessage("§cNo tienes suficientes ítems.");
            return;
        }

        player.getInventory().removeItem(new ItemStack(material, cantidad));

        double total = cantidad * precioUnitario;
        MercadoSkyblock.getEconomy().depositPlayer(player, total);
        MercadoSkyblock.getGestorPrecios().registrarVenta(material, cantidad);

        player.sendMessage("§aVendiste x" + cantidad + " "
                + material.name() + " por §6$"
                + String.format("%.2f", total));

        player.closeInventory();
    }

    // =========================
    // UTILIDADES
    // =========================
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