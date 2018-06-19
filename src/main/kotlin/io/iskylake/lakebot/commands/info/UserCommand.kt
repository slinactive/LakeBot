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

package io.iskylake.lakebot.commands.info

import io.iskylake.lakebot.Immutable
import io.iskylake.lakebot.USERS_WITH_PROCESSES
import io.iskylake.lakebot.commands.Command
import io.iskylake.lakebot.commands.CommandCategory
import io.iskylake.lakebot.entities.EventWaiter
import io.iskylake.lakebot.entities.extensions.*

import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent

import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class UserCommand : Command {
    override val name = "user"
    override val aliases = listOf("userinfo", "usermenu")
    override val description = "The command that sends complete information about your account or the account of the specified user"
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <user (optional)>"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val arguments = event.argsRaw
        if (arguments !== null) {
            when {
                event.message.mentionedMembers.isNotEmpty() -> userMenu(event, event.message.mentionedMembers[0])
                event.guild.searchMembers(arguments).isNotEmpty() -> {
                    val list = event.guild.searchMembers(arguments).take(5)
                    if (list.size > 1) {
                        event.channel.sendMessage(buildEmbed {
                            color { Immutable.SUCCESS }
                            author("Select The User:") { event.selfUser.effectiveAvatarUrl }
                            for ((index, member) in list.withIndex()) {
                                appendln { "${index + 1}. ${member.user.tag}" }
                            }
                            footer { "Type in \"exit\" to kill the process" }
                        }).await {
                            USERS_WITH_PROCESSES += event.author
                            selectUser(event, it, list)
                        }
                    } else {
                        userMenu(event, list[0])
                    }
                }
                args[0] matches Regex.DISCORD_ID && event.guild.getMemberById(args[0]) !== null -> {
                    val member = event.guild.getMemberById(args[0])
                    userMenu(event, member)
                }
                else -> event.sendError("Couldn't find that user!").queue()
            }
        } else {
            userMenu(event, event.member)
        }
    }
    inline fun userInfo(lazy: () -> Member) = buildEmbed {
        val member = lazy()
        val user = member.user
        val hasPermissions = member.isOwner || member.hasPermission(Permission.ADMINISTRATOR) || member.hasPermission(Permission.MANAGE_SERVER) || member.roles.isEmpty()
        val roles = member.roles.filter { !it.isPublicRole }
        author(user.tag) { user.effectiveAvatarUrl }
        color { Immutable.SUCCESS }
        thumbnail { user.effectiveAvatarUrl }
        footer(user.effectiveAvatarUrl) { "Requested by ${user.tag}" }
        timestamp()
        field(true, "Online Status:") { member.onlineStatus.name.replace("_", " ").capitalizeAll(true) }
        field(true, if (member.game !== null) when (member.game) {
            Game.GameType.LISTENING -> "Listening To:"
            Game.GameType.STREAMING -> "Streaming:"
            Game.GameType.WATCHING -> "Watching:"
            else -> "Playing:"
        } else "Game Status:") { member.game?.name ?: "None" }
        field(true, "Creation Date:") { user.creationTime.format(DateTimeFormatter.RFC_1123_DATE_TIME).replace(" GMT", "") }
        field(true, "Join Date:") { member.joinDate.format(DateTimeFormatter.RFC_1123_DATE_TIME).replace(" GMT", "") }
        field(true, "ID:") { user.id }
        field(true, "Color:") { member.color?.rgb?.toHex()?.takeLast(6)?.prepend("#") ?: "Default" }
        field(true, "Username:") { user.name.escapeDiscordMarkdown() }
        field(true, "Nickname:") { member.nickname?.escapeDiscordMarkdown() ?: "No Nickname" }
        field(title = "Join Order:") { member.joinOrder }
        field(hasPermissions, "Join Position:") { "#${member.joinPosition}" }
        if (hasPermissions) {
            field(true, "Acknowledgements:") {
                when {
                    member.isOwner -> "Server Owner"
                    member.hasPermission(Permission.ADMINISTRATOR) -> "Server Admin"
                    member.hasPermission(Permission.MANAGE_SERVER) -> "Server Moderator"
                    else -> "Unknown"
                }
            }
        }
        field(roles.size > 2, if (roles.isEmpty()) "Roles:" else "Roles (${roles.size}):") {
            when {
                roles.isEmpty() -> "No"
                roles.mapNotNull { it.name.escapeDiscordMarkdown() }.joinToString().length > 1024 -> "Too many roles to display"
                else -> roles.mapNotNull { it.name.escapeDiscordMarkdown() }.joinToString()
            }
        }
        if (member.roles.isNotEmpty()) {
            field(true, "Highest Role:") { roles[0].name.escapeDiscordMarkdown() }
        }
        if (member.keyPermissions.isNotEmpty()) {
            field(title = "Key Permissions:") { member.keyPermissions.mapNotNull { it.getName() }.joinToString() }
        }
    }
    suspend fun userMenu(event: MessageReceivedEvent, member: Member) = event.channel.sendMessage(buildEmbed {
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
                    val embed = userInfo { member }
                    event.channel.sendMessage(embed).queue()
                }
                "\u0032\u20E3" -> {
                    it.delete().queue()
                    val embed = buildEmbed {
                        author { "Avatar of:" }
                        description { "[${member.user.tag.escapeDiscordMarkdown()}](${member.user.effectiveAvatarUrl}?size=2048)" }
                        image { "${member.user.effectiveAvatarUrl}?size=2048" }
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
    suspend fun selectUser(event: MessageReceivedEvent, msg: Message, members: List<Member>) {
        val c = event.channel.awaitMessage(event.author)?.contentRaw
        if (c !== null) {
            if (c.isInt) {
                if (c.toInt() in 1..members.size) {
                    msg.delete().queue()
                    userMenu(event, members[c.toInt() - 1])
                    USERS_WITH_PROCESSES -= event.author
                } else {
                    event.sendError("Try again!").await { selectUser(event, msg, members) }
                }
            } else if (c.toLowerCase() == "exit") {
                msg.delete().queue()
                USERS_WITH_PROCESSES -= event.author
                event.sendSuccess("Process successfully stopped!").queue()
            } else {
                event.sendError("Try again!").await { selectUser(event, msg, members) }
            }
        } else {
            msg.delete().queue()
            USERS_WITH_PROCESSES -= event.author
            event.sendError("Time is up!").queue()
        }
    }
}