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
import io.ilakeful.lakebot.entities.extensions.*
import io.ilakeful.lakebot.utils.AudioUtils
import io.ilakeful.lakebot.utils.MusicUtils

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class VolumeCommand : Command {
    override val name = "volume"
    override val aliases = listOf("setvolume", "volumelevel", "volumerate", "set-volume", "volume-level", "volume-rate")
    override val description = "The command allowing volume control on the bot side"
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <volume>"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) = if (AudioUtils[event.guild].audioPlayer.playingTrack === null) {
        event.channel.sendFailure("No track is currently playing!").queue()
    } else {
        if (event.member!!.isConnected) {
            if (args.isNotEmpty()) {
                if (args[0].isInt && args[0].toInt() in 0..100) {
                    val volume = args[0].toInt()
                    AudioUtils[event.guild].audioPlayer.volume = volume
                    val progressBar = MusicUtils.getProgressBar(volume.toLong(), 100)
                    event.channel.sendSuccess("**0%** $progressBar **100%**").queue()
                } else {
                    event.channel.sendFailure("That is an invalid value!").queue()
                }
            } else {
                event.channel.sendFailure("You haven't specified any arguments!").queue()
            }
        } else {
            event.channel.sendFailure("You are not connected to the voice channel!").queue()
        }
    }
}