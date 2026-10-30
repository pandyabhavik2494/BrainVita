package com.pandyabhavik.brainvita

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.regex.Pattern

/**
 * Walks the game the way a person would on the emulator and saves a screenshot at each step.
 * CI runs it at the Fold's cover-screen and inner-screen sizes; the screenshots are the proof.
 */
@RunWith(AndroidJUnit4::class)
class ScreensTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    private val pkg = "com.pandyabhavik.brainvita"
    private val shots: File = File(instrumentation.targetContext.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
    private val board: BySelector = By.descContains("board")

    private fun shot(name: String) {
        device.waitForIdle()
        assertTrue("screenshot $name", device.takeScreenshot(File(shots, "$name.png")))
    }

    private fun launch() {
        val context = instrumentation.targetContext
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        assertNotNull("app window", device.wait(Until.hasObject(By.pkg(pkg).depth(0)), 10_000))
    }

    /** Starts over if a saved game is on the board, so each test begins from 32 pegs. Confirms the dialog if asked. */
    private fun freshGame() {
        if (findBoard().contentDescription.orEmpty().contains("32")) return
        val restart = device.wait(Until.findObject(By.text(Pattern.compile("(?i)restart|new game"))), 5_000)
        assertNotNull("a Restart or New game button", restart)
        restart!!.click()
        device.waitForIdle()
        // A confirmation dialog may appear with its own Restart button; press the last one on screen.
        device.wait(Until.findObjects(By.text(Pattern.compile("(?i)restart|new game|start over"))), 2_000)?.lastOrNull()?.click()
        device.waitForIdle()
        Thread.sleep(400)
        assertTrue("fresh game should have 32 pegs", findBoard().contentDescription.orEmpty().contains("32"))
    }

    private fun findBoard(): UiObject2 {
        val node = device.wait(Until.findObject(board), 10_000)
        assertNotNull("the board should be on screen (a node whose description contains 'board')", node)
        return node!!
    }

    @Test
    fun playAJumpAndUndoIt() {
        launch()
        freshGame()
        val b = findBoard()
        shot("1-fresh-game")

        // The board is drawn 7 holes wide and tall with half a hole of margin, so a hole is
        // boardWidth / 8 across. Pick up the peg two rows above the centre, then drop it in the centre.
        val bounds = b.visibleBounds
        val cx = bounds.centerX()
        val cy = bounds.centerY()
        val hole = minOf(bounds.width(), bounds.height()) / 8
        device.click(cx, cy - 2 * hole)
        device.waitForIdle()
        shot("2-peg-picked-up")
        device.click(cx, cy)
        device.waitForIdle()
        Thread.sleep(600) // let the jump animation finish
        shot("3-after-first-jump")

        val afterJump = findBoard().contentDescription ?: ""
        assertTrue("board should report 31 pegs after the first jump, was: $afterJump", afterJump.contains("31"))

        val undo = device.wait(Until.findObject(By.text(Pattern.compile("(?i)undo"))), 5_000)
        assertNotNull("an Undo button", undo)
        undo!!.click()
        device.waitForIdle()
        Thread.sleep(400)
        shot("4-after-undo")
        val afterUndo = findBoard().contentDescription ?: ""
        assertTrue("board should report 32 pegs after undo, was: $afterUndo", afterUndo.contains("32"))
    }

    @Test
    fun gameResumesAfterTheAppIsClosed() {
        launch()
        freshGame()
        val b = findBoard()
        val bounds = b.visibleBounds
        val hole = minOf(bounds.width(), bounds.height()) / 8
        device.click(bounds.centerX(), bounds.centerY() - 2 * hole)
        device.waitForIdle()
        device.click(bounds.centerX(), bounds.centerY())
        Thread.sleep(600)
        assertTrue(findBoard().contentDescription.orEmpty().contains("31"))

        device.pressHome()
        Thread.sleep(500)
        launch()
        val resumed = findBoard().contentDescription.orEmpty()
        assertTrue("the game should resume with 31 pegs, was: $resumed", resumed.contains("31"))
        shot("5-resumed")
    }
}
