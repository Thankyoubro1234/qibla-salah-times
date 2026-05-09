package com.prayerpilot.app.fragments

import java.util.Calendar
import kotlin.math.floor

object HijriDate {
    private val months = arrayOf("Muharram","Safar","Rabi I","Rabi II","Jumada I","Jumada II",
        "Rajab","Sha'ban","Ramadan","Shawwal","Dhul Qadah","Dhul Hijjah")
    fun todayHijriString(): String {
        val c = Calendar.getInstance()
        val (y, m, d) = toHijri(c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1, c.get(Calendar.DAY_OF_MONTH))
        return "$d ${months[m-1]} $y AH"
    }
    private fun toHijri(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
        val jd = gregorianToJD(gy, gm, gd)
        val l = jd - 1948440 + 10632
        val n = ((l - 1) / 10631).toInt()
        val l2 = l - 10631 * n + 354
        val j = ((10985L - l2) / 5316L).toInt() * ((50L * l2) / 17719L).toInt() +
                ((l2 / 5670L).toInt() * ((43L * l2) / 15238L).toInt())
        val l3 = l2 - ((30L - j) / 15L).toInt() * ((17719L * j) / 50L).toInt() -
                (j / 16).toInt() * ((15238L * j) / 43L).toInt() + 29
        val month = ((24L * l3) / 709L).toInt()
        val day = (l3 - ((709L * month) / 24L).toInt()).toInt()
        val year = 30 * n + j - 30
        return Triple(year, month, day)
    }
    private fun gregorianToJD(y: Int, m: Int, d: Int): Long {
        val (yy, mm) = if (m <= 2) (y-1) to (m+12) else y to m
        val a = floor(yy/100.0).toLong()
        val b = 2 - a + a/4
        return (floor(365.25*(yy+4716)) + floor(30.6001*(mm+1)) + d + b - 1524.5).toLong()
    }
}
