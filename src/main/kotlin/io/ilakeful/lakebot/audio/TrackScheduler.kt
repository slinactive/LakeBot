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

package io.ilakeful.lakebot.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason

import io.ilakeful.lakebot.Immutable

import java.util.Queue
import java.util.LinkedList
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class TrackScheduler(private val player: AudioPlayer) : AudioEventAdapter() {
    val queue: Queue<AudioTrack> = LinkedList()
    var isLoop = false
    var isQueueLoop = false
    operator fun plusAssign(track: AudioTrack) {
        if (player.playingTrack !== null) {
            queue.offer(track)
        } else {
            player.playTrack(track)
        }
    }
    fun nextTrack() {
        val trackToPlay = queue.poll()
        if (trackToPlay !== null) {
            player.playTrack(trackToPlay)
        } else {
            if (player.playingTrack !== null) {
                player.destroy()
            }
        }
    }
    override fun onTrackStart(player: AudioPlayer, track: AudioTrack) {
        queue += track
    }
    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        if (endReason.mayStartNext) {
            when {
                isLoop -> {
                    val clone = track.makeClone()
                    clone.userData = track.userData
                    player.playTrack(clone)
                }
                isQueueLoop -> {
                    val clone = track.makeClone()
                    clone.userData = track.userData
                    queue.offer(clone)
                    queue -= track
                    nextTrack()
                }
                else -> {
                    queue -= track
                    nextTrack()
                }
            }
        }
    }
    override fun onTrackException(player: AudioPlayer, track: AudioTrack, exception: FriendlyException) {
        Immutable.LOGGER.info("Playback exception!")
        Immutable.LOGGER.info(exception.message)
    }
    fun clearQueue() = queue.clear()
    inline fun receiveQueue(block: (MutableList<AudioTrack>) -> Unit) {
        val temp = ArrayList(queue)
        block(temp)
        clearQueue()
        queue += temp
    }
}