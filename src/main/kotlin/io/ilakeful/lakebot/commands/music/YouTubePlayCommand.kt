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

import io.ilakeful.lakebot.Immutable
import io.ilakeful.lakebot.entities.WaiterProcess
import io.ilakeful.lakebot.entities.extensions.*
import io.ilakeful.lakebot.utils.AudioUtils
import io.ilakeful.lakebot.utils.TimeUtils
import io.ilakeful.lakebot.utils.YouTubeUtils

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class YouTubePlayCommand : PlayCommand() {
    override val name = "ytplay"
    override val aliases = listOf("yplay", "youtubeplay", "yp", "ytp", "youtube-play")
    override val description = "The command playing a song from YouTube by the specified query"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        if (event.member!!.isConnected) {
            val arguments = event.argsRaw
            if (arguments !== null) {
                if (!event.selfMember!!.isConnected) {
                    event.guild.audioManager.openAudioConnection(event.member!!.connectedChannel)
                }
                try {
                    if (arguments matches YouTubeUtils.VIDEO_REGEX) {
                        AudioUtils.loadAndPlay(event.author, event.guild, event.textChannel, arguments)
                    } else {
                        val videos = YouTubeUtils.getVideos(arguments)
                        if (videos.isNotEmpty()) {
                            event.channel.sendEmbed {
                                footer(event.author.effectiveAvatarUrl) { "Type in \"exit\" to kill the process" }
                                author("YouTube Results for \"$arguments\":") { event.selfUser.effectiveAvatarUrl }
                                color { Immutable.SUCCESS }
                                for ((index, video) in videos.withIndex()) {
                                    appendln {
                                        "**${index + 1}. " +
                                                "[${video.snippet.title}](https://youtu.be/${video.id})** " +
                                                "— uploaded by ${video.snippet.channelTitle}" +
                                                " (${TimeUtils.asDuration(YouTubeUtils.getDuration(video))})"
                                    }
                                }
                            }.await {
                                selectEntity(
                                        event = event,
                                        message = it,
                                        entities = videos,
                                        addProcess = true,
                                        process = WaiterProcess(
                                                users = mutableListOf(event.author),
                                                channel = event.textChannel,
                                                command = this
                                        )
                                ) { video ->
                                    val link = "https://youtu.be/${video.id}"
                                    AudioUtils.loadAndPlay(event.author, event.guild, event.textChannel, link)
                                }
                            }
                        } else {
                            event.channel.sendFailure("No results found by the query!").queue()
                        }
                    }
                } catch (e: Exception) {
                    event.channel.sendFailure(
                            "Something went wrong while searching YouTube! ${e::class.simpleName}: ${e.message}"
                    ).queue()
                }
            } else {
                event.channel.sendFailure("You haven't specified any arguments!").queue()
            }
        } else {
            event.channel.sendFailure("You are not connected to the voice channel!").queue()
        }
    }
}