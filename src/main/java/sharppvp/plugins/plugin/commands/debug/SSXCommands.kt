@file:Suppress("UNCHECKED_CAST", "DuplicatedCode", "DEPRECATION", "NAME_SHADOWING", "KotlinConstantConditions")

package sharppvp.plugins.plugin.commands.debug

import net.md_5.bungee.api.chat.*
import org.bukkit.BanList
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scoreboard.*
import sharppvp.plugins.plugin.SSX
import sharppvp.plugins.plugin.SSX.Companion.instance
import sharppvp.plugins.plugin.Service.ConfigManager
import sharppvp.plugins.plugin.Service.DataBase.getSSLocation
import sharppvp.plugins.plugin.addPermission
import sharppvp.plugins.plugin.isPlayerInGroup
import sun.audio.AudioPlayer.player
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


private fun <E> java.util.ArrayList<E>.containsNot(value: E): Boolean {
    return !this.contains(value)
}

object SSXCommands : CommandExecutor,Listener {
    val plugin: SSX = instance

    private val waitingForResponse: ArrayList<Player> = ArrayList()

    private val waitingForResponseJoin: ArrayList<Player> = ArrayList()

    private val ss: HashMap<Player, ArrayList<Player>> = HashMap()

    private val targetOnSS: ArrayList<Player?> = arrayListOf(null)

    private val targetLastLocation: HashMap<Player, Location> = HashMap()

    private val playerScoreboard: HashMap<Player, Scoreboard> = HashMap()

    private val newParticipants: HashMap<Player, ArrayList<Player>> = HashMap()

    @EventHandler
    fun onCommandProcess(event: PlayerCommandPreprocessEvent) {
        val player = event.player
        for (target in targetOnSS) {
            if (target == player) {
                event.isCancelled = true
                player.msg("" prefix "&fNon puoi utilizzare comandi durante un ss!".translateToColorCodes())
            }
        }
    }

     @EventHandler
     fun onPlayerAsyncChat(e: AsyncPlayerChatEvent) {
        val player = e.player
        val message = e.message
        if (!player.isOnline) return

        if (waitingForResponse.contains(player)) {
            e.isCancelled = true
            val split = message.split(" ")

            if (split[0].toPlayer() != null) {
                val target: Player = split[0].toPlayer()!!
                var force = false

                if(split[1] == "--force") {
                    force = true
                }

                targetLastLocation[target] = target.location

                playerScoreboard[player] = player.scoreboard
                playerScoreboard[target] = target.scoreboard

                ssScoreboard(player, player, target)
                ssScoreboard(target, player, target)

                startSS(player, target, force)

                target.removePotionsEffects()

                waitingForResponse.remove(player)
                return
            }else {
                player.msg("" prefix "&fNome non valido!".translateToColorCodes())
                waitingForResponse.remove(player)
                return
            }
        }

        if (waitingForResponseJoin.contains(player)) {
            if (e.message.toPlayer() != null) {
                val target: Player = message.toPlayer()!!
                var hacker: Player? = null

                if(!player.hasPermission("ssx.join-ss") || !player.hasPermission("ssx.*")|| !player.hasPermission("ssx.admin-ss")) {
                    player.msg("" prefix "&fNon hai il permesso!".translateToColorCodes())
                    return
                }

                playerScoreboard[player] = player.scoreboard


                for ((key, value) in ss) {
                    if (key == target) {
                        for (j in value) {
                            for (h in targetOnSS) {
                                if (j == h) {
                                    hacker = h
                                    break
                                }
                            }
                        }
                    }
                }


                if (hacker != null) {
                    ssScoreboard(player, target, hacker)
                    joinSS(player, target)
                } else {
                    player.msg("" prefix "&fC'è stato un errore durante l'operazione, ti preghiamo di contattare lo sviluppatore.")
                }

                waitingForResponseJoin.remove(player)
                return
            } else {
                player.sendMessage("" prefix "§cNome dello staffer non valido!".translateToColorCodes())
                waitingForResponseJoin.remove(player)
                return
            }
        }

        for ((key, value) in ss) {
            if (value.contains(player)) {
                for (p in value) {
                    if (!p.isOnline) return
                    e.isCancelled = true
                    if (player == key) {
                        p.msg(plugin.getMessage("chat-format.staffer", sender = player, msg = e.message))
                    } else p.msg(plugin.getMessage("chat-format.normal", sender = player, msg = e.message))
                }

            }

        }

    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {

       val player = event.player

        for ((key, _) in ss) {
            if (key == player) {
                joinSS(player, key)
            }
        }

        if (player.isOnline && !player.isOp) {
            val banned = Bukkit.getBanList(BanList.Type.IP).isBanned(player.displayName)
            val bannedCheck2 = Bukkit.getBanList(BanList.Type.IP).isBanned(player.address.hostName)

            if (banned || bannedCheck2) {
                val message = plugin.getMessage("targetSS-leaves", player, "")
                Bukkit.broadcastMessage(message)

                player.kickPlayer(
                    Bukkit.getBanList(BanList.Type.IP).getBanEntry(player.displayName).reason.translateToColorCodes()
                )
                player.kickPlayer(
                    Bukkit.getBanList(BanList.Type.IP).getBanEntry(player.address.hostName)
                        .reason.translateToColorCodes()
                )
            }
        }
    }

    // If the player leaves the game the program will check if is the host of the ss or the target of the ss
    @EventHandler
    fun onPlayerLeave(event: PlayerQuitEvent) {
        val player = event.player

        // Checking if is the host of the ss

        for ((key, _) in ss) {
            if (key == player) {
                // Finishing the ss
                finishSS(player = player)
            }
        }


        // Checking if is the target of the ss
        for (target in targetOnSS) {
            if (target == player) {
                if(!plugin.config.getBoolean("target-leave-ban")) return
                val broadcast = plugin.getMessage("targetSS-leaves", player, "")

                // Banning the target for 2 weeks
                if(plugin.config.getBoolean("target-leave-ban")) {
                    val date = Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(
                        plugin.config.getInt("target-ban-time")
                            .toLong())
                    )
                    val banMessage = plugin.getMessage("targetSS-ban-message", player, "")
                    Bukkit.getBanList(BanList.Type.IP).addBan(
                        target?.address?.hostName,
                        banMessage,
                        date,
                        null
                    )
                    Bukkit.getBanList(BanList.Type.IP).addBan(
                        target!!.displayName,
                        banMessage,
                        date,
                        null
                    )
                }

                // Broadcasting the message
                Bukkit.broadcastMessage(broadcast)
            }
        }
    }

    private fun joinSS(player: Player, target: Player) {
        object : BukkitRunnable() {
            override fun run() {
                var canProceed = true
                for ((key, value) in ss) {
                    if (key == player) {
                        canProceed = false
                        player.msg(plugin.getMessage("join-already-in-SS", key, "/join ${target.displayName}"))
                    }

                    for (v in value) {
                        if (v == player) {
                            canProceed = false
                            player.msg(plugin.getMessage("join-already-in-SS", key, "/join ${target.displayName}"))
                        }
                    }
                }


                if (canProceed) {

                    for ((key, value) in ss) {
                        if (key == target) {
                            value.add(player)

                            // Then we update the original value
                            ss.replace(key, value)

                            plugin.database
                            val location = getSSLocation(1)

                            val newArray: ArrayList<Player> = newParticipants[target]!!
                            newArray.add(player)
                            newParticipants[target] = newArray

                            player.tp(location)
                            player.msg(plugin.getMessage("join-successfully", player, "/join ${target.displayName}"))
                            target.msg(
                                plugin.getMessage(
                                    "join-staffer-notification",
                                    player,
                                    "/join ${target.displayName}"
                                )
                            )
                            break
                        }
                    }
                }
            }
        }.runTask(plugin)
    }

    private fun quitSS(player: Player) {
        var continueQuitting = true

        for ((key, _) in ss) {
            if (key == player) {
                player.msg(plugin.getMessage("quit-already-in-SS", player, "/quit"))
                continueQuitting = false
                break
            }
        }


        if (continueQuitting) {

            for ((key, value) in ss) {
                for (k in value) {
                    if (k == player) {
                        value.remove(player)

                        val newArray: ArrayList<Player> = newParticipants[key]!!
                        newArray.remove(player)
                        newParticipants[key] = newArray


                        Bukkit.getOnlinePlayers().map {
                            it.showPlayer(player)
                            player.showPlayer(it)
                        }


                        key.msg(plugin.getMessage("quit-staffer-notification", player, "/quit"))

                        player.msg(plugin.getMessage("quit-success", player, "/quit"))
                        player.scoreboard = playerScoreboard[player]
                        player.isTimerCancelled = true
                        break
                    }
                }
            }
        }
    }

    /**
     * Creates a new timer on the scoreboard
     * @param scoreboard The scoreboard to create the timer
     * @param timerPosition The score of the timer
     * @param scoreName The name of the timer
     * @param player The target of the score change
     */
    private fun runTimer(scoreboard: Scoreboard, timerPosition: Int, scoreName: String, player: Player) {
        object : BukkitRunnable() {
            var time = 0
            var minutes = 0
            var seconds = 0
            var hours = 0
            var lastString = scoreName
            var lastSuffix = "{timer}"

            override fun run() {
                val objective = scoreboard.getObjective("ssx")
                if (scoreName == "NULL") return
                if (objective == null) return

                if(player.isOffline) {
                    this.cancel()
                    return
                }

                if(player.isTimerCancelled) {
                    player.isTimerCancelled = false
                    this.cancel()
                    return
                }

                time++
                seconds++
                if (seconds >= 60) {
                    seconds = 0
                    minutes++
                    if (minutes >= 60) {
                        hours++
                        minutes = 0
                    }
                }

                scoreboard.resetScores(lastString)
                val stringTimer =
                    hours.toString().padStart(2, '0')+":"+
                            minutes.toString().padStart(2, '0')+":"+
                                   seconds.toString().padStart(2, '0')
                lastString = lastString.replace(lastSuffix, stringTimer)
                lastSuffix = stringTimer

                val score = objective.getScore(
                    /* entry = */ scoreName.run { replace("{timer}",stringTimer) }
                )
                score.score = timerPosition

                player.scoreboard = scoreboard
            }
        }.runTaskTimer(plugin, 20, 20)
    }

    /**
     * This function displays the scoreboard of the SS
     * to a specified player
     *
     * @param player The player to display the scoreboard
     * @param owner The host of the SS
     * @param target The target of the SS
     *
     * @return Returns true if the scoreboard was successfully displayed
     *         Returns false if the player to display the scoreboard was not found or offline
     */
    private fun createScoreboard(player: Player, owner: Player, target: Player):Boolean {
        val config = plugin.config
        val cp = config.getString("color-prefix")

        if(player.isOffline) {
            return false
        }

        val manager: ScoreboardManager = Bukkit.getScoreboardManager()
        val board: Scoreboard = manager.newScoreboard
        val objective: Objective = board.registerNewObjective("ssx", "dummy")
        objective.displaySlot = DisplaySlot.SIDEBAR
        objective.displayName = config
            .getString("title")
            .replace("{CP}", cp)
            .translateToColorCodes()
        for(i in 10 downTo  1) {
            val currentString = config.getString("line-$i")
                .replace("{CP}", cp)
                .replace("{staffer}", owner.displayName)
                .replace("{suspect}", target.displayName)
                .translateToColorCodes()

            if(currentString.contains("{timer}")) {
                runTimer(board, i, currentString, player)
            }

            if(currentString != "NULL" && !currentString.contains("{timer}")) {
                val score:Score = objective.getScore(
                    currentString
                )
                score.score = i
            }

        }
        player.scoreboard = board
        return true
    }

    /**
     * This function displays the scoreboard of the SS
     * @param player the player to display the scoreboard
     * @param owner the host of the SS
     * @param target the target of the SS
     * */
    private fun ssScoreboard(player: Player, owner: Player, target: Player) {
        object : BukkitRunnable() {
            override fun run() {
                if (player.isOnline) {
                   createScoreboard(player, owner, target)
                }
            }
        }.runTask(instance)

    }

    private fun hidePlayers(owner: Player) {
        object : BukkitRunnable() {
            var bypasses: ArrayList<Player> = ArrayList()
            var `continue` = true

            override fun run() {
                for ((key, value) in newParticipants) {
                    if (key == owner) {
                        bypasses = value
                    }
                }

                if (!newParticipants.containsKey(owner)) {
                    this.`continue` = false

                    bypasses.forEach { player ->
                        Bukkit.getOnlinePlayers().map {
                            it.showPlayer(player)
                            player.showPlayer(it)
                        }
                    }

                }

                if (bypasses.isEmpty()) this.cancel()

                if (this.`continue`) {
                    for (online in Bukkit.getOnlinePlayers()) {
                        for (passer in bypasses) {
                            if (online.canSee(passer) && !bypasses.contains(online)) {
                                passer.hidePlayer(online)
                                online.hidePlayer(passer)
                            } else if (bypasses.contains(online)) {
                                for (i in bypasses) {
                                    if (!online.canSee(i) && !i.canSee(online)) {
                                        online.showPlayer(i)
                                        i.showPlayer(online)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    this.cancel()
                }
            }
        }.runTaskTimer(plugin, 0, 0)
    }

    private fun startSS(staffer: Player, target: Player, force: Boolean = false) {
        val config = plugin.config

        object  : BukkitRunnable() {
            override fun run() {
                var canProceed = true

                for ((key, _) in ss) {
                    if (key == staffer) {
                        canProceed = false
                        break
                    }
                }

                if(target.isOp && config.getBoolean("op-immunity") && !force) {
                    staffer.msg("" prefix "Non puoi fare un controllo hack su un OP!")
                    staffer.msg("" prefix "&fError code: &c${PlayerErrors.LACK_OF_PERMISSIONS}".translateToColorCodes())
                    return
                }

                target.addPotionEffect(
                    PotionEffect(
                        PotionEffectType.BLINDNESS,
                        5,
                        100,
                        false,
                        false
                    )
                )

                if (!canProceed) return
                plugin.database
                val location = getSSLocation(1)

                if (location != null) {
                    staffer.tp(location)
                    target.tp(location)

                    val participants: ArrayList<Player> = ArrayList()
                    participants.add(target)
                    targetOnSS.add(target)
                    participants.add(staffer)
                    ss[staffer] = participants

                    newParticipants[staffer] = arrayListOf(staffer, target)
                    hidePlayers(staffer)

                    target.sendTitle(
                        /* title = */ plugin.getMessage("title-header", target, "/ssx start ${target.displayName}"),
                        /* subtitle = */ plugin.getMessage("title-footer", staffer, "/ssx start ${target.displayName}")
                    )

                    val authorizedGroupsList: List<String> = config.getList("ss-groups-allowed") as List<String>
                    val authorizedGroups: Array<String> = authorizedGroupsList.toTypedArray()

                    Bukkit.getOnlinePlayers().map {
                        authorizedGroups.map { authorized ->
                            if(it.isPlayerInGroup(authorized)) {
                                it.msg(plugin.getMessage("start-notification", sender = staffer, target = target, command = "/ssx start ${target.displayName}"))
                            }
                        }
                    }

                } else {
                    staffer.sendMessage(plugin.getMessage("start-no-location", sender = staffer, command = "/ssx start ${target.displayName}"))
                    staffer.msg("" prefix "&fError code: &c${PlayerErrors.TELEPORT_FAILURE}".translateToColorCodes())
                }
            }
        }.runTask(plugin)
    }

    override fun onCommand(
        sender: CommandSender?,
        command: Command?,
        label: String?,
        args: Array<out String>?
    ): Boolean {
        if (sender?.isPlayer() == false) return true

        val player:Player = sender as Player
        val config = plugin.config
        val authorizedGroupsList: List<String> = config.getList("ss-groups-allowed") as List<String>
        val authorizedGroups: Array<String> = authorizedGroupsList.toTypedArray()

        val blockedUsersList: List<String> = config.getList("blocked-players") as List<String>
        blockedUsersList.map {
            if (it.equals(player.name, true)) {
                player.msg(plugin.getMessage("user-blocked", sender = player, command = "/ssx"))
                player.msg("" prefix "&fError code: &c${PlayerErrors.LACK_OF_PERMISSIONS} &for &c${PlayerErrors.USER_NOT_FOUND}".translateToColorCodes())
                return true
            }
        }

        if (config.getBoolean("luck-perms")) {


            var authorized = false
            authorizedGroups.map { auth ->
                if (player.isPlayerInGroup(auth)) {
                    authorized = true
                }
            }

            if (authorized) {
                command(player, args = args)
            }else {
                player.msg(plugin.getMessage("lack-of-permission", sender = player, command = "/ssx"))
            }
        } else {
            if (player.isOp) {
                command(player, args)
            }
        }

        return true
    }

    private fun command(player: Player, args: Array<out String>?) {
        val config = plugin.config

        if (args?.size == 0) {

            player.msg(plugin.getMessage("list-title", sender = player, command = "/ssx"))
            player.msg(plugin.getMessage("list-divisor", sender = player, command = "/ssx"))

            for(i in 1 .. 5) {
                player.spigot().sendTextComponent(
                    plugin.getMessage("command-$i.name", sender = player, command = "/ssx"),
                    plugin.getMessage("command-$i.description", sender = player, command = "/ssx"),
                    plugin.getMessage("command-$i.command", sender = player, command = "/ssx")
                )
            }

            player.msg(plugin.getMessage("list-footer", sender = player, command = "/ssx"))


            return
        }

        if (args?.size!! >= 1) {
            val subcommand = args[0]
            if (subcommand == "start") {
                if (args.size >= 2) {
                    val target = args[1].toPlayer()
                    var s = ""
                    if(args.size >= 3) {
                        s = args[2]
                    }

                    if (target != null) {
                        var force = false

                        if(s.toLowerCase() == "--force") {
                            force = true
                        }

                        if(target.isOp && config.getBoolean("op-immunity") && !force) {
                            player.msg("" prefix "Non puoi fare un controllo hack su un OP!")
                            player.msg("" prefix "&fError code: &c${PlayerErrors.LACK_OF_PERMISSIONS}".translateToColorCodes())
                            return
                        }

                        if (targetOnSS.contains(target)) {
                            player.msg("" prefix "&fQuesto player è già dentro un SS")
                            player.msg("" prefix "&fError code: &c${PlayerErrors.USER_OCCUPIED}".translateToColorCodes())
                            return
                        }

                        ss.map { (k, _) ->
                            if(target == k) {
                                player.msg("" prefix "&fQuesto player è già dentro un SS")
                                player.msg("" prefix "&fError code: &c${PlayerErrors.USER_OCCUPIED}".translateToColorCodes())
                                return
                            }
                        }


                        targetLastLocation[target] = target.location

                        playerScoreboard[player] = player.scoreboard
                        playerScoreboard[target] = target.scoreboard

                        ssScoreboard(player, player, target)
                        ssScoreboard(target, player, target)

                        target.addPotionEffect(
                            PotionEffect(
                                PotionEffectType.BLINDNESS,
                                5 * 20,
                                99,
                                false,
                                false
                            ),
                            true
                        )

                        target.removePotionsEffects()

                        startSS(player, target, force)
                    } else {
                        player.msg("" prefix "&cIl nome dato è invalido!".translateToColorCodes())
                        player.msg("" prefix "&fError code: &c${PlayerErrors.INVALID_NAME}".translateToColorCodes())
                    }
                } else {
                    player.msg("" prefix "&fImmetti il nome del" color  "Giocatore&f da controllare:".translateToColorCodes())
                    waitingForResponse.add(player)
                }
            } else if (subcommand == "setlocation") {

                val database = plugin.database

                player.run {
                    msg("" prefix "&fPosizione settata con successo".translateToColorCodes())
                    msg(message = "" prefix "&fPosizione settata a " color "${location.x}&f, " color  "${location.y}&f, " color  "${location.z}&f world: " color  "${location.world}".translateToColorCodes())
                }

                if (getSSLocation(1) == null) database?.createSSlocation(1, player.location)
                else
                    database?.updateSSLocation(
                        1,
                        player.location
                    )
            } else if (subcommand == "join") {
                if (args.size == 2) {
                    val target = args[1].toPlayer()
                    if (target != null) {
                        var hacker: Player? = null

                        playerScoreboard[player] = player.scoreboard

                        for ((key, value) in ss) {
                            if (key == target) {
                                for (j in value) {
                                    for (h in targetOnSS) {
                                        if (j == h) {
                                            hacker = h
                                            break
                                        }
                                    }
                                }
                            }
                        }


                        if (hacker != null) {
                            ssScoreboard(player, target, hacker)
                            joinSS(player, target)
                        } else {
                            player.msg("" prefix "&fIl nome dato potrebbe essere non valido oppure c'è stato un errore durante l'operazione.".translateToColorCodes())
                            player.msg("" prefix "&fError code: &c${PlayerErrors.PROCESS_FAILURE} &for &c${PlayerErrors.INVALID_NAME} &for &c${PlayerErrors.INVALID_NICKNAME}".translateToColorCodes())

                        }
                    } else {
                        player.msg("" prefix "&cIl nome dato è invalido!".translateToColorCodes())
                        player.msg("" prefix "&fError code: &c${PlayerErrors.INVALID_NICKNAME} &for &c${PlayerErrors.INVALID_NAME}".translateToColorCodes())
                    }
                } else {
                    player.msg("" prefix "&fImmetti il nome dello " color  "Staffer&f per entrare nel SS:".translateToColorCodes())
                    waitingForResponseJoin.add(player)
                }
            } else if (subcommand == "quit") {
                quitSS(player = player)
            } else if (subcommand == "list") {
                listSS(player)
            } else if (subcommand == "finish") {
                // If args size equals to 1 then we finish the target SS
                if(args.size == 2) {
                    if(!player.isOp) {
                        player.msg("" prefix "&fNon hai il permesso!")
                        player.msg("" prefix "&fError code: &c${PlayerErrors.LACK_OF_PERMISSIONS}".translateToColorCodes())
                        return
                    }

                    val target = args[1].toPlayer()

                    // Checking if target is not null
                    if (target != null) {
                        finishSS(player = target)
                        target.msg("" prefix "" color "${player.displayName }&f ti ha fatto finire il Controllo".translateToColorCodes())
                    }else player.msg("" prefix "&fIl nome dato è invalido!".translateToColorCodes())
                };finishSS(player = player)

            } else if (subcommand == "set") {
                val config = plugin.config

                val addPermission: (Player, String) -> Boolean = {player, permission ->
                    var successful = false
                    var permissionToSet = ""

                    when (permission) {
                        "start" -> permissionToSet = config.getString("start-ss")
                        "join" -> permissionToSet = config.getString("join-ss")
                        "admin" -> permissionToSet = config.getString("admin-ss")
                    }

                    if (!player.hasPermission("ss.${permissionToSet}") || player.isOp) {
                        player.addPermission(permissionToSet)
                        successful = true
                    }
                    successful
                }

                if (args.size >= 2) {
                    val s = args[1]
                    if(s == "permission") {
                        if(config.getBoolean("only-op") ) {
                            if(player.isOp) {
                                if(args.size >= 3) {
                                    val permission = args[2]
                                    val target = args[3].toPlayer()

                                    if(target == null) {
                                        player.msg("" prefix "&cIl nome dato è invalido!".translateToColorCodes())
                                        player.msg("" prefix "&fError code: &c${PlayerErrors.INVALID_NAME} &for &c${PlayerErrors.INVALID_NICKNAME}".translateToColorCodes())
                                    }

                                    if(addPermission(player, permission)) {
                                        player.msg("" prefix "&fPermesso aggiunto!")
                                    }else {
                                        player.msg("" prefix "&fC'è stato un errore durante l'operazione.".translateToColorCodes())
                                        player.msg("" prefix "&fError code: &c${PlayerErrors.PROCESS_FAILURE}".translateToColorCodes())
                                    }
                                }else {
                                    player.msg("" prefix "&fPermessi validi: start, join, admin".translateToColorCodes())
                                }
                            }else {
                                player.msg("" prefix "&fNon hai il permesso!")
                                player.msg("" prefix "&fError code: &c${PlayerErrors.LACK_OF_PERMISSIONS}".translateToColorCodes())
                                return
                            }
                        }else {
                            if(args.size >= 3) {
                                val permission = args[2]
                                val target = args[3].toPlayer()

                                if(target == null) {
                                    player.msg("" prefix "&cIl nome dato è invalido!".translateToColorCodes())
                                }

                                if(addPermission(player, permission)) {
                                    player.msg("" prefix "&fPermesso aggiunto!")
                                }else {
                                    player.msg("" prefix "&fC'è stato un errore durante l'operazione.".translateToColorCodes())
                                    player.msg("" prefix "&fError code: &c${PlayerErrors.PROCESS_FAILURE}".translateToColorCodes())
                                }
                            }else {
                                player.msg("" prefix "&fPermessi validi: start, join, admin".translateToColorCodes())
                            }
                        }
                    }else if (s == "location") {
                        if(player.isOp) {
                            val database = plugin.database

                            player.msg("" prefix "&fPosizione settata con successo".translateToColorCodes())
                            player.msg("" prefix "&fPosizione settata a " color "${player.location.x}&f, " color  "${player.location.y}&f, " color  "${player.location.z}&f world: " color  "${player.location.world}".translateToColorCodes())

                            if(getSSLocation(1) == null) {
                                database?.createSSlocation(1, player.location)
                            }else {
                                database?.updateSSLocation(1, player.location)
                            }
                        }else {
                            player.msg("" prefix "&fNon hai il permesso!")
                            player.msg("" prefix "&fError code: &c${PlayerErrors.LACK_OF_PERMISSIONS}".translateToColorCodes())
                        }
                    }
                }else {
                    player.msg("" prefix "&fComandi disponibili: permission".translateToColorCodes())
                }
            }else if(subcommand == "reload") {
                if(!player.isOp) {return}
                val reloadingStartMillis = System.currentTimeMillis()
                SSX.sendMessageToOps("" prefix "&fReloading...")
                finishAllSS()
                plugin.reloadConfig()
                SSX.reload()
                ConfigManager.reloadMessages()

                SSX.sendMessageToOps("" prefix "&fReloaded!".translateToColorCodes())
                SSX.sendMessageToOps("" prefix "&fReloaded in" color "${System.currentTimeMillis() - reloadingStartMillis}&f milliseconds!")
            }
        }
    }

    private fun finishAllSS() {
        for ((key, _) in ss) {
            finishSS(key)
        }
    }

    private fun finishSS(player: Player) {
        val config = plugin.config
        val authorizedGroupsList: List<String> = config.getList("ss-groups-allowed") as List<String>
        val authorizedGroups: Array<String> = authorizedGroupsList.toTypedArray()

        for ((key, value) in ss) {
            if (key == player) {
                for (p in value) {
                    if (p == key) {
                        key.scoreboard = playerScoreboard[key]
                    } else p.scoreboard = playerScoreboard[p]

                    if (targetOnSS.isNotEmpty()) {
                        for (h: Player? in targetOnSS) {
                            if (h != null) {
                                if (p == h) {
                                    targetOnSS.remove(h)
                                    h.msg("" prefix  "&fIl controllo hack è finito!".translateToColorCodes())
                                    h.isTimerCancelled = true
                                    break
                                }
                            }
                        }
                    }

                    for ((k, v) in targetLastLocation) {
                        if (p == k) {
                            k.tp(v)
                            targetLastLocation.remove(k)
                            break
                        }
                    }
                }
            }
        }

        val returnObject = ss.remove(player)
        if (returnObject == null) {
            player.msg("" prefix "&fNon sei in un SS".translateToColorCodes())
            player.msg("" prefix "&fError code: &c${PlayerErrors.NO_SS_FOUND}".translateToColorCodes())
        } else {
            newParticipants.remove(player)

            player.msg("" prefix "&fSS finita con successo!".translateToColorCodes())
            player.isTimerCancelled = true

            Bukkit.getOnlinePlayers().map { online ->
                authorizedGroups.map { authorizedGroup ->
                    if(online.isPlayerInGroup(authorizedGroup)) {
                        online.msg("" prefix "" color "${player.name} ha finito un SS")
                    }
                }
            }
        }

    }

    private fun listSS(player: Player) {
        player.msg("Lista degli SS attivi: ")
        for ((key, value) in ss) {
            player.msg("" color "SS di ${key.name}:".translateToColorCodes())
            for (j in value) {
                player.msg("- &f${j.name}".translateToColorCodes())
            }
        }

    }


}

fun Player.removePotionsEffects() {
    for (effect in player.activePotionEffects) player.removePotionEffect(effect.type)
}

fun Player.Spigot.sendTextComponent(text: String, hover: String, command: String) {
    val start = TextComponent(text.translateToColorCodes())
    start.hoverEvent =
        HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentBuilder(hover.translateToColorCodes()).create())
    start.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, command)
    msg(start)
}

fun CommandSender.isPlayer(): Boolean {
    return this is Player
}

fun Player.Spigot.msg(com: BaseComponent) {
    sendMessage(com)
}

fun Player.tp(location: Location) {
    player.teleport(location)
}

fun Player.freeze(freeze: Boolean = true) {
    player.isFrozen = freeze
    object : BukkitRunnable() {
        override fun run() {
            if (player.isFrozen) {
                player.teleport(player.location)
                player.isFrozen = true
            } else {
                player.isFrozen = false
                this.cancel()
            }
        }
    }.runTaskTimer(instance, 0, 0)
}

private val Player.isOffline: Boolean
    get() {
        return !isOnline
    }
val frozenPlayers = HashMap<Player, Boolean>()

enum class PlayerErrors(val code: Int = 0) {
    SUCCESS(100),
    INVALID_NAME(101),
    INVALID_NICKNAME(102),
    INVALID_UUID(103),
    INVALID_NICKNAME_OR_UUID(104),
    INVALID_NAME_OR_UUID(105),
    TELEPORT_FAILURE(106),
    LACK_OF_PERMISSIONS(107),
    NO_SS_FOUND(108),
    NO_DATABASE_FOUND(109),
    PROCESS_FAILURE(110),
    USER_OCCUPIED(111),
    USER_NOT_FOUND(112),
    USER_ALREADY_EXISTS(113)
}


var Player.isFrozen: Boolean
    get() {
        if (frozenPlayers.containsKey(player)) return true
        return false
    }
    set(value) {
        if (value) {
            if (!frozenPlayers.containsKey(player)) {
                frozenPlayers[player] = true
            }
        } else {
            if (frozenPlayers.containsKey(player)) {
                frozenPlayers.remove(player)
            }
        }


    }

val timersPlayers = HashMap<Player, Boolean>()
var Player.isTimerCancelled: Boolean
    get() {
        var bool = false
        if(timersPlayers.containsKey(player)) {
            bool = timersPlayers[player] == true
        }
        return bool
    }
    set(value) {
        timersPlayers[player] = value
    }

infix fun String.prefix(string: String) =
    instance.prefix.translateToColorCodes() + "" + string.translateToColorCodes()
infix fun String.color(string: String) =
    this +" "+ instance.color.translateToColorCodes() + "" + string.translateToColorCodes()



