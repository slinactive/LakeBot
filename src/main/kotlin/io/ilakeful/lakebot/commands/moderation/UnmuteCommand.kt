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

import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.extensions.*

import net.dv8tion.jda.api.Permission.*
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class UnmuteCommand : Command {
    override val name = "unmute"
    override val description = "The command unmuting the specified member"
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <user>"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val arguments = event.argsRaw
        if (arguments !== null) {
            when (MANAGE_ROLES) {
                !in event.member!!.permissions -> {
                    event.channel.sendFailure("You do not have the required permission to manage roles!").queue()
                }
                !in event.selfMember!!.permissions -> {
                    event.channel.sendFailure("LakeBot does not have the required permission to manage roles!").queue()
                }
                else -> {
                    event.retrieveMembersWithIsMassProperty(
                            command = this,
                            predicate = { event.selfMember!!.canInteract(it) && event.member!!.canInteract(it) }
                    ) { member, (isMass, massSize) ->
                        if (!event.guild.isMuteRoleEnabled) {
                            event.channel.sendFailure("The mute role is not enabled!").queue()
                            event.guild.clearMute(member.user)
                        } else {
                            if (!isMass || (isMass && massSize == 1)) {
                                unmuteUser(event) { member}
                            } else {
                                if (event.guild.getRoleById(event.guild.muteRole!!)!! !in member.roles) {
                                    if (event.guild.getMute(member.user) !== null) {
                                        event.guild.clearMute(member.user)
                                    }
                                } else {
                                    if (event.guild.getMute(member.user) !== null) {
                                        unmuteUser(event) { member }
                                    } else {
                                        event.guild.clearMute(member.user)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            event.channel.sendFailure("You haven't specified any arguments!").queue()
        }
    }
    suspend fun unmuteUser(event: MessageReceivedEvent, lazyMember: () -> Member) {
        val member = lazyMember()
        val user = member.user
        val role = event.guild.getRoleById(event.guild.muteRole!!)!!
        if (role !in member.roles || event.guild.getMute(user) === null) {
            event.guild.clearMute(user)
            event.channel.sendFailure("${user.asTag} is already unmuted!").queue()
        } else {
            event.channel.sendConfirmation("Are you sure you want to unmute ${user.asTag}?").await {
                val confirmation = it.awaitNullableConfirmation(event.author)
                if (confirmation !== null) {
                    it.delete().queue()
                    if (confirmation) {
                        event.guild.clearMute(user)
                        event.channel.sendSuccess("${user.asTag} has been successfully unmuted!").queue()
                        event.guild.removeRoleFromMember(member, role).queue()
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
}