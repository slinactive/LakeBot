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

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Activity.ActivityType.*
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent

import org.ocpsoft.prettytime.PrettyTime

import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit

class UserCommand : Command {
    override val name = "user"
    override val aliases = listOf("userinfo", "usermenu", "user-info", "user-menu", "whois", "who-is")
    override val description = "The command sending complete information about your account or an account of the specified user"
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <user (optional)>"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        event.retrieveMembers(command = this, massMention = false, useAuthorIfNoArguments = true) {
            userMenu(event, it)
        }
    }
    inline fun userInfo(author: User, lazy: () -> Member) = buildEmbed {
        val member = lazy()
        val user = member.user
        val hasPermissionsOrNoRoles = member.isOwner
                || member.hasPermission(Permission.ADMINISTRATOR)
                || member.hasPermission(Permission.MANAGE_SERVER)
                || member.roles.isEmpty()
        val prettyTime = PrettyTime()
        val roles = member.roles.filter { !it.isPublicRole }
        author(user.asTag) { user.effectiveAvatarUrl }
        color { Immutable.SUCCESS }
        thumbnail { user.effectiveAvatarUrl }
        footer(author.effectiveAvatarUrl) { "Requested by ${author.asTag}" }
        timestamp()
        field(true, "Online Status:") {
            member.onlineStatus.name
                    .split("_")
                    .joinToString(separator = " ")
                    .capitalizeAll(isForce = true)
        }
        field(true, if (member.activities.isNotEmpty()) when (member.activities.first().type) {
            LISTENING -> "Listening To:"
            STREAMING -> "Streaming:"
            WATCHING -> "Watching:"
            DEFAULT -> "Playing:"
            else -> "Game Status:"
        } else "Game Status:") { member.activities.firstOrNull()?.name ?: "None" }
        field(true, "Creation Date:") {
            val date = user.timeCreated
            val formatted = date.format(DateTimeFormatter.RFC_1123_DATE_TIME).removeSuffix("GMT").trim()
            val ago = prettyTime.format(Date.from(date.toInstant()))
            "$formatted ($ago)"
        }
        field(true, "Join Date:") {
            val date = member.timeJoined
            val formatted = date.format(DateTimeFormatter.RFC_1123_DATE_TIME).removeSuffix("GMT").trim()
            val ago = prettyTime.format(Date.from(date.toInstant()))
            "$formatted ($ago)"
        }
        field(true, "ID:") { user.id }
        field(true, "Color:") { member.color?.rgb?.toHex()?.takeLast(6)?.prepend("#") ?: "Default" }
        field(true, "Username:") { user.name.escapeDiscordMarkdown() }
        field(true, "Nickname:") { member.nickname?.escapeDiscordMarkdown() ?: "None" }
        field(true, "Booster:") {
            val bool = member in member.guild.boosters
            bool.asString()
        }
        field(title = "Join Order:") { member.joinOrder }
        field(
                inline = if (!hasPermissionsOrNoRoles) {
                    roles.size <= 2
                } else true,
                title = "Join Position:",
                desc = { "#${member.joinPosition}" }
        )
        if (hasPermissionsOrNoRoles) {
            field(true, "Acknowledgements:") {
                when {
                    member.isOwner -> "Server Owner"
                    member.hasPermission(Permission.ADMINISTRATOR) -> "Server Administrator"
                    member.hasPermission(Permission.MANAGE_SERVER) -> "Server Moderator"
                    else -> "Unknown"
                }
            }
        }
        field(roles.size <= 2, if (roles.isEmpty()) "Roles:" else "Roles (${roles.size}):") {
            when {
                roles.isEmpty() -> "None"
                roles.mapNotNull { it.name.escapeDiscordMarkdown() }.joinToString().length > 1024 -> "Too many roles to display"
                else -> roles.mapNotNull { it.name.escapeDiscordMarkdown() }.joinToString()
            }
        }
        if (member.keyPermissions.isNotEmpty()) {
            field(title = "Key Permissions:") { member.keyPermissions.map { it.getName() }.joinToString() }
        }
    }
    suspend fun userMenu(event: MessageReceivedEvent, member: Member) = event.channel.sendEmbed {
        color { Immutable.SUCCESS }
        author { "Select Action:" }
        description { "\u0031\u20E3 \u2014 Get Information\n\u0032\u20E3 \u2014 Get Avatar" }
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
                    val embed = userInfo(event.author) { member }
                    event.channel.sendMessage(embed).queue()
                }
                "\u0032\u20E3" -> {
                    it.delete().queue()
                    val embed = buildEmbed {
                        author { "Avatar of:" }
                        description { "[${member.user.asTag.escapeDiscordMarkdown()}](${member.user.effectiveAvatarUrl}?size=2048)" }
                        image { "${member.user.effectiveAvatarUrl}?size=2048" }
                        footer(event.author.effectiveAvatarUrl) { "Requested by ${event.author.asTag}" }
                    }
                    event.channel.sendMessage(embed).queue()
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