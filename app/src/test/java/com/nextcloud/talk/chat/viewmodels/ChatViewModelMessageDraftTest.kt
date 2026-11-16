/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nextcloud.talk.chat.viewmodels

import android.app.Application
import com.nextcloud.talk.chat.data.ChatMessageRepository
import com.nextcloud.talk.chat.data.io.AudioFocusRequestManager
import com.nextcloud.talk.chat.data.io.MediaRecorderManager
import com.nextcloud.talk.chat.data.network.ChatNetworkDataSource
import com.nextcloud.talk.conversationlist.data.OfflineConversationsRepository
import com.nextcloud.talk.data.user.model.User
import com.nextcloud.talk.logger.Logger
import com.nextcloud.talk.models.MessageDraft
import com.nextcloud.talk.models.domain.ConversationModel
import com.nextcloud.talk.repositories.reactions.ReactionsRepository
import com.nextcloud.talk.repositories.unifiedsearch.UnifiedSearchRepository
import com.nextcloud.talk.threadsoverview.data.ThreadsRepository
import com.nextcloud.talk.users.UserManager
import com.nextcloud.talk.utils.preferences.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The stored draft is read once per chat. When the input is recreated, e.g. on rotation, the draft in memory is
 * the newer one: the save of an empty input after sending may not have reached the database yet.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
class ChatViewModelMessageDraftTest {

    private val conversations: OfflineConversationsRepository = mockWithEmptyFlows()
    private val storedConversation: ConversationModel = mock()

    private val viewModel = ChatViewModel(
        mock<Logger>(),
        mock<AppPreferences>(),
        mock<ChatNetworkDataSource>(),
        mockWithEmptyFlows<ChatMessageRepository>(),
        mock<ThreadsRepository>(),
        conversations,
        mock<ReactionsRepository>(),
        mock<UnifiedSearchRepository>(),
        mock<MediaRecorderManager>(),
        mock<AudioFocusRequestManager>(),
        mockWithEmptyFlows<UserManager>(),
        CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher()),
        USER,
        TOKEN,
        null
    )

    @Test
    fun `the stored draft is loaded when the chat is opened`() {
        givenStoredDraft("typed before the app was left")

        runBlocking { viewModel.updateMessageDraft() }

        assertEquals("typed before the app was left", viewModel.messageDraft.messageText)
    }

    @Test
    fun `a recreated input keeps the draft in memory instead of an older stored one`() {
        givenStoredDraft("typed before the app was left")
        runBlocking { viewModel.updateMessageDraft() }

        viewModel.messageDraft = MessageDraft(messageText = "")
        runBlocking { viewModel.updateMessageDraft() }

        assertEquals("", viewModel.messageDraft.messageText)
    }

    private fun givenStoredDraft(text: String) {
        whenever(storedConversation.messageDraft).thenReturn(MessageDraft(messageText = text))
        wheneverBlocking { conversations.getLocallyStoredConversation(USER, TOKEN) }.thenReturn(storedConversation)
    }

    private inline fun <reified T : Any> mockWithEmptyFlows(): T =
        mock(
            defaultAnswer = { invocation ->
                if (invocation.method.returnType == Flow::class.java) {
                    emptyFlow<Any>()
                } else {
                    Mockito.RETURNS_DEFAULTS.answer(invocation)
                }
            }
        )

    companion object {
        private const val TOKEN = "room-token"
        private val USER = User(
            id = 1,
            userId = "me",
            username = "me",
            token = "app-password",
            baseUrl = "https://server.example.com"
        )
    }
}
