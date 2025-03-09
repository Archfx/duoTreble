package me.phh.treble.app

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import android.content.Intent
import android.content.IntentFilter
import java.io.File
import java.lang.Exception
import me.phh.treble.app.MSPenCharger
import me.phh.treble.app.WirelessPenChargerBroadcastReceiver
import android.os.ServiceManager
import android.os.RemoteException
import android.os.SystemProperties
import androidx.preference.SwitchPreference
import android.os.BatteryManager;
import android.widget.Toast;

object Duo: EntryStartup {
    var ctxt: Context? = null
    private var isDuo2: Boolean = false
    private var penChargerBroadcastReceiver: WirelessPenChargerBroadcastReceiver? = null
    private var penChargerIntentFilter: IntentFilter? = null

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
                val value = if(b) "1" else "0"
                Misc.safeSetprop("persist.sys.phh.duo.pen_charger_enabled", value)

                val chargeWhenPluggedIn : Boolean = SystemProperties.get("persist.sys.phh.duo.charge_pen_when_device_charging", "0") == "1"
                // val readvalue = MSPenCharger.readPenCharger()
                // Log.e("PHH", "Current value in the ms_pen_charger file (current value: $readvalue)")
                if (isDuo2 && !chargeWhenPluggedIn) {  
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

                if(chargeWhenPluggedIn){
                    Toast.makeText(this.ctxt, "Pen will not charge while charge with device option is enabled!", Toast.LENGTH_SHORT)
                }
            }

            DuoSettings.chargePenOnlyWhenDeviceIsCharging -> {
                val b = sp.getBoolean(key, false)
                val value = if(b) "1" else "0"
                Misc.safeSetprop("persist.sys.phh.duo.charge_pen_when_device_charging", value)
                if (isDuo2) {  
                    when (b) {
                        true -> {
                            //Check current state.
                            setupBroadcastListener()

                            //Do an initial check.
                            if(isDeviceCharging()){
                                MSPenCharger.turnOnPenCharger()
                            }
                            else{
                                MSPenCharger.turnOffPenCharger()
                            }
                        }
                        false -> {
                            destroyBroadcastListener()

                            //Handle change if the pen charger is enabled.
                            if(SystemProperties.get("persist.sys.phh.duo.pen_charger_enabled", "0") == "1"){
                                MSPenCharger.turnOnPenCharger()
                            }
                            else{
                                MSPenCharger.turnOffPenCharger()
                            }
                        }
                    }                   
                }
            }

            DuoSettings.peekModeEnabled -> {
                val b = sp.getBoolean(key, false)
                val value = if(b) "1" else "0"
                Misc.safeSetprop("persist.sys.phh.duo.peek_mode_enabled", value)
            }

            DuoSettings.peekModeHingeClockPosition -> {
                val value = sp.getString(key, "center")
                when(value) {
                    "center" -> {                
                        Misc.safeSetprop("persist.sys.phh.duo.peek_mode_hinge_clock_position", "0")
                    }
                    "top" -> {
                        Misc.safeSetprop("persist.sys.phh.duo.peek_mode_hinge_clock_position", "1")
                    }
                    "bottom" -> {
                        Misc.safeSetprop("persist.sys.phh.duo.peek_mode_hinge_clock_position", "2")
                    }
                    else -> {
                        Misc.safeSetprop("persist.sys.phh.duo.peek_mode_hinge_clock_position", "0")  // default to center
                    }
                }

            }
        }
    }

    private fun setupBroadcastListener() {
        //Unregister if there are still some dangling loose ends to handle.
        try{
            if(penChargerBroadcastReceiver != null){
                this.ctxt?.unregisterReceiver(penChargerBroadcastReceiver)
            }
        } catch (e: IllegalArgumentException){
            // Do nothing. the register is already unregistered it seems.
        }

        // create broadcast receiver.
        penChargerBroadcastReceiver = WirelessPenChargerBroadcastReceiver()

        // On power change, the broadcast receiver should act.
        penChargerIntentFilter = IntentFilter().also{ intentFilter -> 
            intentFilter.addAction("android.intent.action.ACTION_POWER_CONNECTED")
            intentFilter.addAction("android.intent.action.ACTION_POWER_DISCONNECTED")
            this.ctxt?.registerReceiver(penChargerBroadcastReceiver, intentFilter)
            Log.d("PHH", "Broadcast Receiver registered for pen charger!")
        }
    }

    private fun destroyBroadcastListener(){
        try{
            penChargerBroadcastReceiver?.let{
                this.ctxt?.unregisterReceiver(it)
                Log.d("PHH", "Broadcast Receiver unregistered for pen charger!")
            }
        } catch (e: IllegalArgumentException){
            // Do nothing. the register is already unregistered it seems.
        }

    }

    private fun isDeviceCharging() : Boolean {
        var batteryManager : BatteryManager = ctxt?.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.isCharging();
    }

    override fun startup(ctxt: Context) {
        if(!DuoSettings.enabled()) return
        Log.d("PHH", "Starting Duo service")
        val sp = PreferenceManager.getDefaultSharedPreferences(ctxt)
        sp.registerOnSharedPreferenceChangeListener(spListener)

        val hardware = SystemProperties.get("ro.hardware", "N/A")
        if (hardware == "surfaceduo2") {
            isDuo2 = true
        }

        this.ctxt = ctxt.applicationContext

        //Read the current duo preferences and act on it.
        if(isDuo2){
            val chargeWhenPluggedIn : Boolean = SystemProperties.get("persist.sys.phh.duo.charge_pen_when_device_charging", "0") == "1"
            val penChargerEnabled : Boolean = SystemProperties.get("persist.sys.phh.duo.pen_charger_enabled", "0") == "1"
            // The device just started, the broadcast receiver is not setup, set it up if enabled.
            if(chargeWhenPluggedIn){
                setupBroadcastListener()

                if(isDeviceCharging()){
                    MSPenCharger.turnOnPenCharger()
                }
                else{
                    MSPenCharger.turnOffPenCharger()
                }
            }

            //Only turn the pen charger on if explictly enabled, and the plug-in option is disabled.
            if(penChargerEnabled && !chargeWhenPluggedIn){
                MSPenCharger.turnOnPenCharger()
            }

            //In case for users that have the device default set to ON, but have left the option off.
            if(!penChargerEnabled && !chargeWhenPluggedIn){
                MSPenCharger.turnOffPenCharger()
            }
        }
    }
}
