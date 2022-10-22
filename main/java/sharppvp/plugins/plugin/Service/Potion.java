package sharppvp.plugins.plugin.Service;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

public class Potion {
    public static void addPotionEffect(Player player, PotionEffect potionEffect, Boolean force ) {
        player.addPotionEffect(potionEffect,force);
    }
}
