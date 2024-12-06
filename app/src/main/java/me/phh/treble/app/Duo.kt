package me.phh.treble.app

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import android.content.Intent
import java.io.File
import java.lang.Exception
import me.phh.treble.app.MSPenCharger
import android.os.ServiceManager
import android.os.RemoteException
import android.os.SystemProperties
import androidx.preference.SwitchPreference

object Duo: EntryStartup {
    var ctxt: Context? = null
    private var isDuo2: Boolean = false
    val spListener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
        when(key) {
            DuoSettings.disableHingeGap -> {
                val b = sp.getBoolean(key, false)
                val value = if(b) "1" else "0"
                Misc.safeSetprop("persist.sys.phh.duo.disable_hinge", value)
            }

            DuoSettings.lockSingleScreenPosture -> {          
                /*
                    Set as string of one of three variables, convert in posture service to int. 
                    Should only be an integer from 0 to 2. 
                    If an unknown number is provided, default to "0".
                  
                    ...yes I know we should've just passed the string and not converted in the
                    service but it's too late now.    
                */
                val value = sp.getString(key, "dynamic")
                when(value) {
                    "dynamic" -> {                
                        Misc.safeSetprop("persist.sys.phh.duo.posture_lock", "0")
                    }
                    "right" -> {
                        Misc.safeSetprop("persist.sys.phh.duo.posture_lock", "1")
                    }
                    "left" -> {
                        Misc.safeSetprop("persist.sys.phh.duo.posture_lock", "2")
                    }
                    else -> {
                        Misc.safeSetprop("persist.sys.phh.duo.posture_lock", "0")  // default to dynamic
                    }
                }

            }
            
            DuoSettings.wirelessPenCharging -> {
                // Retrieve the boolean value from shared preferences
                val b = sp.getBoolean(key, false)
                // val readvalue = MSPenCharger.readPenCharger()
                // Log.e("PHH", "Current value in the ms_pen_charger file (current value: $readvalue)")
                if (isDuo2) {  
                    when (b) {
                        true -> {
                            val result = MSPenCharger.turnOnPenCharger()
                            if (result == 0) {
                                Log.d("PHH", "Pen charger started successfully")
                            } else {
                                Log.e("PHH", "Failed to start pen charger (error code: $result)")
                            }
                        }
                        false -> {
                            val result = MSPenCharger.turnOffPenCharger()
                            if (result == 0) {
                                Log.d("PHH", "Pen charger stopped successfully")
                            } else {
                                Log.e("PHH", "Failed to stop pen charger (error code: $result)")
                            }
                        }
                    }                   
                }
            }
        }
    }

    override fun startup(ctxt: Context) {
        if(!DuoSettings.enabled()) return
        Log.d("PHH", "Starting Duo service")
        val sp = PreferenceManager.getDefaultSharedPreferences(ctxt)
        sp.registerOnSharedPreferenceChangeListener(spListener)

        val wirelessPenChargingPref: SwitchPreference? = findPreference("wireless_pen_charging")
        val hardware = SystemProperties.get("ro.hardware", "N/A")
        if (hardware == "surfaceduo2") {
            wirelessPenChargingPref?.isVisible = true
            isDuo2 = true
        }

        this.ctxt = ctxt.applicationContext
    }
}