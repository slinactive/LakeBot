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
import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.extensions.*
import io.ilakeful.lakebot.utils.TimeUtils

import net.dv8tion.jda.api.Permission.*
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

import java.util.concurrent.TimeUnit

class MuteCommand : Command {
    override val name = "mute"
    override val description = "The command giving a mute to the specified member"
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <member> <time> <reason>"
    override val examples = fun(prefix: String) = mapOf(
            "$prefix$name ILakeful 1w12h flood" to "mutes ILakeful for 7.5 days because of flood"
    )
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val arguments = event.argsRaw?.split(Regex("\\s+"), 3) ?: emptyList()
        if (arguments.isNotEmpty()) {
            when (MANAGE_ROLES) {
                !in event.member!!.permissions -> {
                    event.channel.sendFailure("You do not have the required permission to manage roles!").queue()
                }
                !in event.selfMember!!.permissions -> {
                    event.channel.sendFailure("LakeBot does not have the required permission to manage roles!").queue()
                }
                else -> {
                    when {
                        arguments.size in 1..2 -> event.channel.sendFailure("You haven't specified the reason or time!").queue()
                        "([1-9][0-9]*)([smhdw])".toRegex().findAll(arguments[1]).toList().isEmpty() -> {
                            event.channel.sendFailure("The specified time format is invalid!").queue()
                        }
                        TimeUtils.parseTime(arguments[1]) > TimeUtils.weeksToMillis(2) -> {
                            event.channel.sendFailure("The mute time must not exceed 2 weeks!").queue()
                        }
                        !event.guild.isMuteRoleEnabled -> event.channel.sendFailure("The mute role is not enabled!").queue()
                        else -> {
                            val time = TimeUtils.parseTime(arguments[1])
                            val reason = arguments[2]
                            event.retrieveMembers(
                                    query = arguments.first(),
                                    command = this,
                                    massMention = false,
                                    predicate = { member ->
                                        member != event.author
                                                && event.member!!.canInteract(member)
                                                && event.selfMember!!.canInteract(member)
                                    }
                            ) {
                                muteUser(event, time, reason) { it }
                            }
                        }
                    }
                }
            }
        } else {
            event.channel.sendFailure("You haven't specified any arguments!").queue()
        }
    }
    suspend fun muteUser(event: MessageReceivedEvent, time: Long, reason: String, lazyMember: () -> Member) {
        val member = lazyMember()
        val user = member.user
        event.channel.sendConfirmation("Are you sure you want to mute ${user.asTag}?").await {
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
                        field(true, "Moderator:") { event.author.asTag.escapeDiscordMarkdown() }
                        field(reason.length < 27, "Reason:") { reason }
                        field(timeAsText.length < 27, "Time:") { timeAsText }
                        timestamp()
                    }
                    event.guild.addRoleToMember(member, role).queue {
                        event.channel.sendSuccess("${user.asTag} has been successfully muted!").queue()
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
                    event.channel.sendSuccess("Successfully canceled!").queue()
                }
            } else {
                it.delete().queue()
                event.channel.sendFailure("Time is up!").queue()
            }
        }
    }
}