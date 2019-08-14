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

class SkipCommand : Command {
    override val name = "skip"
    override val aliases = listOf("omit")
    override val description = "The command playing the next track from the current queue"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val manager = AudioUtils[event.guild]
        if (manager.audioPlayer.playingTrack !== null) {
            if (event.member!!.isConnected) {
                manager.trackScheduler.nextTrack()
                try {
                    val playing = manager.audioPlayer.playingTrack
                    event.channel.sendSuccess("Skipped to the next track ([${playing.info.title}](${playing.info.uri}))!").queue()
                } catch (e: Exception) {
                    event.channel.sendSuccess("Playback has been successfully stopped!").queue()
                }
            } else {
                event.channel.sendFailure("You are not connected to the voice channel!").queue()
            }
        } else {
            event.channel.sendFailure("No track is currently playing!").queue()
        }
    }
}