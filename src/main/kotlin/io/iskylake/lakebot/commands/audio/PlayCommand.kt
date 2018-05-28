/*
 * Copyright 2017-2018 (c) Alexander "ISkylake" Shevchenko
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

package io.iskylake.lakebot.commands.audio

import io.iskylake.lakebot.commands.Command
import io.iskylake.lakebot.entities.extensions.argsRaw
import io.iskylake.lakebot.entities.extensions.connectedChannel
import io.iskylake.lakebot.entities.extensions.isConnected
import io.iskylake.lakebot.entities.extensions.sendError
import io.iskylake.lakebot.utils.AudioUtils

import net.dv8tion.jda.core.events.message.MessageReceivedEvent

open class PlayCommand internal constructor() : Command {
    override val name = "play"
    override val description = "The command that plays song by the specified link"
    override val usage: (String) -> String = { "${super.usage(it)} <query/link>" }
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        if (event.argsRaw !== null) {
            if (!event.member.isConnected) {
                event.sendError("You're not in the voice channel!").queue()
            } else {
                if (!event.guild.selfMember.isConnected) {
                    event.guild.audioManager.openAudioConnection(event.member.connectedChannel)
                }
                AudioUtils.loadAndPlay(event.guild, event.textChannel, event.argsRaw!!)
            }
        } else {
            event.sendError("You specified no link/query!").queue()
        }
    }
}