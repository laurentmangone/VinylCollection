package com.example.vinylcollection

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView

class CropImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    private var touchHandler: ((MotionEvent) -> Boolean)? = null

    fun setTouchHandler(handler: (MotionEvent) -> Boolean) {
        touchHandler = handler
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val handled = touchHandler?.invoke(event) ?: false
        if (event.actionMasked == MotionEvent.ACTION_UP && handled) {
            performClick()
        }
        return handled || super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }
}
