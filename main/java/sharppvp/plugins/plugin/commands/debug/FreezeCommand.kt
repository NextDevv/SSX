package sharppvp.plugins.plugin.commands.debug

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object FreezeCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender?, command: Command?,label: String?, args: Array<out String>) :Boolean {
        if(sender !is Player) return true

        val player = sender as Player
        if(player.isOp) {
            if(args.size == 1) {
                val target = args[0].toPlayer()
                if(target!= null) {
                    if(target.isFrozen) {
                        target.freeze(freeze = false)
                        player.sendMessage("${target.name} is frozen = &c${target.isFrozen}".translateToColorCodes())
                    }else {
                        target.freeze(freeze = true)
                        player.sendMessage("${target.name} is frozen = &a${target.isFrozen}".translateToColorCodes())
                    }
                }
            }
        }

        return true
    }
}