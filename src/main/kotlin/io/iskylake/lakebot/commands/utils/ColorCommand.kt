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

import io.iskylake.lakebot.commands.Command
import io.iskylake.lakebot.entities.extensions.*
import io.iskylake.lakebot.utils.ImageUtils

import khttp.get

import net.dv8tion.jda.core.events.message.MessageReceivedEvent

import java.awt.Color
import java.util.*

import kotlin.math.max

class ColorCommand : Command {
    override val name = "color"
    override val description = "The command that sends a short information about the specified color"
    override val cooldown = 3L
    override val usage = { it: String -> "${super.usage(it)} <HEX code>" }
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        fun Color.toCmyk(): IntArray {
            val r = red / 255f
            val g = green / 255f
            val b = blue / 255f
            val k = 1.0f - max(r, max(g, b))
            val c = (1f - r - k) / (1f - k)
            val m = (1f - g - k) / (1f - k)
            val y = (1f - b - k) / (1f - k)
            return arrayOf(c * 100, m * 100, y * 100, k * 100).map { Math.round(it) }.toIntArray()
        }
        if (args.isNotEmpty()) {
            if (args[0] matches "#?(([A-Fa-f\\d]){3}|([A-Fa-f\\d]){6})".toRegex()) {
                val request = get("http://www.thecolorapi.com/id?hex=${args[0].removePrefix("#").toLowerCase()}")
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
                    field(true, "Info:") {
                        """**HEX**: $hex
                            |**RGB**: ${color.red}, ${color.green}, ${color.blue}
                            |**DEC**: ${clear.toInt(16)}
                            |**CMYK**: ${color.toCmyk().joinToString()}
                            |**HSL**: ${hsl["h"]}, ${hsl["s"]}, ${hsl["l"]}
                        """.trimMargin()
                    }
                    if (closestHex != hex) {
                        field(true, "Closest Color:") {
                            val r = get("http://www.thecolorapi.com/id?hex=${closestHex.removePrefix("#")}")
                            val closestHsl = r.jsonObject.getJSONObject("hsl")
                            val c = Color.decode(closestHex)
                            """**HEX**: $closestHex
                                |**RGB**: ${c.red}, ${c.green}, ${c.blue}
                                |**DEC**: ${closestHex.removePrefix("#").toInt(16)}
                                |**CMYK**: ${c.toCmyk().joinToString()}
                                |**HSL**: ${closestHsl["h"]}, ${closestHsl["s"]}, ${closestHsl["l"]}
                            """.trimMargin()
                        }
                    }
                }
                event.channel.sendMessage(embed).addFile(image, "$clear.png").queue()
            } else {
                event.sendError("That color isn't a valid one!").queue()
            }
        } else {
            val r = Random().nextInt(255)
            val g = Random().nextInt(255)
            val b = Random().nextInt(255)
            val color = Color(r, g, b)
            invoke(event, arrayOf("#${color.rgb.toHex().removePrefix("ffffffffff")}"))
        }
    }
}