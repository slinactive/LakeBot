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

class StopCommand : Command {
    override val name = "stop"
    override val aliases = listOf("clear")
    override val description = "The command stopping the currently playing song and clearing the queue"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        when {
            !event.member!!.isConnected -> event.channel.sendFailure("You are not connected to the voice channel!").queue()
            !event.guild.selfMember.isConnected -> event.channel.sendFailure("${event.selfUser.name} is not connected to the voice channel!").queue()
            else -> {
                AudioUtils.clear(event.guild)
                event.channel.sendSuccess("Playback has been successfully stopped and queue has been successfully cleared!").queue()
            }
        }
    }
}