package de.blablubbabc.shopkeepercopy;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.ShopkeepersPlugin;
import com.nisovin.shopkeepers.shoptypes.AdminShopkeeper;
import com.nisovin.shopkeepers.shoptypes.offers.TradingOffer;

public class ShopkeeperCopyPlugin extends JavaPlugin {

	private static final String SHOPKEEPERS_PLUGIN_NAME = "Shopkeepers";

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length != 5) {
			sender.sendMessage(ChatColor.RED + "Invalid argument count!");
			return false; // print usage
		}

		// check for shopkeepers plugin:
		if (!Bukkit.getPluginManager().isPluginEnabled(SHOPKEEPERS_PLUGIN_NAME)) {
			sender.sendMessage(ChatColor.RED + "This command requires the plugin '" + SHOPKEEPERS_PLUGIN_NAME + "' to be running!");
			return true;
		}
		ShopkeepersPlugin shopkeepersPlugin = (ShopkeepersPlugin) Bukkit.getPluginManager().getPlugin(SHOPKEEPERS_PLUGIN_NAME);

		// get source shop:
		String sourceShopIdArg = args[0];
		Shopkeeper sourceShop = this.getShopkeeper(shopkeepersPlugin, sourceShopIdArg);
		if (sourceShop == null) {
			sender.sendMessage(ChatColor.RED + "Couldn't find source shop: " + sourceShopIdArg);
			return true;
		}

		if (!(sourceShop instanceof AdminShopkeeper)) {
			sender.sendMessage(ChatColor.RED + "Source shop is not an admin shopkeeper!");
			return true;
		}
		AdminShopkeeper sourceAdminShop = (AdminShopkeeper) sourceShop;

		// get spawn location:
		String worldNameArg = args[1];
		World world = Bukkit.getWorld(worldNameArg);
		if (world == null) {
			sender.sendMessage(ChatColor.RED + "Couldn't find world: " + worldNameArg);
			return true;
		}
		String xArg = args[2];
		String yArg = args[3];
		String zArg = args[4];
		Integer x = parseInt(args[2]);
		Integer y = parseInt(args[3]);
		Integer z = parseInt(args[4]);
		if (x == null || y == null || z == null) {
			sender.sendMessage(ChatColor.RED + "Invalid coordinate(s): " + xArg + " " + yArg + " " + zArg);
			return true;
		}
		Location spawnLocation = new Location(world, x, y, z);

		// note: might not work properly for sign shops, due to unknown facing, and facing being off at target location
		AdminShopkeeper newShop = (AdminShopkeeper) shopkeepersPlugin.createNewAdminShopkeeper(new ShopCreationData(null, sourceShop.getType(), sourceShop.getShopObject().getObjectType(), spawnLocation, null));
		if (newShop == null) {
			sender.sendMessage(ChatColor.RED + "Could not create admin shopkeeper! Check the server log for issues!");
			return true;
		}

		// copy offers:
		for (TradingOffer offer : sourceAdminShop.getOffers()) {
			newShop.addOffer(offer.getResultItem(), offer.getItem1(), offer.getItem2());
		}

		newShop.setName(sourceAdminShop.getName());
		newShop.setTradePermission(sourceAdminShop.getTradePremission());
		// TODO copy other shop data, especially shop object data

		// save:
		shopkeepersPlugin.save();

		sender.sendMessage(ChatColor.GREEN + "Shopkeeper copy created!");

		return true;
	}

	public static Integer parseInt(String string) {
		try {
			return Integer.parseInt(string);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private Shopkeeper getShopkeeper(ShopkeepersPlugin plugin, String shopIdArg) {
		if (shopIdArg == null) return null;

		// check if the argument is an uuid:
		UUID shopUniqueId = null;
		try {
			shopUniqueId = UUID.fromString(shopIdArg);
		} catch (IllegalArgumentException e) {
			// invalid uuid
		}

		if (shopUniqueId != null) {
			return plugin.getShopkeeper(shopUniqueId);
		}

		// check if the argument is an integer:
		int shopSessionId = -1;
		try {
			shopSessionId = Integer.parseInt(shopIdArg);
		} catch (NumberFormatException e) {
			// invalid integer
		}

		if (shopSessionId != -1) {
			return plugin.getShopkeeper(shopSessionId);
		}

		// try to get shopkeeper by name:
		return plugin.getShopkeeperByName(shopIdArg);
	}
}
