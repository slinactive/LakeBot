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
import io.iskylake.lakebot.USERS_WITH_PROCESSES
import io.iskylake.lakebot.commands.Command
import io.iskylake.lakebot.entities.EventWaiter
import io.iskylake.lakebot.entities.extensions.*

import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent

import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class ServerCommand : Command {
    override val name = "server"
    override val aliases = listOf("serverinfo", "servermenu", "guild", "guildinfo")
    override val description = "The command that sends complete information about current server"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) = if (event.guild.iconUrl !== null) {
        serverMenu(event)
    } else {
        event.channel.sendMessage(serverInfo(event.author) { event.guild }).queue()
    }
    inline fun serverInfo(author: User, lazy: () -> Guild) = buildEmbed {
        val guild = lazy()
        val roles = guild.roles.filter { !it.isPublicRole }
        author(guild.name) { guild.iconUrl }
        color { Immutable.SUCCESS }
        thumbnail { guild.iconUrl }
        footer(author.effectiveAvatarUrl) { "Requested by ${author.tag}" }
        timestamp()
        field(true, "Name:") { guild.name.escapeDiscordMarkdown() }
        field(true, "ID:") { guild.id }
        field(true, "Creation Date:") {
            guild.creationTime.format(DateTimeFormatter.RFC_1123_DATE_TIME).replace(" GMT", "")
        }
        field(true, "Online Members:") {
            "${guild.memberCache.count { it.onlineStatus == OnlineStatus.ONLINE }}/${guild.memberCache.size()}"
        }
        field(true, "Humans:") {
            "${guild.memberCache.count { !it.user.isBot }}/${guild.memberCache.size()}"
        }
        field(true, "Bots:") {
            "${guild.memberCache.count { it.user.isBot }}/${guild.memberCache.size()}"
        }
        field(true, "Owner:") { guild.owner.user.tag.escapeDiscordMarkdown() }
        field(true, "Region:") { guild.region.getName() }
        field(true, "Emotes:") { guild.emoteCache.size().toString() }
        field(true, "Categories:") { guild.categoryCache.size().toString() }
        field(true, "Text Channels:") { guild.textChannelCache.size().toString() }
        field(true, "Voice Channels:") { guild.voiceChannelCache.size().toString() }
        field(true, "Prefix:") { guild.prefix }
        field(true, "Verification Level:") {
            guild.verificationLevel.name.capitalizeAll(true).replace("_", "")
        }
        field(roles.size > 2, if (roles.isEmpty()) "Roles:" else "Roles (${roles.size}):") {
            when {
                roles.isEmpty() -> "No roles"
                roles.mapNotNull { it.name.escapeDiscordMarkdown() }.joinToString().length > 1024 -> "Too many roles to display"
                else -> roles.mapNotNull { it.name.escapeDiscordMarkdown() }.joinToString()
            }
        }
        field(guild.emoteCache.size() > 2, if (guild.emoteCache.isEmpty) "Emotes:" else "Emotes (${guild.emoteCache.size()}):") {
            when {
                guild.emoteCache.isEmpty -> "No roles"
                guild.emoteCache.mapNotNull { it.asMention }.joinToString().length > 1024 -> "Too many emotes to display"
                else -> guild.emoteCache.mapNotNull { it.asMention }.joinToString()
            }
        }
    }
    suspend fun serverMenu(event: MessageReceivedEvent) = event.channel.sendMessage(buildEmbed {
        color { Immutable.SUCCESS }
        author { "Select The Action:" }
        description { "\u0031\u20E3 \u2014 Get Information\n\u0032\u20E3 \u2014 Get Avatar" }
    }).await {
        USERS_WITH_PROCESSES += event.author
        it.addReaction("\u0031\u20E3").complete()
        it.addReaction("\u0032\u20E3").complete()
        it.addReaction("\u274C").complete()
        val e = EventWaiter.receiveEventRaw<MessageReactionAddEvent>(1, TimeUnit.MINUTES) { e ->
            val name = e.reactionEmote.name
            val condition = name == "\u0031\u20E3" || name == "\u0032\u20E3" || name == "\u274C"
            e.messageIdLong == it.idLong && e.user == event.author && condition
        }
        if (e !== null) {
            when (e.reactionEmote.name) {
                "\u0031\u20E3" -> {
                    it.delete().queue()
                    USERS_WITH_PROCESSES -= event.author
                    val embed = serverInfo(event.author) { event.guild }
                    event.channel.sendMessage(embed).queue()
                }
                "\u0032\u20E3" -> {
                    it.delete().queue()
                    val embed = buildEmbed {
                        author("Server Avatar:", event.guild.iconUrl) { null }
                        image { "${event.guild.iconUrl}?size=2048" }
                        footer(event.author.effectiveAvatarUrl) { "Requested by ${event.author.tag}" }
                    }
                    event.channel.sendMessage(embed).queue()
                    USERS_WITH_PROCESSES -= event.author
                }
                "\u274C" -> {
                    it.delete().queue()
                    event.sendSuccess("Process successfully stopped!").queue()
                    USERS_WITH_PROCESSES -= event.author
                }
            }
        } else {
            it.delete().queue()
            event.sendError("Time is up!").queue()
            USERS_WITH_PROCESSES -= event.author
        }
    }
}