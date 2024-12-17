package me.phh.treble.app

import android.os.Bundle
import android.os.SystemProperties
import androidx.preference.SwitchPreference
import android.util.Log


object DuoSettings : Settings {
    val disableHingeGap = "key_disable_hinge_gap"
    val lockSingleScreenPosture = "key_duo_posture_lock"
    val wirelessPenCharging = "wireless_pen_charging"
    val chargePenOnlyWhenDeviceIsCharging = "charge_pen_while_device_is_charging"

    override fun enabled() = (SystemProperties.get("ro.hardware", "N/A") == "surfaceduo" || SystemProperties.get("ro.hardware", "N/A") == "surfaceduo2")
}

class DuoSettingsFragment : SettingsFragment() {
    override val preferencesResId = R.xml.pref_duo

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        android.util.Log.d("PHH", "Loading duo fragment ${DuoSettings.enabled()}")

        // Check hardware property and hide wirelessPenCharging if condition met
        val hardware = SystemProperties.get("ro.hardware", "N/A")
        if (hardware == "surfaceduo") {
            val wirelessPenChargingPref: SwitchPreference? = findPreference(DuoSettings.wirelessPenCharging)
            wirelessPenChargingPref?.let {
                preferenceScreen.removePreference(it)
                Log.d("PHH", "Wireless Pen Charging preference removed for hardware: $hardware")
            }

            val chargePenOnlyWhenDeviceIsChargingPreference: SwitchPreference? = findPreference(DuoSettings.chargePenOnlyWhenDeviceIsCharging)
            chargePenOnlyWhenDeviceIsChargingPreference?.let {
                preferenceScreen.removePreference(it)
                Log.d("PHH", "Wireless Pen Charger during device charge preference removed for hardware: $hardware")
            }
        }
    }

}

