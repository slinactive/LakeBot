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

package io.ilakeful.lakebot.commands.info

import io.ilakeful.lakebot.Immutable
import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.extensions.*

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class EmotesCommand : Command {
    override val name = "emotes"
    override val description = "The command sending a list of emotes (custom emojis) of the server"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        if (!event.guild.emoteCache.isEmpty) {
            val emotes = event.guild.emoteCache.mapNotNull { it.asMention }.joinToString()
            if (emotes.length <= 2048) {
                buildEmbed {
                    color { Immutable.SUCCESS }
                    author("Emotes of ${event.guild.name.escapeDiscordMarkdown()}:") { event.guild.iconUrl }
                    description { emotes }
                }.let { event.channel.sendMessage(it).queue() }
            } else {
                event.channel.sendFailure("There are too many emotes to display!").queue()
            }
        } else {
            event.channel.sendFailure("No emotes are found on the server!").queue()
        }
    }
}