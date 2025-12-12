package com.braintreepayments.demo.test

import android.view.View
import android.view.ViewGroup
import java.util.LinkedList

/**
 * Provides an iterable for a view's child views.
 */
object TreeIterables {

    fun breadthFirst(root: View): Iterable<View> {
        return object : Iterable<View> {
            override fun iterator(): Iterator<View> {
                val queue = LinkedList<View>()
                queue.add(root)

                return object : Iterator<View> {
                    override fun hasNext(): Boolean {
                        return !queue.isEmpty()
                    }

                    override fun next(): View {
                        val nextView = queue.removeFirst()
                        if (nextView is ViewGroup) {
                            for (i in 0 until nextView.childCount) {
                                queue.add(nextView.getChildAt(i))
                            }
                        }
                        return nextView
                    }
                }
            }
        }
    }
}