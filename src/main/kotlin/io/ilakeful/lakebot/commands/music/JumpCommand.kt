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
import io.ilakeful.lakebot.utils.TimeUtils

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

import java.time.Duration

class JumpCommand : Command {
    companion object {
        val TIME_CODE_REGEX = "(?:(?<hours>\\d{1,2}):)?(?:(?<minutes>\\d{1,2}):)?(?<seconds>\\d{1,2})".toRegex()
    }
    override val name = "jump"
    override val aliases = listOf("settime", "set-time", "seek", "seekto", "seek-to")
    override val description = "The command setting the specified time for the currently playing track"
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <time>"
    override val examples = fun(prefix: String) = mapOf(
            "$prefix$name 00:05" to "seeks the track to 00:05",
            "$prefix$name 2:15" to "seeks the track to 02:15",
            "$prefix$name 02:42:00" to "seeks the track to 02:42:00",
            "$prefix$name 25" to "seeks the track to 00:25"
    )
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        if (event.argsRaw !== null) {
            if (!event.member!!.isConnected) {
                event.channel.sendFailure("You are not connected to the voice channel!").queue()
            } else {
                if (AudioUtils[event.guild].audioPlayer.playingTrack !== null) {
                    val arguments = event.argsRaw!!
                    if (arguments matches TIME_CODE_REGEX) {
                        try {
                            val finder = TIME_CODE_REGEX.find(arguments)!!
                            var hours = finder.groups["hours"]?.value?.toLong()
                            var minutes = finder.groups["minutes"]?.value?.toLong()
                            val seconds = finder.groups["seconds"]!!.value.toLong()
                            if (minutes === null && hours !== null) {
                                minutes = hours
                                hours = null
                            }
                            val iterable = listOf(
                                    Duration.ofHours(hours ?: 0),
                                    Duration.ofMinutes(minutes ?: 0),
                                    Duration.ofSeconds(seconds)
                            )
                            val position = iterable.map { it.toMillis() }.sum()
                            if (position !in 0..AudioUtils[event.guild].audioPlayer.playingTrack.duration) {
                                event.channel.sendFailure("You cannot seek the track to the position!").queue()
                            } else {
                                AudioUtils[event.guild].audioPlayer.playingTrack.position = position
                                event.channel.sendSuccess("Jumped to the specified position (${TimeUtils.asDuration(AudioUtils[event.guild].audioPlayer.playingTrack.position)})!").queue()
                            }
                        } catch (e: Exception) {
                            event.channel.sendFailure("That is an invalid time position!").queue()
                        }
                    } else {
                        event.channel.sendFailure("That is an invalid time position!").queue()
                    }
                } else {
                    event.channel.sendFailure("No track is currently playing!").queue()
                }
            }
        } else {
            event.channel.sendFailure("You haven't specified any arguments!").queue()
        }
    }
}