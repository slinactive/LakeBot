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

package io.iskylake.lakebot.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason

import java.util.Queue
import java.util.concurrent.LinkedBlockingQueue

class TrackScheduler(private val player: AudioPlayer) : AudioEventAdapter() {
    val queue: Queue<AudioTrack> = LinkedBlockingQueue()
    var isLoop = false
    var isQueueLoop = false
    operator fun plusAssign(track: AudioTrack) {
        if (!player.startTrack(track, true)) {
            queue.offer(track)
        }
    }
    fun nextTrack(track: AudioTrack) {
        when {
            isLoop -> player.startTrack(track.makeClone(), false)
            isQueueLoop -> {
                queue += track.makeClone()
                player.startTrack(queue.poll(), false)
            }
            else -> if (queue.isNotEmpty()) {
                player.startTrack(queue.poll(), false)
            } else {
                player.destroy()
            }
        }
    }
    override fun onTrackStart(player: AudioPlayer, track: AudioTrack) {
        queue += track
    }
    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        if (endReason.mayStartNext) {
            queue -= track
            nextTrack(track)
        }
    }
    fun clearQueue() = queue.clear()
    inline fun receiveQueue(block: (MutableList<AudioTrack>) -> Unit) {
        val temp = ArrayList(queue)
        block(temp)
        clearQueue()
        queue += temp
    }
}