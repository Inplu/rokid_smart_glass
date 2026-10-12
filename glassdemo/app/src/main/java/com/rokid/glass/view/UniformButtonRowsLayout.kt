package com.rokid.glass.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout

/** Keeps button rows equally tall while allowing translated and changing labels to wrap. */
class UniformButtonRowsLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val rows = (0 until childCount).map(::getChildAt).filter { it.visibility != View.GONE }
        // Measure natural heights again when a label, locale, or font scale changes.
        rows.forEach { it.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val rowHeight = rows.maxOfOrNull { it.measuredHeight } ?: return
        rows.forEach { it.layoutParams.height = rowHeight }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
