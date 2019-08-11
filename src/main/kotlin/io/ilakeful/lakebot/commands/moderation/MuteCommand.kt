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
import io.ilakeful.lakebot.WAITER_PROCESSES
import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.WaiterProcess
import io.ilakeful.lakebot.entities.extensions.*
import io.ilakeful.lakebot.utils.TimeUtils

import net.dv8tion.jda.api.Permission.*
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

import java.util.concurrent.TimeUnit

class MuteCommand : Command {
    override val name = "mute"
    override val description = "The command giving a mute to the specified member"
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <user> <time> <reason>"
    override val examples = fun(prefix: String) = mapOf("$prefix$name ILakeful 1w12h flood" to "mutes ILakeful for 7.5 days because of flood")
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val arguments = event.argsRaw?.split(Regex("\\s+"), 3) ?: emptyList()
        if (arguments.isNotEmpty()) {
            when {
                MANAGE_ROLES !in event.member!!.permissions -> event.sendFailure("You don't have required permissions!").queue()
                MANAGE_ROLES !in event.guild.selfMember.permissions -> event.sendFailure("LakeBot doesn't have required permissions!").queue()
                arguments.size in 1..2 -> event.sendFailure("You didn't specify reason or time!").queue()
                "([1-9][0-9]*)([smhdw])".toRegex().findAll(arguments[1]).toList().isEmpty() -> event.sendFailure("That's not a valid format of time!").queue()
                TimeUtils.parseTime(arguments[1]) > TimeUtils.weeksToMillis(2) -> event.sendFailure("You can indicate the time until 2 weeks!").queue()
                !event.guild.isMuteRoleEnabled -> event.sendFailure("The mute role isn't enabled!").queue()
                else -> {
                    val time = TimeUtils.parseTime(arguments[1])
                    val reason = arguments[2]
                    when {
                        arguments[0] matches Regex.DISCORD_USER -> {
                            val member = event.guild.getMemberById(arguments[0].replace(Regex.DISCORD_USER, "\$1"))
                            if (member !== null) {
                                muteUser(event, time, reason) { member }
                            } else {
                                event.sendFailure("Couldn't find that user!").queue()
                            }
                        }
                        arguments[0] matches Regex.DISCORD_ID && event.guild.getMemberById(arguments[0]) !== null -> {
                            val member = event.guild.getMemberById(arguments[0])!!
                            muteUser(event, time, reason) { member }
                        }
                        event.guild.getMemberByTagSafely(arguments[0]) !== null -> {
                            val member = event.guild.getMemberByTagSafely(arguments[0])!!
                            muteUser(event, time, reason) { member }
                        }
                        event.guild.searchMembers(arguments[0]).isNotEmpty() -> {
                            val list = event.guild.searchMembers(arguments[0]).take(5)
                            if (list.size > 1) {
                                event.channel.sendMessage(buildEmbed {
                                    color { Immutable.SUCCESS }
                                    author("Select The User:") { event.selfUser.effectiveAvatarUrl }
                                    for ((index, member) in list.withIndex()) {
                                        appendln { "${index + 1}. ${member.user.tag}" }
                                    }
                                    footer { "Type in \"exit\" to kill the process" }
                                }).await {
                                    val process = WaiterProcess(mutableListOf(event.author), event.textChannel, this)
                                    WAITER_PROCESSES += process
                                    selectUser(event, it, list, time, reason, process)
                                }
                            } else {
                                muteUser(event, time, reason) { list.first() }
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
    suspend fun muteUser(event: MessageReceivedEvent, time: Long, reason: String, lazyMember: () -> Member) {
        val member = lazyMember()
        val user = member.user
        if (user != event.author && event.member!!.canInteract(member) && event.guild.selfMember.canInteract(member)) {
            event.channel.sendConfirmation("Are you sure you want to mute this member?").await {
                val confirmation = it.awaitNullableConfirmation(event.author)
                if (confirmation !== null) {
                    it.delete().queue()
                    if (confirmation) {
                        val timeAsText = TimeUtils.asText(time)
                        val role = event.guild.getRoleById(event.guild.muteRole!!)!!
                        event.guild.putMute(user, event.author, reason, time)
                        val embed = buildEmbed {
                            color { Immutable.SUCCESS }
                            author("LakeMute!") { event.selfUser.effectiveAvatarUrl }
                            field(true, "Guild:") { event.guild.name.escapeDiscordMarkdown() }
                            field(true, "Moderator:") { event.author.tag.escapeDiscordMarkdown() }
                            field(reason.length < 27, "Reason:") { reason }
                            field(timeAsText.length < 27, "Time:") { timeAsText }
                            timestamp()
                        }
                        event.guild.addRoleToMember(member, role).queue {
                            event.channel.sendSuccess("${user.asTag} was successfully muted!").queue()
                            if (VOICE_MOVE_OTHERS in event.selfMember!!.permissions) {
                                if (member.isConnected) {
                                    event.guild.kickVoiceMember(member).queue()
                                }
                            }
                            user.openPrivateChannel().queue { channel ->
                                channel.sendMessage(embed).queue(null) {}
                            }
                            event.guild.removeRoleFromMember(member, role).queueAfter(
                                    time,
                                    TimeUnit.MILLISECONDS,
                                    { event.guild.clearMute(user) },
                                    { event.guild.clearMute(user) }
                            )
                        }
                    } else {
                        event.sendSuccess("Process was canceled!").queue()
                    }
                } else {
                    it.delete().queue()
                    event.sendFailure("Time is up!").queue()
                }
            }
        } else {
            event.sendFailure("You can't mute that user!").queue()
        }
    }
    suspend fun selectUser(
            event: MessageReceivedEvent,
            msg: Message,
            members: List<Member>,
            time: Long,
            reason: String,
            process: WaiterProcess
    ) {
        val c = event.channel.awaitMessage(event.author)?.contentRaw
        if (c !== null) {
            if (c.isInt) {
                if (c.toInt() in 1..members.size) {
                    msg.delete().queue()
                    muteUser(event, time, reason) { members[c.toInt() - 1] }
                    WAITER_PROCESSES -= process
                } else {
                    event.sendFailure("Try again!").await { selectUser(event, msg, members, time, reason, process) }
                }
            } else if (c.toLowerCase() == "exit") {
                msg.delete().queue()
                WAITER_PROCESSES -= process
                event.sendSuccess("Process successfully stopped!").queue()
            } else {
                event.sendFailure("Try again!").await { selectUser(event, msg, members, time, reason, process) }
            }
        } else {
            msg.delete().queue()
            WAITER_PROCESSES -= process
            event.sendFailure("Time is up!").queue()
        }
    }
}