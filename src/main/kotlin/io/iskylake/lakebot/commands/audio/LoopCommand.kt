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
import io.iskylake.lakebot.entities.extensions.isConnected
import io.iskylake.lakebot.entities.extensions.sendError
import io.iskylake.lakebot.entities.extensions.sendSuccess
import io.iskylake.lakebot.utils.AudioUtils

import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class LoopCommand : Command {
    override val name = "loop"
    override val aliases = listOf("repeat")
    override val description = "The command that enables or disables song looping"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        if (AudioUtils[event.guild].audioPlayer.playingTrack === null) {
            event.sendError("There is no track that is being played now!").queue()
        } else {
            if (event.member.isConnected) {
                val scheduler = AudioUtils[event.guild].trackScheduler
                scheduler.isLoop = !scheduler.isLoop
                val isLoop = if (scheduler.isLoop) "Enabled" else "Disabled"
                event.sendSuccess("$isLoop song looping!").queue()
            } else {
                event.sendError("You're not in the voice channel!").queue()
            }
        }
    }
}