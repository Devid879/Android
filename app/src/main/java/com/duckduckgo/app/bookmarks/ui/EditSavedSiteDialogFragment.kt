/*
 * Copyright (c) 2021 DuckDuckGo
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

package com.duckduckgo.app.bookmarks.ui

import android.os.Bundle
import android.text.Editable
import android.text.Spanned
import android.view.View
import com.duckduckgo.app.bookmarks.ui.bookmarkfolders.AddBookmarkFolderDialogFragment
import com.duckduckgo.app.browser.R
import com.duckduckgo.app.global.view.TextChangedWatcher
import com.duckduckgo.common.ui.view.text.DaxTextInput
import com.duckduckgo.common.ui.view.text.DaxTextView
import com.duckduckgo.common.utils.extensions.html
import com.duckduckgo.savedsites.api.models.SavedSite
import com.duckduckgo.savedsites.api.models.SavedSite.Bookmark
import com.duckduckgo.savedsites.api.models.SavedSite.Favorite
import com.duckduckgo.savedsites.api.models.SavedSitesNames

class EditSavedSiteDialogFragment : SavedSiteDialogFragment() {

    interface EditSavedSiteListener {
        fun onFavouriteEdited(favorite: Favorite)
        fun onBookmarkEdited(
            bookmark: Bookmark,
            oldFolderId: String,
        )
    }

    interface DeleteBookmarkListener {
        fun onSavedSiteDeleted(
            savedSite: SavedSite,
        )
    }

    var listener: EditSavedSiteListener? = null
    var deleteBookmarkListener: DeleteBookmarkListener? = null

    override fun configureUI() {
        validateBundleArguments()

        if (getSavedSite() is Favorite) {
            setToolbarTitle(getString(R.string.favoriteDialogTitleEdit))
        } else {
            setToolbarTitle(getString(R.string.bookmarkDialogTitleEdit))
            binding.savedSiteLocationContainer.visibility = View.VISIBLE
        }
        configureMenuItems()

        showAddFolderMenu = true

        populateFields(binding.titleInput, binding.urlInput, binding.savedSiteLocation)

        binding.urlInput.addTextChangedListener(urlTextWatcher)
    }

    private fun validateInput(
        newValue: String,
        existingValue: String,
    ) =
        if (newValue.isNotBlank()) newValue else existingValue

    private fun populateFields(
        titleInput: DaxTextInput,
        urlInput: DaxTextInput,
        savedLocation: DaxTextView,
    ) {
        titleInput.text = getExistingTitle()
        urlInput.text = getExistingUrl()
        getExistingBookmarkFolderName()?.let {
            if (it.isNotEmpty()) savedLocation.text = it
        }
    }

    override fun onConfirmation() {
        val savedSite = getSavedSite()

        val updatedTitle = validateInput(binding.titleInput.text, savedSite.title)
        val updatedUrl = validateInput(binding.urlInput.text, savedSite.url)

        when (savedSite) {
            is Bookmark -> {
                val parentId = arguments?.getString(AddBookmarkFolderDialogFragment.KEY_PARENT_FOLDER_ID) ?: SavedSitesNames.BOOKMARKS_ROOT
                val updatedBookmark = savedSite.copy(title = updatedTitle, url = updatedUrl, parentId = parentId)
                listener?.onBookmarkEdited(updatedBookmark, savedSite.parentId)
            }

            is Favorite -> {
                listener?.onFavouriteEdited(
                    savedSite.copy(title = updatedTitle, url = updatedUrl),
                )
            }
        }
    }

    private val urlTextWatcher = object : TextChangedWatcher() {
        override fun afterTextChanged(editable: Editable) {
            when {
                editable.toString().isBlank() -> {
                    setConfirmationVisibility(ValidationState.INVALID)
                }

                editable.toString() != getSavedSite().url -> {
                    setConfirmationVisibility(ValidationState.CHANGED)
                }

                else -> {
                    setConfirmationVisibility(ValidationState.UNCHANGED)
                }
            }
        }
    }

    private fun getSavedSite(): SavedSite = requireArguments().getSerializable(KEY_SAVED_SITE) as SavedSite
    private fun getExistingTitle(): String = getSavedSite().title
    private fun getExistingUrl(): String = getSavedSite().url
    private fun getExistingBookmarkFolderName(): String? =
        requireArguments().getSerializable(AddBookmarkFolderDialogFragment.KEY_PARENT_FOLDER_NAME) as String?

    private fun validateBundleArguments() {
        if (arguments == null) throw IllegalArgumentException("Missing arguments bundle")
        val args = requireArguments()
        if (!args.containsKey(KEY_SAVED_SITE)) {
            throw IllegalArgumentException("Bundle arguments required [KEY_PREEXISTING_TITLE, KEY_PREEXISTING_URL]")
        }
    }

    private fun configureMenuItems() {
        val toolbar = binding.savedSiteAppBar.toolbar
        toolbar.menu.findItem(R.id.action_delete).isVisible = true
        toolbar.menu.findItem(R.id.action_confirm_changes).isEnabled = false
    }

    override fun deleteConfirmationTitle(): String {
        val isFavorite = (getSavedSite() as? Favorite != null)
        val titleId = if (isFavorite) R.string.deleteFavoriteConfirmationDialogTitle else R.string.deleteBookmarkConfirmationDialogTitle
        return getString(titleId)
    }

    override fun deleteConfirmationMessage(): Spanned? {
        val isFavorite = (getSavedSite() as? Favorite != null)
        val messageId = if (isFavorite) R.string.deleteFavoriteConfirmationDialogDescription else R.string.deleteBookmarkConfirmationDialogDescription
        return getString(messageId, getExistingTitle()).html(requireContext())
    }

    override fun onDeleteConfirmed() {
        deleteBookmarkListener?.onSavedSiteDeleted(getSavedSite())
        dismiss()
    }

    companion object {
        const val KEY_SAVED_SITE = "KEY_SAVED_SITE"

        fun instance(
            savedSite: SavedSite,
            parentFolderId: String = SavedSitesNames.BOOKMARKS_ROOT,
            parentFolderName: String? = null,
        ): EditSavedSiteDialogFragment {
            val dialog = EditSavedSiteDialogFragment()
            val bundle = Bundle()
            bundle.putSerializable(KEY_SAVED_SITE, savedSite)
            bundle.putString(AddBookmarkFolderDialogFragment.KEY_PARENT_FOLDER_ID, parentFolderId)
            bundle.putString(AddBookmarkFolderDialogFragment.KEY_PARENT_FOLDER_NAME, parentFolderName)
            dialog.arguments = bundle
            return dialog
        }
    }
}
