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
 *  limitations under the License.
 */

package io.ilakeful.lakebot.commands.moderation

import io.ilakeful.lakebot.Immutable
import io.ilakeful.lakebot.WAITER_PROCESSES
import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.WaiterProcess
import io.ilakeful.lakebot.entities.extensions.*

import net.dv8tion.jda.api.Permission.VOICE_MOVE_OTHERS
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class VoiceKickCommand : Command {
    override val name = "voicekick"
    override val aliases = listOf("vckick", "vc-kick", "voice-kick", "vck")
    override val description = "The command disconnecting the specified member(s) from the voice channel they are connected to"
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <member>"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) = when (VOICE_MOVE_OTHERS) {
        !in event.member!!.permissions -> {
            event.channel.sendFailure("You do not have the required permission to move voice channel members!").queue()
        }
        !in event.selfMember!!.permissions -> {
            event.channel.sendFailure("LakeBot does not have the required permission to move voice channel members!").queue()
        }
        else -> {
            val arguments = event.argsRaw
            if (arguments !== null) {
                when {
                    event.message.mentionedMembers.any { it.isConnected } -> {
                        val members = event.message.mentionedMembers.filter { it.isConnected }
                        for (member in members) {
                            kickVoiceMember(member, event.guild, event.channel)
                        }
                    }
                    event.guild.getMemberByTagSafely(arguments) !== null -> {
                        val member = event.guild.getMemberByTagSafely(arguments)!!
                        kickVoiceMember(member, event.guild, event.channel)
                    }
                    arguments matches Regex.DISCORD_ID && event.guild.getMemberById(arguments) !== null -> {
                        val member = event.guild.getMemberById(arguments)!!
                        kickVoiceMember(member, event.guild, event.channel)
                    }
                    event.guild.searchMembers(arguments).any { it.isConnected } -> {
                        val members = event.guild.searchMembers(arguments).filter { it.isConnected }
                        if (members.size > 1) {
                            event.channel.sendEmbed {
                                color { Immutable.SUCCESS }
                                author("Select the User:") { event.selfUser.effectiveAvatarUrl }
                                for ((index, member) in members.withIndex()) {
                                    appendln { "${index + 1}. ${member.user.asTag}" }
                                }
                                footer { "Type in \"exit\" to kill the process" }
                            }.await {
                                val process = WaiterProcess(mutableListOf(event.author), event.textChannel, this)
                                WAITER_PROCESSES += process
                                selectMember(event, it, members, process)
                            }
                        } else {
                            kickVoiceMember(members.first(), event.guild, event.channel)
                        }
                    }
                    else -> event.channel.sendFailure("LakeBot did not manage to find the required user!").queue()
                }
            } else {
                event.channel.sendFailure("You haven't specified any arguments!").queue()
            }
        }
    }
    private fun kickVoiceMember(member: Member, guild: Guild, channel: MessageChannel) {
        if (member.isConnected) {
            guild.kickVoiceMember(member).queue(
                    { channel.sendSuccess("${member.user.asTag} has been kicked from the voice channel!").queue() },
                    { channel.sendFailure("LakeBot did not manage to perform the voice kick!").queue() }
            )
        } else {
            channel.sendFailure("${member.user.asTag} is not connected to any voice channel!").queue()
        }
    }
    private suspend fun selectMember(
            event: MessageReceivedEvent,
            message: Message,
            members: List<Member>,
            process: WaiterProcess
    ) {
        val content = event.channel.awaitMessage(event.author)?.contentRaw
        if (content !== null) {
            when {
                content.isInt -> {
                    val index = content.toInt()
                    if (index in 1..members.size) {
                        message.delete().queue()
                        val member = members[index - 1]
                        WAITER_PROCESSES -= process
                        kickVoiceMember(member, event.guild, event.channel)
                    } else {
                        event.channel.sendFailure("Try again!").await { selectMember(event, message, members, process) }
                    }
                }
                content.toLowerCase() == "exit" -> {
                    message.delete().queue()
                    WAITER_PROCESSES -= process
                    event.channel.sendSuccess("Successfully stopped!").queue()
                }
                else -> event.channel.sendFailure("Try again!").await { selectMember(event, message, members, process) }
            }
        } else {
            message.delete().queue()
            WAITER_PROCESSES -= process
            event.channel.sendFailure("Time is up!").queue()
        }
    }
}