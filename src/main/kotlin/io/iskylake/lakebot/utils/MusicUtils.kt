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

package io.iskylake.lakebot.utils

import io.iskylake.lakebot.audio.TrackScheduler

object MusicUtils {
    fun getProgressBar(now: Long, total: Long): String {
        val activeBlocks = (now.toFloat() / total * 15).toInt()
        val active = buildString {
            append("[")
            for (i in 0 until 15) {
                append(if (activeBlocks > i) "\u25AC" else "")
            }
            append("](https://google.com)")
        }
        val rest = buildString {
            for (i in 0 until 15) {
                append(if (activeBlocks > i) "" else "\u25AC")
            }
        }
        return "$active$rest"
    }
    fun getLoopingMode(scheduler: TrackScheduler) = when {
        scheduler.isLoop -> "Single"
        scheduler.isQueueLoop -> "Queue"
        else -> "Disabled"
    }
}