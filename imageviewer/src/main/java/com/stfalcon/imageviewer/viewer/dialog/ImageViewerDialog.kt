/*
 * Copyright 2018 stfalcon.com
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

package com.stfalcon.imageviewer.viewer.dialog

import android.content.Context
import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.stfalcon.imageviewer.R
import com.stfalcon.imageviewer.viewer.builder.BuilderData
import com.stfalcon.imageviewer.viewer.view.ImageViewerView


internal class ImageViewerDialog<T>(
    context: Context,
    private val builderData: BuilderData<T>
) {

    private val dialog: AlertDialog
    private val viewerView: ImageViewerView<T> = ImageViewerView(context)
    private var animateOpen = true

    private val dialogStyle: Int
        get() = if (builderData.shouldStatusBarHide)
            R.style.ImageViewerDialog_NoStatusBar
        else
            R.style.ImageViewerDialog_Default

    init {
        setupViewerView()
        dialog = AlertDialog
            .Builder(context, dialogStyle)
            .setView(viewerView)
            .setOnKeyListener { _, keyCode, event -> onDialogKeyEvent(keyCode, event) }
            .create()
            .apply {
                setOnShowListener { viewerView.open(builderData.transitionView, animateOpen) }
                setOnDismissListener { builderData.onDismissListener?.onDismiss() }
            }
    }

    fun show(animate: Boolean) {
        animateOpen = animate

        val window = dialog.window ?: return

        //hide the navigation bar by setting some flags
        //first set 'not focusable' so that the other flags will work correctly
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Haal controller op
        val controller = WindowInsetsControllerCompat(window, window.decorView)

        controller.hide(WindowInsetsCompat.Type.systemBars())

        dialog.show()

        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            //when the current activity is edge-to-edge, the dialog will not extend behind the status bar on API 34 and below
            //this will cause other views from the activity to show
            //therefore we will show the status bar, which has a black background
            val windowInsetsController =
                WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            //when the current activity is edge-to-edge, the dialog will not extend behind the cut out on API 34 and below
            //this will cause other views from the activity to show
            //therefore we will specify that the black background content should draw behind it
            val  layoutParams = window.attributes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
            else {
                layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            window.attributes = layoutParams
        }
    }

    fun close() {
        viewerView.close()
    }

    fun dismiss() {
        dialog.dismiss()
    }

    fun updateImages(images: List<T>) {
        viewerView.updateImages(images)
    }

    fun getCurrentPosition(): Int =
        viewerView.currentPosition

    fun setCurrentPosition(position: Int): Int {
        viewerView.currentPosition = position
        return viewerView.currentPosition
    }

    fun updateTransitionImage(imageView: ImageView?) {
        viewerView.updateTransitionImage(imageView)
    }

    private fun onDialogKeyEvent(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK &&
            event.action == KeyEvent.ACTION_UP &&
            !event.isCanceled
        ) {
            if (viewerView.isScaled) {
                viewerView.resetScale()
            } else {
                viewerView.close()
            }
            return true
        }
        return false
    }

    private fun setupViewerView() {
        viewerView.apply {
            isZoomingAllowed = builderData.isZoomingAllowed
            isSwipeToDismissAllowed = builderData.isSwipeToDismissAllowed

            containerPadding = builderData.containerPaddingPixels
            imagesMargin = builderData.imageMarginPixels
            overlayView = builderData.overlayView

            setBackgroundColor(builderData.backgroundColor)
            setImages(builderData.images, builderData.startPosition, builderData.imageLoader)

            onPageChange = { position -> builderData.imageChangeListener?.onImageChange(position) }
            onDismiss = { dialog.dismiss() }
        }
    }
}