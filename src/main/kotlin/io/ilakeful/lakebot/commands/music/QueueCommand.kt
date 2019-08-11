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

import com.sedmelluq.discord.lavaplayer.track.AudioTrack

import io.ilakeful.lakebot.Immutable
import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.extensions.*
import io.ilakeful.lakebot.entities.pagination.buildPaginator
import io.ilakeful.lakebot.utils.AudioUtils
import io.ilakeful.lakebot.utils.MusicUtils
import io.ilakeful.lakebot.utils.TimeUtils

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class QueueCommand : Command {
    override val name = "queue"
    override val aliases = listOf("playlist", "q")
    override val description = "The command sending the current queue (playlist)"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        if (AudioUtils[event.guild].audioPlayer.playingTrack === null && AudioUtils[event.guild].trackScheduler.queue.isEmpty()) {
            event.sendFailure("Queue is empty!").queue()
        } else {
            if (AudioUtils[event.guild].audioPlayer.playingTrack !== null) {
                val queue = AudioUtils[event.guild].trackScheduler.queue
                queue -= AudioUtils[event.guild].audioPlayer.playingTrack
                if (queue.isNotEmpty()) {
                    val paginator = buildPaginator<AudioTrack> {
                        event { event }
                        size { 10 }
                        list { queue.toList() }
                        embed { num, pages ->
                            for (track in pages[num - 1]) {
                                appendln {
                                    val id = track.userData as? Long
                                    val requester = id?.let { event.jda.getUserById(it) }
                                    "**${elements.indexOf(track) + 1}. " +
                                            "[${track.info.title}](${track.info.uri}) " +
                                            "(${if (track.duration == Long.MAX_VALUE) "LIVE" else TimeUtils.asDuration(track.duration)})**" +
                                            requester.let { if (it !== null) " (requested by ${it.asMention})" else "" }
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
                                val track = AudioUtils[event.guild].audioPlayer.playingTrack
                                val id = track.userData as? Long
                                val requester = id?.let { event.jda.getUserById(it) }
                                "**[${track.info.title}](${track.info.uri})** " +
                                        "(${TimeUtils.asDuration(track.position)}/" +
                                        "${if (track.duration == Long.MAX_VALUE) "LIVE" else TimeUtils.asDuration(track.duration)})" +
                                        requester.let { if (it !== null) " (requested by ${it.asMention})" else "" }
                            }
                            footer(event.author.effectiveAvatarUrl) {
                                "Page $num/${pages.size} | Requested by ${event.author.asTag}"
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
                            val track = AudioUtils[event.guild].audioPlayer.playingTrack
                            val id = track.userData as? Long
                            val requester = id?.let { event.jda.getUserById(it) }
                            "**[${track.info.title}](${track.info.uri})** " +
                                    "(${TimeUtils.asDuration(track.position)}/" +
                                    "${if (track.duration == Long.MAX_VALUE) "LIVE" else TimeUtils.asDuration(track.duration)})" +
                                    requester.let { if (it !== null) " (requested by ${it.asMention})" else "" }
                        }
                        color { Immutable.SUCCESS }
                        author("LakePlayer") { event.selfUser.effectiveAvatarUrl }
                    }
                    event.channel.sendMessage(embed).queue()
                }
            } else {
                event.sendFailure("Queue is empty!").queue()
                AudioUtils.clear(event.guild)
            }
        }
    }
}