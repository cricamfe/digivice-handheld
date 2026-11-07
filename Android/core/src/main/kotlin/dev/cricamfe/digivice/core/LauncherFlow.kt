package dev.cricamfe.digivice.core

/**
 * What the launcher activity does, as a function of whether the app may draw over other apps. Pure state: the activity
 * says what it sees ([onCheck]) and carries out the answer.
 *
 * - With the permission, the activity starts the overlay service and finishes, as it always did: no screen at all.
 * - Without it, the activity shows the first-run page (on the upper screen, where the user is) and keeps it there while
 *   the user is in the system settings granting the permission.
 * - Back from the settings with the permission granted, it starts the overlay service and finishes. This happens once,
 *   however many times the activity is resumed afterwards.
 *
 * Not thread safe: use it from the main thread.
 */
class LauncherFlow {
    enum class Action {
        /** Start the overlay service and finish the activity. */
        StartOverlayAndFinish,

        /** Show the first-run page and wait. */
        ShowFirstRun,

        /** Nothing to do: the page is already up and still waiting, or the overlay was started. */
        None,
    }

    private var firstRunShown = false
    private var overlayStarted = false

    /** Call when the activity is ready to decide and every time it is resumed. */
    fun onCheck(canDrawOverlays: Boolean): Action = when {
        overlayStarted -> Action.None
        canDrawOverlays -> {
            overlayStarted = true
            Action.StartOverlayAndFinish
        }
        firstRunShown -> Action.None
        else -> {
            firstRunShown = true
            Action.ShowFirstRun
        }
    }
}
