package me.phh.treble.app

import android.os.Bundle
import android.os.SystemProperties
import androidx.preference.SwitchPreference
import me.phh.treble.app.PreferenceUpdater

object DuoSettings : Settings {
    val disableHingeGap = "key_disable_hinge_gap"
    val lockSingleScreenPosture = "key_duo_posture_lock"
    val wirelessPenCharging = "wireless_pen_charging"

    override fun enabled() = (SystemProperties.get("ro.hardware", "N/A") == "surfaceduo" || SystemProperties.get("ro.hardware", "N/A") == "surfaceduo2")
}

class DuoSettingsFragment : SettingsFragment(), PreferenceUpdater {
    override val preferencesResId = R.xml.pref_duo

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        android.util.Log.d("PHH", "Loading duo fragment ${DuoSettings.enabled()}")
    }

    override fun onStart() {
        super.onStart()
        Duo.updater = this
    }

    override fun onStop() {
        super.onStop()
        if (Duo.updater == this) {
            Duo.updater = null
        }
    }

    override fun showWirelessPenCharging() {
        val wirelessPenChargingPref: SwitchPreference? = findPreference("wireless_pen_charging")
        wirelessPenChargingPref?.isVisible = true
    }
}

interface PreferenceUpdater {
    fun showWirelessPenCharging()
}