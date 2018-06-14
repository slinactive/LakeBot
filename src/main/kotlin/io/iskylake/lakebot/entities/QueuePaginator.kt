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

package io.iskylake.lakebot.entities

import com.google.common.collect.Lists

import com.sedmelluq.discord.lavaplayer.track.AudioTrack

import io.iskylake.lakebot.Immutable
import io.iskylake.lakebot.entities.extensions.*
import io.iskylake.lakebot.utils.AudioUtils
import io.iskylake.lakebot.utils.TimeUtils

import kotlinx.coroutines.experimental.async

import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.core.requests.RestAction

import java.util.concurrent.TimeUnit

data class QueuePaginator(
        private val tracks: List<AudioTrack>,
        private val event: MessageReceivedEvent,
        private val action: (Message) -> Unit = {
            try {
                it.clearReactions().queue(null) { _ -> }
            } catch (ignored: Exception) {
            }
        }
) {
    companion object {
        const val BIG_LEFT = "\u23EA"
        const val LEFT = "\u25C0"
        const val STOP = "\u23FA"
        const val RIGHT = "\u25B6"
        const val BIG_RIGHT = "\u23E9"
    }
    private val pages: List<List<AudioTrack>> = Lists.partition(tracks, 10)
    operator fun invoke(channel: MessageChannel = event.channel, page: Int = 1) = accept(channel.sendMessage(this[page]), page)
    private fun accept(rest: RestAction<Message>, pageNum: Int) = rest.queue { m ->
        if (pages.size > 1) {
            m.addReaction(BIG_LEFT).queue()
            m.addReaction(LEFT).queue()
            m.addReaction(STOP).queue()
            m.addReaction(RIGHT).queue()
            m.addReaction(BIG_RIGHT).queue({ waiter(m, pageNum) }) {
                waiter(m, pageNum)
            }
        } else {
            action(m)
        }
    }
    private fun waiter(msg: Message, num: Int = 1) = async(EventWaiter) {
        val e = EventWaiter.receiveEventRaw<MessageReactionAddEvent>(1, TimeUnit.MINUTES) {
            val isValidEmote = BIG_LEFT == it.reactionEmote.name || BIG_RIGHT == it.reactionEmote.name || LEFT == it.reactionEmote.name || STOP == it.reactionEmote.name || RIGHT == it.reactionEmote.name
            it.messageId == msg.id && isValidEmote && event.author == it.user
        }
        if (e !== null) {
            var newPageNum = num
            when (e.reactionEmote.name) {
                BIG_LEFT -> newPageNum = 1
                LEFT -> {
                    if (newPageNum > 1) {
                        newPageNum--
                    }
                }
                RIGHT -> {
                    if (newPageNum < pages.size) {
                        newPageNum++
                    }
                }
                BIG_RIGHT -> newPageNum = pages.size
                STOP -> {
                    action(msg)
                    return@async
                }
            }
            try {
                e.reaction.removeReaction(e.user).queue()
            } catch (ignored: Exception) {
            }
            msg.editMessage(this@QueuePaginator[newPageNum]).queue { msg -> waiter(msg, newPageNum) }
        } else {
            action(msg)
        }
    }
    private operator fun get(num: Int = 1) = buildEmbed {
        val page = when {
            num >= pages.size -> pages.size
            num <= 0 -> 1
            else -> num
        }
        val trackPage = pages[page - 1]
        for (track in trackPage) {
            appendln {
                "**${tracks.indexOf(track) + 1}. [${track.info.title}](${track.info.uri}) (${if (track.duration == Long.MAX_VALUE) "LIVE" else TimeUtils.asDuration(track.duration)})**"
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