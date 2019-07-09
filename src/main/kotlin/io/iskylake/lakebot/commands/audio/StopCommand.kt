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

package io.iskylake.lakebot.commands.audio

import io.iskylake.lakebot.commands.Command
import io.iskylake.lakebot.entities.extensions.*
import io.iskylake.lakebot.utils.AudioUtils

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class StopCommand : Command {
    override val name = "stop"
    override val description = "The command that stops the song that is currently playing"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        when {
            !event.member!!.isConnected -> event.sendError("You're not in the voice channel!").queue()
            !event.guild.selfMember.isConnected -> event.sendError("I'm not in the voice channel!").queue()
            else -> {
                AudioUtils.clear(event.guild)
                event.sendSuccess("Playback was successfully stopped and queue was successfully cleared!").queue()
            }
        }
    }
}