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

package io.ilakeful.lakebot.commands.`fun`

import io.ilakeful.lakebot.Immutable
import io.ilakeful.lakebot.USERS_WITH_PROCESSES
import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.EventWaiter
import io.ilakeful.lakebot.entities.extensions.*

import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

import java.util.concurrent.TimeUnit

class TicTacToeCommand : Command {
    companion object {
        const val TTT_CROSS = "❌"
        const val TTT_NOUGHT = "⭕"
    }
    class TicTacToe(private val starter: User, private val opponent: User) {
        companion object {
            const val ROWS = 3
            const val COLUMNS = 3
            @JvmStatic
            val BOARD_REGEX = "[1-9]\u20E3".toRegex()
            @JvmStatic
            val COORDS = mapOf(
                    1 to (0 to 0),
                    2 to (0 to 1),
                    3 to (0 to 2),
                    4 to (1 to 0),
                    5 to (1 to 1),
                    6 to (1 to 2),
                    7 to (2 to 0),
                    8 to (2 to 1),
                    9 to (2 to 2)
            )
        }
        val board = arrayOf(
                arrayOf("1\u20E3", "2\u20E3", "3\u20E3"),
                arrayOf("4\u20E3", "5\u20E3", "6\u20E3"),
                arrayOf("7\u20E3", "8\u20E3", "9\u20E3")
        )
        var currentTurn = starter
        val printableBoard: String
            get() = buildString {
                for (row in 0 until ROWS) {
                    for (column in 0 until COLUMNS) {
                        append(board[row][column])
                    }
                    append("\n")
                }
            }
        val isOver: Boolean
            get() {
                for (r in 0 until ROWS) {
                    if (!board[r][0].matches(BOARD_REGEX) && board[r][0] == board[r][1] && board[r][1] == board[r][2]) {
                        return true
                    }
                }
                for (c in 0 until COLUMNS) {
                    if (!board[0][c].matches(BOARD_REGEX) && board[0][c] == board[1][c] && board[1][c] == board[2][c]) {
                        return true
                    }
                }
                if (!board[0][0].matches(BOARD_REGEX) && board[0][0] == board[1][1] && board[1][1] == board[2][2]) {
                    return true
                }
                if (!board[0][2].matches(BOARD_REGEX) && board[0][2] == board[1][1] && board[1][1] == board[2][0]) {
                    return true
                }
                return false
            }
        fun coordsToInt(row: Int, column: Int) = COORDS.keys.first { COORDS[it] == row to column }
        fun turn(row: Int, column: Int, sign: String) {
            if (board[row][column] matches BOARD_REGEX) {
                board[row][column] = sign
            } else {
                throw IllegalArgumentException()
            }
        }
        fun inverseCurrentTurn() {
            currentTurn = if (currentTurn == starter) opponent else starter
        }
    }
    override val name = "tictactoe"
    override val description = "The command allowing you to challenge someone to the tic-tac-toe game"
    override val aliases = listOf("ttt", "tic-tac-toe")
    override val usage: (String) -> String = { "${super.usage(it)} <user>" }
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        if (event.argsRaw !== null) {
            try {
                val starter = event.author
                val opponent = when {
                    event.message.mentionedMembers.isNotEmpty() -> event.message.mentionedMembers.first().user
                    event.guild.getMemberByTagSafely(args[0]) !== null -> event.jda.getUserByTagSafely(args[0])!!
                    event.guild.searchMembers(args[0]).isNotEmpty() -> event.guild.searchMembers(args[0]).first().user
                    else -> throw IllegalArgumentException("Couldn't find this user!")
                }
                if (!opponent.isBot && !opponent.isFake && opponent != event.author) {
                    USERS_WITH_PROCESSES += starter
                    USERS_WITH_PROCESSES += opponent
                    event.channel.sendMessage(opponent.asMention).embed(buildEmbed {
                        color { Immutable.CONFIRMATION }
                        author { "Warning!" }
                        description { "You were challenged by ${starter.asMention} to the tic-tac-toe game! Want to accept the challenge?" }
                    }).await {
                        val isWillingToPlay = EventWaiter.awaitNullableConfirmation(it, opponent)
                        if (isWillingToPlay !== null) {
                            if (isWillingToPlay) {
                                it.delete().queue()
                                val ttt = TicTacToe(starter, opponent)
                                event.channel.sendMessage(buildEmbed {
                                    description { "${ttt.printableBoard}It's ${starter.asMention}'s turn!" }
                                    color { Immutable.SUCCESS }
                                }).await {
                                    awaitTurn(ttt, event, starter, opponent, TTT_CROSS, TTT_NOUGHT)
                                }
                            } else {
                                it.delete().queue()
                                event.channel.sendSuccess("The challenge was declined!").queue()
                                USERS_WITH_PROCESSES -= starter
                                USERS_WITH_PROCESSES -= opponent
                            }
                        } else {
                            it.delete().queue()
                            event.channel.sendFailure("Time is up!").queue()
                            USERS_WITH_PROCESSES -= starter
                            USERS_WITH_PROCESSES -= opponent
                        }
                    }
                } else {
                    throw IllegalArgumentException("You have specified the wrong user!")
                }
            } catch (e: IllegalArgumentException) {
                event.sendFailure(e.message ?: "Something went wrong!").queue()
            }
        } else {
            event.sendFailure("You haven't specified the user you want to challenge!").queue()
        }
    }
    private suspend fun awaitTurn(
            ttt: TicTacToe,
            event: MessageReceivedEvent,
            author: User,
            mentioned: User,
            cross: String,
            nought: String
    ) {
        val awaitedMessage = EventWaiter.awaitMessage(ttt.currentTurn, event.channel, 30, TimeUnit.SECONDS)
        if (awaitedMessage !== null) {
            val content = awaitedMessage.contentRaw
            when {
                content.isInt && content.toInt() in 1..9 -> {
                    try {
                        val (row, column) = TicTacToe.COORDS.getValue(content.toInt())
                        val sign = if (ttt.currentTurn == author) cross else nought
                        ttt.turn(row, column, sign)
                        if (!ttt.isOver) {
                            if (!ttt.board.none { rowLambda -> !rowLambda.none { it matches TicTacToe.BOARD_REGEX } }) {
                                ttt.inverseCurrentTurn()
                                event.channel.sendMessage(buildEmbed {
                                    description { "${ttt.printableBoard}It's ${ttt.currentTurn.asMention}'s turn!" }
                                    color { Immutable.SUCCESS }
                                }).await {
                                    awaitTurn(ttt, event, author, mentioned, cross, nought)
                                }
                            } else {
                                event.channel.sendMessage(buildEmbed {
                                    description { "${ttt.printableBoard}It's a draw!" }
                                    color { Immutable.SUCCESS }
                                }).queue {
                                    USERS_WITH_PROCESSES -= author
                                    USERS_WITH_PROCESSES -= mentioned
                                }
                            }
                        } else {
                            event.channel.sendMessage(buildEmbed {
                                description { "${ttt.printableBoard}And ${ttt.currentTurn.asMention} wins! Well done!" }
                                color { Immutable.FAILURE }
                            }).queue {
                                USERS_WITH_PROCESSES -= author
                                USERS_WITH_PROCESSES -= mentioned
                            }
                        }
                    } catch (e: IllegalArgumentException) {
                        event.channel.sendFailure("The gap is already taken!").await {
                            awaitTurn(ttt, event, author, mentioned, cross, nought)
                        }
                    }
                }
                content.toLowerCase() == "exit" -> {
                    event.channel.sendConfirmation("Are you sure you want to exit?").await {
                        val isWillingToExit = EventWaiter.awaitNullableConfirmation(it, event.author)
                        if (isWillingToExit !== null) {
                            if (isWillingToExit) {
                                it.delete().queue()
                                event.channel.sendSuccess("The process was finished!").queue()
                                USERS_WITH_PROCESSES -= author
                                USERS_WITH_PROCESSES -= mentioned
                            } else {
                                it.delete().queue()
                                event.channel.sendSuccess("Let's carry on!").queue()
                                awaitTurn(ttt, event, author, mentioned, cross, nought)
                            }
                        }
                    }
                }
                else -> {
                    event.channel.sendFailure("Try again!").await {
                        awaitTurn(ttt, event, author, mentioned, cross, nought)
                    }
                }
            }
        } else {
            event.channel.sendFailure("Time is up!").queue()
            USERS_WITH_PROCESSES -= author
            USERS_WITH_PROCESSES -= mentioned
        }
    }
}