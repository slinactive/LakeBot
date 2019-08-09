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
import io.ilakeful.lakebot.entities.extensions.argsRaw
import io.ilakeful.lakebot.entities.extensions.connectedChannel
import io.ilakeful.lakebot.entities.extensions.isConnected
import io.ilakeful.lakebot.entities.extensions.sendFailure
import io.ilakeful.lakebot.utils.AudioUtils

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

open class PlayCommand : Command {
    override val name = "play"
    override val aliases = listOf("p", "pr", "playraw", "play-raw")
    override val description = "The command playing a song by the specified link"
    override val usage: (String) -> String = { "${super.usage(it)} <query/link>" }
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        if (event.argsRaw !== null) {
            if (!event.member!!.isConnected) {
                event.channel.sendFailure("You are not connected to the voice channel!").queue()
            } else {
                if (!event.guild.selfMember.isConnected) {
                    event.guild.audioManager.openAudioConnection(event.member!!.connectedChannel)
                }
                AudioUtils.loadAndPlay(event.guild, event.textChannel, event.argsRaw!!)
            }
        } else {
            val attachment = event.message.attachments.firstOrNull()
            if (attachment !== null) {
                if (!event.member!!.isConnected) {
                    event.channel.sendFailure("You are not connected to the voice channel!").queue()
                } else {
                    if (!event.guild.selfMember.isConnected) {
                        event.guild.audioManager.openAudioConnection(event.member!!.connectedChannel)
                    }
                    AudioUtils.loadAndPlay(event.guild, event.textChannel, attachment.url)
                }
            } else {
                event.channel.sendFailure("You specified no link/query and attached no file!").queue()
            }
        }
    }
}