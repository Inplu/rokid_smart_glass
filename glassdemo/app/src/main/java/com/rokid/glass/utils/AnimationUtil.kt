package com.rokid.glass.utils

import android.animation.Animator
import android.animation.ObjectAnimator
import android.view.View
import android.widget.ImageView

object AnimationUtil {


    fun fadeOutImageView(imageView: ImageView) {
        // 创建淡出动画：alpha从1.0f（不透明）变为0.0f（透明）
        // Create a fade-out animation from alpha 1.0f (opaque) to 0.0f (transparent).
        val fadeOut = ObjectAnimator.ofFloat(
            imageView,
            "alpha",
            1.0f, 0.0f
        ).apply {
            // 动画时长，单位：毫秒（500ms即0.5秒）
            // Animation duration in milliseconds (500 ms equals 0.5 seconds).
            duration = 300
            // 动画结束后可执行额外操作（如隐藏视图）
            // Perform additional actions when the animation ends, such as hiding the view.
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    // 动画结束后隐藏
                    // Hide the view when the animation ends.
                    imageView.visibility = View.INVISIBLE
                    // 可选：重置alpha为1.0f，避免下次显示时透明
                    // Optionally reset alpha to 1.0f so the view is opaque the next time it is shown.
                    imageView.alpha = 0.0f
                }

                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
        }
        // 启动动画
        // Start the animation.
        fadeOut.start()
    }


    fun fadeInImageView(imageView: ImageView) {
        // 创建淡出动画：alpha从1.0f（不透明）变为0.0f（透明）
        // Create a fade-out animation from alpha 1.0f (opaque) to 0.0f (transparent).
        imageView.visibility = View.VISIBLE
        val fadeOut = ObjectAnimator.ofFloat(imageView, "alpha", 0.0f, 1.0f).apply {
            // 动画时长，单位：毫秒（500ms即0.5秒）
            // Animation duration in milliseconds (500 ms equals 0.5 seconds).
            duration = 1000
            // 动画结束后可执行额外操作（如隐藏视图）
            // Perform additional actions when the animation ends, such as hiding the view.
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    imageView.visibility = View.VISIBLE
                    // 可选：重置alpha为1.0f，避免下次显示时透明
                    // Optionally reset alpha to 1.0f so the view is opaque the next time it is shown.
                    imageView.alpha = 0.0f
                }

                override fun onAnimationEnd(animation: Animator) {
                    imageView.visibility = View.VISIBLE
                    // 可选：重置alpha为1.0f，避免下次显示时透明
                    // Optionally reset alpha to 1.0f so the view is opaque the next time it is shown.
                    imageView.alpha = 1.0f
                }

                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
        }
        // 启动动画
        // Start the animation.
        fadeOut.start()
    }
}
