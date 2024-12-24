package com.example.celebrare_test

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var canvasLayout: FrameLayout
    private var currentTextView: TextView? = null

    private val undoStack = mutableListOf<View>()
    private val redoStack = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        canvasLayout = findViewById(R.id.canvasLayout)

        val btnAddText: Button = findViewById(R.id.btnAddText)
        val btnUndo: ImageButton = findViewById(R.id.btnUndo)
        val btnSave: ImageButton = findViewById(R.id.btnSave)
        val btnRedo: ImageButton = findViewById(R.id.btnRedo)
        val fontSelector: Spinner = findViewById(R.id.fontSelector)
        val btnBold: ToggleButton = findViewById(R.id.btnBold)
        val btnItalic: ToggleButton = findViewById(R.id.btnItalic)
        val btnUnderline: ToggleButton = findViewById(R.id.btnUnderline)

        // Add Text
        btnAddText.setOnClickListener {
            val textView = TextView(this)
            textView.text = "New Text"
            textView.textSize = 16f
            textView.setTextColor(Color.BLACK)
            textView.setOnTouchListener { v, event -> handleDrag(v, event) }
            canvasLayout.addView(textView)
            undoStack.add(textView)
            currentTextView = textView
        }

        // Undo Action
        btnUndo.setOnClickListener {
            if (undoStack.isNotEmpty()) {
                val lastView = undoStack.removeLast()
                canvasLayout.removeView(lastView)
                redoStack.add(lastView)
            }
        }

        // Redo Action
        btnRedo.setOnClickListener {
            if (redoStack.isNotEmpty()) {
                val lastView = redoStack.removeLast()
                canvasLayout.addView(lastView)
                undoStack.add(lastView)
            }
        }

        // Font Selection
        fontSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val fontName = resources.getStringArray(R.array.font_styles)[position]
                currentTextView?.typeface = Typeface.create(fontName, Typeface.NORMAL)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Apply Styles
        btnBold.setOnCheckedChangeListener { _, isChecked ->
            currentTextView?.setTypeface(null, if (isChecked) Typeface.BOLD else Typeface.NORMAL)
        }

        btnItalic.setOnCheckedChangeListener { _, isChecked ->
            currentTextView?.setTypeface(null, if (isChecked) Typeface.ITALIC else Typeface.NORMAL)
        }

        btnUnderline.setOnCheckedChangeListener { _, isChecked ->
            currentTextView?.paintFlags = if (isChecked)
                currentTextView?.paintFlags?.or(Paint.UNDERLINE_TEXT_FLAG) ?: 0
            else
                currentTextView?.paintFlags?.and(Paint.UNDERLINE_TEXT_FLAG.inv()) ?: 0
        }
    }

    // Handle Dragging of Text
    private fun handleDrag(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                view.x = event.rawX - view.width / 5
                view.y = event.rawY - view.height / 5
            }
        }
        return true
    }
}