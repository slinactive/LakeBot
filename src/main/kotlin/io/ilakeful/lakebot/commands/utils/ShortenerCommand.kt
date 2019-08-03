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

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

import khttp.get

import java.net.URLEncoder

class ShortenerCommand : Command {
    override val name = "shorten"
    override val aliases = listOf("shortener", "urlshortener", "shortenlink", "shorturl", "shortenedurl", "url-shortener", "shorten-link", "short-url", "shortened-url")
    override val description = "The command shortening the specified link"
    override val cooldown = 3L
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) = if (args.isNotEmpty()) {
        try {
            val api = "https://is.gd/create.php?format=simple&url=${URLEncoder.encode(args[0], "UTF-8")}"
            val response = get(api, headers = mapOf())
            if ("${response.statusCode}".startsWith('2')) {
                val embed = buildEmbed {
                    author("LakeShortener") { event.selfUser.effectiveAvatarUrl }
                    color { Immutable.SUCCESS }
                    description { response.text }
                }
                event.channel.sendMessage(embed).queue()
            } else {
                event.sendFailure("That's not a valid URL!").queue()
            }
        } catch (e: Exception) {
            event.sendFailure("Something went wrong!").queue()
        }
    } else {
        event.sendFailure("You specified no URL!").queue()
    }
}