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

import io.iskylake.lakebot.commands.Command
import io.iskylake.lakebot.entities.extensions.*
import io.iskylake.lakebot.utils.AudioUtils
import io.iskylake.lakebot.utils.TimeUtils

import net.dv8tion.jda.core.events.message.MessageReceivedEvent

import java.time.Duration

class JumpCommand : Command {
    companion object {
        val TIMECODE_REGEX = "(?:(?<hours>\\d{1,2}):)?(?:(?<minutes>\\d{1,2}):)?(?<seconds>\\d{1,2})".toRegex()
    }
    override val name = "jump"
    override val aliases = listOf("rewind")
    override val description = "The command that rewinds currently playing track by the specified timecode"
    override val usage: (String) -> String = { "${super.usage(it)} <timecode>" }
    override val examples = { it: String ->
        mapOf(
            "$it$name 00:05" to "rewinds track to 00:05",
            "$it$name 2:15" to "rewinds track to 02:15",
            "$it$name 02:42:00" to "rewinds track to 02:42:00",
            "$it$name 25" to "rewinds track to 00:25"
        )
    }
    override fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        if (event.argsRaw !== null) {
            if (!event.member.isConnected) {
                event.sendError("You're not in the voice channel!").queue()
            } else {
                if (AudioUtils[event.guild].audioPlayer.playingTrack !== null) {
                    val arguments = event.argsRaw!!
                    if (arguments matches TIMECODE_REGEX) {
                        try {
                            val finder = TIMECODE_REGEX.find(arguments)!!
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
                                event.sendError("You can't make track jump to that position!").queue()
                            } else {
                                AudioUtils[event.guild].audioPlayer.playingTrack.position = position
                                event.channel.sendSuccess("Jumped to the specified position (${TimeUtils.asDuration(AudioUtils[event.guild].audioPlayer.playingTrack.position)})!").queue()
                            }
                        } catch (e: Exception) {
                            event.sendError("That's not a valid timecode!").queue()
                        }
                    } else {
                        event.sendError("That's not a valid timecode!").queue()
                    }
                } else {
                    event.sendError("There is no track that is being played now!").queue()
                }
            }
        } else {
            event.sendError("You specified no timecode!").queue()
        }
    }
}