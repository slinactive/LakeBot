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

import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.extensions.*
import io.ilakeful.lakebot.utils.ImageUtils

import khttp.get

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

import java.awt.Color

class ColorCommand : Command {
    override val name = "color"
    override val aliases = listOf("colorinfo", "color-info")
    override val description = "The command sending short information about the specified color"
    override val cooldown = 3L
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <HEX code>"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        fun Color.toCmyk(): IntArray {
            val rgbArray = arrayOf(red, green, blue).map { it / 255f }
            val k = 1f - (rgbArray.max() ?: 0f)
            val arr = mutableListOf<Float>().apply {
                for (digit in rgbArray) {
                    this += (1f - digit - k) / (1f - k) * 100
                }
                this += k * 100
            }
            return arr.map { Math.round(it) }.toIntArray()
        }
        if (args.isNotEmpty()) {
            if (args[0] matches "#?(([A-Fa-f\\d]){3}|([A-Fa-f\\d]){6})".toRegex()) {
                val request = get("http://www.thecolorapi.com/id?hex=${args[0].removePrefix("#").toLowerCase()}", headers = emptyMap())
                val json = request.jsonObject
                val name = json.getJSONObject("name").getString("value")
                val closestHex = json.getJSONObject("name").getString("closest_named_hex").toLowerCase()
                val hex = json.getJSONObject("hex").getString("value").toLowerCase()
                val hsl = json.getJSONObject("hsl")
                val clear = hex.removePrefix("#")
                val color = Color.decode(hex)
                val image = ImageUtils.getColorImage(color, 250, 250)
                val embed = buildEmbed {
                    color { color }
                    author { name }
                    image { "attachment://$clear.png" }
                    if (closestHex == hex) {
                        description {
                            """**HEX**: $hex
                                |**RGB**: ${color.red}, ${color.green}, ${color.blue}
                                |**DEC**: ${clear.toInt(16)}
                                |**CMYK**: ${color.toCmyk().joinToString("%, ", postfix = "%")}
                                |**HSL**: ${hsl["h"]}, ${hsl["s"]}, ${hsl["l"]}
                            """.trimMargin()
                        }
                    } else {
                        field(title = "Info:") {
                            """**HEX**: $hex
                                |**RGB**: ${color.red}, ${color.green}, ${color.blue}
                                |**DEC**: ${clear.toInt(16)}
                                |**CMYK**: ${color.toCmyk().joinToString("%, ", postfix = "%")}
                                |**HSL**: ${hsl["h"]}, ${hsl["s"]}, ${hsl["l"]}
                            """.trimMargin()
                        }
                        field(title = "Closest Color:") {
                            val r = get("http://www.thecolorapi.com/id?hex=${closestHex.removePrefix("#")}", headers = emptyMap())
                            val closestHsl = r.jsonObject.getJSONObject("hsl")
                            val c = Color.decode(closestHex)
                            """**HEX**: $closestHex
                                |**RGB**: ${c.red}, ${c.green}, ${c.blue}
                                |**DEC**: ${closestHex.removePrefix("#").toInt(16)}
                                |**CMYK**: ${c.toCmyk().joinToString("%, ", postfix = "%")}
                                |**HSL**: ${closestHsl["h"]}, ${closestHsl["s"]}, ${closestHsl["l"]}
                            """.trimMargin()
                        }
                    }
                }
                event.channel.sendMessage(embed).addFile(image, "$clear.png").queue()
            } else {
                event.channel.sendFailure("The color is invalid!").queue()
            }
        } else {
            val r = Int.random(0..255)
            val g = Int.random(0..255)
            val b = Int.random(0..255)
            val color = Color(r, g, b)
            invoke(event, arrayOf("#${color.rgb.toHex().takeLast(6)}"))
        }
    }
}