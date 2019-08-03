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

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class RemoveCommand : Command {
    override val name = "remove"
    override val aliases = listOf("removeat", "unadd", "takeaway", "remove-at", "take-away")
    override val description = "The command removing the specified song from the queue"
    override val usage: (String) -> String = { "${super.usage(it)} <index>" }
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        if (event.argsRaw !== null) {
            if (!event.member!!.isConnected) {
                event.sendFailure("You're not in the voice channel!").queue()
            } else {
                val manager = AudioUtils[event.guild]
                val track = manager.audioPlayer.playingTrack
                manager.trackScheduler.queue -= manager.audioPlayer.playingTrack
                if (manager.trackScheduler.queue.isNotEmpty()) {
                    if (args[0].isInt) {
                        var index = args[0].toInt()
                        if (index in 1..manager.trackScheduler.queue.size) {
                            index--
                            manager.trackScheduler.receiveQueue {
                                val removable = it[index]
                                event.channel.sendSuccess("[${removable.info.title}](${removable.info.uri}) successfully removed!").queue()
                                it.removeAt(index)
                            }
                            manager.trackScheduler.queue += track
                        } else {
                            event.sendFailure("You specified no correct number!").queue()
                        }
                    } else {
                        event.sendFailure("You specified no correct number!").queue()
                    }
                } else {
                    event.sendFailure("Queue is empty!").queue()
                }
            }
        } else {
            event.sendFailure("You specified no index of track!").queue()
        }
    }
}