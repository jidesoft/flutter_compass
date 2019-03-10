package com.hemanthraj.fluttercompass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.PluginRegistry.Registrar

class FlutterCompassPlugin private constructor(context: Context, sensorType: Int) : EventChannel.StreamHandler {
  private var mAzimuth = 0.0 // degree
  private var newAzimuth = 0.0 // degree
  private var mPitch = 0.0 // degree
  private var newPitch = 0.0 // degree
  private var mFilter = 1f
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
      channel.setStreamHandler(FlutterCompassPlugin(registrar.context(), Sensor.TYPE_ROTATION_VECTOR))
    }
  }

  init {
    sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
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
        // get the azimuth value (orientation[0]) in degree

        SensorManager.getOrientation(rMat, orientation)
        newAzimuth = (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0].toDouble()) + 360) % 360

        SensorManager.remapCoordinateSystem(rMat, SensorManager.AXIS_X, SensorManager.AXIS_Z, arMat)
        SensorManager.getOrientation(arMat, orientation)
//                newAzimuth = (Math.toDegrees(orientation[0].toDouble()) + 360) % 360
        newPitch = (Math.toDegrees(orientation[1].toDouble()) + 360) % 360

        //dont react to changes smaller than the filter value
        if (Math.abs(mAzimuth - newAzimuth) < mFilter && Math.abs(mPitch - newPitch) < mFilter) {
          return
        }
        mAzimuth = newAzimuth
        mPitch = newPitch

        events.success(((newAzimuth * 10).toInt() + ((newPitch * 10).toInt() shl 16)).toDouble())
      }
    }
  }
}

