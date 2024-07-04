package de.c4vxl.utils

import de.c4vxl.Racing.Companion.plugin
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable

object PlayerUtils {
    fun Player.moveTo(to: Location, duration: Long, runAfter: Runnable? = null) {
        val outputYaw = to.yaw
        val outputPitch = to.pitch
        val from: Location = this.location.clone()
        val totalSteps = duration.toInt()
        val xIncrement = (to.x - from.x) / totalSteps
        val yIncrement = (to.y - from.y) / totalSteps
        val zIncrement = (to.z - from.z) / totalSteps
        val pitchIncrement = (outputPitch - from.pitch) / totalSteps
        val yawIncrement = (outputYaw - from.yaw) / totalSteps

        object : BukkitRunnable() {
            var step = 0
            override fun run() {
                if (step >= totalSteps) {
                    this@moveTo.teleport(to)
                    this@moveTo.setRotation(outputYaw, outputPitch)
                    cancel()
                    runAfter?.run()
                    return
                }

                val newLocation = from.clone().add(
                    xIncrement * step,
                    yIncrement * step,
                    zIncrement * step
                )
                val newPitch = from.pitch + pitchIncrement * step
                val newYaw = from.yaw + yawIncrement * step
                this@moveTo.teleport(newLocation)
                this@moveTo.setRotation(newYaw, newPitch)

                step++
            }
        }.runTaskTimer(plugin, 0L, 0)
    }
}