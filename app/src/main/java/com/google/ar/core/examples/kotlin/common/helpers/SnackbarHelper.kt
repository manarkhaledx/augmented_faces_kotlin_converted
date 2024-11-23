package com.google.ar.core.examples.kotlin.common.helpers

import android.app.Activity
import android.view.View
import android.widget.TextView
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback
import com.google.android.material.snackbar.Snackbar

/**
 * Helper to manage the sample snackbar. Hides the Android boilerplate code and exposes simpler
 * methods.
 */
class SnackbarHelper {
    private var messageSnackbar: Snackbar? = null

    private enum class DismissBehavior {
        HIDE, SHOW, FINISH
    }

    private var maxLines = 2
    private var lastMessage = ""
    private var snackbarView: View? = null

    val isShowing: Boolean
        get() = messageSnackbar != null

    /** Shows a snackbar with a given message.  */
    fun showMessage(activity: Activity, message: String) {
        if (message.isNotEmpty() && (!isShowing || lastMessage != message)) {
            lastMessage = message
            show(activity, message, DismissBehavior.HIDE)
        }
    }

    /** Shows a snackbar with a given message, and a dismiss button.  */
    fun showMessageWithDismiss(activity: Activity, message: String) {
        show(activity, message, DismissBehavior.SHOW)
    }

    /** Shows a snackbar with a given message for Snackbar.LENGTH_SHORT milliseconds.  */
    fun showMessageForShortDuration(activity: Activity, message: String) {
        show(activity, message, DismissBehavior.SHOW, Snackbar.LENGTH_SHORT)
    }

    /** Shows a snackbar with a given message for Snackbar.LENGTH_LONG milliseconds.  */
    fun showMessageForLongDuration(activity: Activity, message: String) {
        show(activity, message, DismissBehavior.SHOW, Snackbar.LENGTH_LONG)
    }

    /**
     * Shows a snackbar with a given error message. When dismissed, will finish the activity. Useful
     * for notifying errors where no further interaction with the activity is possible.
     */
    fun showError(activity: Activity, errorMessage: String) {
        show(activity, errorMessage, DismissBehavior.FINISH)
    }

    /**
     * Hides the currently showing snackbar, if there is one. Safe to call from any thread. Safe to
     * call even if snackbar is not shown.
     */
    fun hide(activity: Activity) {
        if (!isShowing) return

        lastMessage = ""
        val snackbarToHide = messageSnackbar!!
        messageSnackbar = null
        activity.runOnUiThread { snackbarToHide.dismiss() }
    }

    fun setMaxLines(lines: Int) {
        maxLines = lines
    }

    val isDurationIndefinite: Boolean
        /** Returns whether the snackbar is currently being shown with an indefinite duration.  */
        get() = isShowing && messageSnackbar!!.duration == Snackbar.LENGTH_INDEFINITE

    /**
     * Sets the view that will be used to find a suitable parent view to hold the Snackbar view.
     *
     * To use the root layout ([android.R.id.content]), pass in `null`.
     *
     * @param snackbarView the view to pass to [Snackbar.make] which will be used to find a
     * suitable parent, which is a [androidx.coordinatorlayout.widget.CoordinatorLayout], or
     * the window decor's content view, whichever comes first.
     */
    fun setParentView(snackbarView: View?) {
        this.snackbarView = snackbarView
    }

    private fun show(
        activity: Activity,
        message: String,
        dismissBehavior: DismissBehavior,
        duration: Int = Snackbar.LENGTH_INDEFINITE
    ) {
        activity.runOnUiThread {
            val parentView = snackbarView ?: activity.findViewById<View>(android.R.id.content)
            if (parentView == null) {
                throw IllegalStateException("No suitable parent view found for Snackbar.")
            }

            messageSnackbar = Snackbar.make(parentView, message, duration)
            messageSnackbar!!.view.setBackgroundColor(BACKGROUND_COLOR)

            if (dismissBehavior != DismissBehavior.HIDE && duration == Snackbar.LENGTH_INDEFINITE) {
                messageSnackbar!!.setAction("Dismiss") { messageSnackbar!!.dismiss() }
                if (dismissBehavior == DismissBehavior.FINISH) {
                    messageSnackbar!!.addCallback(
                        object : BaseCallback<Snackbar?>() {
                            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                super.onDismissed(transientBottomBar, event)
                                activity.finish()
                            }
                        }
                    )
                }
            }

            val textView = messageSnackbar!!.view.findViewById<TextView>(
                com.google.android.material.R.id.snackbar_text
            )
            textView.maxLines = maxLines

            messageSnackbar!!.show()
        }
    }

    companion object {
        private const val BACKGROUND_COLOR = -0x40cdcdce // Custom background color for the Snackbar.
    }
}
