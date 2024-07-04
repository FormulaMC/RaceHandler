package de.c4vxl.commands

import de.c4vxl.racing.RaceHandler
import de.c4vxl.vehicle.VehicleEntity.Companion.isOnVehicleEntity
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class RaceCommand(plugin: JavaPlugin) {
    init {
        plugin.getCommand("race")?.let { plc ->
            // command logic
            plc.setExecutor { commandSender, command, s, args ->
                commandSender.takeIf { it is Player }?.let { playerSender: CommandSender ->
                    val laps: Int = args.getOrNull(0)?.toIntOrNull() ?: 1
                    val players: MutableList<Player> = args.mapNotNull { Bukkit.getPlayer(it) }.filter { it.isOnVehicleEntity }.toMutableList()
                    val animation = !args.getOrNull(1).contentEquals("animation:off")

                    RaceHandler(laps, players).start(animation) { game ->
                        game.winner.let { winner ->
                            game.initialPlayer.forEach {
                                it.sendMessage("======== Race Results ========")
                                it.sendMessage("Winner: ${winner.name} (${game.finishTime[winner]})")
                                it.sendMessage("Your Time: ${game.finishTime[it]}")
                                it.sendMessage("============================")
                            }
                        }
                    }
                }

                return@setExecutor false
            }

            // tab completer
            plc.setTabCompleter { commandSender, command, s, args ->
                val list: MutableList<String> = mutableListOf()

                if (args.size == 1) list.addAll(mutableListOf("1", "2", "3", "4", "5"))
                if (args.size == 2) list.addAll(mutableListOf("animation:on", "animation:off"))
                else list.addAll(Bukkit.getOnlinePlayers().filter { !args.contains(it.name) && it.isOnVehicleEntity }.map { it.name })

                return@setTabCompleter list
            }
        }
    }
}