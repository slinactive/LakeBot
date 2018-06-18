/*
 * Copyright 2017-2018 (c) Alexander "ISkylake" Shevchenko
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.iskylake.lakebot.commands.utils

import io.iskylake.lakebot.Immutable
import io.iskylake.lakebot.commands.Command
import io.iskylake.lakebot.entities.extensions.*

import net.dv8tion.jda.core.events.message.MessageReceivedEvent

import khttp.get

import java.net.URLEncoder

class ShortenerCommand : Command {
    override val name = "shortener"
    override val aliases = listOf("shorten", "shortenlink", "shorturl")
    override val description = "The command that creates short URL from your link"
    override val cooldown = 3L
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) = if (args.isNotEmpty()) {
        try {
            val api = "https://is.gd/create.php?format=simple&url=${URLEncoder.encode(args[0], "UTF-8")}"
            val response = get(api)
            if ("${response.statusCode}".startsWith('2')) {
                val embed = buildEmbed {
                    author("LakeShortener") { event.selfUser.effectiveAvatarUrl }
                    color { Immutable.SUCCESS }
                    description { response.text }
                }
                event.channel.sendMessage(embed).queue()
            } else {
                event.sendError("That's not a valid URL!").queue()
            }
        } catch (e: Exception) {
            event.sendError("Something went wrong!").queue()
        }
    } else {
        event.sendError("You specified no URL!").queue()
    }
}