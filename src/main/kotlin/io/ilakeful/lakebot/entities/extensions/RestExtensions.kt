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

@file:Suppress("UNUSED")
package io.ilakeful.lakebot.entities.extensions

import io.ilakeful.lakebot.entities.handlers.CommandHandler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

import net.dv8tion.jda.api.requests.RestAction

import kotlin.coroutines.CoroutineContext

suspend inline fun <reified T> RestAction<T>.await(
        context: CoroutineContext = CommandHandler,
        noinline func: suspend (T) -> Unit = {}
) = queue {
    CoroutineScope(context).async {
        func(it)
    }
}