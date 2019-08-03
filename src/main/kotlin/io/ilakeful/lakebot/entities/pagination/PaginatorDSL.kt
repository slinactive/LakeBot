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

package io.ilakeful.lakebot.entities.pagination

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

inline fun <reified T> buildPaginator(block: PaginatorBuilder<T>.() -> Unit) = PaginatorBuilder<T>().apply(block)()
inline fun <reified T> buildPaginator(
        event: MessageReceivedEvent,
        size: Int,
        elements: List<T>,
        noinline block: PaginatorEmbed<T>
) = buildPaginator<T> {
    event { event }
    size { size }
    list { elements }
    embed(block)
}