/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nextcloud.talk.upcomingmeetings.data

import com.nextcloud.talk.data.user.model.User
import com.nextcloud.talk.models.json.dashboardEvents.DashboardEvent

/** Upcoming meetings for the conversation list - the same data the web dashboard shows. */
interface UpcomingMeetingsRepository {

    /** Up to 10 events in the next 7 days, sorted by start. Fails (Result.failure) on servers before Talk 21. */
    suspend fun fetchUpcomingMeetings(user: User): Result<List<DashboardEvent>>
}
