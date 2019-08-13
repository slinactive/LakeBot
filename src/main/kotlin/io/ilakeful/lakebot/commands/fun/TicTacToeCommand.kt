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
import io.ilakeful.lakebot.WAITER_PROCESSES
import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.EventWaiter
import io.ilakeful.lakebot.entities.WaiterProcess
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
        event.retrieveMembers(
                command = this,
                massMention = false,
                predicate = { member ->
                    WAITER_PROCESSES.none {
                        member.user.idLong in it.users && event.channel.idLong == it.channel
                    } && !member.user.isBot && !member.user.isFake && member.user != event.author
                }
        ) { member ->
            val starter = event.author
            val opponent = member.user
            val process = WaiterProcess(mutableListOf(starter, opponent), event.textChannel, this)
            WAITER_PROCESSES += process
            event.channel.sendMessage(opponent.asMention).embed {
                color { Immutable.CONFIRMATION }
                author { "Warning!" }
                description { "You were challenged by ${starter.asMention} to the tic-tac-toe game! Want to accept the challenge?" }
            }.await {
                val isWillingToPlay = EventWaiter.awaitNullableConfirmation(it, opponent)
                if (isWillingToPlay !== null) {
                    if (isWillingToPlay) {
                        it.delete().queue()
                        val ttt = TicTacToe(starter, opponent)
                        event.channel.sendEmbed {
                            description { "${ttt.printableBoard}It's ${starter.asMention}'s turn!" }
                            color { Immutable.SUCCESS }
                        }.await {
                            awaitTurn(ttt, event, starter, opponent, TTT_CROSS, TTT_NOUGHT, process)
                        }
                    } else {
                        it.delete().queue()
                        event.channel.sendSuccess("The challenge was declined!").queue()
                        WAITER_PROCESSES -= process
                    }
                } else {
                    it.delete().queue()
                    event.channel.sendFailure("Time is up!").queue()
                    WAITER_PROCESSES -= process
                }
            }
        }
    }
    private suspend fun awaitTurn(
            ttt: TicTacToe,
            event: MessageReceivedEvent,
            author: User,
            mentioned: User,
            cross: String,
            nought: String,
            process: WaiterProcess
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
                                event.channel.sendEmbed {
                                    description { "${ttt.printableBoard}It's ${ttt.currentTurn.asMention}'s turn!" }
                                    color { Immutable.SUCCESS }
                                }.await {
                                    awaitTurn(ttt, event, author, mentioned, cross, nought, process)
                                }
                            } else {
                                event.channel.sendEmbed {
                                    description { "${ttt.printableBoard}It's a draw!" }
                                    color { Immutable.SUCCESS }
                                }.queue {
                                    WAITER_PROCESSES -= process
                                }
                            }
                        } else {
                            event.channel.sendEmbed {
                                description { "${ttt.printableBoard}And ${ttt.currentTurn.asMention} wins! Well done!" }
                                color { Immutable.FAILURE }
                            }.queue {
                                WAITER_PROCESSES -= process
                            }
                        }
                    } catch (e: IllegalArgumentException) {
                        event.channel.sendFailure("The gap is already taken!").await {
                            awaitTurn(ttt, event, author, mentioned, cross, nought, process)
                        }
                    }
                }
                content.toLowerCase() == "exit" -> {
                    event.channel.sendConfirmation("Are you sure you want to exit?").await {
                        val isWillingToExit = EventWaiter.awaitNullableConfirmation(it, ttt.currentTurn)
                        if (isWillingToExit !== null) {
                            if (isWillingToExit) {
                                it.delete().queue()
                                event.channel.sendSuccess("Successfully finished!").queue()
                                WAITER_PROCESSES -= process
                            } else {
                                it.delete().queue()
                                event.channel.sendSuccess("Successfully canceled!").queue()
                                awaitTurn(ttt, event, author, mentioned, cross, nought, process)
                            }
                        }
                    }
                }
                else -> {
                    event.channel.sendFailure("Try again!").await {
                        awaitTurn(ttt, event, author, mentioned, cross, nought, process)
                    }
                }
            }
        } else {
            event.channel.sendFailure("Time is up!").queue()
            WAITER_PROCESSES -= process
        }
    }
}