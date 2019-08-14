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
import io.ilakeful.lakebot.entities.EventWaiter
import io.ilakeful.lakebot.entities.extensions.*
import io.ilakeful.lakebot.utils.TimeUtils

import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import org.jetbrains.kotlin.backend.common.onlyIf

import org.ocpsoft.prettytime.PrettyTime

import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit

class ServerCommand : Command {
    override val name = "server"
    override val aliases = listOf("serverinfo", "servermenu", "guild", "guildinfo", "server-info", "server-menu", "guild-info")
    override val description = "The command sending complete information about the server"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) = if (event.guild.iconUrl !== null) {
        serverMenu(event)
    } else {
        event.channel.sendMessage(serverInfo(event.author) { event.guild }).queue()
    }
    inline fun serverInfo(author: User, lazy: () -> Guild) = buildEmbed {
        val guild = lazy()
        val roles = guild.roles.filter { !it.isPublicRole }
        val members = guild.memberCache.size()
        val prettyTime = PrettyTime()
        author(guild.name) { guild.iconUrl }
        color { Immutable.SUCCESS }
        thumbnail { guild.iconUrl }
        description { "**Prefix**: ${guild.prefix}" }
        footer(author.effectiveAvatarUrl) { "Requested by ${author.asTag}" }
        timestamp()
        field(true, "Name:") { guild.name.escapeDiscordMarkdown() }
        field(true, "ID:") { guild.id }
        field(true, "Creation Date:") {
            val date = guild.timeCreated
            val formatted = date.format(DateTimeFormatter.RFC_1123_DATE_TIME).removeSuffix("GMT").trim()
            val ago = prettyTime.format(Date.from(date.toInstant()))
            "$formatted ($ago)"
        }
        field(true, "Online Members:") {
            "${guild.memberCache.count { it.onlineStatus == OnlineStatus.ONLINE }}/$members"
        }
        field(true, "Humans:") {
            "${guild.memberCache.count { !it.user.isBot }}/$members"
        }
        field(true, "Bots:") {
            "${guild.memberCache.count { it.user.isBot }}/$members"
        }
        field(true, "Owner:") { guild.owner?.user?.asTag?.escapeDiscordMarkdown() ?: "N/A" }
        field(true, "Region:") { guild.region.getName() }
        field(true, "Emotes:") { guild.emoteCache.size().toString() }
        field(true, "Categories:") { guild.categoryCache.size().toString() }
        field(true, "Text Channels:") { guild.textChannelCache.size().toString() }
        field(true, "Voice Channels:") { guild.voiceChannelCache.size().toString() }
        field(true, "AFK Channel:") { guild.afkChannel?.name ?: "None" }
        field(true, "AFK Timeout:") {
            if (guild.afkChannel !== null) {
                val seconds = guild.afkTimeout.seconds
                TimeUtils.asText(seconds.toLong(), TimeUnit.SECONDS)
            } else "None"
        }
        field(true, "Boosts:") { guild.boostCount.toString() }
        field(true, "Boost Tier:") {
            guild.boostTier.let { "${it.maxBitrate / 1000} kbps, ${it.maxEmotes} emotes" }
        }
        field(true, "Verification Level:") {
            guild.verificationLevel.name
                    .split("_")
                    .joinToString(separator = " ")
                    .capitalizeAll(isForce = true)
        }
        field(true, "Notification Level:") {
            guild.defaultNotificationLevel.name
                    .split("_")
                    .joinToString(separator = " ")
                    .capitalizeAll(isForce = true)
        }
        field(true, "MFA Requirement:") {
            val bool = guild.requiredMFALevel == Guild.MFALevel.TWO_FACTOR_AUTH
            bool.asString()
        }
        field(roles.size > 2, if (roles.isEmpty()) "Roles:" else "Roles (${roles.size}):") {
            when {
                roles.isEmpty() -> "None"
                roles.mapNotNull { it.name.escapeDiscordMarkdown() }.joinToString().length > 1024 -> "Too many roles to display"
                else -> roles.mapNotNull { it.name.escapeDiscordMarkdown() }.joinToString()
            }
        }
        field(guild.emoteCache.size() > 2, if (guild.emoteCache.isEmpty) "Emotes:" else "Emotes (${guild.emoteCache.size()}):") {
            when {
                guild.emoteCache.isEmpty -> "None"
                guild.emoteCache.mapNotNull { it.asMention }.joinToString().length > 1024 -> "Too many emotes to display"
                else -> guild.emoteCache.mapNotNull { it.asMention }.joinToString()
            }
        }
        field(guild.features.size > 3, "Features:") {
            if (guild.features.isNotEmpty()) {
                guild.features.mapNotNull {
                    it.split("_").joinToString(" ") { el ->
                        el.takeIf { ex ->
                            ex !in arrayOf("URL", "VIP")
                        }?.capitalizeAll(isForce = true) ?: el
                    }
                }.joinToString()
            } else "None"
        }
    }
    suspend fun serverMenu(event: MessageReceivedEvent) = event.channel.sendEmbed {
        color { Immutable.SUCCESS }
        author { "Select Action:" }
        description { "\u0031\u20E3 \u2014 Get Information\n\u0032\u20E3 \u2014 Get Icon" }
    }.await {
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
                    val embed = serverInfo(event.author) { event.guild }
                    event.channel.sendMessage(embed).queue()
                }
                "\u0032\u20E3" -> {
                    it.delete().queue()
                    event.channel.sendEmbed {
                        author("Server Icon:", event.guild.iconUrl) { null }
                        image { "${event.guild.iconUrl}?size=2048" }
                        footer(event.author.effectiveAvatarUrl) { "Requested by ${event.author.asTag}" }
                    }.queue()
                }
                "\u274C" -> {
                    it.delete().queue()
                    event.channel.sendSuccess("Successfully canceled!").queue()
                }
            }
        } else {
            it.delete().queue()
            event.channel.sendFailure("Time is up!").queue()
        }
    }
}