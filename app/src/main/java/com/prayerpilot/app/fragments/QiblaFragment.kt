package com.prayerpilot.app.fragments

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.*
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
    private var rot: Sensor? = null
    private var bearing = 0.0
    private lateinit var compass: QiblaCompassView

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        inflater.inflate(R.layout.fragment_qibla, c, false)

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        compass = view.findViewById(R.id.compass)
        sm = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rot = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        val app = requireContext().applicationContext as PrayerPilotApp
        viewLifecycleOwner.lifecycleScope.launch {
            val (lat, lng) = app.repository.getLocation() ?: return@launch
            bearing = Qibla.bearingFrom(lat, lng)
            compass.qiblaBearing = bearing.toFloat()
            view.findViewById<TextView>(R.id.tv_bearing).text = "Qibla bearing: ${"%.1f".format(bearing)}°"
            view.findViewById<TextView>(R.id.tv_distance).text = "Distance to Mecca: ${"%.0f".format(Qibla.distanceKm(lat, lng))} km"
        }
    }

    override fun onResume() { super.onResume(); rot?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) } }
    override fun onPause() { super.onPause(); sm.unregisterListener(this) }
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
        val r = FloatArray(9); val o = FloatArray(3)
        SensorManager.getRotationMatrixFromVector(r, event.values)
        SensorManager.getOrientation(r, o)
        val az = Math.toDegrees(o[0].toDouble()).toFloat()
        compass.deviceAzimuth = az
        compass.invalidate()
    }
    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
}
