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

import com.sedmelluq.discord.lavaplayer.track.AudioTrack

import io.iskylake.lakebot.Immutable
import io.iskylake.lakebot.commands.Command
import io.iskylake.lakebot.entities.extensions.*
import io.iskylake.lakebot.entities.pagination.buildPaginator
import io.iskylake.lakebot.utils.AudioUtils
import io.iskylake.lakebot.utils.MusicUtils
import io.iskylake.lakebot.utils.TimeUtils

import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class QueueCommand : Command {
    override val name = "queue"
    override val aliases = listOf("playlist")
    override val description = "The command that sends the current queue (playlist)"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        if (AudioUtils[event.guild].audioPlayer.playingTrack === null && AudioUtils[event.guild].trackScheduler.queue.isEmpty()) {
            event.sendError("Queue is empty!").queue()
        } else {
            val queue = AudioUtils[event.guild].trackScheduler.queue
            queue -= AudioUtils[event.guild].audioPlayer.playingTrack
            if (queue.isNotEmpty()) {
                val paginator = buildPaginator<AudioTrack> {
                    event { event }
                    size { 10 }
                    list { queue.toList() }
                    embed { num, pages ->
                        if (pages.firstOrNull()?.isNotEmpty() == true) {
                            for (track in pages[num - 1]) {
                                appendln {
                                    "**${elements.indexOf(track) + 1}. [${track.info.title}](${track.info.uri}) (${if (track.duration == Long.MAX_VALUE) "LIVE" else TimeUtils.asDuration(track.duration)})**"
                                }
                            }
                            color { Immutable.SUCCESS }
                            author("LakePlayer") { event.selfUser.effectiveAvatarUrl }
                            field(title = "Total Songs:") {
                                "${AudioUtils[event.guild].trackScheduler.queue.size + 1} songs"
                            }
                            field(title = "Total Duration:") {
                                TimeUtils.asDuration(AudioUtils[event.guild].trackScheduler.queue.map { it.duration }.filter { it != Long.MAX_VALUE }.sum())
                            }
                            field(title = "Looping:") {
                                MusicUtils.getLoopingMode(AudioUtils[event.guild].trackScheduler)
                            }
                            field(title = "Volume") { "${AudioUtils[event.guild].audioPlayer.volume}%" }
                            field(title = "Now Playing:") {
                                "**[${AudioUtils[event.guild].audioPlayer.playingTrack.info.title}](${AudioUtils[event.guild].audioPlayer.playingTrack.info.uri})** (${TimeUtils.asDuration(AudioUtils[event.guild].audioPlayer.playingTrack.position)}/${if (AudioUtils[event.guild].audioPlayer.playingTrack.duration == Long.MAX_VALUE) "LIVE" else TimeUtils.asDuration(AudioUtils[event.guild].audioPlayer.playingTrack.duration)})"
                            }
                            footer(event.author.effectiveAvatarUrl) {
                                "Page $num/${pages.size} | Requested by ${event.author.tag}"
                            }
                        } else {
                            color { Immutable.FAILURE }
                            description { "Queue is empty!" }
                            author { "Incorrect usage!" }
                        }
                    }
                }
                paginator(args.firstOrNull()?.toInt() ?: 1)
            } else {
                val embed = buildEmbed {
                    field(title = "Looping:") {
                        MusicUtils.getLoopingMode(AudioUtils[event.guild].trackScheduler)
                    }
                    field(title = "Volume") { "${AudioUtils[event.guild].audioPlayer.volume}%" }
                    field(title = "Now Playing:") {
                        "**[${AudioUtils[event.guild].audioPlayer.playingTrack.info.title}](${AudioUtils[event.guild].audioPlayer.playingTrack.info.uri})** (${TimeUtils.asDuration(AudioUtils[event.guild].audioPlayer.playingTrack.position)}/${if (AudioUtils[event.guild].audioPlayer.playingTrack.duration == Long.MAX_VALUE) "LIVE" else TimeUtils.asDuration(AudioUtils[event.guild].audioPlayer.playingTrack.duration)})"
                    }
                    color { Immutable.SUCCESS }
                    author("LakePlayer") { event.selfUser.effectiveAvatarUrl }
                }
                event.channel.sendMessage(embed).queue()
            }
        }
    }
}