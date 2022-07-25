package com.loneoaktech.tests.coloranimator

import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.Color.argb
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MainActivity : AppCompatActivity() {

    companion object {
        val COLORS = listOf(
            R.color.color0,
            R.color.color1,
            R.color.color2,
            R.color.color3,
            R.color.color4,
            R.color.color2,
            R.color.color1
        )

        fun Int.nextPhase() = ((this % COLORS.size) + 1) % COLORS.size

        const val PHASE_DURATION = 4_000L
        const val START_DELAY = 2_000L
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val mainContainer = findViewById<View>(R.id.mainContainer)
                val label = findViewById<TextView>(R.id.labelView)
                fun getPhaseColor(phase: Int) = getColor(COLORS[phase % COLORS.size])

                var phase = 0
                val startColor = getPhaseColor(phase)
                mainContainer.setBackgroundColor( startColor )
                label.setTextColor( startColor.complementaryColor() )

                while(true) {
                    animateColor(
                        mainContainer,
                        label,
                        getPhaseColor(phase),
                        getPhaseColor(phase.nextPhase())
                    )

                    phase = phase.nextPhase()
                }
            }
        }
    }

    private suspend fun animateColor(
        colorView: View,
        labelView: TextView,
        fromColor: Int,
        toColor: Int
    ) = suspendCancellableCoroutine { continuation ->

        val animator = ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor)

        val touchListener = object: View.OnClickListener {
            override fun onClick(v: View?) {
                if (animator.isRunning) {
                    if (!animator.isPaused)
                        animator.pause()
                    else
                        animator.resume()
                }
            }
        }

        colorView.setOnClickListener(touchListener)


        animator.duration = PHASE_DURATION
        animator.addUpdateListener { a ->
            val backgroundColor = a.animatedValue as Int
            colorView.setBackgroundColor(backgroundColor)


            val textColor = backgroundColor.complementaryColor()
            labelView.text = String.format("#%06X", 0xFFFFFF and backgroundColor)
            labelView.setTextColor( textColor )
            labelView.alpha = 1.0f
        }

        fun exit(animation: Animator?) {
            colorView.setOnClickListener(null)
            if (continuation.isActive) {
                // Don't call resume if not currently active. Animator can make multiple end callbacks.
                continuation.resume(
                    ((animation as? ValueAnimator)?.animatedValue as? Int) ?: 0
                )
            }
        }

        continuation.invokeOnCancellation {
            animator.cancel()
            colorView.setOnClickListener(null)
        }

        animator.addListener( object: Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
                exit(animation)
            }

            override fun onAnimationCancel(animation: Animator?) {
                exit(animation)
            }

            override fun onAnimationRepeat(animation: Animator?) {
            }

        })

        animator.start()
    }

    fun Int.complementaryColor(): Int {
        val a = this shr 24 and 0xff
        val r = this shr 16 and 0xff
        val g = this shr 8 and 0xff
        val b = this and 0xff

        return argb(
            a,
            (r xor 0xff) and 0xff,
            (g xor 0xff) and 0xff,
            (b xor 0xff) and 0xff
        )
    }
}