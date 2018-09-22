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

package io.iskylake.lakebot.commands.utils

import io.iskylake.lakebot.Immutable
import io.iskylake.lakebot.commands.Command
import io.iskylake.lakebot.entities.extensions.*
import io.iskylake.lakebot.utils.TranslateUtils

import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class TranslateCommand : Command {
    override val name = "translate"
    override val description = "The command that translates specified text into specified language and sends the result"
    override val aliases = listOf("translator")
    override val cooldown = 2L
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <language> <text>"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val allArguments = event.argsRaw
        if (allArguments !== null) {
            val arguments = allArguments.split("\\s+".toRegex(), 2)
            if (arguments.size == 1) {
                event.sendError("You didn't specified all needed arguments!").queue()
            } else {
                try {
                    val translated = TranslateUtils.translate(arguments[0], arguments[1])
                    if (translated !== null) {
                        val sourceLanguage = TranslateUtils.getLanguageName(TranslateUtils.detect(arguments[1]))
                        val embed = buildEmbed {
                            color { Immutable.SUCCESS }
                            author("LakeTranslate") { event.selfUser.effectiveAvatarUrl }
                            description { translated.safeSubstring(end = 1900).escapeDiscordMarkdown() }
                            footer { "$sourceLanguage -> ${arguments[0].capitalizeAll(true)}" }
                        }
                        event.sendMessage(embed).queue()
                    } else {
                        event.sendError("The language is invalid or not supported!").queue()
                    }
                } catch (e: Exception) {
                    event.sendError("Something went wrong!").queue()
                }
            }
        } else {
            event.sendError("You specified no content!").queue()
        }
    }
}