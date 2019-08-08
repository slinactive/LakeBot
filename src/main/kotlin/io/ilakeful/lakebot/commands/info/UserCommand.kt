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
import io.ilakeful.lakebot.WAITER_PROCESSES
import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.EventWaiter
import io.ilakeful.lakebot.entities.WaiterProcess
import io.ilakeful.lakebot.entities.extensions.*

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent

import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class UserCommand : Command {
    override val name = "user"
    override val aliases = listOf("userinfo", "usermenu", "user-info", "user-menu")
    override val description = "The command sending complete information about your account or an account of the specified user"
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <user (optional)>"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val arguments = event.argsRaw
        val process = WaiterProcess(mutableListOf(event.author), event.textChannel)
        if (arguments !== null) {
            when {
                event.message.mentionedMembers.isNotEmpty() -> userMenu(event, event.message.mentionedMembers[0], process)
                event.guild.getMemberByTagSafely(arguments) !== null -> userMenu(event, event.guild.getMemberByTagSafely(arguments)!!, process)
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
                            WAITER_PROCESSES += process
                            selectUser(event, it, list, process)
                        }
                    } else {
                        userMenu(event, list[0], process)
                    }
                }
                args[0] matches Regex.DISCORD_ID && event.guild.getMemberById(args[0]) !== null -> {
                    val member = event.guild.getMemberById(args[0])
                    userMenu(event, member!!, process)
                }
                else -> event.sendFailure("Couldn't find that user!").queue()
            }
        } else {
            userMenu(event, event.member!!, process)
        }
    }
    inline fun userInfo(author: User, lazy: () -> Member) = buildEmbed {
        val member = lazy()
        val user = member.user
        val hasPermissions = member.isOwner || member.hasPermission(Permission.ADMINISTRATOR) || member.hasPermission(Permission.MANAGE_SERVER) || member.roles.isEmpty()
        val roles = member.roles.filter { !it.isPublicRole }
        author(user.tag) { user.effectiveAvatarUrl }
        color { Immutable.SUCCESS }
        thumbnail { user.effectiveAvatarUrl }
        footer(author.effectiveAvatarUrl) { "Requested by ${author.tag}" }
        timestamp()
        field(true, "Online Status:") { member.onlineStatus.name.replace("_", " ").capitalizeAll(true) }
        field(true, if (member.activities.isNotEmpty()) when (member.activities.first().type) {
            Activity.ActivityType.LISTENING -> "Listening To:"
            Activity.ActivityType.STREAMING -> "Streaming:"
            Activity.ActivityType.WATCHING -> "Watching:"
            else -> "Playing:"
        } else "Game Status:") { member.activities.firstOrNull()?.name ?: "None" }
        field(true, "Creation Date:") { user.timeCreated.format(DateTimeFormatter.RFC_1123_DATE_TIME).replace(" GMT", "") }
        field(true, "Join Date:") { member.timeJoined.format(DateTimeFormatter.RFC_1123_DATE_TIME).replace(" GMT", "") }
        field(true, "ID:") { user.id }
        field(true, "Color:") { member.color?.rgb?.toHex()?.takeLast(6)?.prepend("#") ?: "Default" }
        field(true, "Username:") { user.name.escapeDiscordMarkdown() }
        field(true, "Nickname:") { member.nickname?.escapeDiscordMarkdown() ?: "No Nickname" }
        field(title = "Join Order:") { member.joinOrder }
        if (hasPermissions) {
            field(true, "Join Position:") { "#${member.joinPosition}" }
            field(true, "Acknowledgements:") {
                when {
                    member.isOwner -> "Server Owner"
                    member.hasPermission(Permission.ADMINISTRATOR) -> "Server Admin"
                    member.hasPermission(Permission.MANAGE_SERVER) -> "Server Moderator"
                    else -> "Unknown"
                }
            }
        } else {
            field(roles.size <= 2, "Join Position:") { "#${member.joinPosition}" }
        }
        field(roles.size <= 2, if (roles.isEmpty()) "Roles:" else "Roles (${roles.size}):") {
            when {
                roles.isEmpty() -> "No roles"
                roles.mapNotNull { it.name.escapeDiscordMarkdown() }.joinToString().length > 1024 -> "Too many roles to display"
                else -> roles.mapNotNull { it.name.escapeDiscordMarkdown() }.joinToString()
            }
        }
        if (roles.size >= 2) {
            field(true, "Highest Role:") { roles[0].name.escapeDiscordMarkdown() }
        }
        if (member.keyPermissions.isNotEmpty()) {
            field(title = "Key Permissions:") { member.keyPermissions.mapNotNull { it.getName() }.joinToString() }
        }
    }
    suspend fun userMenu(event: MessageReceivedEvent, member: Member, process: WaiterProcess) = event.channel.sendMessage(buildEmbed {
        color { Immutable.SUCCESS }
        author { "Select the Action:" }
        description { "\u0031\u20E3 \u2014 Get Information\n\u0032\u20E3 \u2014 Get an Avatar" }
    }).await {
        //WAITER_PROCESSES += process
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
                    WAITER_PROCESSES -= process
                    val embed = userInfo(event.author) { member }
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
                    WAITER_PROCESSES -= process
                }
                "\u274C" -> {
                    it.delete().queue()
                    event.sendSuccess("Process successfully stopped!").queue()
                    WAITER_PROCESSES -= process
                }
            }
        } else {
            it.delete().queue()
            event.sendFailure("Time is up!").queue()
            WAITER_PROCESSES -= process
        }
    }
    suspend fun selectUser(event: MessageReceivedEvent, msg: Message, members: List<Member>, process: WaiterProcess) {
        val c = event.channel.awaitMessage(event.author)?.contentRaw
        if (c !== null) {
            if (c.isInt) {
                if (c.toInt() in 1..members.size) {
                    msg.delete().queue()
                    userMenu(event, members[c.toInt() - 1], process)
                    //WAITER_PROCESSES -= process
                } else {
                    event.sendFailure("Try again!").await { selectUser(event, msg, members, process) }
                }
            } else if (c.toLowerCase() == "exit") {
                msg.delete().queue()
                WAITER_PROCESSES -= process
                event.sendSuccess("Process successfully stopped!").queue()
            } else {
                event.sendFailure("Try again!").await { selectUser(event, msg, members, process) }
            }
        } else {
            msg.delete().queue()
            WAITER_PROCESSES -= process
            event.sendFailure("Time is up!").queue()
        }
    }
}