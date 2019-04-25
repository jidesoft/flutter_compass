package com.hemanthraj.fluttercompass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.WindowManager
import android.view.Surface
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.PluginRegistry.Registrar

class FlutterCompassPlugin private constructor(registrar: Registrar, sensorType: Int) : EventChannel.StreamHandler {
  private var mAzimuth = 0.0 // degree
  private var newAzimuth = 0.0 // degree
  private var mFilter = 1f
  private val windowManager: WindowManager
  private var sensorEventListener: SensorEventListener? = null
  private val sensorManager: SensorManager
  private var sensor: Sensor?
  private val orientation = FloatArray(3)
  private val rMat = FloatArray(9)
  private val arMat = FloatArray(9)

  companion object {
    @JvmStatic
    fun registerWith(registrar: Registrar): Unit {
      val channel = EventChannel(registrar.messenger(), "hemanthraj/flutter_compass")
      channel.setStreamHandler(FlutterCompassPlugin(registrar, Sensor.TYPE_ROTATION_VECTOR))
    }
  }

  init {
    windowManager = registrar.activity().getWindow().getWindowManager()
    sensorManager = registrar.context().getSystemService(Context.SENSOR_SERVICE) as SensorManager
    sensor = sensorManager.getDefaultSensor(sensorType)
  }

  override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
    sensorEventListener = createSensorEventListener(events)
    sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_UI)
  }

  override fun onCancel(arguments: Any?) {
    sensorManager.unregisterListener(sensorEventListener)
  }

  internal fun createSensorEventListener(events: EventChannel.EventSink): SensorEventListener {
    return object : SensorEventListener {
      override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

      override fun onSensorChanged(event: SensorEvent) {
        // calculate th rotation matrix
        SensorManager.getRotationMatrixFromVector(rMat, event.values)

        val worldAxisForDeviceAxisX: Int
        val worldAxisForDeviceAxisY: Int

        // Remap the axes as if the device screen was the instrument panel,
        // and adjust the rotation matrix for the device orientation.
        when (windowManager.getDefaultDisplay().getRotation()) {
          Surface.ROTATION_0 -> {
            worldAxisForDeviceAxisX = SensorManager.AXIS_X
            worldAxisForDeviceAxisY = SensorManager.AXIS_Z
            newAzimuth = (((Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0].toDouble()) + 360) % 360 - Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[2].toDouble()) + 360) % 360)
          }
          Surface.ROTATION_90 -> {
            worldAxisForDeviceAxisX = SensorManager.AXIS_Z
            worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_X
            newAzimuth = (((Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0].toDouble()) + 360) % 360 + Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[1].toDouble()) + 360 + 90) % 360)
          }
          Surface.ROTATION_180 -> {
            worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_X
            worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_Z
            newAzimuth = (((Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0].toDouble()) + 360) % 360 + Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[2].toDouble()) + 360) % 360)
          }
          Surface.ROTATION_270 -> {
            worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_Z
            worldAxisForDeviceAxisY = SensorManager.AXIS_X
            newAzimuth = (((Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0].toDouble()) + 360) % 360 - Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[1].toDouble()) + 360 - 90) % 360)
          }
          else -> {
            worldAxisForDeviceAxisX = SensorManager.AXIS_X
            worldAxisForDeviceAxisY = SensorManager.AXIS_Z
            newAzimuth = (((Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0].toDouble()) + 360) % 360 - Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[2].toDouble()) + 360) % 360)
          }
        }

//        val adjustedRotationMatrix = FloatArray(9)
//        SensorManager.remapCoordinateSystem(rMat, worldAxisForDeviceAxisX,
//                worldAxisForDeviceAxisY, adjustedRotationMatrix)

//        newAzimuth = (Math.toDegrees(adjustedRotationMatrix[0].toDouble()) + 270) % 360

//        newAzimuth = (((Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0].toDouble()) + 360) % 360 - Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[2].toDouble()) + 360) % 360)

        //dont react to changes smaller than the filter value
        if (Math.abs(mAzimuth - newAzimuth) < mFilter) {
          return
        }

        mAzimuth = newAzimuth

        events.success(newAzimuth.toDouble())
      }
    }
  }
}

