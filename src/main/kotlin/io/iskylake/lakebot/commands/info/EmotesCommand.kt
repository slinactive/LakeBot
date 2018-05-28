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

package io.iskylake.lakebot.commands.info

import io.iskylake.lakebot.Immutable
import io.iskylake.lakebot.commands.Command
import io.iskylake.lakebot.entities.extensions.*

import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class EmotesCommand : Command {
    override val name = "emotes"
    override val description = "The command that sends the list of emotes of this server"
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
                event.sendError("There are too many emotes to display!").queue()
            }
        } else {
            event.sendError("There are no emotes!").queue()
        }
    }
}