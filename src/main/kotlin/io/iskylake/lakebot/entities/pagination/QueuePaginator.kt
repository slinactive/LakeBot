/*
 * Copyright 2017-2018 (c) Alexander "ISkylake" Shevchenko
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.iskylake.lakebot.entities.pagination

import com.sedmelluq.discord.lavaplayer.track.AudioTrack

import io.iskylake.lakebot.Immutable
import io.iskylake.lakebot.entities.Paginator
import io.iskylake.lakebot.entities.extensions.*
import io.iskylake.lakebot.utils.AudioUtils
import io.iskylake.lakebot.utils.TimeUtils

import net.dv8tion.jda.core.events.message.MessageReceivedEvent

data class QueuePaginator(override val list: List<AudioTrack>, override val event: MessageReceivedEvent) : Paginator<AudioTrack> {
    override val pageSize = 10
    override fun get(num: Int) = buildEmbed {
        val page = when {
            num >= pages.size -> pages.size
            num <= 0 -> 1
            else -> num
        }
        for (track in pages[page - 1]) {
            appendln {
                "**${list.indexOf(track) + 1}. [${track.info.title}](${track.info.uri}) (${if (track.duration == Long.MAX_VALUE) "LIVE" else TimeUtils.asDuration(track.duration)})**"
            }
        }
        color {
            Immutable.SUCCESS
        }
        author("LakePlayer") {
            event.selfUser.effectiveAvatarUrl
        }
        field(title = "Total Songs:") {
            "${AudioUtils[event.guild].trackScheduler.queue.size + 1} songs"
        }
        field(title = "Total Duration:") {
            TimeUtils.asDuration(AudioUtils[event.guild].trackScheduler.queue.map { it.duration }.filter { it != Long.MAX_VALUE }.sum())
        }
        field(title = "Looping:") {
            if (AudioUtils[event.guild].trackScheduler.isLoop) "Enabled" else "Disabled"
        }
        field(title = "Now Playing:") {
            "**[${AudioUtils[event.guild].audioPlayer.playingTrack.info.title}](${AudioUtils[event.guild].audioPlayer.playingTrack.info.uri})** (${TimeUtils.asDuration(AudioUtils[event.guild].audioPlayer.playingTrack.position)}/${if (AudioUtils[event.guild].audioPlayer.playingTrack.duration == Long.MAX_VALUE) "LIVE" else TimeUtils.asDuration(AudioUtils[event.guild].audioPlayer.playingTrack.duration)})"
        }
        footer(event.author.effectiveAvatarUrl) {
            "Page $page/${pages.size} | Requested by ${event.author.tag}"
        }
    }
}