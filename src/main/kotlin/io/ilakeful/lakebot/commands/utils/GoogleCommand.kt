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

import khttp.get

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

import org.json.JSONArray

import java.io.IOException
import java.net.URLEncoder

class GoogleCommand : Command {
    override val name = "google"
    override val aliases = listOf("search")
    override val description = "The command searching something in Google by the specified query"
    override val cooldown = 3L
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <query>"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val arguments = event.argsRaw
        if (arguments !== null) {
            try {
                val results = searchAsJSON(arguments, isNSFW = event.textChannel.isNSFW)
                val embed = buildEmbed {
                    color { Immutable.SUCCESS }
                    author("LakeSearch:") { event.selfUser.effectiveAvatarUrl }
                    footer(event.author.effectiveAvatarUrl) { "Requested by ${event.author.asTag}" }
                    for (index in 0 until results.count()) {
                        val json = results.getJSONObject(index)
                        val title = json.getString("title")
                        val link = json.getString("link").replace(")", "\\)")
                        appendln { "\u2022 **[$title]($link)**" }
                    }
                }
                event.channel.sendMessage(embed).queue()
            } catch (e: Exception) {
                event.channel.sendFailure("No results found by the query!").queue()
            }
        } else {
            event.channel.sendFailure("You haven't specified any arguments!").queue()
        }
    }
    @Throws(IOException::class)
    fun searchAsJSON(query: String, isNSFW: Boolean, limit: Long = 5L): JSONArray {
        val api = "https://www.googleapis.com"
        val endpoint = "/customsearch/v1"
        for (key in Immutable.GOOGLE_API_KEYS) {
            try {
                val params = "?safe=${if (isNSFW) "off" else "medium"}&cx=018291224751151548851%3Ajzifriqvl1o&key=$key&num=$limit"
                val queryParam = "&q=${URLEncoder.encode(query, "UTF-8")}"
                val req = get("$api$endpoint$params$queryParam", headers = emptyMap())
                if ("${req.statusCode}".startsWith('2')) {
                    return req.jsonObject.getJSONArray("items")
                } else {
                    continue
                }
            } catch (e: Exception) {
                throw IOException(e)
            }
        }
        throw IOException("Couldn't access Google!")
    }
}