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

import com.google.api.services.youtube.model.Video

import io.iskylake.lakebot.Immutable
import io.iskylake.lakebot.USERS_WITH_PROCESSES
import io.iskylake.lakebot.entities.EventWaiter
import io.iskylake.lakebot.entities.extensions.*
import io.iskylake.lakebot.utils.AudioUtils
import io.iskylake.lakebot.utils.TimeUtils
import io.iskylake.lakebot.utils.YouTubeUtils

import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class YouTubePlayCommand : PlayCommand() {
    override val name = "ytplay"
    override val aliases = listOf("yplay", "youtubeplay", "yp", "ytp")
    override val description = "The command that plays song from YouTube by the specified query"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        if (event.argsRaw !== null) {
            if (!event.member.isConnected) {
                event.sendError("You're not in the voice channel!").queue()
            } else {
                if (!event.guild.selfMember.isConnected) {
                    event.guild.audioManager.openAudioConnection(event.member.connectedChannel)
                }
                try {
                    val results = YouTubeUtils.getResults(event.argsRaw!!)
                    if (!results.isEmpty()) {
                        val videos = YouTubeUtils.getVideos(event.argsRaw!!)
                        if (!event.argsRaw!!.matches(YouTubeUtils.VIDEO_REGEX)) {
                            val description = buildString {
                                for ((index, video) in videos.withIndex()) {
                                    appendln("**${index + 1}. [${video.snippet.title}](https://youtu.be/${video.id})** — uploaded by ${video.snippet.channelTitle} (${TimeUtils.asDuration(YouTubeUtils.getDuration(video))})")
                                }
                            }
                            event.channel.sendMessage(buildEmbed {
                                color {
                                    Immutable.SUCCESS
                                }
                                thumbnail {
                                    event.jda.selfUser.effectiveAvatarUrl
                                }
                                footer(event.author.effectiveAvatarUrl) {
                                    "Type in \"exit\" to kill the process"
                                }
                                field(title = "YouTube Results For ${event.argsRaw}:") {
                                    description
                                }
                            }).queue {
                                try {
                                    this.awaitInt(event, videos, it)
                                    USERS_WITH_PROCESSES += event.author
                                } catch (e: Exception) {
                                    event.sendError("Something went wrong searching YouTube!").queue()
                                }
                            }
                        } else {
                            AudioUtils.loadAndPlay(event.guild, event.textChannel, event.argsRaw!!)
                        }
                    } else {
                        event.sendError("I can't find that on YouTube!").queue()
                    }
                } catch (e: Exception) {
                    event.sendError("Something went wrong searching YouTube!").queue()
                }
            }
        } else {
            event.sendError("You specified no link/query!").queue()
        }
    }
    private fun awaitInt(event: MessageReceivedEvent, videos: List<Video>, msg: Message) {
        EventWaiter.awaitMessageAsync(event.author, event.channel) {
            val c = it?.contentRaw
            if (c !== null) {
                if (c.isInt) {
                    if (c.toInt() in 1..videos.size) {
                        msg.delete().queue()
                        val index: Int = c.toInt() - 1
                        val video = videos[index]
                        val link = "https://youtu.be/${video.id}"
                        AudioUtils.loadAndPlay(event.guild, event.textChannel, link)
                        USERS_WITH_PROCESSES -= event.author
                    } else {
                        event.sendError("Try again!").queue { awaitInt(event, videos, msg) }
                    }
                } else if (c.toLowerCase() == "exit") {
                    msg.delete().queue()
                    USERS_WITH_PROCESSES -= event.author
                    event.sendSuccess("Process successfully stopped!").queue()
                } else {
                    event.sendError("Try again!").queue { awaitInt(event, videos, msg) }
                }
            } else {
                msg.delete().queue()
                USERS_WITH_PROCESSES -= event.author
                event.sendError("Time is up!").queue()
            }
        }
    }
}