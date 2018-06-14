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

package io.iskylake.lakebot.entities.extensions

import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.requests.restaction.MessageAction

import java.util.concurrent.TimeUnit

suspend fun MessageReceivedEvent.awaitMessage(user: User, delay: Long = 1, unit: TimeUnit = TimeUnit.MINUTES) = channel.awaitMessage(user, delay, unit)
val MessageReceivedEvent.selfUser: SelfUser
    get() = this.jda.selfUser
val MessageReceivedEvent.selfMember: Member?
    get() = try {
        guild.selfMember
    } catch (e: Exception) {
        null
    }
val MessageReceivedEvent.argsRaw: String?
    get() = try {
        message.contentRaw.split("\\s+".toRegex(), 2)[1]
    } catch (e: Exception) {
        null
    }
val MessageReceivedEvent.argsDisplay: String?
    get() = try {
        message.contentDisplay.split("\\s+".toRegex(), 2)[1]
    } catch (e: Exception) {
        null
    }
val MessageReceivedEvent.argsStripped: String?
    get() = try {
        message.contentStripped.split("\\s+".toRegex(), 2)[1]
    } catch (e: Exception) {
        null
    }
fun MessageReceivedEvent.sendMessage(text: String): MessageAction = this.channel.sendMessage(text)
fun MessageReceivedEvent.sendMessage(embed: MessageEmbed): MessageAction = this.channel.sendMessage(embed)
fun MessageReceivedEvent.sendMessage(builder: EmbedBuilder): MessageAction = this.channel.sendMessage(builder.build())
fun MessageReceivedEvent.sendSuccess(text: String): MessageAction = this.channel.sendSuccess(text)
fun MessageReceivedEvent.sendError(text: String): MessageAction = this.channel.sendError(text)
fun MessageReceivedEvent.sendConfirmation(text: String): MessageAction = this.channel.sendConfirmation(text)