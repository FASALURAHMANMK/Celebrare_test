package com.example.celebrare_test

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding

class MainActivity : AppCompatActivity() {

    private lateinit var canvasLayout: FrameLayout
    private var selectedTextView: TextView? = null

    // Undo/Redo Stacks
    private val undoStack = mutableListOf<Triple<TextView, Int, TextViewState>>() // 1: Add, 2: Delete, 3: Modify
    private val redoStack = mutableListOf<Triple<TextView, Int, TextViewState>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        canvasLayout = findViewById(R.id.canvasLayout)

        val btnAddText: Button = findViewById(R.id.btnAddText)
        val btnUndo: ImageButton = findViewById(R.id.btnUndo)
        val btnRedo: ImageButton = findViewById(R.id.btnRedo)
        val btnDelete: ImageButton = findViewById(R.id.btnDelete)
        val btnBold: ToggleButton = findViewById(R.id.btnBold)
        val btnItalic: ToggleButton = findViewById(R.id.btnItalic)
        val btnUnderline: ToggleButton = findViewById(R.id.btnUnderline)
        val fontSelector: Spinner = findViewById(R.id.fontSelector)

        // Setup Font Selector
        val fonts = resources.getStringArray(R.array.font_styles)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fonts)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        fontSelector.adapter = adapter

        // Add Text
        btnAddText.setOnClickListener {
            val textView = createEditableTextView("Edit Me")
            canvasLayout.addView(textView)
            selectedTextView = textView
            undoStack.add(Triple(textView, 1, saveState(textView))) // Add to undo stack
        }

        // Undo
        btnUndo.setOnClickListener {
            if (undoStack.isNotEmpty()) {
                val lastAction = undoStack.removeLast()
                redoStack.add(lastAction)

                when (lastAction.second) {
                    1 -> canvasLayout.removeView(lastAction.first) // Undo Add
                    2 -> canvasLayout.addView(lastAction.first) // Undo Delete
                    3 -> restoreState(lastAction.first, lastAction.third) // Undo Modify
                }
            }
        }

        // Redo
        btnRedo.setOnClickListener {
            if (redoStack.isNotEmpty()) {
                val lastAction = redoStack.removeLast()
                undoStack.add(lastAction)

                when (lastAction.second) {
                    1 -> canvasLayout.addView(lastAction.first) // Redo Add
                    2 -> canvasLayout.removeView(lastAction.first) // Redo Delete
                    3 -> restoreState(lastAction.first, lastAction.third) // Redo Modify
                }
            }
        }

        // Delete Selected Text
        btnDelete.setOnClickListener {
            selectedTextView?.let {
                undoStack.add(Triple(it, 2, saveState(it))) // Save delete to undo stack
                canvasLayout.removeView(it)
                selectedTextView = null
            }
        }

        // Bold Style
        btnBold.setOnCheckedChangeListener { _, isChecked ->
            selectedTextView?.let {
                undoStack.add(Triple(it, 3, saveState(it)))
                it.typeface = Typeface.create(it.typeface, if (isChecked) Typeface.BOLD else Typeface.NORMAL)
            }
        }

        // Italic Style
        btnItalic.setOnCheckedChangeListener { _, isChecked ->
            selectedTextView?.let {
                undoStack.add(Triple(it, 3, saveState(it)))
                it.typeface = Typeface.create(it.typeface, if (isChecked) Typeface.ITALIC else Typeface.NORMAL)
            }
        }

        // Underline Style
        btnUnderline.setOnCheckedChangeListener { _, isChecked ->
            selectedTextView?.let {
                undoStack.add(Triple(it, 3, saveState(it)))
                it.paintFlags = if (isChecked) it.paintFlags or Paint.UNDERLINE_TEXT_FLAG
                else it.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
            }
        }

        // Font Selection
        fontSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val fontName = fonts[position]
                selectedTextView?.let {
                    undoStack.add(Triple(it, 3, saveState(it)))
                    it.typeface = Typeface.create(fontName, it.typeface?.style ?: Typeface.NORMAL)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun createEditableTextView(initialText: String): TextView {
        return TextView(this).apply {
            text = initialText
            textSize = 16f
            setTextColor(Color.BLACK)
            setPadding(8)
            isFocusable = false
            isClickable = true
            isFocusableInTouchMode = false

            // Handle selection and double-click to edit
            setOnClickListener { setSelectedTextView(this) }
            setOnTouchListener { v, event -> handleDrag(v, event) }

            // Handle double-click to edit
            setOnLongClickListener {
                switchToEditMode(this)
                true
            }

            x = canvasLayout.width / 2f - 100
            y = canvasLayout.height / 2f - 50
        }
    }

    private fun switchToEditMode(textView: TextView) {
        val editText = EditText(this).apply {
            setText(textView.text.toString())
            textSize = textView.textSize / resources.displayMetrics.scaledDensity
            setTextColor(textView.currentTextColor)
            setTypeface(textView.typeface, textView.typeface?.style ?: Typeface.NORMAL)
            paintFlags = textView.paintFlags
            setPadding(8)
            x = textView.x
            y = textView.y

            // On focus lost, switch back to TextView
            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    textView.text = this.text.toString()
                    canvasLayout.removeView(this)
                    canvasLayout.addView(textView)
                    selectedTextView = textView
                }
            }
        }

        canvasLayout.removeView(textView)
        canvasLayout.addView(editText)
        editText.requestFocus()
    }

    private fun handleDrag(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                view.x = event.rawX - view.width / 5
                view.y = event.rawY - view.height / 5
            }
        }
        return true
    }

    private fun setSelectedTextView(textView: TextView) {
        // Deselect the previous selection
        selectedTextView?.setBackgroundColor(Color.TRANSPARENT)

        // Select the new TextView
        selectedTextView = textView
        selectedTextView?.setBackgroundColor(Color.LTGRAY)
    }

    private fun saveState(textView: TextView): TextViewState {
        return TextViewState(
            textView.text.toString(),
            textView.x,
            textView.y,
            textView.typeface,
            textView.paintFlags
        )
    }

    private fun restoreState(textView: TextView, state: TextViewState) {
        textView.text = state.text
        textView.x = state.x
        textView.y = state.y
        textView.typeface = state.typeface
        textView.paintFlags = state.paintFlags
    }
}

data class TextViewState(
    val text: String,
    val x: Float,
    val y: Float,
    val typeface: Typeface?,
    val paintFlags: Int
)