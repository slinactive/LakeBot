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

package io.iskylake.lakebot.entities.extensions

import io.iskylake.lakebot.entities.EventWaiter

import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.User

import java.util.concurrent.TimeUnit

suspend fun Message.awaitConfirmation(user: User, delay: Long = 1, unit: TimeUnit = TimeUnit.MINUTES) = EventWaiter.awaitConfirmation(this, user, delay, unit)
suspend fun Message.awaitNullableConfirmation(user: User, delay: Long = 1, unit: TimeUnit = TimeUnit.MINUTES) = EventWaiter.awaitNullableConfirmation(this, user,  delay, unit)