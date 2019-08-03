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

package io.ilakeful.lakebot.commands.utils

import io.ilakeful.lakebot.Immutable
import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.extensions.*
import io.iskylake.weather.lakeWeather

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

import khttp.get

class TimeCommand : Command {
    override val name = "time"
    override val aliases = listOf("timezone")
    override val description = "The command sending the specified location's current time and timezone"
    override val cooldown = 3L
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <location>"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val arguments = event.argsRaw
        if (arguments !== null) {
            try {
                val api = lakeWeather(token = Immutable.WEATHER_API_KEY)
                val forecast = api[arguments]
                if (forecast !== null) {
                    val (longitude, latitude) = forecast.coordinates
                    val url = "http://api.geonames.org/timezoneJSON?lat=$latitude&lng=$longitude&username=${Immutable.GEONAME_API_USER}"
                    val response = get(url, headers = mapOf()).jsonObject
                    val time = response.getString("time").takeLast(5)
                    val name = "${forecast.city.name}, ${forecast.system.countryCode}"
                    event.sendMessage(buildEmbed {
                        color { Immutable.SUCCESS }
                        author("Time - $name") { event.selfUser.effectiveAvatarUrl }
                        field(true, "12-Hour Time:") {
                            val timeAsHoursAndMinutes = time.split(":")
                            val hours = timeAsHoursAndMinutes[0].toInt()
                            val minutes = timeAsHoursAndMinutes[1]
                            val twentyHoursFormat = when (hours) {
                                0 -> 12
                                in 13..23 -> hours - 12
                                else -> hours
                            }
                            val amOrPm = if (hours in 0..11) "AM" else "PM"
                            "$twentyHoursFormat:$minutes $amOrPm"
                        }
                        field(true, "24-Hour Time:") { time }
                    }).queue()
                } else {
                    event.sendFailure("Couldn't find that city or town!").queue()
                }
            } catch (e: Exception) {
                event.sendFailure("Something went wrong! Try again or contact developers!").queue()
            }
        } else {
            event.sendFailure("You specified no content!").queue()
        }
    }
}