
package com.halati.statussaver

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile

class MainActivity : Activity() {
    private val prefs by lazy {
        getSharedPreferences("halati_folders", MODE_PRIVATE)
    }

    private lateinit var results: LinearLayout
    private var pendingKey: String = "wa"

    private val sources = listOf(
        "wa" to "واتساب العادي",
        "business" to "واتساب الأعمال",
        "gb" to "GBWhatsApp"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(10, 10, 10)
        window.navigationBarColor = android.graphics.Color.rgb(10, 10, 10)
        render()
    }

    private fun render() {
        val scroll = ScrollView(this)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 28)
            setBackgroundColor(android.graphics.Color.rgb(10, 10, 10))
        }

        scroll.addView(page)
        setContentView(scroll)

        page.addView(label("حالاتي", 30f, true))
        page.addView(label("حافظ الحالات والصور والفيديوهات", 15f, false))
        page.addView(label("اختر مجلد الحالات لكل نسخة واتساب مرة واحدة.", 14f, false))

        sources.forEach { (key, title) ->
            val saved = prefs.getString(key, null)

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(14, 14, 14, 14)
                setBackgroundColor(android.graphics.Color.rgb(27, 27, 27))
            }

            row.addView(label(title, 18f, true))
            row.addView(
                label(
                    if (saved == null) "لم يتم اختيار المجلد بعد"
                    else "تم ربط مجلد الحالات",
                    13f,
                    false
                )
            )

            row.addView(
                actionButton(
                    if (saved == null) "اختيار مجلد الحالات"
                    else "تغيير المجلد"
                ) {
                    pendingKey = key
                    startActivityForResult(
                        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE),
                        410
                    )
                }
            )

            val params = LinearLayout.LayoutParams(-1, -2)
            params.setMargins(0, 18, 0, 0)
            page.addView(row, params)
        }

        page.addView(actionButton("عرض الحالات المتاحة") {
            showStatuses()
        })

        results = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        page.addView(results)
    }

    @Deprecated("Deprecated in Android API; retained for broad device compatibility")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 410 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return

            val flags = data.flags and (
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            contentResolver.takePersistableUriPermission(uri, flags)
            prefs.edit().putString(pendingKey, uri.toString()).apply()

            Toast.makeText(
                this,
                "تم ربط المجلد. اضغط عرض الحالات.",
                Toast.LENGTH_LONG
            ).show()

            render()
        }
    }

    private fun showStatuses() {
        results.removeAllViews()
        results.addView(label("الحالات المكتشفة", 20f, true))

        var count = 0

        sources.forEach { (key, title) ->
            val raw = prefs.getString(key, null) ?: return@forEach

            val folder = DocumentFile.fromTreeUri(
                this,
                Uri.parse(raw)
            ) ?: return@forEach

            folder.listFiles()
                .filter {
                    it.isFile &&
                    (
                        it.type?.startsWith("image/") == true ||
                        it.type?.startsWith("video/") == true
                    )
                }
                .sortedByDescending { it.lastModified() }
                .forEach { file ->
                    count++

                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(12, 12, 12, 12)
                        setBackgroundColor(
                            android.graphics.Color.rgb(24, 24, 24)
                        )
                    }

                    row.addView(
                        label(
                            "$title • ${file.name ?: "وسائط"}",
                            14f,
                            true
                        )
                    )

                    row.addView(actionButton("فتح") {
                        openFile(file)
                    })

                    row.addView(
                        actionButton("حفظ في مجلد Download/Halati") {
                            saveFile(file)
                        }
                    )

                    val p = LinearLayout.LayoutParams(-1, -2)
                    p.setMargins(0, 8, 0, 0)
                    results.addView(row, p)
                }
        }

        if (count == 0) {
            results.addView(
                label(
                    "لم نعثر على ملفات. تأكد أنك اخترت مجلد .Statuses الصحيح وأنك شاهدت الحالات في واتساب أولاً.",
                    14f,
                    false
                )
            )
        }
    }

    private fun openFile(file: DocumentFile) {
        try {
            val uri = file.uri
            val mime = file.type ?: "*/*"

            startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, mime)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            )
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "تعذر فتح الملف على هذا الجهاز.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun saveFile(file: DocumentFile) {
        try {
            val mime = file.type ?: "application/octet-stream"

            val values = ContentValues().apply {
                put(
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    file.name ?: "halati_${System.currentTimeMillis()}"
                )
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "Download/Halati"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val collection = MediaStore.Downloads.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY
            )

            val dest = contentResolver.insert(collection, values)
                ?: throw IllegalStateException("تعذر إنشاء ملف الوجهة")

            contentResolver.openInputStream(file.uri).use { input ->
                if (input == null) {
                    throw IllegalStateException("تعذر قراءة المصدر")
                }

                contentResolver.openOutputStream(dest).use { output ->
                    if (output == null) {
                        throw IllegalStateException("تعذر الكتابة")
                    }

                    input.copyTo(output)
                }
            }

            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            contentResolver.update(dest, values, null, null)

            Toast.makeText(
                this,
                "تم الحفظ والتحقق من إنشاء الملف",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Toast.makeText(
                this,
                "فشل الحفظ: ${e.message ?: "خطأ غير معروف"}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun label(
        text: String,
        size: Float,
        bold: Boolean
    ): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size

            setTextColor(
                if (bold) android.graphics.Color.WHITE
                else android.graphics.Color.LTGRAY
            )

            if (bold) {
                setTypeface(
                    typeface,
                    android.graphics.Typeface.BOLD
                )
            }

            setPadding(0, 8, 0, 8)
        }

    private fun actionButton(
        text: String,
        action: () -> Unit
    ): Button =
        Button(this).apply {
            this.text = text
            setOnClickListener { action() }
        }
}
