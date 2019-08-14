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

package io.ilakeful.lakebot.commands.utils

import com.google.api.services.youtube.model.Video

import io.ilakeful.lakebot.Immutable
import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.WaiterProcess
import io.ilakeful.lakebot.entities.extensions.*
import io.ilakeful.lakebot.utils.TimeUtils
import io.ilakeful.lakebot.utils.YouTubeUtils

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class YouTubeCommand : Command {
    override val name = "youtube"
    override val aliases = listOf("yt", "ytsearch", "youtubesearch", "yts", "ys", "youtube-search")
    override val description = "The command searching for videos on YouTube by the specified query"
    override val cooldown = 5L
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <query>"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val arguments = event.argsRaw
        if (arguments !== null) {
            try {
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
                            event.channel.sendMessage(getYTEmbed(video)).queue()
                        }
                    }
                } else {
                    event.channel.sendFailure("No results found by the query!").queue()
                }
            } catch (e: Exception) {
                event.channel.sendFailure(
                        "Something went wrong while searching YouTube! " +
                                "${e::class.simpleName ?: "Unknown exception"}: ${e.message ?: "absent message"}"
                ).queue()
            }
        } else {
            event.channel.sendFailure("You haven't specified any arguments!").queue()
        }
    }
    private fun getYTEmbed(video: Video) = buildEmbed {
        val thumbs = video.snippet.thumbnails
        val thumbnail = when {
            thumbs.maxres !== null -> thumbs.maxres
            thumbs.high !== null -> thumbs.high
            thumbs.medium !== null -> thumbs.medium
            thumbs.default !== null -> thumbs.default
            thumbs.standard !== null -> thumbs.standard
            else -> null
        }
        color { Immutable.SUCCESS }
        setAuthor(video.snippet.channelTitle, "https://www.youtube.com/channel/${video.snippet.channelId}")
        title("https://youtu.be/${video.id}") { video.snippet.title }
        field(title ="Description:") {
            when {
                video.snippet.description.isNullOrEmpty() -> "No description provided"
                video.snippet.description.count() > 1024 -> video.snippet.description.substring(0, 1021) + "..."
                else -> video.snippet.description
            }
        }
        field(true, "Duration:") { TimeUtils.asDuration(YouTubeUtils.getDuration(video)) }
        image { thumbnail?.url }
    }
}