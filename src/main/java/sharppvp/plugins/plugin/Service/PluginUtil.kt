package sharppvp.plugins.plugin.Service

/*
 * #%L
 * PlugMan
 * %%
 * Copyright (C) 2010 - 2014 PlugMan
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import com.google.common.base.Joiner
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.PluginCommand
import org.bukkit.command.SimpleCommandMap
import org.bukkit.event.Event
import org.bukkit.plugin.*
import sharppvp.plugins.plugin.SSX
import java.io.File
import java.io.IOException
import java.net.URLClassLoader
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger


/**
 * Utilities for managing plugins.
 *
 * @author rylinaux
 */
object PluginUtil {
    /**
     * Enable a plugin.
     *
     * @param plugin the plugin to enable
     */
    fun enable(plugin: Plugin?) {
        if (plugin != null && !plugin.isEnabled) {
            Bukkit.getPluginManager().enablePlugin(plugin)
        }
    }


    /**
     * Disable a plugin.
     *
     * @param plugin the plugin to disable
     */
    fun disable(plugin: Plugin?) {
        if (plugin != null && plugin.isEnabled) {
            Bukkit.getPluginManager().disablePlugin(plugin)
        }
    }


    /**
     * Returns the formatted name of the plugin.
     *
     * @param plugin the plugin to format
     * @return the formatted name
     */
    fun getFormattedName(plugin: Plugin): String {
        return getFormattedName(plugin, false)
    }

    /**
     * Returns the formatted name of the plugin.
     *
     * @param plugin          the plugin to format
     * @param includeVersions whether to include the version
     * @return the formatted name
     */
    fun getFormattedName(plugin: Plugin, includeVersions: Boolean): String {
        val color = if (plugin.isEnabled) ChatColor.GREEN else ChatColor.RED
        var pluginName = color.toString() + plugin.name
        if (includeVersions) {
            pluginName += " (" + plugin.description.version + ")"
        }
        return pluginName
    }


    /**
     * Returns a plugin from a String.
     *
     * @param name the name of the plugin
     * @return the plugin
     */
    fun getPluginByName(name: String): Plugin? {
        for (plugin in Bukkit.getPluginManager().plugins) {
            if (name.equals(plugin.name, ignoreCase = true)) {
                return plugin
            }
        }
        return null
    }

    /**
     * Returns a List of plugin names.
     *
     * @return list of plugin names
     */
    fun getPluginNames(fullName: Boolean): List<String> {
        val plugins: MutableList<String> = ArrayList()
        for (plugin in Bukkit.getPluginManager().plugins) {
            plugins.add(if (fullName) plugin.description.fullName else plugin.name)
        }
        return plugins
    }

    /**
     * Get the version of another plugin.
     *
     * @param name the name of the other plugin.
     * @return the version.
     */
    fun getPluginVersion(name: String): String? {
        val plugin = getPluginByName(name)
        return if (plugin != null && plugin.description != null) {
            plugin.description.version
        } else null
    }

    /**
     * Returns the commands a plugin has registered.
     *
     * @param plugin the plugin to deal with
     * @return the commands registered
     */
    fun getUsages(plugin: Plugin): String {
        val parsedCommands: MutableList<String?> = ArrayList()
        val commands: Map<*, *>? = plugin.description.commands
        if (commands != null) {
            val commandsIt: Iterator<*> = commands.entries.iterator()
            while (commandsIt.hasNext()) {
                val thisEntry = commandsIt.next() as Map.Entry<*, *>?
                if (thisEntry != null) {
                    parsedCommands.add(thisEntry.key as String?)
                }
            }
        }
        return if (parsedCommands.isEmpty()) "No commands registered." else Joiner.on(", ").join(parsedCommands)
    }

    /**
     * Find which plugin has a given command registered.
     *
     * @param command the command.
     * @return the plugin.
     */
    fun findByCommand(command: String?): List<String> {
        val plugins: MutableList<String> = ArrayList()
        for (plugin in Bukkit.getPluginManager().plugins) {

            // Map of commands and their attributes.
            val commands = plugin.description.commands
            if (commands != null) {

                // Iterator for all the plugin's commands.
                val commandIterator: Iterator<Map.Entry<String, Map<String, Any>>> = commands.entries.iterator()
                while (commandIterator.hasNext()) {

                    // Current value.
                    val (key, value) = commandIterator.next()

                    // Plugin name matches - return.
                    if (key.equals(command, ignoreCase = true)) {
                        plugins.add(plugin.name)
                        continue
                    }

                    // No match - let's iterate over the attributes and see if
                    // it has aliases.
                    val attributeIterator = value.entries.iterator()
                    while (attributeIterator.hasNext()) {

                        // Current value.
                        val (key1, aliases) = attributeIterator.next()

                        // Has an alias attribute.
                        if (key1 == "aliases") {
                            if (aliases is String) {
                                if (aliases.equals(command, ignoreCase = true)) {
                                    plugins.add(plugin.name)
                                    continue
                                }
                            } else {

                                // Cast to a List of Strings.
                                val array = aliases as List<String>

                                // Check for matches here.
                                for (str in array) {
                                    if (str.equals(command, ignoreCase = true)) {
                                        plugins.add(plugin.name)
                                        continue
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // No matches.
        return plugins
    }


    /**
     * Loads and enables a plugin.
     *
     * @param plugin plugin to load
     * @return status message
     */
    private fun load(plugin: Plugin) {
        return load(plugin.name)
    }

    /**
     * Loads and enables a plugin.
     *
     * @param name plugin's name
     * @return status message
     */
    fun load(name: String) {
        var target: Plugin? = null
        val pluginDir = File("plugins")
        if (!pluginDir.isDirectory) return
        var pluginFile = File(pluginDir, "$name.jar")
        if (!pluginFile.isFile) {
            for (f in pluginDir.listFiles()!!) {
                if (f.name.endsWith(".jar")) {
                    try {
                        val desc: PluginDescriptionFile =
                            SSX.instance.pluginLoader.getPluginDescription(f)
                        if (desc.name.equals(name, ignoreCase = true)) {
                            pluginFile = f
                            break
                        }
                    } catch (e: InvalidDescriptionException) {
                    }
                }
            }
        }
        target = try {
            Bukkit.getPluginManager().loadPlugin(pluginFile)
        } catch (ignored: InvalidDescriptionException) {
            return
        } catch (ignored: InvalidPluginException) {
            return
        }
        target?.onLoad()
        Bukkit.getPluginManager().enablePlugin(target)
    }

    /**
     * Reload a plugin.
     *
     * @param plugin the plugin to reload
     */
    fun reload(plugin: Plugin?) {
        if (plugin != null) {
            unload(plugin)
            load(plugin)
        }
    }


    /**
     * Unload a plugin.
     *
     * @param plugin the plugin to unload
     * @return the message to send to the user.
     */
    fun unload(plugin: Plugin) {
        val name = plugin.name
        val pluginManager = Bukkit.getPluginManager()
        var commandMap: SimpleCommandMap? = null
        var plugins: MutableList<Plugin?>? = null
        var names: MutableMap<String?, Plugin?>? = null
        var commands: MutableMap<String?, Command>? = null
        var listeners: Map<Event?, SortedSet<RegisteredListener>>? = null
        var reloadlisteners = true
        if (pluginManager != null) {
            pluginManager.disablePlugin(plugin)
            try {
                val pluginsField = Bukkit.getPluginManager().javaClass.getDeclaredField("plugins")
                pluginsField.isAccessible = true
                plugins = pluginsField[pluginManager] as MutableList<Plugin?>
                val lookupNamesField = Bukkit.getPluginManager().javaClass.getDeclaredField("lookupNames")
                lookupNamesField.isAccessible = true
                names = lookupNamesField[pluginManager] as MutableMap<String?, Plugin?>
                try {
                    val listenersField = Bukkit.getPluginManager().javaClass.getDeclaredField("listeners")
                    listenersField.isAccessible = true
                    listeners = listenersField[pluginManager] as Map<Event?, SortedSet<RegisteredListener>>
                } catch (e: Exception) {
                    reloadlisteners = false
                }
                val commandMapField = Bukkit.getPluginManager().javaClass.getDeclaredField("commandMap")
                commandMapField.isAccessible = true
                commandMap = commandMapField[pluginManager] as SimpleCommandMap
                val knownCommandsField = SimpleCommandMap::class.java.getDeclaredField("knownCommands")
                knownCommandsField.isAccessible = true
                commands = knownCommandsField[commandMap] as MutableMap<String?, Command>
            } catch (e: NoSuchFieldException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }
        pluginManager!!.disablePlugin(plugin)
        if (plugins != null && plugins.contains(plugin)) plugins.remove(plugin)
        if (names != null && names.containsKey(name)) names.remove(name)
        if (listeners != null && reloadlisteners) {
            for (set in listeners.values) {
                val it = set.iterator()
                while (it.hasNext()) {
                    val value = it.next()
                    if (value.plugin === plugin) {
                        it.remove()
                    }
                }
            }
        }
        if (commandMap != null) {
            val it: MutableIterator<Map.Entry<String?, Command>> = commands!!.entries.iterator()
            while (it.hasNext()) {
                val (_, value) = it.next()
                if (value is PluginCommand) {
                    val c = value
                    if (c.plugin === plugin) {
                        c.unregister(commandMap)
                        it.remove()
                    }
                }
            }
        }

        // Attempt to close the classloader to unlock any handles on the plugin's jar file.
        val cl = plugin.javaClass.classLoader
        if (cl is URLClassLoader) {
            try {
                val pluginField = cl.javaClass.getDeclaredField("plugin")
                pluginField.isAccessible = true
                pluginField[cl] = null
                val pluginInitField = cl.javaClass.getDeclaredField("pluginInit")
                pluginInitField.isAccessible = true
                pluginInitField[cl] = null
            } catch (ex: NoSuchFieldException) {
                Logger.getLogger(PluginUtil::class.java.name).log(Level.SEVERE, null, ex)
            } catch (ex: SecurityException) {
                Logger.getLogger(PluginUtil::class.java.name).log(Level.SEVERE, null, ex)
            } catch (ex: IllegalArgumentException) {
                Logger.getLogger(PluginUtil::class.java.name).log(Level.SEVERE, null, ex)
            } catch (ex: IllegalAccessException) {
                Logger.getLogger(PluginUtil::class.java.name).log(Level.SEVERE, null, ex)
            }
            try {
                cl.close()
            } catch (ex: IOException) {
                Logger.getLogger(PluginUtil::class.java.name).log(Level.SEVERE, null, ex)
            }
        }

        // Will not work on processes started with the -XX:+DisableExplicitGC flag, but let's try it anyway.
        // This tries to get around the issue where Windows refuse to unlock jar files that were previously loaded into the JVM.
        System.gc()
    }
}