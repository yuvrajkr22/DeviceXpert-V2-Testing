package com.devicexpert.app.data.collector

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class BatteryCollector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getBatteryMetrics(): BatteryResult {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val tempTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val voltageMv = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val chargePlug = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val healthCode = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
            ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
        val technology = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)?.ifBlank { "Unknown" } ?: "Unknown"

        val percentage = if (level >= 0 && scale > 0) (level.toFloat() / scale.toFloat()).coerceIn(0f, 1f) else 0f
        val tempCelsius = if (tempTenths > 0) tempTenths / 10.0f else 0f
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val source = when (chargePlug) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "WIRELESS"
            else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                chargePlug == BatteryManager.BATTERY_PLUGGED_DOCK
            ) "DOCK" else "BATTERY"
        }

        val health = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
            else -> "Unknown"
        }

        val currentMa = readNormalizedCurrentMa(batteryManager, isCharging)
        val powerWatts = if (voltageMv > 0 && currentMa != null) {
            (voltageMv / 1000f) * (abs(currentMa) / 1000f)
        } else null

        val rawChargeCounterUah = batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val chargeCounterMah = if (rawChargeCounterUah != null && rawChargeCounterUah > 0L && rawChargeCounterUah != Long.MIN_VALUE) {
            rawChargeCounterUah / 1000f
        } else null

        val estimatedFullMah = if (chargeCounterMah != null && percentage >= 0.05f) {
            chargeCounterMah / percentage
        } else null

        val chargeTimeRemainingMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && isCharging) {
            batteryManager?.computeChargeTimeRemaining()?.takeIf { it > 0L }
        } else null

        val cycleCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            intent?.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1)?.takeIf { it >= 0 }
        } else null

        return BatteryResult(
            percentage = percentage,
            tempC = tempCelsius,
            isCharging = isCharging,
            source = source,
            voltageMv = voltageMv,
            currentMa = currentMa,
            powerWatts = powerWatts,
            health = health,
            technology = technology,
            chargeCounterMah = chargeCounterMah,
            estimatedFullMah = estimatedFullMah,
            chargeTimeRemainingMs = chargeTimeRemainingMs,
            cycleCount = cycleCount
        )
    }

    private fun readNormalizedCurrentMa(batteryManager: BatteryManager?, isCharging: Boolean): Float? {
        val raw = batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: return null
        if (raw == Long.MIN_VALUE || raw == Int.MIN_VALUE.toLong() || raw == 0L) return null
        val magnitudeMa = if (abs(raw) > 10_000L) abs(raw) / 1000f else abs(raw).toFloat()
        return if (isCharging) magnitudeMa else -magnitudeMa
    }

    data class BatteryResult(
        val percentage: Float,
        val tempC: Float,
        val isCharging: Boolean,
        val source: String,
        val voltageMv: Int,
        val currentMa: Float?,
        val powerWatts: Float?,
        val health: String,
        val technology: String,
        val chargeCounterMah: Float?,
        val estimatedFullMah: Float?,
        val chargeTimeRemainingMs: Long?,
        val cycleCount: Int?
    )
}
