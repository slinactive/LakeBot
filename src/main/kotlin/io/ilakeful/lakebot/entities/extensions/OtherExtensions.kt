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

package io.ilakeful.lakebot.entities.extensions

import io.ilakeful.lakebot.WAITER_PROCESSES
import io.ilakeful.lakebot.entities.WaiterProcess

import java.util.concurrent.TimeUnit

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

import org.reflections.Reflections

inline fun <reified T> Reflections.getSubTypesOf() = getSubTypesOf(T::class.java)
suspend fun <T> selectEntity(
        event: MessageReceivedEvent,
        message: Message?,
        entities: List<T>,
        addProcess: Boolean = false,
        process: WaiterProcess?,
        time: Long = 1L,
        unit: TimeUnit = TimeUnit.MINUTES,
        block: (T) -> Unit
) = selectEntity(event, message, entities, addProcess, process, time to unit, block)
suspend fun <T> selectEntity(
        event: MessageReceivedEvent,
        message: Message?,
        entities: List<T>,
        addProcess: Boolean = false,
        process: WaiterProcess?,
        delay: Pair<Long, TimeUnit> = 1L to TimeUnit.MINUTES,
        block: (T) -> Unit
) {
    if (addProcess && process !== null) {
        WAITER_PROCESSES += process
    }
    fun removeProcess() {
        if (process !== null) {
            WAITER_PROCESSES -= process
        }
    }
    val (time, unit) = delay
    val content = event.channel.awaitMessage(event.author, time, unit)?.contentRaw
    if (content !== null) {
        when {
            content.isInt -> {
                val index = content.toInt()
                if (index in 1..entities.size) {
                    message?.delete()?.queue()
                    val entity = entities[index - 1]
                    removeProcess()
                    block(entity)
                } else {
                    event.channel.sendFailure("Try again!").await {
                        selectEntity(event, message, entities, addProcess, process, delay, block)
                    }
                }
            }
            content.toLowerCase() == "exit" -> {
                message?.delete()?.queue()
                removeProcess()
                event.channel.sendSuccess("Successfully stopped!").queue()
            }
            else -> event.channel.sendFailure("Try again!").await {
                selectEntity(event, message, entities, addProcess, process, delay, block)
            }
        }
    } else {
        message?.delete()?.queue()
        removeProcess()
        event.channel.sendFailure("Time is up!").queue()
    }
}