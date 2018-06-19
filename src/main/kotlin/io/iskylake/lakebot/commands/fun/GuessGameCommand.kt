/*
 * Copyright 2017-2018 (c) Alexander "ISkylake" Shevchenko
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.iskylake.lakebot.commands.`fun`

import io.iskylake.lakebot.Immutable
import io.iskylake.lakebot.USERS_WITH_PROCESSES
import io.iskylake.lakebot.commands.Command
import io.iskylake.lakebot.entities.extensions.*

import net.dv8tion.jda.core.events.message.MessageReceivedEvent

import java.util.*

class GuessGameCommand : Command {
    override val name = "guess"
    override val aliases = listOf("guessnum", "guessgame", "guessnumber")
    override val description = "The command that launches a game in which you must guess the number in the range from 1 to the specified number (the limit is 250000). If you want to kill game, type in \"exit\"."
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <limit>"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        if (args.isNotEmpty()) {
            if (args[0].isInt) {
                val max = args[0].toInt()
                if (max in 1..250000) {
                    val toGuess = Random().nextInt(max)
                    val round = 1
                    event.channel.sendMessage(buildEmbed {
                        author { "Attempt #$round" }
                        description { "Let's go!" }
                        footer { "Type in \"exit\" to kill the process" }
                        color { Immutable.SUCCESS }
                    }).await {
                        USERS_WITH_PROCESSES += event.author
                        awaitInt(round, toGuess, event)
                    }
                } else {
                    event.sendError("The number is not in the correct range!").queue()
                }
            } else {
                event.sendError("You must specify a maximum number!").queue()
            }
        } else {
            event.sendError("You specified no content!").queue()
        }
    }
    private suspend fun awaitInt(round: Int, toGuess: Int, event: MessageReceivedEvent) {
        var attempt = round
        val content = event.channel.awaitMessage(event.author)?.contentRaw
        if (content !== null) {
            when {
                content.isInt -> {
                    val input = content.toInt()
                    when {
                        input == toGuess -> {
                            event.channel.sendSuccess("GG! Game took $attempt attempts!").queue()
                            USERS_WITH_PROCESSES -= event.author
                        }
                        input > toGuess -> {
                            attempt++
                            event.channel.sendMessage(buildEmbed {
                                color { Immutable.FAILURE }
                                description { "It's too large!" }
                                author { "Attempt #$attempt" }
                            }).await { awaitInt(attempt, toGuess, event) }
                        }
                        input < toGuess -> {
                            attempt++
                            event.channel.sendMessage(buildEmbed {
                                color { Immutable.FAILURE }
                                description { "It's too small!" }
                                author { "Attempt #$attempt" }
                            }).await { awaitInt(attempt, toGuess, event) }
                        }
                    }
                }
                content.toLowerCase() == "exit" -> {
                    USERS_WITH_PROCESSES -= event.author
                    event.sendSuccess("Process successfully stopped!").queue()
                }
                else -> event.sendError("Try again!").await { awaitInt(attempt, toGuess, event) }
            }
        } else {
            USERS_WITH_PROCESSES -= event.author
            event.sendError("Time is up!").queue()
        }
    }
}