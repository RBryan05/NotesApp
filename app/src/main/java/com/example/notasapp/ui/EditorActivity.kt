package com.example.notasapp.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.notasapp.databinding.ActivityEditorBinding
import com.example.notasapp.viewmodel.NotaViewModel
import kotlinx.coroutines.launch

class EditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditorBinding
    private val viewModel: NotaViewModel by viewModels()
    private var notaId: Long = -1
    private var isEditMode = false

    private var originalTitulo: String = ""
    private var originalContenido: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        notaId = intent.getLongExtra("NOTA_ID", -1)
        isEditMode = notaId != -1L

        setupTopBar()
        binding.loadingProgress.visibility = View.VISIBLE

        binding.root.post {
            setupRichEditor()
            setupEditorButtons()

            if (isEditMode) {
                cargarNota()
            }
            binding.loadingProgress.visibility = View.GONE
        }
    }

    private fun setupTopBar() {
        // Botón de retroceso
        binding.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Botón de guardar
        binding.saveButton.setOnClickListener {
            guardarNota()
        }

        // Botón de eliminar
        binding.deleteButton.setOnClickListener {
            if (isEditMode) {
                mostrarDialogoEliminar()
            } else {
                Toast.makeText(this, "No hay nota para eliminar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRichEditor() {
        binding.richEditor.apply {
            setEditorHeight(200)
            setEditorFontSize(16)
            setPlaceholder("Escribe tu nota aquí...")
            setBackgroundColor(android.graphics.Color.TRANSPARENT)

            // Simplifica la detección de modo oscuro
            val isNightMode = (resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES

            setEditorFontColor(if (isNightMode) android.graphics.Color.WHITE else android.graphics.Color.BLACK)

            setOnTextChangeListener {
                // Reduce el delay
                binding.richEditor.postDelayed({ updateButtonStates() }, 100)
            }
        }
    }

    private fun setupEditorButtons() {
        // Formato de texto
        binding.boldButton.setOnClickListener {
            binding.richEditor.setBold()
            binding.richEditor.postDelayed({ updateButtonStates() }, 50)
        }

        binding.italicButton.setOnClickListener {
            binding.richEditor.setItalic()
            binding.richEditor.postDelayed({ updateButtonStates() }, 50)
        }

        // Alineación
        binding.alignLeftButton.setOnClickListener {
            binding.richEditor.setAlignLeft()
            resetAlignmentButtons()
            binding.alignLeftButton.isSelected = true
        }

        binding.alignCenterButton.setOnClickListener {
            binding.richEditor.setAlignCenter()
            resetAlignmentButtons()
            binding.alignCenterButton.isSelected = true
        }

        binding.alignRightButton.setOnClickListener {
            binding.richEditor.setAlignRight()
            resetAlignmentButtons()
            binding.alignRightButton.isSelected = true
        }

        binding.justifyButton.setOnClickListener {
            binding.richEditor.focusEditor()
            binding.richEditor.loadUrl("javascript:document.execCommand('justifyFull', false, null);")
            resetAlignmentButtons()
            binding.justifyButton.isSelected = true
        }

        // Botón de texto normal (quita todos los formatos)
        binding.normalTextButton.setOnClickListener {
            binding.richEditor.removeFormat()
            binding.richEditor.postDelayed({ updateButtonStates() }, 50)
        }

        binding.richEditor.setOnClickListener {
            updateButtonStates()
        }
    }

    private fun updateButtonStates() {
        val jsCode = """
            (function() {
                var result = {
                    bold: document.queryCommandState('bold'),
                    italic: document.queryCommandState('italic'),
                    justifyLeft: document.queryCommandState('justifyLeft'),
                    justifyCenter: document.queryCommandState('justifyCenter'),
                    justifyRight: document.queryCommandState('justifyRight'),
                    justifyFull: document.queryCommandState('justifyFull')
                };
                return JSON.stringify(result);
            })();
        """.trimIndent()

        binding.richEditor.evaluateJavascript(jsCode) { result ->
            runOnUiThread {
                try {
                    val cleanResult = result?.removeSurrounding("\"")?.replace("\\\"", "\"")
                    if (cleanResult != null && cleanResult != "null") {
                        val jsonObject = org.json.JSONObject(cleanResult)

                        binding.boldButton.isSelected = jsonObject.optBoolean("bold", false)
                        binding.italicButton.isSelected = jsonObject.optBoolean("italic", false)

                        updateAlignmentButtonState(
                            jsonObject.optBoolean("justifyLeft", false),
                            jsonObject.optBoolean("justifyCenter", false),
                            jsonObject.optBoolean("justifyRight", false),
                            jsonObject.optBoolean("justifyFull", false)
                        )
                    } else {
                        resetAllFormatButtons()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    resetAllFormatButtons()
                }
            }
        }
    }

    private fun updateAlignmentButtonState(
        justifyLeft: Boolean,
        justifyCenter: Boolean,
        justifyRight: Boolean,
        justifyFull: Boolean
    ) {
        resetAlignmentButtons()

        when {
            justifyLeft -> binding.alignLeftButton.isSelected = true
            justifyCenter -> binding.alignCenterButton.isSelected = true
            justifyRight -> binding.alignRightButton.isSelected = true
            justifyFull -> binding.justifyButton.isSelected = true
        }
    }

    private fun resetAlignmentButtons() {
        binding.alignLeftButton.isSelected = false
        binding.alignCenterButton.isSelected = false
        binding.alignRightButton.isSelected = false
        binding.justifyButton.isSelected = false
    }

    private fun resetAllFormatButtons() {
        binding.boldButton.isSelected = false
        binding.italicButton.isSelected = false
        resetAlignmentButtons()
    }

    private fun cargarNota() {
        lifecycleScope.launch {
            val nota = viewModel.getNotaById(notaId)
            nota?.let {
                binding.tituloEditText.setText(it.titulo)

                originalTitulo = it.titulo
                originalContenido = it.contenido

                // Cargar HTML después de que el editor esté completamente listo
                binding.richEditor.post {
                    binding.richEditor.html = it.contenido
                    binding.richEditor.postDelayed({ updateButtonStates() }, 200)
                }
            }
        }
    }

    private fun guardarNota() {
        val titulo = binding.tituloEditText.text.toString().trim()
        val contenido = binding.richEditor.html ?: ""

        if (titulo.isEmpty() && contenido.isEmpty()) {
            Toast.makeText(this, "No se puede guardar una nota vacía", Toast.LENGTH_SHORT).show()
            return
        }

        if (isEditMode) {
            viewModel.updateNota(notaId, titulo, contenido)
            Toast.makeText(this, "Nota actualizada", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.insertNota(titulo, contenido) { id ->
                runOnUiThread {
                    Toast.makeText(this, "Nota guardada", Toast.LENGTH_SHORT).show()
                    notaId = id
                    isEditMode = true
                }
            }
        }

        originalTitulo = titulo
        originalContenido = contenido
    }

    private fun mostrarDialogoEliminar() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Eliminar nota")
            .setMessage("¿Estás seguro de que quieres eliminar esta nota?")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    val nota = viewModel.getNotaById(notaId)
                    nota?.let {
                        viewModel.deleteNota(it)
                        Toast.makeText(this@EditorActivity, "Nota eliminada", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onPause() {
        super.onPause()
        autoGuardarNota()
    }

    private fun autoGuardarNota() {
        val titulo = binding.tituloEditText.text.toString().trim()
        val contenido = binding.richEditor.html ?: ""

        // No guardes si está vacío
        if (titulo.isEmpty() && contenido.isEmpty()) return

        // No guardes si no cambió nada
        if (titulo == originalTitulo && contenido == originalContenido) return

        if (isEditMode) {
            viewModel.updateNota(notaId, titulo, contenido)
        } else {
            viewModel.insertNota(titulo, contenido) { id ->
                notaId = id
                isEditMode = true
            }
        }

        originalTitulo = titulo
        originalContenido = contenido
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val tituloActual = binding.tituloEditText.text.toString().trim()
        val contenidoActual = binding.richEditor.html ?: ""

        val hayCambios = tituloActual != originalTitulo || contenidoActual != originalContenido

        if (hayCambios) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Guardar cambios")
                .setMessage("¿Quieres guardar los cambios antes de salir?")
                .setPositiveButton("Guardar") { _, _ ->
                    guardarNota()
                    super.onBackPressed()
                }
                .setNegativeButton("Descartar") { _, _ ->
                    super.onBackPressed()
                }
                .setNeutralButton("Cancelar", null)
                .show()
        } else {
            super.onBackPressed()
        }
    }
}