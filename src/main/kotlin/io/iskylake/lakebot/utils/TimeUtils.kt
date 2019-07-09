/*
 * Copyright 2017-2019 (c) Alexander "ILakeful" Shevchenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.iskylake.lakebot.utils

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object TimeUtils {
    val TIME_REGEX = "([1-9][0-9]*)(s|m|h)".toRegex()
    val FORCE_TIME_REGEX = "([1-9][0-9]*)(s|m|h|d|w|y)".toRegex()
    fun millisToDate(millis: Long, zone: ZoneId = ZoneId.of("Etc/GMT0")): OffsetDateTime {
        val instant: Instant = Instant.ofEpochMilli(millis)
        return instant.atZone(zone).toOffsetDateTime()
    }
    inline fun millisToDate(zone: ZoneId = ZoneId.of("Etc/GMT0"), crossinline millis: () -> Long) = millisToDate(millis(), zone)
    fun weeksToDays(weeks: Long): Long = weeks * 7
    fun weeksToHours(weeks: Long): Long = weeksToDays(weeks) * 24
    fun weeksToMinutes(weeks: Long): Long = weeksToHours(weeks) * 60
    fun weeksToSeconds(weeks: Long): Long = weeksToMinutes(weeks) * 60
    fun weeksToMillis(weeks: Long): Long = weeksToSeconds(weeks) * 1000
    fun millisToWeeks(millis: Long): Long = TimeUnit.MILLISECONDS.toDays(millis) / 7
    fun secondsToWeeks(seconds: Long): Long = TimeUnit.SECONDS.toDays(seconds) / 7
    fun minutesToWeeks(minutes: Long): Long = TimeUnit.MINUTES.toDays(minutes) / 7
    fun hoursToWeeks(hours: Long): Long = TimeUnit.HOURS.toDays(hours) / 7
    fun daysToWeeks(hours: Long): Long = TimeUnit.DAYS.toDays(hours) / 7
    fun yearsToDays(years: Long): Long = years * 365
    fun yearsToHours(years: Long): Long = yearsToDays(years) * 24
    fun yearsToMinutes(years: Long): Long = yearsToHours(years) * 60
    fun yearsToSeconds(years: Long): Long = yearsToMinutes(years) * 60
    fun yearsToMillis(years: Long): Long = yearsToSeconds(years) * 1000
    fun millisToYears(millis: Long): Long = TimeUnit.MILLISECONDS.toDays(millis) / 365
    fun secondsToYears(seconds: Long): Long = TimeUnit.SECONDS.toDays(seconds) / 365
    fun minutesToYears(minutes: Long): Long = TimeUnit.MINUTES.toDays(minutes) / 365
    fun hoursToYears(hours: Long): Long = TimeUnit.HOURS.toDays(hours) / 365
    fun daysToYears(hours: Long): Long = TimeUnit.DAYS.toDays(hours) / 365
    fun parseTime(input: String): Long {
        var time: Long = 0
        for (result in FORCE_TIME_REGEX.findAll(input)) {
            val int = result.value.substring(0, result.value.count() - 1).toLong()
            when (result.value.last()) {
                's' -> time += TimeUnit.SECONDS.toMillis(int)
                'm' -> time += TimeUnit.MINUTES.toMillis(int)
                'h' -> time += TimeUnit.HOURS.toMillis(int)
                'd' -> time += TimeUnit.DAYS.toMillis(int)
                'w' -> time += weeksToMillis(int)
                'y' -> time += yearsToMillis(int)
                else -> {}
            }
        }
        return time
    }
    fun asDuration(time: Long, hoursIfNull: Boolean = false, daysIfNull: Boolean = false): String {
        val seconds = time / 1000L % 60L
        val minutes = time / 60000L % 60L
        val hours = time / 3600000L % 24L
        val days = time / 86400000L % 30L
        return buildString {
            val daysS = if (days < 10) "0$days:" else "$days:"
            if (daysIfNull) append(daysS) else if (days > 0) append(daysS) else {}
            val hoursS = if (hours < 10) "0$hours:" else "$hours:"
            if (hoursIfNull || days > 0 || daysIfNull) append(hoursS) else if (hours > 0) append(hoursS) else {}
            val minutesS = if (minutes < 10) "0$minutes:" else "$minutes:"
            append(minutesS)
            val secondsS = if (seconds < 10) "0$seconds" else "$seconds"
            append(secondsS)
        }
    }
    fun asText(millis: Long): String {
        val days = TimeUnit.MILLISECONDS.toDays(millis)
        val hours = TimeUnit.MILLISECONDS.toHours(millis) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(millis))
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis))
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        return buildString {
            var leading = false
            if (days > 0L) {
                append(days)
                append(" ")
                append(if (days == 1L) "day" else "days")
                leading = true
            }
            if (hours > 0L) {
                if (leading && (minutes != 0L || seconds != 0L)) {
                    append(", ")
                }
                if (!toString().isEmpty() && (minutes == 0L && seconds == 0L)) {
                    append(" and ")
                }
                append(hours)
                append(" ")
                append(if (hours == 1L) "hour" else "hours")
                leading = true
            }
            if (minutes > 0L) {
                if (leading && seconds != 0L) {
                    append(", ")
                }
                if (!toString().isEmpty() && seconds == 0L) {
                    append(" and ")
                }
                leading = true
                append(minutes)
                append(" ")
                append(if (minutes == 1L) "minute" else "minutes")
            }
            if (seconds > 0L) {
                if (leading) {
                    append(" and ")
                }
                append(seconds)
                append(" ")
                append(if (seconds == 1L) "second" else "seconds")
            }
            if (toString().isEmpty() && !leading) {
                append("0 seconds")
            }
        }
    }
}