package de.c4vxl

import de.c4vxl.commands.RaceCommand
import de.c4vxl.listeners.ConnectionListeners
import org.bukkit.plugin.java.JavaPlugin

class Racing : JavaPlugin() {
    companion object {
        lateinit var plugin: JavaPlugin
    }

    override fun onEnable() {
        plugin = this

        logger.info("[+] Enabling ${pluginMeta.name} by c4vxl (Version: ${pluginMeta.version})!")

        RaceCommand(this)
        ConnectionListeners(this)
    }

    override fun onDisable() {
        logger.info("[-] Disabling ${pluginMeta.name} by c4vxl!")
    }
}