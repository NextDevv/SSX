package sharppvp.plugins.plugin.commands.debug

import net.md_5.bungee.api.chat.BaseComponent
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.ChatColor.*
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sharppvp.plugins.plugin.SSX

object HidePlayerDebug : CommandExecutor {

    val plugin:SSX = SSX.instance

    override fun onCommand(
        sender: CommandSender?,
        command: Command?,
        label: String?,
        args: Array<out String>?
    ): Boolean {
        val config = plugin.config
        val debug:Boolean = config.getBoolean("debug")

        if(debug) {
            if(sender is Player) {
                val player = sender as Player
                if(args?.size == 1) {
                    val targetString = args[0]
                    val target = targetString.toPlayer()
                    if(target!= null) {
                        player.hidePlayer(target)
                    }else {
                        player.msg("Cannot find player with name '$targetString'")
                    }

                }
            }
        }else {
            plugin.server.consoleSender.sendMessage("${plugin.prefix}${RED}Debug mode is disabled!")
        }

        return true
    }

}

fun Player.msg(message: String) {
    player.sendMessage(message)
}

fun String.toPlayer(): Player? {
    return if(Bukkit.getPlayer(this) != null) Bukkit.getPlayer(this) else null
}

fun SSX.log(message: String) {
    this.server.consoleSender.sendMessage(this.prefix+message.translateToColorCodes('&'))
}

fun String.translateToColorCodes(code: Char = '&') : String {
    return ChatColor.translateAlternateColorCodes(code, this)
}