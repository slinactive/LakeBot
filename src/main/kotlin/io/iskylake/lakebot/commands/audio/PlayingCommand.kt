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

import io.iskylake.lakebot.Immutable
import io.iskylake.lakebot.commands.Command
import io.iskylake.lakebot.entities.extensions.*
import io.iskylake.lakebot.utils.AudioUtils
import io.iskylake.lakebot.utils.MusicUtils
import io.iskylake.lakebot.utils.TimeUtils

import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class PlayingCommand : Command {
    override val name = "playing"
    override val aliases = listOf("now", "np")
    override val description = "The command that sends info about the song that is currently playing"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        if (AudioUtils[event.guild].audioPlayer.playingTrack === null) {
            event.sendError("There is no track that is being played now!").queue()
        } else {
            val player = AudioUtils[event.guild].audioPlayer
            val track = player.playingTrack
            val title = track.info.title
            val url = track.info.uri
            val now = TimeUtils.asDuration(track.position)
            val total = TimeUtils.asDuration(track.duration)
            val timeline = MusicUtils.getProgressBar(if (track.duration == Long.MAX_VALUE) Long.MAX_VALUE else track.position, track.duration)
            val bar = "$timeline ($now/${if (track.duration == Long.MAX_VALUE) "LIVE" else total})"
            val embed = buildEmbed {
                color { Immutable.SUCCESS }
                author("LakePlayer") { event.selfUser.effectiveAvatarUrl }
                thumbnail { event.selfUser.effectiveAvatarUrl }
                field(title = "Now Playing:") { "**[$title]($url)**" }
                field(title = "Looping:") {
                    MusicUtils.getLoopingMode(AudioUtils[event.guild].trackScheduler)
                }
                field(title = "Volume") { "${AudioUtils[event.guild].audioPlayer.volume}%" }
                field(title = "Duration:") { bar }
                footer(event.author.effectiveAvatarUrl) { "Requested by ${event.author.tag}" }
            }
            event.channel.sendMessage(embed).queue()
        }
    }
}