package me.phh.treble.app

import android.service.quicksettings.TileService
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.Context
import android.os.SystemProperties
import android.util.Log

object MSPenCharger {
    init {
        System.loadLibrary("ms_pen_charger")
    }

    @JvmStatic
    external fun turnOnPenCharger(): Int

    @JvmStatic
    external fun turnOffPenCharger(): Int

    @JvmStatic
    external fun readPenCharger(): Int
}

class MsPENTileService: TileService() {

    // Called when the user adds your tile.
    override fun onTileAdded() {
      super.onTileAdded()
    }
    // Called when your app can update your tile.
    override fun onStartListening() {
      super.onStartListening()
    }
  
    // Called when your app can no longer update your tile.
    override fun onStopListening() {
      super.onStopListening()
    }
  
    // Called when the user taps on your tile in an active or inactive state.
    override fun onClick() {
      super.onClick()
    }
    // Called when the user removes your tile.
    override fun onTileRemoved() {
      super.onTileRemoved()
    }
  }


// This should only work when the broadcast receiver is registered
// and ready to change the power state of the pen charger.
public class WirelessPenChargerBroadcastReceiver: BroadcastReceiver() {
  private var isDuo2: Boolean = false

  companion object {
    const val TAG = "WIRELESS PEN CHARGER"
  }

  override fun onReceive(context: Context, intent: Intent){
    val action: String? = intent.getAction()
    isDuo2 = SystemProperties.get("ro.hardware", "N/A") == "surfaceduo2"

    //exit early if duo 1 is here
    if(!isDuo2){
      Log.d(TAG, "Exiting as this device is not a duo 2")
      return
    }
    
    when(action){
      "ACTION_POWER_CONNECTED" -> {
        // Turn power on.
        MSPenCharger.turnOnPenCharger()
        Log.d(TAG, "Pen Charger turned on from plugged in event.")
      }
      "ACTION_POWER_DISCONNECTED" -> {
        // Shut power off.
        MSPenCharger.turnOffPenCharger()
        Log.d(TAG, "Pen Charger turned off from unplugged event.")
      }
    }
  }
}