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

class UrbanCommand : Command {
    override val name = "urban"
    override val description = "The command that searches meaning of the specified word on Urban"
    override val cooldown = 3L
    override val usage = { it: String -> "${super.usage(it)} <term>" }
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val arguments = event.argsRaw
        if (arguments !== null) {
            try {
                val base = "http://api.urbandictionary.com/v0"
                val endpoint = "$base/define?term=${URLEncoder.encode(arguments, "UTF-8")}"
                val response = get(endpoint)
                val json = response.jsonObject
                if (json.getJSONArray("list").toList().isEmpty()) {
                    event.sendError("Couldn't find that on Urban!").queue()
                } else {
                    val embed = buildEmbed {
                        val result = json.getJSONArray("list").getJSONObject(0)
                        val word: String = result.getString("word")
                        val meaning: String = result.getString("definition")
                        val example: String = result.getString("example")
                        val author: String = result.getString("author")
                        val like: Int = result.getInt("thumbs_up")
                        val dislike: Int = result.getInt("thumbs_down")
                        val link = "https://www.urbandictionary.com/define.php?term=${URLEncoder.encode(arguments, "UTF-8")}"
                        author("Result For $word:", link) { event.selfUser.effectiveAvatarUrl }
                        color { Immutable.SUCCESS }
                        thumbnail { event.selfUser.effectiveAvatarUrl }
                        field(title = "Meaning:") {
                            when {
                                meaning.isEmpty() -> "No meaning provided"
                                meaning.length > 1024 -> "Too much text to display. Check on the site"
                                else -> meaning.replace(Regex("\\[(.+)]"), "\$1")
                            }
                        }
                        field(title = "Example:") {
                            when {
                                example.isEmpty() -> "No example provided"
                                example.length > 1024 -> "Too much text to display. Check on the site"
                                else -> example.replace(Regex("\\[(.+)]"), "\$1")
                            }
                        }
                        field(title = "Author:") { author }
                        field(true, "Likes:") {
                            "${event.jda.getEmoteById(391954570735124480).asMention} \u2014 $like"
                        }
                        field(true, "Dislikes:") {
                            "${event.jda.getEmoteById(391954570705895434).asMention} \u2014 $dislike"
                        }
                        footer(event.author.effectiveAvatarUrl) { "Requested by ${event.author.tag}" }
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