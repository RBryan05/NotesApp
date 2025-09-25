package com.example.notasapp.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.notasapp.R
import com.example.notasapp.databinding.ActivityEditorBinding
import com.example.notasapp.viewmodel.NotaViewModel
import jp.wasabeef.richeditor.RichEditor
import kotlinx.coroutines.launch

class EditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditorBinding
    private val viewModel: NotaViewModel by viewModels()
    private var notaId: Long = -1
    private var isEditMode = false

    // Estado original para detectar cambios
    private var originalTitulo: String = ""
    private var originalContenido: String = ""

    // Timer para evitar actualizaciones demasiado frecuentes
    private var lastSelectionChangeTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        notaId = intent.getLongExtra("NOTA_ID", -1)
        isEditMode = notaId != -1L

        setupToolbar()
        setupRichEditor()
        setupEditorButtons()

        if (isEditMode) {
            cargarNota()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = if (isEditMode) "Editar nota" else "Nueva nota"
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRichEditor() {
        binding.richEditor.apply {
            setEditorHeight(200)
            setEditorFontSize(16)
            setPadding(16, 16, 16, 16)
            setPlaceholder("Escribe tu nota aquí...")

            // Fondo transparente
            setBackgroundColor(android.graphics.Color.TRANSPARENT)

            // Cambiar color de texto según el tema
            val nightModeFlags = resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK
            when (nightModeFlags) {
                android.content.res.Configuration.UI_MODE_NIGHT_YES -> {
                    // Modo oscuro: texto blanco
                    setEditorFontColor(android.graphics.Color.WHITE)
                }
                android.content.res.Configuration.UI_MODE_NIGHT_NO,
                android.content.res.Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                    // Modo claro: texto negro
                    setEditorFontColor(android.graphics.Color.BLACK)
                }
            }

            // Listener para detectar cambios en la selección usando JavaScript
            setOnTextChangeListener {
                // Actualizar estados después de un pequeño delay para asegurar que la selección está lista
                binding.richEditor.postDelayed({
                    updateButtonStates()
                }, 50)
            }
        }
    }

    private fun setupEditorButtons() {
        // Formato de texto - ahora con manejo de estado correcto
        binding.boldButton.setOnClickListener {
            binding.richEditor.setBold()
            // Actualizar estado después de un pequeño delay
            binding.richEditor.postDelayed({
                updateButtonStates()
            }, 50)
        }

        binding.italicButton.setOnClickListener {
            binding.richEditor.setItalic()
            binding.richEditor.postDelayed({
                updateButtonStates()
            }, 50)
        }

        binding.underlineButton.setOnClickListener {
            binding.richEditor.setUnderline()
            binding.richEditor.postDelayed({
                updateButtonStates()
            }, 50)
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

        // Agregar listener de clic al editor para detectar cambios de selección
        binding.richEditor.setOnClickListener {
            updateButtonStates()
        }
    }

    /**
     * Actualiza el estado de los botones de formato usando JavaScript
     */
    private fun updateButtonStates() {
        // Consultar el estado de formato usando JavaScript
        val jsCode = """
            (function() {
                var result = {
                    bold: document.queryCommandState('bold'),
                    italic: document.queryCommandState('italic'),
                    underline: document.queryCommandState('underline'),
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
                    // Remover las comillas extras que pueda tener el resultado
                    val cleanResult = result?.removeSurrounding("\"")?.replace("\\\"", "\"")
                    if (cleanResult != null && cleanResult != "null") {
                        val jsonObject = org.json.JSONObject(cleanResult)

                        // Actualizar botones de formato de texto
                        binding.boldButton.isSelected = jsonObject.optBoolean("bold", false)
                        binding.italicButton.isSelected = jsonObject.optBoolean("italic", false)
                        binding.underlineButton.isSelected = jsonObject.optBoolean("underline", false)

                        // Actualizar botones de alineación
                        updateAlignmentButtonState(
                            jsonObject.optBoolean("justifyLeft", false),
                            jsonObject.optBoolean("justifyCenter", false),
                            jsonObject.optBoolean("justifyRight", false),
                            jsonObject.optBoolean("justifyFull", false)
                        )
                    } else {
                        // Si no hay selección o resultado, desactivar todos los botones
                        resetAllFormatButtons()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // En caso de error, desactivar todos los botones
                    resetAllFormatButtons()
                }
            }
        }
    }

    /**
     * Actualiza el estado de los botones de alineación
     */
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
            else -> {
                // Si no hay alineación específica, no seleccionar ningún botón
                // o podrías considerar left como default si prefieres
            }
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
        binding.underlineButton.isSelected = false
        resetAlignmentButtons()
    }

    private fun cargarNota() {
        lifecycleScope.launch {
            val nota = viewModel.getNotaById(notaId)
            nota?.let {
                binding.tituloEditText.setText(it.titulo)
                binding.richEditor.html = it.contenido

                // Guardar estado original
                originalTitulo = it.titulo
                originalContenido = it.contenido

                // Actualizar el estado de los botones después de cargar el contenido
                binding.richEditor.postDelayed({
                    updateButtonStates()
                }, 100)
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
                    supportActionBar?.title = "Editar nota"
                }
            }
        }

        // Actualizamos el estado original después de guardar
        originalTitulo = titulo
        originalContenido = contenido
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                guardarNota()
                true
            }
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

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