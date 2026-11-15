/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Julius Linus <julius.linus@nextcloud.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.attachmentpreview

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import autodagger.AutoInjector
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.talk.application.NextcloudTalkApplication
import com.nextcloud.talk.ui.theme.ViewThemeUtils
import com.nextcloud.talk.ui.theme.hostViewThemeUtils
import com.nextcloud.talk.utils.SharedFileCopies
import com.nextcloud.talk.utils.preferences.AppPreferences
import javax.inject.Inject

@AutoInjector(NextcloudTalkApplication::class)
class FileAttachmentPreviewFragment : DialogFragment() {
    private lateinit var filesList: ArrayList<String>
    private var conversationName: String = ""
    private var showFilePermissionsOption: Boolean = false
    private var composeView: ComposeView? = null

    // set when the files are sent, so their copies are kept for the upload
    private var isSent = false

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: FileAttachmentPreviewViewModel by lazy {
        ViewModelProvider(this, viewModelFactory)[FileAttachmentPreviewViewModel::class.java]
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Back is handled by the screen, which asks before discarding a typed caption
        isCancelable = false
        arguments?.let {
            filesList = it.getStringArrayList(FILES_TO_UPLOAD_ARG)!!
            conversationName = it.getString(CONVERSATION_NAME_ARG, "")
            showFilePermissionsOption = it.getBoolean(FILE_PERMISSIONS_OPTION_ARG, false)
        }

        composeView = ComposeView(requireContext())
        return MaterialAlertDialogBuilder(requireContext()).setView(composeView).create()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        composeView

    @Suppress("DEPRECATION")
    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            setBackgroundDrawableResource(android.R.color.transparent)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            WindowCompat.setDecorFitsSystemWindows(this, false)
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.TRANSPARENT

            val surfaceColor = viewThemeUtils.getColorScheme(requireActivity()).surface
            val isLightSurface = surfaceColor.luminance() > LIGHT_LUMINANCE_THRESHOLD
            WindowInsetsControllerCompat(this, decorView).apply {
                isAppearanceLightStatusBars = isLightSurface
                isAppearanceLightNavigationBars = isLightSurface
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        NextcloudTalkApplication.sharedApplication!!.componentApplication.inject(this)
        viewThemeUtils = hostViewThemeUtils(activity, viewThemeUtils)

        viewModel.setInitialFiles(filesList)

        composeView?.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme(colorScheme = viewThemeUtils.getColorScheme(requireActivity())) {
                    FileAttachmentPreviewContent(
                        viewModel = viewModel,
                        conversationName = conversationName,
                        initialCompressImages = appPreferences.compressUploadImages,
                        showFilePermissionsOption = showFilePermissionsOption,
                        onDismiss = { dismiss() },
                        onSend = { files, caption, compressImages, allowUpdate ->
                            // a fragment result still reaches the chat when this fragment was recreated, e.g. on rotation
                            parentFragmentManager.setFragmentResult(
                                REQUEST_KEY,
                                bundleOf(
                                    RESULT_FILES to ArrayList(files),
                                    RESULT_CAPTION to caption,
                                    RESULT_COMPRESS_IMAGES to compressImages,
                                    RESULT_ALLOW_UPDATE to allowUpdate
                                )
                            )
                            isSent = true
                            dismiss()
                        }
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        composeView = null
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        // unsent shared files remove their copies (see SharedFileCopies), not when the chat is only recreated
        val isClosedWhileShown = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        if (!isSent && (isClosedWhileShown || activity?.isFinishing == true)) {
            SharedFileCopies.removeSharesLater(filesList.toList())
        }
    }

    companion object {

        private const val LIGHT_LUMINANCE_THRESHOLD = 0.5f
        private const val FILES_TO_UPLOAD_ARG = "FILES_TO_UPLOAD_ARG"
        private const val CONVERSATION_NAME_ARG = "CONVERSATION_NAME_ARG"
        private const val FILE_PERMISSIONS_OPTION_ARG = "FILE_PERMISSIONS_OPTION_ARG"

        const val REQUEST_KEY = "FILE_ATTACHMENT_PREVIEW_REQUEST"
        const val RESULT_FILES = "RESULT_FILES"
        const val RESULT_CAPTION = "RESULT_CAPTION"
        const val RESULT_COMPRESS_IMAGES = "RESULT_COMPRESS_IMAGES"
        const val RESULT_ALLOW_UPDATE = "RESULT_ALLOW_UPDATE"

        @JvmStatic
        fun newInstance(
            filesToUpload: MutableList<String>,
            conversationName: String,
            showFilePermissionsOption: Boolean = false
        ): FileAttachmentPreviewFragment {
            val fileAttachmentFragment = FileAttachmentPreviewFragment()
            val args = Bundle()
            args.putStringArrayList(FILES_TO_UPLOAD_ARG, ArrayList(filesToUpload))
            args.putString(CONVERSATION_NAME_ARG, conversationName)
            args.putBoolean(FILE_PERMISSIONS_OPTION_ARG, showFilePermissionsOption)
            fileAttachmentFragment.arguments = args
            return fileAttachmentFragment
        }

        val TAG: String = FileAttachmentPreviewFragment::class.java.simpleName
    }
}
