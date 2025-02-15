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

package com.duckduckgo.sync.impl.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.duckduckgo.anvil.annotations.InjectWith
import com.duckduckgo.common.ui.DuckDuckGoActivity
import com.duckduckgo.common.ui.view.show
import com.duckduckgo.common.ui.viewbinding.viewBinding
import com.duckduckgo.di.scopes.ActivityScope
import com.duckduckgo.sync.impl.databinding.ActivityShowQrCodeBinding
import com.duckduckgo.sync.impl.ui.ShowQRCodeViewModel.Command.Error
import com.duckduckgo.sync.impl.ui.ShowQRCodeViewModel.Command.LoginSucess
import com.duckduckgo.sync.impl.ui.ShowQRCodeViewModel.Command.ShowMessage
import com.duckduckgo.sync.impl.ui.ShowQRCodeViewModel.ViewState
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@InjectWith(ActivityScope::class)
class ShowQRCodeActivity : DuckDuckGoActivity() {
    private val binding: ActivityShowQrCodeBinding by viewBinding()
    private val viewModel: ShowQRCodeViewModel by bindViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupToolbar(binding.includeToolbar.toolbar)
        observeUiEvents()
    }

    private fun observeUiEvents() {
        viewModel
            .viewState()
            .flowWithLifecycle(lifecycle, Lifecycle.State.CREATED)
            .onEach { viewState -> renderViewState(viewState) }
            .launchIn(lifecycleScope)

        viewModel
            .commands()
            .flowWithLifecycle(lifecycle, Lifecycle.State.CREATED)
            .onEach { processCommand(it) }
            .launchIn(lifecycleScope)
    }

    private fun renderViewState(viewState: ViewState) {
        viewState.qrCodeBitmap?.let {
            binding.qrCodeImageView.show()
            binding.qrCodeImageView.setImageBitmap(it)
            binding.copyCodeButton.setOnClickListener {
                viewModel.onCopyCodeClicked()
            }
        }
    }

    private fun processCommand(command: ShowQRCodeViewModel.Command) {
        when (command) {
            Error -> {
                setResult(RESULT_CANCELED)
                finish()
            }

            LoginSucess -> {
                setResult(RESULT_OK)
                finish()
            }

            is ShowMessage -> {
                Toast.makeText(this, command.messageId, Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        internal fun intent(context: Context): Intent {
            return Intent(context, ShowQRCodeActivity::class.java)
        }
    }
}
