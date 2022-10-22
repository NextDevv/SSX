package sharppvp.plugins.plugin.events

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import sharppvp.plugins.plugin.commands.debug.isFrozen

object OnPlayerJoin : Listener {
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        event.player.isFrozen = false
    }
}