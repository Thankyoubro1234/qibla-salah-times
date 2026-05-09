package com.prayerpilot.app.prayer

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.*

/**
 * PrayTimes engine: hyper accurate astronomical computation.
 * Adapted from praytimes.org open algorithm with high latitude rules,
 * elevation correction, and full method coverage.
 */
class PrayTimes(method: Method = Method.ISNA) {

    enum class Method(
        val fajrAngle: Double,
        val ishaAngle: Double,
        val ishaMinutes: Int = 0,
        val maghribAngle: Double = 0.0,
        val maghribMinutes: Int = 0
    ) {
        MWL(18.0, 17.0),
        ISNA(15.0, 15.0),
        EGYPT(19.5, 17.5),
        MAKKAH(18.5, 0.0, ishaMinutes = 90),
        KARACHI(18.0, 18.0),
        TEHRAN(17.7, 14.0, maghribAngle = 4.5),
        JAFARI(16.0, 14.0, maghribAngle = 4.0),
        SINGAPORE(20.0, 18.0),
        FRANCE(12.0, 12.0),
        TURKEY(18.0, 17.0),
        RUSSIA(16.0, 15.0)
    }

    enum class HighLatRule { NONE, MIDNIGHT, ONE_SEVENTH, ANGLE_BASED }
    enum class AsrJuristic { STANDARD, HANAFI }

    var asrJuristic: AsrJuristic = AsrJuristic.STANDARD
    var highLatRule: HighLatRule = HighLatRule.ANGLE_BASED
    var method: Method = method
    var elevation: Double = 0.0
    var manualOffsetsMinutes: IntArray = IntArray(6)

    data class Times(
        val fajr: Double, val sunrise: Double, val dhuhr: Double,
        val asr: Double, val maghrib: Double, val isha: Double, val midnight: Double
    )

    fun getTimes(
        date: Calendar, lat: Double, lng: Double,
        timeZoneOffsetHours: Double = TimeZone.getDefault().getOffset(date.timeInMillis) / 3_600_000.0
    ): Times {
        val jDate = julianDate(date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1, date.get(Calendar.DAY_OF_MONTH)) - lng / (15.0 * 24.0)

        fun dayPortion(t: Double) = t / 24.0

        var fajr = sunAngleTime(method.fajrAngle, dayPortion(5.0), jDate, lat, ccw = true)
        val sunrise = sunAngleTime(riseSetAngle(), dayPortion(6.0), jDate, lat, ccw = true)
        val dhuhr = midDay(dayPortion(12.0), jDate)
        val asr = asrTime(if (asrJuristic == AsrJuristic.HANAFI) 2.0 else 1.0, dayPortion(13.0), jDate, lat)
        val sunset = sunAngleTime(riseSetAngle(), dayPortion(18.0), jDate, lat)
        var maghrib = if (method.maghribAngle > 0) sunAngleTime(method.maghribAngle, dayPortion(18.0), jDate, lat)
                      else sunset + method.maghribMinutes / 60.0
        var isha = if (method.ishaMinutes > 0) maghrib + method.ishaMinutes / 60.0
                   else sunAngleTime(method.ishaAngle, dayPortion(18.0), jDate, lat)

        // Adjust high latitudes
        val nightDuration = timeDiff(sunset, sunrise)
        fajr = adjustHighLatTime(fajr, sunrise, method.fajrAngle, nightDuration, ccw = true)
        isha = adjustHighLatTime(isha, sunset, method.ishaAngle, nightDuration)
        if (method.maghribAngle > 0)
            maghrib = adjustHighLatTime(maghrib, sunset, method.maghribAngle, nightDuration)

        // Elevation correction (refraction)
        val elevAdjMin = 0.0347 * sqrt(elevation.coerceAtLeast(0.0))
        val timezoneAdj = timeZoneOffsetHours - lng / 15.0
        val midnight = (sunset + timeDiff(sunset, fajr) / 2.0) + timezoneAdj

        return Times(
            fajr = (fajr + timezoneAdj - elevAdjMin / 60.0) + manualOffsetsMinutes[0] / 60.0,
            sunrise = (sunrise + timezoneAdj - elevAdjMin / 60.0) + manualOffsetsMinutes[1] / 60.0,
            dhuhr = (dhuhr + timezoneAdj) + manualOffsetsMinutes[2] / 60.0,
            asr = (asr + timezoneAdj) + manualOffsetsMinutes[3] / 60.0,
            maghrib = (maghrib + timezoneAdj + elevAdjMin / 60.0) + manualOffsetsMinutes[4] / 60.0,
            isha = (isha + timezoneAdj + elevAdjMin / 60.0) + manualOffsetsMinutes[5] / 60.0,
            midnight = midnight
        )
    }

    private fun riseSetAngle(): Double {
        val angle = 0.0347 * sqrt(elevation.coerceAtLeast(0.0))
        return 0.833 + angle
    }

    // --- core astronomical helpers ---

    private fun julianDate(y: Int, m: Int, d: Int): Double {
        val (yy, mm) = if (m <= 2) (y - 1) to (m + 12) else y to m
        val a = floor(yy / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (yy + 4716)) + floor(30.6001 * (mm + 1)) + d + b - 1524.5
    }

    private fun sunAngleTime(angle: Double, time: Double, jDate: Double, lat: Double, ccw: Boolean = false): Double {
        val decl = sunPosition(jDate + time).first
        val noon = midDay(time, jDate)
        val t = 1.0 / 15.0 * arccos((-sin(angle.toRad()) - sin(lat.toRad()) * sin(decl.toRad())) / (cos(lat.toRad()) * cos(decl.toRad())))
        return noon + (if (ccw) -t else t)
    }

    private fun midDay(time: Double, jDate: Double): Double {
        val eqt = sunPosition(jDate + time).second
        return fixHour(12.0 - eqt)
    }

    private fun asrTime(factor: Double, time: Double, jDate: Double, lat: Double): Double {
        val decl = sunPosition(jDate + time).first
        val angle = -arccot(factor + tan(abs(lat - decl).toRad()))
        return sunAngleTime(angle, time, jDate, lat)
    }

    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(g.toRad()) + 0.020 * sin((2 * g).toRad()))
        val e = 23.439 - 0.00000036 * d
        val ra = arctan2(cos(e.toRad()) * sin(l.toRad()), cos(l.toRad())) / 15.0
        val decl = arcsin(sin(e.toRad()) * sin(l.toRad()))
        val eqt = q / 15.0 - fixHour(ra)
        return decl to eqt
    }

    private fun adjustHighLatTime(time: Double, base: Double, angle: Double, night: Double, ccw: Boolean = false): Double {
        if (highLatRule == HighLatRule.NONE) return time
        val portion = nightPortion(angle, night)
        val td = if (ccw) timeDiff(time, base) else timeDiff(base, time)
        return if (td > portion) base + (if (ccw) -portion else portion) else time
    }

    private fun nightPortion(angle: Double, night: Double): Double = when (highLatRule) {
        HighLatRule.ANGLE_BASED -> 1.0 / 60.0 * angle * night
        HighLatRule.MIDNIGHT -> night / 2.0
        HighLatRule.ONE_SEVENTH -> night / 7.0
        else -> night
    }

    // --- math helpers ---
    private fun Double.toRad() = this * PI / 180.0
    private fun Double.toDeg() = this * 180.0 / PI
    private fun fixAngle(a: Double): Double = ((a % 360 + 360) % 360)
    private fun fixHour(a: Double): Double = ((a % 24 + 24) % 24)
    private fun timeDiff(a: Double, b: Double): Double = fixHour(b - a)
    private fun arcsin(x: Double) = asin(x).toDeg()
    private fun arccos(x: Double) = acos(x).toDeg()
    private fun arctan2(y: Double, x: Double) = atan2(y, x).toDeg()
    private fun arccot(x: Double) = atan(1.0 / x).toDeg()

    companion object {
        fun formatTime(decimalHours: Double, twentyFour: Boolean = true): String {
            if (decimalHours.isNaN()) return "--:--"
            val h = floor(decimalHours).toInt()
            val m = floor((decimalHours - h) * 60.0 + 0.5).toInt()
            val (hh, mm) = if (m == 60) (h + 1) to 0 else h to m
            return if (twentyFour) String.format("%02d:%02d", (hh + 24) % 24, mm)
            else {
                val ampm = if (hh < 12) "AM" else "PM"
                val h12 = ((hh + 11) % 12) + 1
                String.format("%d:%02d %s", h12, mm, ampm)
            }
        }
    }
}
