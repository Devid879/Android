/*
 * Copyright (c) 2023 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.sync.impl.ui.setup

import app.cash.turbine.test
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.sync.impl.SyncAccountRepository
import com.duckduckgo.sync.impl.ui.setup.SetupAccountActivity.Companion.Screen
import com.duckduckgo.sync.impl.ui.setup.SetupAccountViewModel.Command
import com.duckduckgo.sync.impl.ui.setup.SetupAccountViewModel.ViewMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

@ExperimentalCoroutinesApi
class SetupAccountViewModelTest {
    @get:Rule
    val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

    private val syncRepostitory: SyncAccountRepository = mock()

    private val testee = SetupAccountViewModel(
        syncRepostitory,
        coroutineTestRule.testDispatcherProvider,
    )

    @Test
    fun whenFlowStartedFromSetupScreenViewModeDeviceConnected() = runTest {
        testee.viewState(Screen.DEVICE_SYNCED).test {
            val viewState = awaitItem()
            assertTrue(viewState.viewMode is ViewMode.DeviceSynced)
        }
    }

    @Test
    fun whenOnBackPressedAndViewModeDeviceConnectedThenClose() = runTest {
        testee.viewState(Screen.DEVICE_SYNCED).test {
            val viewState = awaitItem()
            assertTrue(viewState.viewMode is ViewMode.DeviceSynced)
            testee.onBackPressed()
        }
        testee.commands().test {
            val command = awaitItem()
            assertTrue(command is Command.Close)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun whenFlowStartedFromConnectionScreenViewThenRecoveryCodeMode() = runTest {
        testee.viewState(Screen.RECOVERY_CODE).test {
            val viewState = awaitItem()
            assertTrue(viewState.viewMode is ViewMode.AskSaveRecoveryCode)
        }
    }

    @Test
    fun whenOnBackPressedAndViewModeRecoveryCodeThenClose() = runTest {
        testee.viewState(Screen.RECOVERY_CODE).test {
            val viewState = awaitItem()
            assertTrue(viewState.viewMode is ViewMode.AskSaveRecoveryCode)
            testee.onBackPressed()
        }
        testee.commands().test {
            val command = awaitItem()
            assertTrue(command is Command.Close)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun whenSetupCompleteThenAskSaveRecoveryCodeCommandSent() = runTest {
        testee.viewState(Screen.DEVICE_SYNCED).test {
            testee.onSetupComplete()
            val viewState = expectMostRecentItem()
            assertTrue(viewState.viewMode is ViewMode.AskSaveRecoveryCode)
        }
    }
}
