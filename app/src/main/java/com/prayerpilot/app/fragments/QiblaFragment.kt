package com.prayerpilot.app.fragments

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.*
import android.view.Surface
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.prayerpilot.app.PrayerPilotApp
import com.prayerpilot.app.R
import com.prayerpilot.app.prayer.Qibla
import com.prayerpilot.app.ui.QiblaCompassView
import kotlinx.coroutines.launch

class QiblaFragment : Fragment(), SensorEventListener {
    private lateinit var sm: SensorManager
    private var rotationSensor: Sensor? = null
    private var bearing = 0.0
    private var magneticDeclination = 0f
    private lateinit var compass: QiblaCompassView
    private var lastAzimuth = 0f
    private val SMOOTHING = 0.15f
    private var accuracyState = SensorManager.SENSOR_STATUS_UNRELIABLE

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        inflater.inflate(R.layout.fragment_qibla, c, false)

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        compass = view.findViewById(R.id.compass)
        sm = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        // Prefer TYPE_ROTATION_VECTOR (uses gyro+accel+mag fused) for stability.
        // Fall back to TYPE_GEOMAGNETIC_ROTATION_VECTOR (no gyro) on older devices.
        rotationSensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sm.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)

        val app = requireContext().applicationContext as PrayerPilotApp
        viewLifecycleOwner.lifecycleScope.launch {
            val (lat, lng) = app.repository.getLocation() ?: return@launch
            bearing = Qibla.bearingFrom(lat, lng)

            // Compute magnetic declination at user's location to convert magnetic north -> true north
            val gmf = GeomagneticField(lat.toFloat(), lng.toFloat(), 0f, System.currentTimeMillis())
            magneticDeclination = gmf.declination

            compass.qiblaBearing = bearing.toFloat()
            view.findViewById<TextView>(R.id.tv_bearing).text =
                "Qibla bearing: ${"%.1f".format(bearing)}° true"
            view.findViewById<TextView>(R.id.tv_distance).text =
                "Distance to Mecca: ${"%.0f".format(Qibla.distanceKm(lat, lng))} km"
            view.findViewById<TextView>(R.id.tv_calibration_status).text =
                "Magnetic declination: ${"%+.1f".format(magneticDeclination)}°"
        }

        view.findViewById<android.widget.Button>(R.id.btn_recalibrate)?.setOnClickListener {
            android.widget.Toast.makeText(requireContext(),
                "Wave the phone in a figure 8 a few times to recalibrate.",
                android.widget.Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        rotationSensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    override fun onPause() {
        super.onPause()
        sm.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR &&
            event.sensor.type != Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) return

        val rotMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)

        // Remap based on display rotation so the compass works in any orientation
        val displayRotation = requireActivity().windowManager.defaultDisplay.rotation
        val (axisX, axisY) = when (displayRotation) {
            Surface.ROTATION_0 -> SensorManager.AXIS_X to SensorManager.AXIS_Y
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
        val remapped = FloatArray(9)
        SensorManager.remapCoordinateSystem(rotMatrix, axisX, axisY, remapped)

        val orientation = FloatArray(3)
        SensorManager.getOrientation(remapped, orientation)
        var azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
        // Convert magnetic north to true north
        azimuthDeg += magneticDeclination
        // Normalize to 0..360
        azimuthDeg = ((azimuthDeg % 360) + 360) % 360

        // Low pass filter for stability — handle wrap around 0/360 boundary
        var diff = azimuthDeg - lastAzimuth
        if (diff > 180) diff -= 360
        if (diff < -180) diff += 360
        lastAzimuth = ((lastAzimuth + SMOOTHING * diff) % 360 + 360) % 360

        compass.deviceAzimuth = lastAzimuth
        compass.accuracy = accuracyState
        compass.invalidate()

        // Update calibration status text (only when state changes)
        view?.findViewById<TextView>(R.id.tv_accuracy)?.let { tv ->
            tv.text = when (accuracyState) {
                SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> getString(R.string.qibla_accuracy_high)
                SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> getString(R.string.qibla_accuracy_medium)
                SensorManager.SENSOR_STATUS_ACCURACY_LOW,
                SensorManager.SENSOR_STATUS_UNRELIABLE -> getString(R.string.qibla_accuracy_low)
                else -> getString(R.string.qibla_calibration_needed)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, acc: Int) {
        accuracyState = acc
    }
}
