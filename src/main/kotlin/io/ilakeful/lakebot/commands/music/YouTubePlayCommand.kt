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

import com.google.api.services.youtube.model.Video

import io.ilakeful.lakebot.Immutable
import io.ilakeful.lakebot.WAITER_PROCESSES
import io.ilakeful.lakebot.entities.WaiterProcess
import io.ilakeful.lakebot.entities.extensions.*
import io.ilakeful.lakebot.utils.AudioUtils
import io.ilakeful.lakebot.utils.TimeUtils
import io.ilakeful.lakebot.utils.YouTubeUtils

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class YouTubePlayCommand : PlayCommand() {
    override val name = "ytplay"
    override val aliases = listOf("yplay", "youtubeplay", "yp", "ytp", "youtube-play")
    override val description = "The command playing a song from YouTube by the specified query"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        if (event.argsRaw !== null) {
            if (!event.member!!.isConnected) {
                event.sendFailure("You're not in the voice channel!").queue()
            } else {
                if (!event.guild.selfMember.isConnected) {
                    event.guild.audioManager.openAudioConnection(event.member!!.connectedChannel)
                }
                try {
                    val results = YouTubeUtils.getResults(event.argsRaw!!)
                    if (results.isNotEmpty()) {
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
                            }).await {
                                try {
                                    val process = WaiterProcess(mutableListOf(event.author), event.textChannel)
                                    WAITER_PROCESSES += process
                                    this.awaitInt(event, videos, it, process)
                                } catch (e: Exception) {
                                    event.sendFailure("Something went wrong searching YouTube!").queue()
                                }
                            }
                        } else {
                            AudioUtils.loadAndPlay(event.guild, event.textChannel, event.argsRaw!!)
                        }
                    } else {
                        event.sendFailure("I can't find that on YouTube!").queue()
                    }
                } catch (e: Exception) {
                    event.sendFailure("Something went wrong searching YouTube!").queue()
                }
            }
        } else {
            event.sendFailure("You specified no link/query!").queue()
        }
    }
    private suspend fun awaitInt(event: MessageReceivedEvent, videos: List<Video>, msg: Message, process: WaiterProcess) {
        val c = event.channel.awaitMessage(event.author)?.contentRaw
        if (c !== null) {
            if (c.isInt) {
                if (c.toInt() in 1..videos.size) {
                    msg.delete().queue()
                    val index: Int = c.toInt() - 1
                    val video = videos[index]
                    val link = "https://youtu.be/${video.id}"
                    AudioUtils.loadAndPlay(event.guild, event.textChannel, link)
                    WAITER_PROCESSES -= process
                } else {
                    event.sendFailure("Try again!").await { awaitInt(event, videos, msg, process) }
                }
            } else if (c.toLowerCase() == "exit") {
                msg.delete().queue()
                WAITER_PROCESSES -= process
                event.sendSuccess("Process successfully stopped!").queue()
            } else {
                event.sendFailure("Try again!").await { awaitInt(event, videos, msg, process) }
            }
        } else {
            msg.delete().queue()
            WAITER_PROCESSES -= process
            event.sendFailure("Time is up!").queue()
        }
    }
}