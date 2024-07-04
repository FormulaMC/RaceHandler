package de.c4vxl.racing

import de.c4vxl.Racing.Companion.plugin
import de.c4vxl.utils.PlayerUtils.moveTo
import de.c4vxl.vehicle.VehicleEntity.Companion.asVehicleEntity
import de.c4vxl.vehicle.VehicleEntity.Companion.isOnVehicleEntity
import de.c4vxl.vehicle.VehicleEntity.Companion.isVehicleEntity
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable

open class RaceHandler(val laps: Int = 1, val players: MutableList<Player> = mutableListOf(), val trackWidth: Double = 7.0) {
    private val gameTickRunnable: BukkitRunnable = object : BukkitRunnable() { override fun run() { gameTick() } }
    val timer: Timer = Timer()
    var isRunning: Boolean = false
        set(value) {
            if (value) gameTickRunnable.runTaskTimer(plugin, 0, 0)
            else {
                gameTickRunnable.cancel()
                runAfter?.invoke(this)
            }
            field = value
        }

    val initialPlayer: MutableList<Player> = mutableListOf<Player>().apply { this.addAll(players) }

    val finishTime: MutableMap<Player, String> = mutableMapOf()

    val lap: MutableMap<Player, Int> = mutableMapOf()
    val startLocation: MutableMap<Player, Location> = mutableMapOf()

    lateinit var winner: Player

    var runAfter: ((RaceHandler) -> Unit)? = null

    private val ignorePlayers: MutableList<Player> = mutableListOf()

    fun startAnimation(movingDuration: Long = 100L, stayUpDuration: Long = 100, runAfter: Runnable? = null) {
        players.forEach {
            it.gameMode = GameMode.SPECTATOR
            it.setRotation(it.yaw, 3.0f)

            val start: Location = it.location.clone()
            it.moveTo(start.clone().add(0.0, 60.0, 0.0), movingDuration) {
                object : BukkitRunnable() {
                    override fun run() {
                        it.moveTo(start, movingDuration) {
                            runAfter?.run()
                        }
                    }
                }.runTaskLater(plugin, stayUpDuration)
            }
        }
    }

    fun sendCountdown(player: Player, after: Runnable?) {
        object : BukkitRunnable() {
            var step = 0
            override fun run() {
                when (step) {
                    0 -> player.sendTitle("§a■§c■■", "Get ready!", 10, 70, 20)
                    1 -> player.sendTitle("§a■■§c■", "Get ready!", 10, 70, 20)
                    2 -> {
                        player.sendTitle("§a■■■", "GO!", 10, 70, 20)
                        after?.run()
                    }
                    else -> cancel()
                }
                step++
            }
        }.runTaskTimer(plugin, 10L, 50L)
    }

    fun gameTick() {
        // Remove players who are not on vehicle entities before iterating
        val iterator = players.iterator()
        while (iterator.hasNext()) {
            val player = iterator.next()
            if (!player.isOnVehicleEntity) {
                iterator.remove()
            }
        }

        // Check if players list is empty
        if (players.isEmpty()) {
            timer.isRunning = false // stop timer
            this.isRunning = false // stop game
            return
        }

        // Create a list to hold players to be removed after the iteration
        val playersToRemove = mutableListOf<Player>()

        // Iterate over the players list
        players.forEach { player: Player ->
            // send current timer to player's action bar
            player.sendActionBar(
                Component.text("Timer: ").decorate(TextDecoration.BOLD).color(NamedTextColor.GRAY)
                    .append(Component.text(timer.timeString).color(NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false))
            )

            if (ignorePlayers.contains(player)) return@forEach

            var lap: Int = lap[player] ?: 0 // get current lap
            val startLocation: Location = startLocation[player] ?: return@forEach // get start position

            // check if player is about to go to the next lap
            val newLap: Boolean = startLocation.getNearbyEntities(trackWidth, 1.0, trackWidth)
                .find { it.isVehicleEntity }?.asVehicleEntity?.hasDriver ?: false
            if (!newLap) return@forEach

            if (lap >= laps) { // finish
                playersToRemove.add(player)
                handlePlayerFinish(player)
            } else { // next lap
                // add 1 to laps
                lap += 1
                this.lap[player] = lap

                if (lap > 0) player.sendTitle("", "Lap: $lap") // send lap

                ignorePlayers.add(player)

                object : BukkitRunnable() {
                    override fun run() {
                        ignorePlayers.remove(player)
                    }
                }.runTaskLater(plugin, 20 * 5)
            }
        }

        // Remove players after iteration
        playersToRemove.forEach {
            handlePlayerEarlyLeave(it)
            players.remove(it)
        }
    }


    open fun handlePlayerFinish(player: Player) {
        if (!this::winner.isInitialized) winner = player

        val time = timer.timeString

        finishTime[player] = time

        player.sendTitle("§afinished all laps ($laps)", "Time: $time")

        players.forEach { it.sendMessage("§7§l${player.name} §r§afinished in $time") }
    }

    open fun handlePlayerEarlyLeave(player: Player) { }

    fun gameInit(after: Runnable?) {
        players.forEach { player: Player ->
            player.gameMode = GameMode.CREATIVE
            player.getNearbyEntities(0.3, 0.3, 0.3).find { it.isVehicleEntity }?.asVehicleEntity?.mountDriver(player)
            val cont = player.inventory.contents
            player.inventory.clear()

            startLocation[player] = player.location.clone()
            lap[player] = 0

            sendCountdown(player) {
                player.inventory.contents = cont

                after?.run()
            }
        }
    }

    fun start(doAnimation: Boolean = true, runAfter: ((RaceHandler) -> Unit)? = null) {
        this.runAfter = runAfter

        fun start() = gameInit { // send countdown to players
            timer.isRunning = true // start timer
            this.isRunning = true // start game
        }

        if (doAnimation) startAnimation { start() } // starting animation
        else start() // no starting animation
    }

    // timer logic
    class Timer {
        var time: Int = 0

        val timeString: String
            get() {
                val hours = time / 3600
                val minutes = (time % 3600) / 60
                val secs = time % 60

                return buildString {
                    if (hours > 0) append("$hours h ")
                    if (minutes > 0 || hours > 0) append("$minutes m ")
                    append("$secs s")
                }
            }

        private val runnable: BukkitRunnable = object : BukkitRunnable() { override fun run() { time++ } }

        var isRunning: Boolean = false
            set(value) {
                if (value) runnable.runTaskTimer(plugin, 0, 20)
                else runnable.cancel()
                field = value
            }
    }
}