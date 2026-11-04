package io.github.kormcrete.orderdesk.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import io.github.kormcrete.lagoon.compose.LagoonMetrics
import io.github.kormcrete.orderdesk.model.Day
import io.github.kormcrete.orderdesk.model.Seed
import kotlin.test.Test

/**
 * Clicks through the real screens and checks what they show afterwards: a change in the model has to reach the screen.
 * Runs headless, on both layouts.
 */
@OptIn(ExperimentalTestApi::class)
class InteractionTest {
    private val product = "A4 paper, 500 sheets"

    private fun newState() = OrderDeskState(Seed.store { Day(2026, 10, 12) })

    private fun desktop(state: OrderDeskState = newState(), test: ComposeUiTest.(OrderDeskState) -> Unit) =
        runDesktopComposeUiTest(width = 1280, height = 800) {
            setContent { App(state = state, metrics = LagoonMetrics.Desktop) }
            test(state)
        }

    private fun phone(state: OrderDeskState = newState(), test: ComposeUiTest.(OrderDeskState) -> Unit) =
        runDesktopComposeUiTest(width = 390, height = 844) {
            setContent { App(state = state, metrics = LagoonMetrics.Mobile) }
            test(state)
        }

    private fun ComposeUiTest.moreCount(expected: Int) =
        onAllNodesWithContentDescription("One more $product").assertCountEquals(expected)

    @Test
    fun desktopPlusAddsTheProductAndTheCartShowsIt() = desktop { state ->
        onNodeWithText("New order").performClick()
        waitForIdle()
        // The dialog is drawn last, over the orders that may show the same name.
        onAllNodesWithText(state.store.customers.first().name).onLast().performClick()
        waitForIdle()

        onNodeWithContentDescription("Add $product").performClick()
        waitForIdle()
        // The stepper replaces the + in the products table, and the cart panel has one too.
        moreCount(2)
        onNodeWithText("1 products · 1 items").assertExists()
    }

    @Test
    fun desktopSteppersChangeTheQuantityEverywhere() = desktop { state ->
        state.startDraft(state.store.customers.first())
        waitForIdle()
        onNodeWithContentDescription("Add $product").performClick()
        waitForIdle()

        onAllNodesWithContentDescription("One more $product").onFirst().performClick()
        waitForIdle()
        onNodeWithText("1 products · 2 items").assertExists()
        // The quantity shows in the table's stepper and in the cart's.
        kotlin.test.assertTrue(onAllNodesWithText("2").fetchSemanticsNodes().size >= 2, "the new quantity is not in both steppers")

        onAllNodesWithContentDescription("One less $product").onFirst().performClick()
        waitForIdle()
        onNodeWithText("1 products · 1 items").assertExists()

        onNodeWithContentDescription("Remove $product").performClick()
        waitForIdle()
        moreCount(0)
        onNodeWithContentDescription("Add $product").assertExists()
    }

    @Test
    fun desktopCancellingAnOrderChangesItsStatusOnScreen() = desktop { state ->
        val order = state.store.orders.orders.first { it.cancellable }
        state.selected = order.number
        waitForIdle()
        val cancelledBefore = onAllNodesWithText("Cancelled").fetchSemanticsNodes().size

        state.cancel(order)
        waitForIdle()
        val cancelledAfter = onAllNodesWithText("Cancelled").fetchSemanticsNodes().size
        kotlin.test.assertTrue(cancelledAfter > cancelledBefore, "Cancelled shown $cancelledBefore times before, $cancelledAfter after")
    }

    @Test
    fun desktopPlacingAnOrderPutsItInTheListAndTakesTheStock() = desktop { state ->
        val store = state.store
        val draft = state.startDraft(store.customers.first())
        val paper = store.catalog.find("PAP-A4-500")!!
        val stock = paper.stock
        val number = store.orders.nextNumber
        waitForIdle()
        onNodeWithContentDescription("Add $product").performClick()
        waitForIdle()

        state.startCheckout(draft)
        repeat(2) { state.continueCheckout() } // address, shipping
        state.checkout?.poNumber = "PO-TEST"
        state.continueCheckout() // payment
        waitForIdle()
        state.continueCheckout() // review: places the order
        waitForIdle()

        kotlin.test.assertEquals(stock - 1, paper.stock)
        kotlin.test.assertTrue(onAllNodesWithText(number).fetchSemanticsNodes().isNotEmpty(), "$number is not on screen")
    }

    /** The order on the right is taller than a low window: its last line comes into view by scrolling. */
    @Test
    fun desktopOrderScrollsInALowWindow() = runDesktopComposeUiTest(width = 1280, height = 560) {
        val state = newState().apply { selected = "ORD-1637" }
        setContent { App(state = state, metrics = LagoonMetrics.Desktop) }
        waitForIdle()
        val line = state.store.orders.find("ORD-1637")!!.lines.last().name
        onNodeWithText(line).assertIsNotDisplayed()
        onRoot().performMouseInput {
            moveTo(Offset(1100f, 300f))
            repeat(12) { scroll(1f) }
        }
        mainClock.advanceTimeBy(1000)
        waitForIdle()
        onNodeWithText(line).assertIsDisplayed()
    }

    @Test
    fun phonePlusAddsTheProductAndTheCartBarShowsIt() = phone { state ->
        state.startDraft(state.store.customers.first())
        state.push(Screen.Products)
        waitForIdle()

        onNodeWithContentDescription("Add $product").performClick()
        waitForIdle()
        moreCount(1)

        onNodeWithContentDescription("One more $product").performClick()
        waitForIdle()
        onNodeWithContentDescription("One less $product").assertExists()
        kotlin.test.assertEquals(2, state.activeDraft!!.cart.units)
        onAllNodesWithText("2").onFirst().assertExists()
    }
}
