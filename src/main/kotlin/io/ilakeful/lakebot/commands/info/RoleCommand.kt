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
import io.ilakeful.lakebot.USERS_WITH_PROCESSES
import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.commands.utils.ColorCommand
import io.ilakeful.lakebot.entities.EventWaiter
import io.ilakeful.lakebot.entities.extensions.*
import io.ilakeful.lakebot.utils.ImageUtils

import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent

import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class RoleCommand : Command {
    override val name = "role"
    override val aliases = listOf("roleinfo", "rolemenu", "role-info", "role-menu")
    override val description = "The command sending complete information about the specified role"
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <role>"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val arguments = event.argsRaw
        if (arguments !== null) {
            when {
                event.message.mentionedRoles.isNotEmpty() -> roleMenu(event, event.message.mentionedRoles[0])
                event.guild.searchRoles(arguments).isNotEmpty() -> {
                    val list = event.guild.searchRoles(arguments).take(5)
                    if (list.size > 1) {
                        event.channel.sendMessage(buildEmbed {
                            color { Immutable.SUCCESS }
                            author("Select The Role:") { event.selfUser.effectiveAvatarUrl }
                            for ((index, role) in list.withIndex()) {
                                appendln { "${index + 1}. ${role.name}" }
                            }
                            footer { "Type in \"exit\" to kill the process" }
                        }).await {
                            USERS_WITH_PROCESSES += event.author
                            selectRole(event, it, list)
                        }
                    } else {
                        roleMenu(event, list[0])
                    }
                }
                args[0] matches Regex.DISCORD_ID && event.guild.getRoleById(args[0]) !== null -> {
                    val role = event.guild.getRoleById(args[0])
                    roleMenu(event, role!!)
                }
                else -> event.sendFailure("Couldn't find that role!").queue()
            }
        } else {
            event.sendFailure("You specified no role!").queue()
        }
    }
    inline fun roleInfo(author: User, lazy: () -> Role) = buildEmbed {
        val role = lazy()
        color { role.color }
        if (role.color !== null) {
            thumbnail { "attachment://${role.color!!.rgb.toHex().takeLast(6)}.png" }
        }
        footer(author.effectiveAvatarUrl) { "Requested by ${author.tag}" }
        timestamp()
        field(true, "Name:") { role.name.escapeDiscordMarkdown() }
        field(true, "ID:") { role.id }
        field(true, "Mention:") { "`${role.asMention}`" }
        field(true, "Members:") { "${role.members.size}" }
        field(true, "Position:") { "#${role.position + 1}" }
        field(true, "Hoisted:") { role.isHoisted.asString() }
        field(true, "Mentionable:") { role.isMentionable.asString() }
        field(true, "Color:") { role.color?.rgb?.toHex()?.takeLast(6)?.prepend("#") ?: "Default" }
        field(true, "Creation Date:") { role.timeCreated.format(DateTimeFormatter.RFC_1123_DATE_TIME).replace(" GMT", "") }
        if (role.keyPermissions.isNotEmpty()) {
            field(title = "Key Permissions:") { role.keyPermissions.mapNotNull { it.getName() }.joinToString() }
        }
    }
    suspend fun roleMenu(event: MessageReceivedEvent, role: Role) = if (role.color === null) {
        USERS_WITH_PROCESSES -= event.author
        val embed = roleInfo(event.author) { role }
        event.channel.sendMessage(embed).queue()
    } else {
        event.channel.sendMessage(buildEmbed {
            color { Immutable.SUCCESS }
            author { "Select The Action:" }
            description { "\u0031\u20E3 \u2014 Get Information\n\u0032\u20E3 \u2014 Get Color Information" }
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
                        val embed = roleInfo(event.author) { role }
                        val color = ImageUtils.getColorImage(role.color!!, 250, 250)
                        event.channel.sendMessage(embed).addFile(color, "${role.color!!.rgb.toHex().takeLast(6)}.png").queue()
                    }
                    "\u0032\u20E3" -> {
                        it.delete().queue()
                        ColorCommand()(event, arrayOf(role.color!!.rgb.toHex().takeLast(6)))
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
                event.sendFailure("Time is up!").queue()
                USERS_WITH_PROCESSES -= event.author
            }
        }
    }
    suspend fun selectRole(event: MessageReceivedEvent, msg: Message, roles: List<Role>) {
        val c = event.channel.awaitMessage(event.author)?.contentRaw
        if (c !== null) {
            if (c.isInt) {
                if (c.toInt() in 1..roles.size) {
                    msg.delete().queue()
                    roleMenu(event, roles[c.toInt() - 1])
                    USERS_WITH_PROCESSES -= event.author
                } else {
                    event.sendFailure("Try again!").await { selectRole(event, msg, roles) }
                }
            } else if (c.toLowerCase() == "exit") {
                msg.delete().queue()
                USERS_WITH_PROCESSES -= event.author
                event.sendSuccess("Process successfully stopped!").queue()
            } else {
                event.sendFailure("Try again!").await { selectRole(event, msg, roles) }
            }
        } else {
            msg.delete().queue()
            USERS_WITH_PROCESSES -= event.author
            event.sendFailure("Time is up!").queue()
        }
    }
}