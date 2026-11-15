/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nextcloud.talk.upcomingmeetings.data

import com.nextcloud.talk.api.NcApiCoroutines
import com.nextcloud.talk.data.user.model.User
import com.nextcloud.talk.models.json.dashboardEvents.DashboardEvent
import com.nextcloud.talk.utils.ApiUtils

class UpcomingMeetingsRepositoryImpl(private val ncApiCoroutines: NcApiCoroutines) : UpcomingMeetingsRepository {

    override suspend fun fetchUpcomingMeetings(user: User): Result<List<DashboardEvent>> =
        runCatching {
            val credentials = ApiUtils.getCredentials(user.username, user.token)!!
            val response = ncApiCoroutines.getDashboardEvents(
                credentials,
                ApiUtils.getUrlForDashboardEvents(user.baseUrl!!)
            )
            response.ocs?.data.orEmpty().filter { it.roomToken != null && it.start != null }
        }
}
