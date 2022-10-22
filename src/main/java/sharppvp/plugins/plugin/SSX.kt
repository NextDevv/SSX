package sharppvp.plugins.plugin

import net.luckperms.api.LuckPerms
import net.luckperms.api.model.group.Group
import net.luckperms.api.model.user.User
import net.luckperms.api.node.Node
import org.bukkit.Bukkit
import org.bukkit.ChatColor.*
import org.bukkit.entity.Player
import org.bukkit.plugin.RegisteredServiceProvider
import org.bukkit.plugin.java.JavaPlugin
import sharppvp.plugins.plugin.Service.ConfigManager
import sharppvp.plugins.plugin.Service.DataBase
import sharppvp.plugins.plugin.Service.PluginUtil
import sharppvp.plugins.plugin.commands.debug.*
import sharppvp.plugins.plugin.events.OnPlayerJoin
import java.util.*


class SSX : JavaPlugin() {

    private var luckperms:LuckPerms? = null;
    var prefix: String = "";
    var color: String = "";
    var database: DataBase? = null;

    companion object {
        lateinit var instance:SSX

        @JvmStatic
        fun reload() {
            PluginUtil.reload(this.instance)
            instance.sendMessage("Plugin reloaded successfully")
        }

        fun sendMessageToOps(message: String) {
            Bukkit.getOnlinePlayers().map {
                it.run {
                    if(isOp) msg(message)
                }
            }
        }
    }

    init {
        instance = this
    }

    override fun onEnable() {
        // Plugin startup logic

        getCommand("hide").executor = HidePlayerDebug
        getCommand("ssx").executor = SSXCommands
        getCommand("freeze").executor = FreezeCommand

        Bukkit.getPluginManager().registerEvents(OnPlayerJoin, this)
        Bukkit.getPluginManager().registerEvents(SSXCommands, this)

        // Getting the configuration file
        saveDefaultConfig()
        ConfigManager.setup()
        ConfigManager.saveDefaultConfig()
        ConfigManager.saveDefaultLocation()

        prefix = config.getString("prefix").translateToColorCodes()
        color = config.getString("color-prefix").translateToColorCodes()

        // Debug plugin initialization
        if(config.getBoolean("debug")) {
            debug()
        }

        database = DataBase()
        DataBase.InitilizeDatabase()

        // Loading luckperms
        if(config.getBoolean("luck-perms")) {
            luckPerms()
        }
    }

    fun getMessage(path: String, sender: Player? = null, command: String = "", target: Player? = null, msg: String = ""): String {
        val config = ConfigManager.get()
        var message = config.getString(path) ?: return ""

        val keyWords:HashMap<String, Any> = hashMapOf(
            "%prefix%" to prefix, "%player%" to (sender?.displayName ?: ""),
            "%command%" to command, "%color-prefix%" to color, "%target%" to (target?.displayName ?: ""),
            "%message%" to msg
        )
        keyWords.map {(k,v)->
            message = message.replace(k, v.toString())
        }
        message = message.translateToColorCodes()

        return message
    }

    private fun sendMessage(message: String) {
        Bukkit.getServer().consoleSender?.sendMessage(message)
    }

    private fun debug() {
        val console = Bukkit.getServer().consoleSender

        // Custom confing test
        console.sendMessage("$prefix${GRAY}Debug mode activated")
        console.sendMessage("${GRAY}-------------------------------------------------")
        console.sendMessage("$prefix${GRAY}messages.yml equals to null: ${YELLOW}${ConfigManager.get() == null}")
        console.sendMessage("$prefix${GRAY}Message from messages.yml: ${YELLOW}${ConfigManager.get().getString("test-message")}")
        console.sendMessage("$prefix${GRAY}Message from messages.yml: ${YELLOW}${ConfigManager.get().getString("test-message-boolean")}")
        console.sendMessage("$prefix${GRAY}Message from messages.yml: ${YELLOW}${ConfigManager.get().getString("test-message-string")}")
        console.sendMessage("$prefix${GRAY}Message from messages.yml: ${YELLOW}${ConfigManager.get().getString("test-message-int")}")
        var messageDone:ArrayList<Boolean> = ArrayList<Boolean>()
        messageDone.add(ConfigManager.get().getString("test-message") != null)
        messageDone.add(ConfigManager.get().getString("test-message-boolean")!= null)
        messageDone.add(ConfigManager.get().getString("test-message-string")!= null)
        messageDone.add(ConfigManager.get().getString("test-message-int")!= null)

        console.sendMessage("${GRAY}-------------------------------------------------")
        console.sendMessage("$prefix${GRAY}Messages status: ")
        for(i in messageDone.indices) {
            console.sendMessage("$i.$GRAY "+messageDone[i])
        }
        console.sendMessage("${GRAY}-------------------------------------------------")
        console.sendMessage("$prefix${GRAY}Messages debug finished")
        console.sendMessage("$prefix${GRAY}Continues debug query...")
        console.sendMessage("${GRAY}-------------------------------------------------")
        console.sendMessage("$prefix${GRAY}LuckPerms debug starting...")
        if(config.getBoolean("luck-perms")) {

        }else {
            console.sendMessage("$prefix${RED}LuckPerms is not enabled")
            console.sendMessage("$prefix${RED}LuckPerms debug stopped")
            console.sendMessage("${GRAY}-------------------------------------------------")
        }
    }

    private fun luckPerms() {
        val provider: RegisteredServiceProvider<LuckPerms> =
            Bukkit.getServicesManager().getRegistration(LuckPerms::class.java)

        if (provider != null) {
            luckperms = provider.provider
        }
    }

    fun isPlayerInGroup(player: Player, group: String): Boolean {
        return player.hasPermission("group.$group")
    }

    fun addPermission(userUuid: UUID?, permission: String?) {
        // Load, modify, then save
        getLuckPerms().userManager.modifyUser(userUuid!!) { user ->
            // Add the permission
            user.data().add(Node.builder(permission!!).build())
        }
    }

    fun hasPermission(user: User, permission: String?): Boolean {
        return user.cachedData.permissionData.checkPermission(permission!!).asBoolean()
    }

    fun getLuckPerms():LuckPerms {
        return luckperms!!
    }

    override fun onDisable() {
        // Plugin shutdown logic
        ConfigManager.saveLocationData()
    }
}

fun Player.isPlayerInGroup(group: String): Boolean {
    return player.hasPermission("group.$group")
}

val Player.isAdmin:Boolean get() {
    val user: User = SSX.instance.getLuckPerms().userManager.loadUser(player.uniqueId) as User

    val inheritedGroups: Collection<Group> = user.getInheritedGroups(user.queryOptions)
    return inheritedGroups.stream().anyMatch { g: Group ->
        g.name == "admin"
    }
}

fun Player.addPermission(permission: String):Boolean {
    val user: User? = SSX.instance.getLuckPerms().userManager.getUser(player.uniqueId)
    user?.data()?.add(Node.builder("ssx.${permission}").build());
    if(user != null) {
        SSX.instance.getLuckPerms().userManager.saveUser(user)
        return true
    }
    return false
}