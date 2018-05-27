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

@file:Suppress("UNCHECKED_CAST", "UNUSED")
package io.iskylake.lakebot.entities

import io.iskylake.lakebot.entities.annotations.Author

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

import kotlin.coroutines.experimental.CoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses

import kotlinx.coroutines.experimental.*

import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.hooks.EventListener
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent

/**
 * <b>EventWaiter</b> is designed to await for JDA events after launch, handling them and returning their instances.
 *
 * This class also includes several useful utilities for use in commands.
 *
 * <b>NOTE</b>: Do NOT forget to add EventWaiter as event listener to your JDA instance!
 *
 * <p>
 *  <i>Some pieces of code have been inspired from LaxusBot</i>
 *  <a href="https://github.com/LaxusBot/Laxus/blob/master/commons/jda/src/main/kotlin/xyz/laxus/jda/listeners/EventWaiter.kt">GitHub Link</a>
 * </p>
 */
@Author("ISkylake", "TheMonitorLizard (LaxusBot implementation)")
object EventWaiter : EventListener, CoroutineContext by newFixedThreadPoolContext(3, "EventWaiter"), AutoCloseable by newFixedThreadPoolContext(3, "EventWaiter") {
    val tasks = ConcurrentHashMap<KClass<*>, MutableSet<AwaitableTask<*>>>()
    inline fun <reified E: Event> receiveEvent(
        delay: Long = -1,
        unit: TimeUnit = TimeUnit.SECONDS,
        noinline condition: suspend (E) -> Boolean
    ): Deferred<E?> = receiveEvent(E::class, condition, delay, unit)
    /**
     * Waits a predetermined amount of time for an {@link net.dv8tion.jda.core.events.Event event},
     * receives and returns it
     *
     * @param <T> the type of awaiting event
     * @param type the {@link kotlin.reflect.KClass} of event to wait for
     * @param check checks the match of the received event with the expected by the specified condition
     * @param delay the maximum amount of time to wait for. Default is -1 (infinity awaiting)
     * @param unit measurement of the timeout
     *
     * @return deferred value (non-blocking cancellable future) of possibly-null event instance
     */
    @Author("ISkylake", "TheMonitorLizard (LaxusBot implementation)")
    fun <E: Event> receiveEvent(type: KClass<E>, check: suspend (E) -> Boolean, delay: Long = -1, unit: TimeUnit = TimeUnit.SECONDS): Deferred<E?> {
        val deferred = CompletableDeferred<E?>()
        val eventSet = taskSetType(type)
        val waiting = AwaitableTask(check, deferred)
        eventSet += waiting
        if (delay > 0) {
            launch(this) {
                delay(delay, unit)
                eventSet -= waiting
                deferred.complete(null)
            }
        }
        return deferred
    }
    /**
     * Waits for a reaction for {@link net.dv8tion.jda.core.entities.Message message}
     *
     * @param msg the message from which the reaction is received
     * @param author the user from whom the reaction must be received
     * @param delay the maximum amount of time to wait for. Default is 1 minute.
     * @param unit measurement of the timeout
     *
     * @return boolean value (true, if user reacted with "\u2705", or false, if user reacted with "\u274E" or reaction wasn't received)
     */
    @Author("ISkylake")
    fun awaitConfirmation(msg: Message, author: User, delay: Long = 1, unit: TimeUnit = TimeUnit.MINUTES): Boolean {
        return runBlocking(this) {
            msg.addReaction("\u2705").complete()
            msg.addReaction("\u274E").complete()
            this@EventWaiter.receiveEvent<MessageReactionAddEvent>(delay, unit) {
                val emote = it.reactionEmote.name
                it.user == author && it.messageIdLong == msg.idLong && (emote == "\u2705" || emote == "\u274E")
            }.await()?.reactionEmote?.name == "\u2705"
        }
    }
    @Author("ISkylake")
    fun awaitConfirmationAsync(msg: Message, author: User, delay: Long = 1, unit: TimeUnit = TimeUnit.MINUTES, action: (Boolean) -> Unit): Deferred<Boolean> {
        return async(this) {
            msg.addReaction("\u2705").complete()
            msg.addReaction("\u274E").complete()
            val bool = this@EventWaiter.receiveEvent<MessageReactionAddEvent>(delay, unit) {
                val emote = it.reactionEmote.name
                it.user == author && it.messageIdLong == msg.idLong && (emote == "\u2705" || emote == "\u274E")
            }.await()?.reactionEmote?.name == "\u2705"
            action(bool)
            bool
        }
    }
    /**
     * Waits for a reaction for {@link net.dv8tion.jda.core.entities.Message message}
     *
     * @param msg the message from which the reaction is received
     * @param author the user from whom the reaction must be received
     * @param delay the maximum amount of time to wait for. Default is 1 minute.
     * @param unit measurement of the timeout
     *
     * @return possibly-null boolean value (true, if user reacted with "\u2705", or false, if user reacted with "\u274E")
     */
    @Author("ISkylake")
    fun awaitNullableConfirmation(msg: Message, author: User, delay: Long = 1, unit: TimeUnit = TimeUnit.MINUTES): Boolean? {
        return runBlocking(this) {
            msg.addReaction("\u2705").complete()
            msg.addReaction("\u274E").complete()
            val name = this@EventWaiter.receiveEvent<MessageReactionAddEvent>(delay, unit) {
                val emote = it.reactionEmote.name
                it.user == author && it.messageIdLong == msg.idLong && (emote == "\u2705" || emote == "\u274E")
            }.await()?.reactionEmote?.name
            if (name != null) name == "\u2705" else null
        }
    }
    @Author("ISkylake")
    fun awaitNullableConfirmationAsync(msg: Message, author: User, delay: Long = 1, unit: TimeUnit = TimeUnit.MINUTES, action: (Boolean?) -> Unit): Deferred<Boolean?> {
        return async(this) {
            msg.addReaction("\u2705").complete()
            msg.addReaction("\u274E").complete()
            val name = this@EventWaiter.receiveEvent<MessageReactionAddEvent>(delay, unit) {
                val emote = it.reactionEmote.name
                it.user == author && it.messageIdLong == msg.idLong && (emote == "\u2705" || emote == "\u274E")
            }.await()?.reactionEmote?.name
            val bool = if (name != null) name == "\u2705" else null
            action(bool)
            bool
        }
    }
    /**
     * Waits for an {@link net.dv8tion.jda.core.entities.Message message},
     * receives and returns it
     *
     * @param user current user the user from whom the message is received
     * @param channel the channel in which the message is received
     *
     * @return possibly-null received message
     */
    @Author("ISkylake")
    fun awaitMessage(user: User, channel: MessageChannel, delay: Long = 1, unit: TimeUnit = TimeUnit.MINUTES): Message? {
        return runBlocking(this) {
            this@EventWaiter.receiveEvent<MessageReceivedEvent>(delay, unit) {
                it.author == user && it.channel == channel
            }.await()?.message
        }
    }
    @Author("ISkylake")
    fun awaitMessageAsync(user: User, channel: MessageChannel, delay: Long = 1, unit: TimeUnit = TimeUnit.MINUTES, action: (Message?) -> Unit): Deferred<Message?> {
        return async(this) {
            val msg = this@EventWaiter.receiveEvent<MessageReceivedEvent>(delay, unit) {
                it.author == user && it.channel == channel
            }.await()?.message
            action(msg)
            msg
        }
    }
    override fun onEvent(event: Event) {
        launch(this) {
            val type = event::class
            dispatchEventType(event, type)
            for (superclass in type.allSuperclasses) {
                dispatchEventType(event, superclass)
            }
        }
    }
    private fun <E: Event> taskSetType(type: KClass<E>): MutableSet<AwaitableTask<E>> {
        return tasks.computeIfAbsent(type) {
            ConcurrentHashMap.newKeySet()
        } as MutableSet<AwaitableTask<E>>
    }
    private suspend fun <E: Event> dispatchEventType(event: E, type: KClass<*>) {
        val set = tasks[type] ?: return
        val filtered = set.filterTo(hashSetOf()) {
            val waiting = (it as AwaitableTask<E>)
            waiting(event)
        }
        set -= filtered
        if (set.isEmpty()) {
            tasks -= type
        }
    }
    class AwaitableTask<in E: Event>(val condition: suspend (E) -> Boolean, val completion: CompletableDeferred<in E?>) {
        suspend operator fun invoke(event: E): Boolean = try {
            if (condition(event)) {
                completion.complete(event)
                true
            }
            else {
                false
            }
        } catch (t: Throwable) {
            completion.completeExceptionally(t)
            true
        }
    }
}