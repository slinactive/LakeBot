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

import io.iskylake.lakebot.commands.Command
import io.iskylake.lakebot.entities.extensions.*
import net.dv8tion.jda.core.entities.MessageEmbed

import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class AvatarCommand : Command {
    override val name = "avatar"
    override val description = "The command that sends your avatar or the avatar of the specified member"
    override val usage = fun(prefix: String): String {
        val command = super.usage(prefix)
        val dash = '\u2014'
        return """
            |$command $dash your avatar
            |$command <user> $dash user avatar
        """.trimMargin()
    }
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val arguments = event.argsRaw
        lateinit var embed: MessageEmbed
        if (arguments !== null) {
            when {
                event.message.mentionedMembers.isNotEmpty() -> {
                    val user = event.message.mentionedMembers[0].user
                    embed = buildEmbed {
                        author { "Avatar of:" }
                        description { "[${user.tag.escapeDiscordMarkdown()}](${user.effectiveAvatarUrl}?size=2048)" }
                        image { "${user.effectiveAvatarUrl}?size=2048" }
                        footer(event.author.effectiveAvatarUrl) { "Requested by ${event.author.tag}" }
                    }
                    event.channel.sendMessage(embed).queue()
                }
                event.guild.searchMembers(arguments).isNotEmpty() -> {
                    val user = event.guild.searchMembers(arguments)[0].user
                    embed = buildEmbed {
                        author { "Avatar of:" }
                        description { "[${user.tag.escapeDiscordMarkdown()}](${user.effectiveAvatarUrl}?size=2048)" }
                        image { "${user.effectiveAvatarUrl}?size=2048" }
                        footer(event.author.effectiveAvatarUrl) { "Requested by ${event.author.tag}" }
                    }
                    event.channel.sendMessage(embed).queue()
                }
                else -> event.sendError("Couldn't find that user!").queue()
            }
        } else {
            embed = buildEmbed {
                author { event.author.tag }
                description { "[Your avatar:](${event.author.effectiveAvatarUrl}?size=2048)" }
                image { "${event.author.effectiveAvatarUrl}?size=2048" }
                footer(event.author.effectiveAvatarUrl) { "Requested by ${event.author.tag}" }
            }
            event.channel.sendMessage(embed).queue()
        }
    }
}