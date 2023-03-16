package me.rebelmythik.rebelscraftingrecipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public final class RebelsCraftingRecipes extends JavaPlugin implements Listener {

    private Set<NamespacedKey> knownRecipes = new HashSet<>();

    @Override
    public void onEnable() {
        // Load the config file
        saveDefaultConfig();
        FileConfiguration config = getConfig();

        // Register crafting recipes
        if (config.contains("crafting")) {
            ConfigurationSection craftingSection = config.getConfigurationSection("crafting");
            for (String key : craftingSection.getKeys(false)) {
                ConfigurationSection recipeSection = craftingSection.getConfigurationSection(key);
                int amount = 1;
                if (recipeSection.contains("result.amount")) {amount = recipeSection.getInt("result.amount");}
                ItemStack result = new ItemStack(Material.valueOf(recipeSection.getString("result.type")), amount);
                ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, key), result);
                String[] shapeThing = recipeSection.getString("shape").split("\\n");
                recipe.shape(shapeThing[0], shapeThing[1], shapeThing[2]);
                ConfigurationSection ingredientsSection = recipeSection.getConfigurationSection("ingredients");
                for (String ingredientKey : ingredientsSection.getKeys(false)) {
                    char keyChar = ingredientKey.charAt(0);
                    ItemStack ingredient = new ItemStack(Material.valueOf(ingredientsSection.getString(ingredientKey + ".type")));
                    getLogger().info(keyChar + ":" + ingredient.getType());
                    recipe.setIngredient(keyChar, ingredient.getType());
                }
                Bukkit.getServer().addRecipe(recipe);
                knownRecipes.add(new NamespacedKey(this, key));
            }
        }

        // Register smelting recipes
        if (config.contains("smelting")) {
            ConfigurationSection smeltingSection = config.getConfigurationSection("smelting");
            for (String key : smeltingSection.getKeys(false)) {
                ConfigurationSection recipeSection = smeltingSection.getConfigurationSection(key);
                ItemStack result = new ItemStack(Material.valueOf(recipeSection.getString("result.type")), recipeSection.getInt("result.amount"));
                ItemStack ingredient = new ItemStack(Material.valueOf(recipeSection.getString("ingredient.type")));
                float experience = (float) recipeSection.getDouble("experience");
                int cookingTime = recipeSection.getInt("cooking-time");
                Bukkit.getServer().addRecipe(new FurnaceRecipe(new NamespacedKey(this, key), result, ingredient.getType(), experience, cookingTime));
                knownRecipes.add(new NamespacedKey(this, key));
            }
        }

        // Discover recipes for online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            discoverRecipes(player);
        }

        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        discoverRecipes(player);
    }

    private void discoverRecipes(Player player) {
        for (NamespacedKey recipeKey : knownRecipes) {
            if (!player.hasDiscoveredRecipe(recipeKey)) {
                player.discoverRecipe(recipeKey);
                player.getInventory().addItem(Bukkit.getServer().getRecipe(recipeKey).getResult());
            }
        }
        this.getLogger().info(player.getName() + " has discovered some recipes!");
    }
}
