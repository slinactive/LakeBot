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

package io.ilakeful.lakebot.utils

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.track.AudioTrack

import io.ilakeful.lakebot.audio.GuildMusicManager
import io.ilakeful.lakebot.entities.extensions.*

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object AudioUtils {
    @JvmField
    val PLAYER_MANAGER = DefaultAudioPlayerManager()
    @JvmField
    val MUSIC_MANAGERS = mutableMapOf<Long, GuildMusicManager>()
    init {
        PLAYER_MANAGER.setPlayerCleanupThreshold(90000)
        PLAYER_MANAGER.configuration.resamplingQuality = AudioConfiguration.ResamplingQuality.LOW
        PLAYER_MANAGER.configuration.opusEncodingQuality = 10
        val youtube = YoutubeAudioSourceManager()
        youtube.setPlaylistPageCount(Integer.MAX_VALUE)
        PLAYER_MANAGER.registerSourceManager(youtube)
        AudioSourceManagers.registerRemoteSources(PLAYER_MANAGER)
        AudioSourceManagers.registerLocalSource(PLAYER_MANAGER)
    }
    @Synchronized
    infix operator fun get(guild: Guild): GuildMusicManager {
        val manager = MUSIC_MANAGERS[guild.idLong] ?: GuildMusicManager(PLAYER_MANAGER)
        MUSIC_MANAGERS.putIfAbsent(guild.idLong, manager)
        guild.audioManager.sendingHandler = manager.sendHandler
        return manager
    }
    fun loadAndPlay(guild: Guild, channel: TextChannel, trackUrl: String) {
        val musicManager = this[guild]
        PLAYER_MANAGER.loadItemOrdered(musicManager, trackUrl, object : AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                musicManager.trackScheduler += track
                channel.sendSuccess("[${track.info.title}](${track.info.uri}) has been added to queue!").queue()
            }
            override fun playlistLoaded(playlist: AudioPlaylist) {
                for (track in playlist.tracks) {
                    musicManager.trackScheduler += track
                }
                val description = "${if (!playlist.isSearchResult) "[${playlist.name}]($trackUrl)" else playlist.name} has been added to queue!"
                channel.sendSuccess(description).queue()
            }
            override fun noMatches() = channel.sendFailure("Nothing found by $trackUrl!").queue()
            override fun loadFailed(exception: FriendlyException) = channel.sendFailure("This track can't be played!").queue()
        })
    }
    inline fun skipTrack(crossinline g: () -> Guild) {
        val manager = this[g()]
        manager.trackScheduler.queue -= manager.audioPlayer.playingTrack
        if (manager.trackScheduler.queue.isNotEmpty()) {
            val track = manager.trackScheduler.queue.poll()
            manager.audioPlayer.startTrack(if (manager.trackScheduler.queue.count { it == track } > 1) track.makeClone() else track, false)
        } else {
            manager.audioPlayer.destroy()
        }
    }
    inline fun joinChannel(crossinline e: () -> MessageReceivedEvent) {
        val event = e()
        when {
            !event.member!!.isConnected -> event.sendFailure("You're not in the voice channel!").queue()
            event.guild.selfMember.isConnected && event.member!!.connectedChannel == event.guild.selfMember.connectedChannel -> event.sendFailure("I'm already in this voice channel!").queue()
            else -> {
                event.guild.audioManager.openAudioConnection(event.member!!.connectedChannel)
                event.channel.sendSuccess("Joined the voice channel!").queue()
            }
        }
    }
    fun clear(manager: GuildMusicManager) {
        manager.audioPlayer.isPaused = false
        manager.audioPlayer.playingTrack?.stop()
        manager.audioPlayer.destroy()
        manager.audioPlayer.volume = 100
        manager.trackScheduler.isLoop = false
        manager.trackScheduler.isQueueLoop = false
        manager.trackScheduler.clearQueue()
    }
    fun clear(guild: Guild) = clear(this[guild])
    fun clear(guild: Guild, manager: GuildMusicManager) {
        clear(manager)
        guild.audioManager.closeAudioConnection()
    }
    inline fun leaveChannel(crossinline e: () -> MessageReceivedEvent) {
        val event = e()
        when {
            !event.member!!.isConnected -> event.sendFailure("You're not in the voice channel!").queue()
            event.selfMember?.isConnected == false -> event.sendFailure("I'm not in the voice channel!").queue()
            else -> {
                clear(event.guild, this[event.guild])
                event.channel.sendSuccess("Left the voice channel!").queue()
            }
        }
    }
}