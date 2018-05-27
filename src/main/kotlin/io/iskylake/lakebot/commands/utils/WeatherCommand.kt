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

import khttp.get

import net.dv8tion.jda.core.events.message.MessageReceivedEvent

import java.net.URLEncoder

import kotlin.math.round

class WeatherCommand : Command {
    override val name = "weather"
    override val description = "The command that displays weather in the specified city/town"
    override val cooldown = 3L
    override val usage = { it: String -> "${super.usage(it)} <city/town>" }
    override fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val arguments = event.argsRaw
        if (arguments !== null) {
            try {
                val base = "http://api.openweathermap.org/data/2.5/weather"
                val endpoint = "$base?appid=${Immutable.WEATHER_API_KEY}&units=metric&q=${URLEncoder.encode(arguments, "UTF-8")}"
                val response = get(endpoint)
                if (!response.statusCode.toString().startsWith('2')) {
                    when (response.statusCode) {
                        400, 401 -> return
                        404, 502 -> event.sendError("Couldn't find that town!").queue()
                        else -> return
                    }
                } else {
                    val embed = buildEmbed {
                        val json = response.jsonObject
                        val city = "${json.getString("name")}, ${json.getJSONObject("sys").getString("country")}"
                        val temp = json.getJSONObject("main").getInt("temp")
                        val tempF = (round((temp * 1.8 + 32) * 100) / 100).toInt()
                        val humidity = if (json.getJSONObject("main").has("humidity")) "${json.getJSONObject("main").getInt("humidity")}%" else "N/A"
                        val pressure = if (json.getJSONObject("main").has("pressure")) "${json.getJSONObject("main").getInt("pressure")} hPA" else "N/A"
                        val speed = if (json.getJSONObject("wind").has("speed")) "${Math.round((json.getJSONObject("wind").getInt("speed")) * 3.6)} km/h" else "N/A"
                        val direction = if (json.getJSONObject("wind").has("deg")) "${json.getJSONObject("wind").getInt("deg")}°" else "N/A"
                        val weather = json.getJSONArray("weather").getJSONObject(0).getString("main")
                        author("Weather - $city") {
                            event.selfUser.effectiveAvatarUrl
                        }
                        color {
                            Immutable.SUCCESS
                        }
                        thumbnail {
                            event.selfUser.effectiveAvatarUrl
                        }
                        field(true, "Temperature (°C):") {
                            "$temp°C"
                        }
                        field(true, "Temperature (°F):") {
                            "$tempF°F"
                        }
                        field(true, "Wind Direction:") {
                            direction
                        }
                        field(true, "Wind Speed:") {
                            speed
                        }
                        field(true, "Humidity:") {
                            humidity
                        }
                        field(true, "Pressure:") {
                            pressure
                        }
                        field(true, "Condition:") {
                            weather
                        }
                        footer(event.author.effectiveAvatarUrl) {
                            "Requested by ${event.author.tag}"
                        }
                        timestamp()
                    }
                    event.channel.sendMessage(embed).queue()
                }
            } catch (e: Exception) {
                event.sendError("Something went wrong!").queue()
            }
        } else {
            event.sendError("You specified no query!").queue()
        }
    }
}