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

package io.ilakeful.lakebot.commands.moderation

import io.ilakeful.lakebot.Immutable
import io.ilakeful.lakebot.USERS_WITH_PROCESSES
import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.extensions.*

import net.dv8tion.jda.api.Permission.*
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class UnmuteCommand : Command {
    override val name = "unmute"
    override val description = "The command unmuting the specified member"
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <user>"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val arguments = event.argsRaw
        if (arguments !== null) {
            when {
                MANAGE_ROLES !in event.member!!.permissions -> event.sendFailure("You don't have required permissions!").queue()
                MANAGE_ROLES !in event.guild.selfMember.permissions -> event.sendFailure("LakeBot doesn't have required permissions!").queue()
                !event.guild.isMuteRoleEnabled -> event.sendFailure("The mute role isn't enabled!").queue()
                else -> {
                    when {
                        args[0] matches Regex.DISCORD_USER -> {
                            val member = event.guild.getMemberById(args[0].replace(Regex.DISCORD_USER, "\$1"))
                            if (member !== null) {
                                unmuteUser(event) { member }
                            } else {
                                event.sendFailure("Couldn't find that user!").queue()
                            }
                        }
                        args[0] matches Regex.DISCORD_ID && event.guild.getMemberById(args[0]) !== null -> {
                            val member = event.guild.getMemberById(args[0])!!
                            unmuteUser(event) { member }
                        }
                        event.guild.getMemberByTagSafely(args[0]) !== null -> {
                            val member = event.guild.getMemberByTagSafely(args[0])!!
                            unmuteUser(event) { member }
                        }
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
                                unmuteUser(event) { list.first() }
                            }
                        }
                        else -> event.sendFailure("Couldn't find that user!").queue()
                    }
                }
            }
        } else {
            event.sendFailure("You specified no content!").queue()
        }
    }
    suspend fun unmuteUser(event: MessageReceivedEvent, lazyMember: () -> Member) {
        val member = lazyMember()
        val user = member.user
        if (event.member!!.canInteract(member) && event.guild.selfMember.canInteract(member)) {
            val role = event.guild.getRoleById(event.guild.muteRole!!)!!
            if (role !in member.roles || event.guild.getMute(user) === null) {
                event.guild.clearMute(user)
                event.sendFailure("That user is already unmuted!").queue()
            } else {
                event.channel.sendConfirmation("Are you sure you want to unmute this member?").await {
                    val confirmation = it.awaitNullableConfirmation(event.author)
                    if (confirmation !== null) {
                        it.delete().queue()
                        if (confirmation) {
                            event.guild.clearMute(user)
                            event.sendSuccess("${user.tag} was successfully unmuted!").queue()
                            event.guild.removeRoleFromMember(member, role).queue()
                        } else {
                            event.sendSuccess("Process was canceled!").queue()
                        }
                    } else {
                        it.delete().queue()
                        event.sendFailure("Time is up!").queue()
                    }
                }
            }
        } else {
            event.sendFailure("You can't unmute that user!").queue()
        }
    }
    suspend fun selectUser(event: MessageReceivedEvent, msg: Message, members: List<Member>) {
        val c = event.channel.awaitMessage(event.author)?.contentRaw
        if (c !== null) {
            if (c.isInt) {
                if (c.toInt() in 1..members.size) {
                    msg.delete().queue()
                    USERS_WITH_PROCESSES -= event.author
                    unmuteUser(event) { members[c.toInt() - 1] }
                } else {
                    event.sendFailure("Try again!").await { selectUser(event, msg, members) }
                }
            } else if (c.toLowerCase() == "exit") {
                msg.delete().queue()
                USERS_WITH_PROCESSES -= event.author
                event.sendSuccess("Process successfully stopped!").queue()
            } else {
                event.sendFailure("Try again!").await { selectUser(event, msg, members) }
            }
        } else {
            msg.delete().queue()
            USERS_WITH_PROCESSES -= event.author
            event.sendFailure("Time is up!").queue()
        }
    }
}