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

import com.markozajc.akiwrapper.Akiwrapper
import com.markozajc.akiwrapper.Akiwrapper.Answer
import com.markozajc.akiwrapper.core.entities.Server

import io.ilakeful.lakebot.Immutable
import io.ilakeful.lakebot.WAITER_PROCESSES
import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.WaiterProcess
import io.ilakeful.lakebot.entities.extensions.*
import io.ilakeful.lakebot.entities.handlers.CommandHandler

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class AkinatorCommand : Command {
    val languages: Map<String, Server.Language> = mapOf(
            "russian" to Server.Language.RUSSIAN,
            "english" to Server.Language.ENGLISH,
            "arabic" to Server.Language.ARABIC,
            "chinese" to Server.Language.CHINESE,
            "dutch" to Server.Language.DUTCH,
            "french" to Server.Language.FRENCH,
            "german" to Server.Language.GERMAN,
            "hebrew" to Server.Language.HEBREW,
            "italian" to Server.Language.ITALIAN,
            "japanese" to Server.Language.JAPANESE,
            "korean" to Server.Language.KOREAN,
            "polish" to Server.Language.POLISH,
            "portuguese" to Server.Language.PORTUGUESE,
            "spanish" to Server.Language.SPANISH,
            "turkish" to Server.Language.TURKISH
    )
    override val name = "akinator"
    override val description = """The command launching Akinator game.
        |After the game launch you're supposed to answer with **YES**, **NO**, **DONT KNOW**, **PROBABLY**, or **PROBABLY NOT** until Akinator guesses your character. Type in **BACK** in order to return to previous question. Type in "exit" in order to terminate the current game. Type in "aliases" in order to get more ways of answering.
    """.trimMargin()
    override val usage = { it: String -> "${super.usage(it)} <language (optional)>" }
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        try {
            val language = languages[args.firstOrNull()?.toLowerCase() ?: "english"] ?: throw IllegalArgumentException("That's invalid language!")
            val api = buildAkiwrapper {
                localization { language }
                filterProfanity { !event.textChannel.isNSFW }
            }
            val declined = mutableSetOf<Long>()
            event.channel.sendEmbed {
                author { "Question #${api.currentQuestion!!.step + 1}:" }
                description { api.currentQuestion!!.question.capitalize() }
                footer { "Type in \"exit\" to kill the process" }
                color { Immutable.SUCCESS }
            }.await {
                val process = WaiterProcess(mutableListOf(event.author), event.textChannel, this)
                WAITER_PROCESSES += process
                awaitAnswer(event, api, declined, process)
            }
        } catch (e: Exception) {
            event.channel.sendFailure(e.message ?: "Something went wrong!").queue()
        }
    }
    private suspend fun awaitAnswer(
            event: MessageReceivedEvent,
            wrapper: Akiwrapper,
            declined: MutableSet<Long>,
            process: WaiterProcess
    ) {
        try {
            val content = event.channel.awaitMessage(event.author)?.contentRaw?.toLowerCase()
            if (content !== null) {
                val possibleAnswers = setOf(
                        "help",
                        "back", "b", "return",
                        "alias", "aliases",
                        "y", "yes", "yup", "yep", "yeah", "yea",
                        "n", "no", "nope", "nah",
                        "d", "dk", "dont know", "dunno", "idk", "don't know",
                        "p", "probably", "perhaps", "likely",
                        "pn", "probably not", "unlikely",
                        "exit"
                )
                if (content in possibleAnswers) {
                    when (content) {
                        "exit" -> event.channel.sendConfirmation("Are you sure you want to exit?").await {
                            val bool = it.awaitNullableConfirmation(event.author)
                            if (bool !== null) {
                                if (bool) {
                                    it.delete().queue()
                                    event.channel.sendSuccess("Successfully stopped!").queue()
                                    WAITER_PROCESSES -= process
                                } else {
                                    it.delete().queue()
                                    event.channel.sendSuccess("Let's go!").await {
                                        awaitAnswer(event, wrapper, declined, process)
                                    }
                                }
                            }
                        }
                        "alias", "aliases" -> event.channel.sendEmbed {
                            color { Immutable.SUCCESS }
                            author("Aliases For Answers:") { event.selfUser.effectiveAvatarUrl }
                            field(title = "Yes:") { "\u2022 Yeah\n\u2022 Yep\n\u2022 Yup\n\u2022 Y\n\u2022 Yea" }
                            field(title = "No:") { "\u2022 Nah\n\u2022 Nope\n\u2022 N" }
                            field(title = "Don't Know:") { "\u2022 Dont Know\n\u2022 Idk\n\u2022 Dk\n\u2022 Dunno\n\u2022 D" }
                            field(title = "Probably:") { "\u2022 P\n\u2022 Perhaps\n\u2022 Likely" }
                            field(title = "Probably Not:") { "\u2022 PN\n\u2022 Unlikely" }
                            field(title = "Back:") { "\u2022 Return\n\u2022 B" }
                        }.await { awaitAnswer(event, wrapper, declined, process) }
                        "help" -> {
                            CommandHandler["help"]!!(event, arrayOf(name))
                            awaitAnswer(event, wrapper, declined, process)
                        }
                        else -> {
                            val guess = wrapper.guesses.firstOrNull { it.probability >= 0.85 && it.idLong !in declined }
                            val answer = when (content) {
                                "yes", "yeah", "yep", "yup", "y", "yea" -> Answer.YES
                                "no", "nah", "n", "nope" -> Answer.NO
                                "dont know", "idk", "dunno", "don't know", "d" -> Answer.DONT_KNOW
                                "probably", "p", "perhaps", "likely" -> Answer.PROBABLY
                                "probably not", "pn", "unlikely" -> Answer.PROBABLY_NOT
                                else -> Answer.DONT_KNOW
                            }
                            val backCheck = content == "back" || content == "return" || content == "b"
                            if (guess === null && !backCheck) {
                                event.channel.sendEmbed {
                                    val question = wrapper.answerCurrentQuestion(answer)
                                    author { "Question #${question!!.step + 1}:" }
                                    description { question!!.question.capitalize() }
                                    footer { "Type in \"exit\" to kill the process" }
                                    color { Immutable.SUCCESS }
                                }.await {
                                    awaitAnswer(event, wrapper, declined, process)
                                }
                            } else {
                                when {
                                    wrapper.currentQuestion!!.step > 80 -> {
                                        WAITER_PROCESSES -= process
                                        event.channel.sendFailure("You win! Akinator was unable to guess the character!").queue()
                                    }
                                    backCheck -> {
                                        event.channel.sendEmbed {
                                            val question = if (wrapper.currentQuestion?.step != 0) wrapper.undoAnswer() else wrapper.currentQuestion
                                            author { "Question #${question!!.step + 1}:" }
                                            description { question!!.question.capitalize() }
                                            footer { "Type in \"exit\" to kill the process" }
                                            color { Immutable.SUCCESS }
                                        }.await {
                                            awaitAnswer(event, wrapper, declined, process)
                                        }
                                    }
                                    guess !== null -> {
                                        event.channel.sendEmbed {
                                            field(true, "Name:") { guess.name }
                                            field(true, "Description:") { guess.description?.capitalize() ?: "None" }
                                            image { guess.image?.toString() }
                                            color { Immutable.SUCCESS }
                                        }.await {
                                            event.channel.sendEmbed {
                                                author { "Guess Review!" }
                                                description { "Is that the character?" }
                                                color { Immutable.SUCCESS }
                                            }.await {
                                                val bool = it.awaitNullableConfirmation(event.author)
                                                if (bool !== null) {
                                                    if (bool) {
                                                        it.delete().queue()
                                                        WAITER_PROCESSES -= process
                                                    } else {
                                                        it.delete().queue()
                                                        declined += guess.idLong
                                                        event.channel.sendEmbed {
                                                            val question = if (content == "back" || content == "return" || content == "b") {
                                                                if (wrapper.currentQuestion?.step != 0) wrapper.undoAnswer() else wrapper.currentQuestion
                                                            } else wrapper.answerCurrentQuestion(answer)
                                                            author { "Question #${question!!.step + 1}:" }
                                                            description { question!!.question.capitalize() }
                                                            footer { "Type in \"exit\" to kill the process" }
                                                            color { Immutable.SUCCESS }
                                                        }.await {
                                                            awaitAnswer(event, wrapper, declined, process)
                                                        }
                                                    }
                                                } else {
                                                    it.delete().queue()
                                                    WAITER_PROCESSES -= process
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    event.channel.sendFailure("Incorrect answer! Use `aliases` or `help` to get documentation!").await {
                        awaitAnswer(event, wrapper, declined, process)
                    }
                }
            } else {
                WAITER_PROCESSES -= process
                event.channel.sendFailure("Time is up!").queue()
            }
        } catch (ignored: Exception) {
            WAITER_PROCESSES -= process
            event.channel.sendFailure("You win! Akinator was unable to guess the character!").queue()
        }
    }
}