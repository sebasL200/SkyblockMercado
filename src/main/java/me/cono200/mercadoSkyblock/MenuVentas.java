package me.cono200.mercadoSkyblock;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.earth2me.essentials.Essentials;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Level;

public class MenuVentas {

    private final Essentials ess;
    private final Map<Categoria, Set<Material>> categoriasMap;

    public enum Categoria {
        AGRICULTURA, COMIDA, MOBS, MADERAS, MINERALES, PIEDRAS, CONSTRUCCION, VARIOS, NINGUNA
    }

    public MenuVentas() {
        this.ess = (Essentials) MercadoSkyblock.getInstance().getServer().getPluginManager().getPlugin("Essentials");
        this.categoriasMap = new HashMap<>();
        cargarCategorias();
    }

    private void cargarCategorias() {
        ConfigurationSection section = MercadoSkyblock.getInstance().getConfig().getConfigurationSection("categorias");

        if (section == null) {
            MercadoSkyblock.getInstance().getLogger().severe("ERROR CRÍTICO: No se encuentra la sección 'categorias' en config.yml");
            return;
        }

        for (String key : section.getKeys(false)) {
            try {
                Categoria cat = Categoria.valueOf(key.toUpperCase());
                Set<Material> materiales = new HashSet<>();
                List<String> listaConfig = section.getStringList(key);

                for (String matName : listaConfig) {
                    try {
                        materiales.add(Material.valueOf(matName.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        MercadoSkyblock.getInstance().getLogger().warning("Material inválido en config: " + matName);
                    }
                }
                categoriasMap.put(cat, materiales);

                // DIAGNÓSTICO EN CONSOLA
                MercadoSkyblock.getInstance().getLogger().info("Categoría " + key + " cargada con " + materiales.size() + " ítems.");

            } catch (IllegalArgumentException e) {
                MercadoSkyblock.getInstance().getLogger().warning("Categoría inválida en config: " + key);
            }
        }
    }

    // --- NIVEL 1: MENÚ PRINCIPAL ---
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

    private GuiItem crearBoton(Player player, Material icon, String nombre, Categoria cat) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(nombre);
        item.setItemMeta(meta);
        return new GuiItem(item, e -> abrirMenuCategoria(player, nombre, cat, false));
    }

    // --- NIVEL 2: MENÚ DE CATEGORÍA ---
    private void abrirMenuCategoria(Player player, String titulo, Categoria categoriaDeseada, boolean soloInventario) {
        ChestGui gui = new ChestGui(6, titulo);
        gui.setOnGlobalClick(e -> e.setCancelled(true));

        PaginatedPane paginas = new PaginatedPane(0, 0, 9, 5);

        // Obtener materiales
        Set<Material> setMateriales = categoriasMap.getOrDefault(categoriaDeseada, new HashSet<>());

        // DIAGNÓSTICO: Si no hay materiales, avisar al jugador
        if (setMateriales.isEmpty()) {
            player.sendMessage("§cERROR: No hay ítems cargados en la categoría " + categoriaDeseada.name());
            player.sendMessage("§cRevisa la consola para ver si el config.yml se leyó bien.");
            return;
        }

        List<Material> materiales = new ArrayList<>(setMateriales);
        materiales.sort(Comparator.comparing(Enum::name));

        OutlinePane paginaActual = new OutlinePane(0, 0, 9, 5);
        int itemsEnPagina = 0;
        int pageIndex = 0;

        for (Material mat : materiales) {
            if (soloInventario && !player.getInventory().contains(mat)) continue;

            // --- INTENTO DE OBTENER PRECIO ---
            double precioBase = 0.0;
            boolean tienePrecio = false;

            try {
                BigDecimal bd = ess.getWorth().getPrice(ess, new ItemStack(mat));
                if (bd != null) {
                    precioBase = bd.doubleValue();
                    tienePrecio = true;
                }
            } catch (Exception e) {
                // Error en Essentials, ignoramos y mostramos como sin precio
            }

            // --- MOSTRAR ÍTEM INCLUSO SI NO TIENE PRECIO ---
            // Así sabremos si el problema es de Essentials

            ItemStack displayItem = new ItemStack(mat);
            ItemMeta meta = displayItem.getItemMeta();
            List<String> lore = new ArrayList<>();

            double precioReal = 0.0;
            if (tienePrecio) {
                precioReal = MercadoSkyblock.getGestorPrecios().calcularPrecio(mat, precioBase);
                lore.add("§7Base: §8$" + precioBase);
                lore.add("§eActual: §6$" + String.format("%.2f", precioReal));
                lore.add("");
                lore.add("§aClic para vender");
            } else {
                lore.add("§c¡Sin Precio en Essentials!");
                lore.add("§7Usa /setworth en la mano");
            }

            meta.setLore(lore);
            displayItem.setItemMeta(meta);

            final double precioFinal = precioReal; // Para la lambda
            final boolean finalTienePrecio = tienePrecio;

            paginaActual.addItem(new GuiItem(displayItem, e -> {
                if (finalTienePrecio) {
                    abrirMenuDetalle(player, mat, precioFinal);
                } else {
                    player.sendMessage("§cEste ítem no tiene precio configurado en Essentials.");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
                }
            }));

            itemsEnPagina++;

            if (itemsEnPagina >= 45) {
                paginas.addPane(pageIndex, paginaActual);
                pageIndex++;
                paginaActual = new OutlinePane(0, 0, 9, 5);
                itemsEnPagina = 0;
            }
        }

        if (itemsEnPagina > 0 || pageIndex == 0) {
            paginas.addPane(pageIndex, paginaActual);
        }

        gui.addPane(paginas);

        // NAVEGACIÓN
        StaticPane nav = new StaticPane(0, 5, 9, 1);

        nav.addItem(new GuiItem(crearItemSimple(Material.ARROW, "§eAnterior"), e -> {
            if (paginas.getPage() > 0) {
                paginas.setPage(paginas.getPage() - 1);
                gui.update();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
        }), 2, 0);

        ItemStack itemFiltro = crearItemSimple(Material.HOPPER, soloInventario ? "§aSolo inventario: ON" : "§7Solo inventario: OFF");
        nav.addItem(new GuiItem(itemFiltro, e -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
            abrirMenuCategoria(player, titulo, categoriaDeseada, !soloInventario);
        }), 4, 0);

        nav.addItem(new GuiItem(crearItemSimple(Material.ARROW, "§eSiguiente"), e -> {
            if (paginas.getPage() < paginas.getPages() - 1) {
                paginas.setPage(paginas.getPage() + 1);
                gui.update();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
        }), 6, 0);

        nav.addItem(new GuiItem(crearItemSimple(Material.BARRIER, "§cVolver"), e -> abrirMenuPrincipal(player)), 8, 0);

        gui.addPane(nav);
        gui.show(player);
    }

    // --- NIVEL 3: DETALLE ---
    private void abrirMenuDetalle(Player player, Material material, double precioUnitario) {
        ChestGui gui = new ChestGui(3, "Vender: " + material.name());
        gui.setOnGlobalClick(e -> e.setCancelled(true));
        StaticPane pane = new StaticPane(0, 0, 9, 3);
        pane.addItem(new GuiItem(crearItemSimple(Material.GOLD_NUGGET, "§eVender 1"), e -> vender(player, material, 1, precioUnitario)), 2, 1);
        pane.addItem(new GuiItem(crearItemSimple(Material.GOLD_INGOT, "§6Vender 64"), e -> vender(player, material, 64, precioUnitario)), 4, 1);
        pane.addItem(new GuiItem(crearItemSimple(Material.GOLD_BLOCK, "§aVender Todo"), e -> vender(player, material, contarItems(player, material), precioUnitario)), 6, 1);
        pane.addItem(new GuiItem(crearItemSimple(Material.BARRIER, "§cCancelar"), e -> abrirMenuCategoria(player, "Volver", identificarCategoria(material), false)), 8, 1);
        gui.addPane(pane);
        gui.show(player);
    }

    private void vender(Player player, Material material, int cantidad, double precioUnitario) {
        if (cantidad <= 0) { player.sendMessage("§cNo tienes ese ítem."); return; }
        if (!player.getInventory().containsAtLeast(new ItemStack(material), cantidad)) { player.sendMessage("§cInsuficiente."); return; }

        player.getInventory().removeItem(new ItemStack(material, cantidad));
        double total = cantidad * precioUnitario;
        MercadoSkyblock.getEconomy().depositPlayer(player, total);
        MercadoSkyblock.getGestorPrecios().registrarVenta(material, cantidad);

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        player.sendMessage("§aVendiste x" + cantidad + " " + material.name() + " por §6$" + String.format("%.2f", total));
        player.closeInventory();
    }

    private Categoria identificarCategoria(Material mat) {
        for (Map.Entry<Categoria, Set<Material>> entry : categoriasMap.entrySet()) {
            if (entry.getValue().contains(mat)) return entry.getKey();
        }
        return Categoria.VARIOS;
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
            if (item != null && item.getType() == material) total += item.getAmount();
        }
        return total;
    }
}