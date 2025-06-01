package com.bobbyprabowo.qrgen

import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.nayuki.fastqrcodegen.QrCode
import android.widget.ImageView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import android.os.Handler
import android.os.Looper
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout

private const val HINT = "Enter text for QR code"
class MainActivity : AppCompatActivity() {

    private var editText: EditText? = null
    private var bitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Set theme to follow system (dark mode or light mode)
        AppCompatDelegate.setDefaultNightMode(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create ConstraintLayout programmatically
        val constraintLayout = ConstraintLayout(this).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
            )
        }
        setContentView(constraintLayout)

        ViewCompat.setOnApplyWindowInsetsListener(constraintLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val text = ""
        val errorCorrectionLevel = QrCode.Ecc.MEDIUM

        // Helper function to generate QR Bitmap
        fun generateQrBitmap(text: String, maxQrWidth: Int, errorCorrectionLevel: QrCode.Ecc): Bitmap {
            val qr = QrCode.encodeText(text, errorCorrectionLevel)
            val size = qr.size
            val scale = maxQrWidth / size
            bitmap?.recycle()
            bitmap = createBitmap(size * scale, size * scale)
            // Detect dark mode and get background color from theme
            val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val typedValue = android.util.TypedValue()
            val theme = this@MainActivity.theme
            theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
            val bgColor = typedValue.data
            val fgColor = if (isDarkMode) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
            for (y in 0 until size) {
                for (x in 0 until size) {
                    val color = if (qr.getModule(x, y)) fgColor else bgColor
                    for (dy in 0 until scale) {
                        for (dx in 0 until scale) {
                            bitmap?.set(x * scale + dx, y * scale + dy, color)
                        }
                    }
                }
            }
            return bitmap ?: throw IllegalStateException("Bitmap generation failed")
        }

        // Decide scale size based on screen width
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        // Leave some margin (e.g., 90% of screen width)
        val maxQrWidth = (screenWidth * 0.9).toInt()

        val bitmap = generateQrBitmap(text, maxQrWidth, errorCorrectionLevel)

        // Add ImageView to display the QR code
        val imageView = ImageView(this).apply {
            setImageBitmap(bitmap)
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
                rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
            }
        }
        constraintLayout.addView(imageView)

        // Add EditText below the ImageView
        editText = EditText(this).apply {
            hint = HINT
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToBottom = imageView.id
                leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
                rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
                topMargin = 32
            }
        }
        // Assign a unique ID to the ImageView if not set
        if (imageView.id == View.NO_ID) {
            imageView.id = View.generateViewId()
        }
        editText?.id = View.generateViewId()
        // Update ImageView layout to only constrain to top
        (imageView.layoutParams as ConstraintLayout.LayoutParams).apply {
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
            rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = 0 // Remove bottom constraint
        }
        // Update EditText layout to be below the ImageView
        (editText?.layoutParams as ConstraintLayout.LayoutParams).apply {
            topToBottom = imageView.id
            leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
            rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
            topMargin = 32
            leftMargin = 32
            rightMargin = 32
        }
        constraintLayout.addView(editText)

        // Restore text if available
        val restoredText = savedInstanceState?.getString("qr_input_text", null)
        if (restoredText != null) {
            editText?.setText(restoredText)
        }

        // Listen for text changes and update QR code with debounce
        val debounceDelay = 300L // milliseconds
        val handler = Handler(Looper.getMainLooper())
        var debounceRunnable: Runnable? = null
        editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                debounceRunnable?.let { handler.removeCallbacks(it) }
                debounceRunnable = Runnable {
                    val inputText = s?.toString() ?: ""
                    val bitmapDynamic = generateQrBitmap(inputText, maxQrWidth, errorCorrectionLevel)
                    imageView.setImageBitmap(bitmapDynamic)
                }
                handler.postDelayed(debounceRunnable, debounceDelay)
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("qr_input_text", editText?.text.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val restoredText = savedInstanceState.getString("qr_input_text", "")
        editText?.setText(restoredText)
    }
}
