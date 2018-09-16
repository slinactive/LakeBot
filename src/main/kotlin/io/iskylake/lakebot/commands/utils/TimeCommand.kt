/*
 * Copyright 2017-2018 (c) Alexander "ISkylake" Shevchenko
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

package io.iskylake.lakebot.commands.utils

import io.iskylake.lakebot.Immutable
import io.iskylake.lakebot.commands.Command
import io.iskylake.lakebot.entities.extensions.*
import io.iskylake.weather.lakeWeather

import net.dv8tion.jda.core.events.message.MessageReceivedEvent

import khttp.get

class TimeCommand : Command {
    override val name = "time"
    override val description = "The command that sends specified location's current time"
    override val cooldown = 3L
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val arguments = event.argsRaw
        if (arguments !== null) {
            try {
                val api = lakeWeather(token = Immutable.WEATHER_API_KEY)
                val forecast = api[arguments]
                if (forecast !== null) {
                    val (longitude, latitude) = forecast.coordinates
                    val url = "http://api.geonames.org/timezoneJSON?lat=$latitude&lng=$longitude&username=${Immutable.GEONAME_API_USER}"
                    val response = get(url).jsonObject
                    val time = response.getString("time").takeLast(5)
                    event.sendMessage(buildEmbed {
                        color { Immutable.SUCCESS }
                        author { "${forecast.city.name} Time:" }
                        description { time }
                    }).queue()
                } else {
                    event.sendError("Couldn't find that city or town!").queue()
                }
            } catch (e: Exception) {
                event.sendError("Something went wrong! Try again or contact developers!").queue()
            }
        } else {
            event.sendError("You specified no content!").queue()
        }
    }
}