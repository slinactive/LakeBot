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

package io.iskylake.lakebot.entities.pagination

import io.iskylake.lakebot.entities.extensions.buildEmbed

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

import kotlin.math.*

class PaginatorBuilder<T> {
    var size = 1
    var elements = emptyList<T>()
    lateinit var embedProperty: PaginatorEmbed<T>
    lateinit var e: MessageReceivedEvent
    fun embed(block: PaginatorEmbed<T>) {
        this.embedProperty = block
    }
    inline fun event(block: () -> MessageReceivedEvent) {
        this.e = block()
    }
    inline fun list(block: () -> List<T>) {
        this.elements = block()
    }
    inline fun size(block: () -> Int) {
        val int = block()
        if (int <= 0) {
            throw IllegalArgumentException("Size cannot be negative!")
        } else {
            this.size = int
        }
    }
    operator fun invoke() = if (::e.isInitialized && ::embedProperty.isInitialized) object : Paginator<T> {
        override val pageSize = size
        override val event = e
        override val list = elements
        override fun get(num: Int) = buildEmbed {
            embedProperty(this, min(max(num, 1), pages.size), pages)
        }
    } else throw IllegalStateException("Some important properties were not initialized!")
}