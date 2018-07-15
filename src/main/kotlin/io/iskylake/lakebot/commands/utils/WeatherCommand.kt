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
import io.iskylake.weather.Units
import io.iskylake.weather.lakeWeather

import net.dv8tion.jda.core.events.message.MessageReceivedEvent

import kotlin.math.round

class WeatherCommand : Command {
    override val name = "weather"
    override val description = "The command that displays weather in the specified city/town"
    override val cooldown = 3L
    override val usage = { it: String -> "${super.usage(it)} <city/town>" }
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val arguments = event.argsRaw
        if (arguments !== null) {
            try {
                val api = lakeWeather {
                    units { Units.IMPERIAL }
                    key { Immutable.WEATHER_API_KEY }
                }
                val forecast = api.getForecast(arguments)
                if (forecast !== null) {
                    val city = "${forecast.city.name}, ${forecast.system.countryCode}"
                    val fahrenheit = forecast.temperature.temperature.toShort()
                    val celsius = round((fahrenheit - 32) / 1.8).toShort()
                    val humidity = forecast.temperature.humidity
                    val pressure = forecast.temperature.pressure?.toShort()
                    val speed = "${round(forecast.wind.speed).toShort()} mph"
                    val (degrees, name) = forecast.wind.direction to forecast.wind.directionName
                    val condition = forecast.weather.main
                    val cloudiness = "${forecast.clouds.cloudiness}%"
                    val link = "https://openweathermap.org/city/${forecast.city.id}"
                    val embed = buildEmbed {
                        author("Weather - $city", link) { event.selfUser.effectiveAvatarUrl }
                        color { Immutable.SUCCESS }
                        thumbnail { event.selfUser.effectiveAvatarUrl }
                        field(true, "Temperature (°F):") { "$fahrenheit°F" }
                        field(true, "Temperature (°C):") { "$celsius°C" }
                        field(true, "Wind Direction:") {
                            if (name !== null && degrees !== null) "$name ($degrees°)" else "N/A"
                        }
                        field(true, "Wind Speed:") { speed }
                        field(true, "Humidity:") { if (humidity !== null) "$humidity%" else "N/A" }
                        field(true, "Pressure:") { if (pressure !== null) "$pressure hPA" else "N/A" }
                        field(true, "Condition:") { condition }
                        field(true, "Cloudiness:") { cloudiness }
                        footer(event.author.effectiveAvatarUrl) { "Requested by ${event.author.tag}" }
                        timestamp()
                    }
                    event.channel.sendMessage(embed).queue()
                } else {
                    event.sendError("Couldn't find that town!").queue()
                }
            } catch (e: Exception) {
                event.sendError("Something went wrong!").queue()
            }
        } else {
            event.sendError("You specified no query!").queue()
        }
    }
}