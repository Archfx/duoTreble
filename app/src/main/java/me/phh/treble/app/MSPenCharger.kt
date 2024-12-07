package me.phh.treble.app

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