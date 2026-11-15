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
import com.nextcloud.talk.models.json.dashboardEvents.DashboardEventsOCS
import com.nextcloud.talk.models.json.dashboardEvents.DashboardEventsOverall
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.wheneverBlocking

class UpcomingMeetingsRepositoryImplTest {

    private val ncApiCoroutines: NcApiCoroutines = mock()
    private val repository = UpcomingMeetingsRepositoryImpl(ncApiCoroutines)

    @Test
    fun `events that do not point at a conversation or have no start are left out`() {
        val meeting = event(roomToken = "abc123", start = START)
        givenEvents(
            listOf(
                meeting,
                event(roomToken = null, start = START),
                event(roomToken = "def456", start = null)
            )
        )

        val result = runBlocking { repository.fetchUpcomingMeetings(USER) }

        assertEquals(listOf(meeting), result.getOrThrow())
    }

    @Test
    fun `the dashboard events of the account's server are requested`() {
        givenEvents(emptyList())

        val result = runBlocking { repository.fetchUpcomingMeetings(USER) }

        assertEquals(emptyList<DashboardEvent>(), result.getOrThrow())
    }

    @Test
    fun `a server without the dashboard api gives a failure, not an exception`() {
        wheneverBlocking { ncApiCoroutines.getDashboardEvents(any(), any()) }
            .thenThrow(IllegalStateException("404"))

        val result = runBlocking { repository.fetchUpcomingMeetings(USER) }

        assertTrue(result.isFailure)
    }

    private fun givenEvents(events: List<DashboardEvent>) {
        wheneverBlocking {
            ncApiCoroutines.getDashboardEvents(
                any(),
                eq("https://cloud.example.com/ocs/v2.php/apps/spreed/api/v4/dashboard/events")
            )
        }.thenReturn(DashboardEventsOverall(DashboardEventsOCS(null, events)))
    }

    private fun event(roomToken: String?, start: Long?) =
        DashboardEvent(
            eventName = "Planning",
            eventDescription = null,
            eventLink = null,
            start = start,
            end = null,
            roomToken = roomToken,
            roomName = roomToken,
            roomDisplayName = "Project",
            roomType = null,
            roomActiveSince = null,
            invited = null,
            accepted = null
        )

    companion object {
        private const val START = 1_800_000_000L
        private val USER = User(
            id = 1,
            userId = "me",
            username = "me",
            token = "app-password",
            baseUrl = "https://cloud.example.com"
        )
    }
}
