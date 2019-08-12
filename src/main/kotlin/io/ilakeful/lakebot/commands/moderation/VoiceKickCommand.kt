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

import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.extensions.*

import net.dv8tion.jda.api.Permission.VOICE_MOVE_OTHERS
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
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
            event.retrieveMembers(command = this, predicate = { it.isConnected }) {
                kickVoiceMember(it, event.guild, event.channel)
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
}