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

package io.iskylake.lakebot.entities.handlers

import io.iskylake.lakebot.Immutable
import io.iskylake.lakebot.entities.EventWaiter.receiveEventRaw
import io.iskylake.lakebot.entities.extensions.*
import io.iskylake.lakebot.utils.AudioUtils

import kotlinx.coroutines.experimental.launch

import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException
import net.dv8tion.jda.core.hooks.ListenerAdapter

import java.util.concurrent.TimeUnit

object EventHandler : ListenerAdapter() {
    override fun onMessageReceived(event: MessageReceivedEvent) = try {
        CommandHandler(event)
    } catch (ignored: InsufficientPermissionException) {
    } catch (t: Throwable) {
        event.jda.asBot().applicationInfo.queue {
            it.owner.privateChannel.sendMessage(buildEmbed {
                color { Immutable.FAILURE }
                author(t::class.simpleName ?: t.javaClass.simpleName ?: "Unknown Exception") { event.selfUser.effectiveAvatarUrl }
                field(true, "Command:") {
                    try {
                        event.message.contentRaw.split("\\s+".toRegex(), 2)[0]
                    } catch (e: Exception) {
                        "Unknown command"
                    }
                }
                field(true, "Arguments:") { event.argsRaw ?: "None" }
                field(true, "Guild:") { event.guild?.name ?: "Unknown" }
                field(true, "Author:") { event.author.tag }
                field(true, "Guild ID:") { event.guild?.id ?: "Unknown" }
                field(true, "Author ID:") { event.author.id }
                field(title = "Message:") { t.message?.safeSubstring(0, 1024) ?: "None" }
                timestamp()
                thumbnail { event.selfUser.effectiveAvatarUrl }
            })
        }
    }
    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
        if (event.guild.selfMember.isConnected && event.guild.selfMember.connectedChannel == event.channelLeft) {
            val members = event.channelLeft.members.filter { !it.user.isBot }
            if (members.isEmpty()) {
                AudioUtils[event.guild].audioPlayer.isPaused = true
                launch(CommandHandler) {
                    val check: suspend (GuildVoiceJoinEvent) -> Boolean = { it.channelJoined == event.channelLeft && !it.member.user.isBot }
                    if (receiveEventRaw(90, TimeUnit.SECONDS, check) !== null) {
                        AudioUtils[event.guild].audioPlayer.isPaused = false
                    } else {
                        AudioUtils.clear(event.guild, AudioUtils[event.guild])
                    }
                }
            }
        }
    }
    override fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
        if (event.guild.selfMember.isConnected && event.guild.selfMember.connectedChannel == event.channelLeft) {
            val members = event.channelLeft.members.filter { !it.user.isBot }
            if (members.isEmpty()) {
                AudioUtils[event.guild].audioPlayer.isPaused = true
                launch(CommandHandler) {
                    val check: suspend (GuildVoiceJoinEvent) -> Boolean = { it.channelJoined == event.channelLeft && !it.member.user.isBot }
                    if (receiveEventRaw(90, TimeUnit.SECONDS, check) !== null) {
                        AudioUtils[event.guild].audioPlayer.isPaused = false
                    } else {
                        AudioUtils.clear(event.guild, AudioUtils[event.guild])
                    }
                }
            }
        }
    }
}