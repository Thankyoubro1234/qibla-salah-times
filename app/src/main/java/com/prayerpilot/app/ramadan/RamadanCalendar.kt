package com.prayerpilot.app.ramadan

import java.util.Calendar

object RamadanCalendar {
    // Static fallback table for current Ramadan window (Hijri 1447 / Gregorian 2026)
    // In a real release this is supplemented by the Hijri calculation in HijriDate.
    private val ramadanWindows = mapOf(
        1447 to (Calendar.getInstance().apply { set(2026, Calendar.FEBRUARY, 18) } to
                  Calendar.getInstance().apply { set(2026, Calendar.MARCH, 19) }),
        1448 to (Calendar.getInstance().apply { set(2027, Calendar.FEBRUARY, 8) } to
                  Calendar.getInstance().apply { set(2027, Calendar.MARCH, 9) })
    )

    fun dayLabel(): String {
        val now = Calendar.getInstance()
        for ((y, range) in ramadanWindows) {
            val (start, end) = range
            if (!now.before(start) && !now.after(end)) {
                val diff = ((now.timeInMillis - start.timeInMillis) / 86_400_000L).toInt() + 1
                return "Ramadan day $diff of 30"
            }
        }
        return "Ramadan companion"
    }
}
