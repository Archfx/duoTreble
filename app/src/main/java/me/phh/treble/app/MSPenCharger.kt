package me.phh.treble.app

object MSPenCharger {
    init {
        System.loadLibrary("pencharger") // Load the native library named "pencharger"
    }

    @JvmStatic
    external fun turnOnPenCharger(): Int

    @JvmStatic
    external fun turnOffPenCharger(): Int

    @JvmStatic
    external fun readPenCharger(): Int
}