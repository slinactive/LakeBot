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

import io.iskylake.lakebot.utils.TimeUtils

import net.dv8tion.jda.core.JDA

import java.lang.management.ManagementFactory
import java.lang.management.RuntimeMXBean
import java.time.OffsetDateTime

val JDA.runtimeMXBean: RuntimeMXBean
    get() = ManagementFactory.getRuntimeMXBean()
val JDA.uptime: Long
    get() = runtimeMXBean.uptime
val JDA.formattedUptime: String
    get() = TimeUtils.asText(uptime)
val JDA.startTime: Long
    get() = runtimeMXBean.startTime
val JDA.startDate: OffsetDateTime
    get() = TimeUtils.millisToDate(startTime)