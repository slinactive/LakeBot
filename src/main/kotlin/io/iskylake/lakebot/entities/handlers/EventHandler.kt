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
import io.iskylake.lakebot.entities.EventWaiter
import io.iskylake.lakebot.entities.extensions.*
import io.iskylake.lakebot.utils.AudioUtils

import kotlinx.coroutines.experimental.launch

import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.channel.text.TextChannelCreateEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.role.RoleDeleteEvent
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException
import net.dv8tion.jda.core.hooks.ListenerAdapter

import org.bson.Document

import java.util.Timer
import java.util.concurrent.TimeUnit

import kotlin.concurrent.schedule

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
    override fun onRoleDelete(event: RoleDeleteEvent) {
        if (event.guild.muteRole !== null) {
            if (event.guild.muteRole == event.role.id) {
                event.guild.clearMuteRole()
            }
        }
    }
    override fun onReady(event: ReadyEvent) {
        for (guild in event.jda.guildCache.filter { it.muteRole !== null }) {
            val role = guild.getRoleById(guild.muteRole)
            val members = role.members
            for (member in members) {
                try {
                    val mute = guild.getMute(member.user)
                    if (mute !== null) {
                        val muteObject = mute["mute", Document::class.java]
                        val ago = muteObject.getLong("time")
                        val muteTime = muteObject.getLong("long")
                        if (System.currentTimeMillis() >= ago + muteTime) {
                            guild.controller.removeSingleRoleFromMember(member, role).queue()
                        } else {
                            val timer = Timer()
                            timer.schedule(ago + muteTime - System.currentTimeMillis()) {
                                guild.controller.removeSingleRoleFromMember(member, role).queue()
                            }
                        }
                    }
                } catch (ignored: Exception) {
                } finally {
                    guild.clearMute(member.user)
                }
            }
        }
    }
    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        if (event.guild.muteRole !== null) {
            val role = event.guild.getRoleById(event.guild.muteRole)
            val mute = event.guild.getMute(event.user)
            if (mute !== null) {
                try {
                    event.guild.controller.addSingleRoleToMember(event.member, role).queue()
                    val muteObject = mute["mute", Document::class.java]
                    val ago = muteObject.getLong("time")
                    val muteTime = muteObject.getLong("long")
                    if (System.currentTimeMillis() >= ago + muteTime) {
                        event.guild.controller.removeSingleRoleFromMember(event.member, role).queue()
                    } else {
                        val timer = Timer()
                        timer.schedule(ago + muteTime - System.currentTimeMillis()) {
                            event.guild.controller.removeSingleRoleFromMember(event.member, role).queue()
                        }
                    }
                } catch (ignored: Exception) {
                } finally {
                    event.guild.clearMute(event.user)
                }
            }
        }
    }
    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
        if (event.guild.selfMember.isConnected && event.guild.selfMember.connectedChannel == event.channelLeft) {
            val members = event.channelLeft.members.filter { !it.user.isBot }
            if (members.isEmpty()) {
                AudioUtils[event.guild].audioPlayer.isPaused = true
                launch(CommandHandler) {
                    if (EventWaiter.receiveEventRaw<GuildVoiceJoinEvent>(90, TimeUnit.SECONDS) {
                        it.channelJoined == event.channelLeft && !it.member.user.isBot
                    } !== null) {
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
                    if (EventWaiter.receiveEventRaw<GuildVoiceJoinEvent>(90, TimeUnit.SECONDS) {
                        it.channelJoined == event.channelLeft && !it.member.user.isBot
                    } !== null) {
                        AudioUtils[event.guild].audioPlayer.isPaused = false
                    } else {
                        AudioUtils.clear(event.guild, AudioUtils[event.guild])
                    }
                }
            }
        }
    }
    override fun onTextChannelCreate(event: TextChannelCreateEvent) {
        if (event.guild.isMuteRoleEnabled) {
            try {
                val id = event.guild.muteRole
                val role = event.guild.getRoleById(id)
                val override = event.channel.putPermissionOverride(role)
                override.deny = 34880
                override.queue()
            } catch (ignored: Exception) {
            }
        }
    }
}