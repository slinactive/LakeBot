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

package io.ilakeful.lakebot.commands.music

import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.extensions.isConnected
import io.ilakeful.lakebot.entities.extensions.sendFailure
import io.ilakeful.lakebot.entities.extensions.sendSuccess
import io.ilakeful.lakebot.utils.AudioUtils

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class ResumeCommand : Command {
    override val name = "resume"
    override val aliases = listOf("continue", "carryon", "carry-on")
    override val description = "The command resuming playback after being paused"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        if (AudioUtils[event.guild].audioPlayer.playingTrack === null) {
            event.sendFailure("There is no track that is being played now!").queue()
        } else {
            if (event.member!!.isConnected) {
                val player = AudioUtils[event.guild].audioPlayer
                if (!player.isPaused) {
                    event.sendFailure("Track is already being played!").queue()
                } else {
                    player.isPaused = false
                    event.sendSuccess("Track has been resumed!").queue()
                }
            } else {
                event.sendFailure("You're not in the voice channel!").queue()
            }
        }
    }
}