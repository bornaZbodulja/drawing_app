package com.androidDev.drawingapp

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.toColor
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlinx.android.synthetic.main.save_drawing_pop_up.view.*
import kotlinx.android.synthetic.main.stroke_width_pop_up.view.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.Exception
import java.util.jar.Manifest
import kotlin.math.abs


class MainActivity : AppCompatActivity() {

    private lateinit var canvasView: CanvasView
    private val STORAGE_PERMISSION_CODE = 101
    private val SHARE_REQUEST_CODE = 1001
    private lateinit var uri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_main)
        canvasView = CanvasView(context = applicationContext)
        canvasView.apply {
            systemUiVisibility = SYSTEM_UI_FLAG_FULLSCREEN
            contentDescription = getString(R.string.canvas_content_description)
        }
        setContentView(canvasView)

        // hide status bar
        this.window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater.inflate(R.menu.main_activity_menu, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.strokeSizeAction -> { strokeSizePopUpSetup() }
            R.id.strokeColorAction -> { strokeColorPopUpSetup() }
            R.id.clearDrawingAction -> { canvasView.clearCanvas() }
            R.id.saveDrawingAction -> {
                if(checkForWritePermission()){
                    saveDrawingPopUpSetup()
                }else{
                    requestForWritePermission()
                }
            }
            R.id.shareDrawingAction -> { shareOptionAction() }
        }

        return true
    }

    private fun strokeSizePopUpSetup(){
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        val view = LayoutInflater.from(this).inflate(R.layout.stroke_width_pop_up, null)

        dialogBuilder.setView(view)
        dialogBuilder.setCancelable(true)
        val alertDialog = dialogBuilder.create()

        view.strokeSizePopUpEditText.setText(canvasView.STROKE_WIDTH.toString())

        view.setStrokeSizeButton.setOnClickListener {
            val strokeSize = view.strokeSizePopUpEditText.text.trim().toString()
            canvasView.STROKE_WIDTH = clampStrokeSize(strokeSize.toFloat())

            alertDialog.dismiss()
            canvasView.changeStrokeSize()
        }

        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
        alertDialog.show()
    }

    private fun strokeColorPopUpSetup() {
        val builder: ColorPickerDialog.Builder =
            ColorPickerDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)

        builder.apply {
            setTitle("Pick Stroke Color: ")
            setPreferenceName("MyColorPickerDialog")

            colorPickerView.setInitialColor(canvasView.drawColor)

            setPositiveButton(R.string.select_text,
                ColorEnvelopeListener { envelope, _ ->
                    Log.d("COLOR_TAG", envelope.color.toString())
                    canvasView.changeStrokeColor(envelope.color)
                })

            setNegativeButton(R.string.cancel_text, DialogInterface.OnClickListener { dialog, which -> dialog.dismiss() })

            attachAlphaSlideBar(true)
            attachBrightnessSlideBar(true)
            setBottomSpace(12)
            show()
        }

    }

    private fun clampStrokeSize(strokeSize: Float): Float{
        when{
            abs(strokeSize) in 1.0f..1000.0f -> { return  abs(strokeSize) }
            abs(strokeSize) < 1.0f -> { return 1.0f }
            else -> { return 1000.0f }
        }
    }

    private fun saveDrawingPopUpSetup(){
        val dialogBuilder = AlertDialog.Builder(this)
        val view = LayoutInflater.from(this).inflate(R.layout.save_drawing_pop_up, null)

        dialogBuilder.setView(view)
        dialogBuilder.setCancelable(true)

        val alertDialog = dialogBuilder.create()

        view.saveButton.setOnClickListener {
            val fileName = view.drawingNameEditText.text.toString().trim()

            if(fileName.isEmpty()){
                Toast.makeText(applicationContext, "Please enter non empty file name", Toast.LENGTH_SHORT).show()
            }else{
                // saveBitmap(takeScreenshot(canvasView), fileName)
                saveToGallery(applicationContext, takeScreenshot(canvasView), "DrawingAppSketches", fileName + ".png")
                alertDialog.dismiss()
            }
        }

        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
        alertDialog.show()
    }

    private fun takeScreenshot(view: View): Bitmap{
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) bgDrawable.draw(canvas)
        else canvas.drawColor(Color.WHITE)
        view.draw(canvas)
        return returnedBitmap
    }

    private fun saveToGallery(context: Context, bitmap: Bitmap, albumName: String, filename: String) {
        val write: (OutputStream) -> Boolean = {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/$albumName")
            }

            context.contentResolver.let {
                it.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?.let { uri ->
                    it.openOutputStream(uri)?.let(write)
                }
            }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + File.separator + albumName
            val file = File(imagesDir)
            if (!file.exists()) {
                file.mkdir()
            }
            val image = File(imagesDir, filename)
            try {
                write(FileOutputStream(image))
                Toast.makeText(applicationContext, "Drawing successfully saved", Toast.LENGTH_SHORT).show()
            }catch (e: Exception){
                Toast.makeText(applicationContext, "An error occurred while saving the drawing", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun checkForWritePermission(): Boolean{
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestForWritePermission(){
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == STORAGE_PERMISSION_CODE && grantResults.isNotEmpty()){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(applicationContext, "Storage Permission Granted", Toast.LENGTH_SHORT).show()
                saveDrawingPopUpSetup()
            }else{
                Toast.makeText(applicationContext, "Storage Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareOptionAction(){

        val shareIntent = Intent()

        try {
            uri = getImageUri(applicationContext, takeScreenshot(canvasView))

            shareIntent.apply {
                setAction(Intent.ACTION_SEND)
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "image/png"
            }
            startActivityForResult(Intent.createChooser(shareIntent,"Share via: "), SHARE_REQUEST_CODE)

        }catch (e: Exception){
            e.printStackTrace()
            Toast.makeText(applicationContext, "Error occurred while trying to share a drawing", Toast.LENGTH_SHORT).show()
        }

    }

    private fun getImageUri(context: Context, image: Bitmap): Uri{
        val bytes = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.PNG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(context.contentResolver, image, "Title", null)
        Log.d("IMAGE_PATH", path)
        return Uri.parse(path)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == SHARE_REQUEST_CODE && this::uri.isInitialized){
            this.contentResolver.delete(uri, null, null)
            // uri.recycle()
            Log.d("URI_DELETE_TAG", "image uri successfully deleted")
        }
    }
}