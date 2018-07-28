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
import io.iskylake.lakebot.entities.EventWaiter
import io.iskylake.lakebot.entities.extensions.*
import io.iskylake.lakebot.utils.AudioUtils

import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent

import java.util.concurrent.TimeUnit

class LoopCommand : Command {
    override val name = "loop"
    override val aliases = listOf("repeat")
    override val description = "The command that enables or disables looping"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        if (AudioUtils[event.guild].audioPlayer.playingTrack === null) {
            event.sendError("There is no track that is being played now!").queue()
        } else {
            if (event.member.isConnected) {
                val scheduler = AudioUtils[event.guild].trackScheduler
                event.sendMessage(buildEmbed {
                    description {
                        when {
                            scheduler.isLoop -> """|${"\u0031\u20E3"} - queue loop
                                |${"\u0032\u20E3"} - disable single loop
                            """.trimMargin()
                            scheduler.isQueueLoop -> """|${"\u0031\u20E3"} - single loop
                                |${"\u0032\u20E3"} - disable queue loop
                            """.trimMargin()
                            else -> """|${"\u0031\u20E3"} - single loop
                                |${"\u0032\u20E3"} - queue loop
                            """.trimMargin()
                        }
                    }
                    color { Immutable.SUCCESS }
                    author("Select The Mode:") { event.selfUser.effectiveAvatarUrl }
                }).await {
                    it.addReaction("\u0031\u20E3").complete()
                    it.addReaction("\u0032\u20E3").complete()
                    it.addReaction("\u274C").complete()
                    val e = EventWaiter.receiveEventRaw<MessageReactionAddEvent>(1, TimeUnit.MINUTES) { e ->
                        val name = e.reactionEmote.name
                        val condition = name == "\u0031\u20E3" || name == "\u0032\u20E3" || name == "\u274C"
                        e.messageIdLong == it.idLong && e.user == event.author && condition
                    }
                    if (e !== null) {
                        when (e.reactionEmote.name) {
                            "\u0031\u20E3" -> {
                                it.delete().queue()
                                if (scheduler.isLoop) {
                                    scheduler.isQueueLoop = true
                                    scheduler.isLoop = false
                                    event.sendSuccess(text = "Queue looping is enabled!").queue()
                                } else {
                                    scheduler.isLoop = true
                                    event.sendSuccess(text = "Single repeating is enabled!").queue()
                                }
                            }
                            "\u0032\u20E3" -> {
                                it.delete().queue()
                                when {
                                    scheduler.isLoop -> {
                                        scheduler.isLoop = false
                                        event.sendSuccess(text = "Single repeating is disabled!").queue()
                                    }
                                    scheduler.isQueueLoop -> {
                                        scheduler.isQueueLoop = false
                                        event.sendSuccess(text = "Queue looping is disabled!").queue()
                                    }
                                    else -> {
                                        scheduler.isQueueLoop = true
                                        event.sendSuccess(text = "Queue looping is enabled!").queue()
                                    }
                                }
                            }
                            "\u274C" -> {
                                it.delete().queue()
                                event.sendSuccess("Process successfully stopped!").queue()
                            }
                        }
                    } else {
                        it.delete().queue()
                        event.sendError("Time is up!").queue()
                    }
                }
            } else {
                event.sendError("You're not in the voice channel!").queue()
            }
        }
    }
}