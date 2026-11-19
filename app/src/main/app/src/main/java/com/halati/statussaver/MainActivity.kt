
package com.halati.statussaver

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile

class MainActivity : Activity() {

    private val prefs by lazy {
        getSharedPreferences("halati_folders", MODE_PRIVATE)
    }

    private val bg = Color.rgb(13, 17, 19)
    private val panel = Color.rgb(25, 32, 34)
    private val green = Color.rgb(27, 190, 157)
    private val white = Color.WHITE
    private val muted = Color.rgb(165, 178, 177)

    private var currentTab = 0
    private var pendingKey = "wa"

    private val sources = listOf(
        Triple("wa", "واتساب العادي", "◉"),
        Triple("business", "واتساب الأعمال", "▣"),
        Triple("gb", "جي بي واتساب", "◆")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bg
        window.navigationBarColor = bg

        render()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun makeText(
        value: String,
        size: Float,
        color: Int = white,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER_VERTICAL
            if (bold) setTypeface(null, Typeface.BOLD)
        }
    }

    private fun rounded(color: Int, radius: Int = 16): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun button(
        title: String,
        action: () -> Unit
    ): TextView {
        return TextView(this).apply {
            text = title
            textSize = 14f
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(green, 12)
            setOnClickListener { action() }
        }
    }

    private fun addSpace(parent: LinearLayout, height: Int) {
        parent.addView(
            View(this),
            LinearLayout.LayoutParams(1, dp(height))
        )
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(14))
            setBackgroundColor(bg)
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val logo = makeText("ح", 25f, bg, true).apply {
            gravity = Gravity.CENTER
            background = rounded(green, 14)
            layoutParams = LinearLayout.LayoutParams(dp(46), dp(46))
        }

        val titleBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }

        titleBox.addView(makeText("حالتي", 23f, white, true))
        titleBox.addView(makeText("حافظ الحالات والوسائط", 12f, muted))

        top.addView(logo)
        top.addView(titleBox)

        val spacer = View(this)
        top.addView(
            spacer,
            LinearLayout.LayoutParams(0, 1, 1f)
        )

        top.addView(makeText("●", 15f, green))

        header.addView(top)
        root.addView(header)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(20))
        }

        scroll.addView(content)

        when (currentTab) {
            0 -> showHome(content)
            1 -> showSource(content, "wa")
            2 -> showSource(content, "business")
            3 -> showSource(content, "gb")
            4 -> showSettings(content)
        }

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(8), dp(4), dp(8))
            setBackgroundColor(Color.rgb(20, 26, 28))
        }

        val navItems = listOf(
            "الرئيسية",
            "واتساب",
            "الأعمال",
            "GB",
            "الإعدادات"
        )

        navItems.forEachIndexed { index, name ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(2), dp(5), dp(2), dp(5))
                setOnClickListener {
                    currentTab = index
                    render()
                }
            }

            val icon = listOf("⌂", "◉", "▣", "◆", "⚙")[index]

            item.addView(
                makeText(
                    icon,
                    20f,
                    if (currentTab == index) green else muted,
                    true
                ).apply { gravity = Gravity.CENTER }
            )

            item.addView(
                makeText(
                    name,
                    10f,
                    if (currentTab == index) green else muted,
                    currentTab == index
                ).apply { gravity = Gravity.CENTER }
            )

            nav.addView(
                item,
                LinearLayout.LayoutParams(0, dp(48), 1f)
            )
        }

        root.addView(nav)
        setContentView(root)
    }

    private fun showHome(page: LinearLayout) {
        page.addView(makeText("أهلاً بك في حالتي 👋", 21f, white, true))
        addSpace(page, 5)
        page.addView(
            makeText("احفظ حالات الصور والفيديو بسهولة", 14f, muted)
        )

        addSpace(page, 22)

        val welcome = card()
        welcome.addView(makeText("حالاتك في مكان واحد", 19f, green, true))
        addSpace(welcome, 8)
        welcome.addView(
            makeText(
                "اختر نسخة واتساب لعرض الحالات المتاحة وحفظها على جهازك.",
                14f,
                white
            )
        )
        page.addView(welcome)

        addSpace(page, 18)
        page.addView(makeText("اختر التطبيق", 18f, white, true))
        addSpace(page, 8)

        sources.forEachIndexed { index, source ->
            val row = card()

            val line = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            line.addView(
                makeText(
                    "${source.third}   ${source.second}",
                    16f,
                    white,
                    true
                ),
                LinearLayout.LayoutParams(0, dp(45), 1f)
            )

            line.addView(
                button("فتح") {
                    currentTab = index + 1
                    render()
                }
            )

            row.addView(line)

            val saved = prefs.getString(source.first, null)
            row.addView(
                makeText(
                    if (saved == null) "لم يتم ربط مجلد الحالات"
                    else "مجلد الحالات مرتبط",
                    12f,
                    if (saved == null) muted else green
                )
            )

            page.addView(row)
            addSpace(page, 10)
        }

        addSpace(page, 10)
        page.addView(
            makeText(
                "ملاحظة: يجب اختيار مجلد الحالات ومنح التطبيق الإذن بالوصول إليه.",
                12f,
                muted
            )
        )
    }

    private fun card(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(14), dp(15), dp(14))
            background = rounded(panel, 16)
        }
    }

    private fun showSource(page: LinearLayout, key: String) {
        val source = sources.first { it.first == key }

        page.addView(
            makeText(source.second, 21f, white, true)
        )
        addSpace(page, 5)
        page.addView(
            makeText("الصور والفيديوهات المتاحة للحفظ", 13f, muted)
        )
        addSpace(page, 16)

        val saved = prefs.getString(key, null)

        val folderCard = card()
        folderCard.addView(
            makeText(
                if (saved == null) "لم يتم اختيار مجلد الحالات"
                else "تم ربط مجلد الحالات بنجاح",
                14f,
                if (saved == null) muted else green,
                true
            )
        )

        addSpace(folderCard, 10)

        folderCard.addView(
            button(
                if (saved == null) "اختيار مجلد الحالات"
                else "تغيير مجلد الحالات"
            ) {
                pendingKey = key
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                startActivityForResult(intent, 410)
            }
        )

        page.addView(folderCard)
        addSpace(page, 18)

        page.addView(
            button("عرض الحالات المتاحة") {
                render()
            }
        )

        addSpace(page, 14)
        page.addView(makeText("الحالات والوسائط", 18f, white, true))
        addSpace(page, 10)

        if (saved == null) {
            page.addView(
                makeText(
                    "اختر مجلد الحالات أولاً حتى نتمكن من عرض الملفات.",
                    13f,
                    muted
                )
            )
            return
        }

        val folder = try {
            DocumentFile.fromTreeUri(this, Uri.parse(saved))
        } catch (_: Exception) {
            null
        }

        if (folder == null || !folder.canRead()) {
            page.addView(
                makeText("تعذر الوصول إلى المجلد. حاول ربطه من جديد.", 13f, muted)
            )
            return
        }

        val files = try {
            folder.listFiles()
                .filter {
                    it.isFile && (
                        it.type?.startsWith("image/") == true ||
                        it.type?.startsWith("video/") == true
                    )
                }
                .sortedByDescending { it.lastModified() }
        } catch (_: Exception) {
            emptyList()
        }

        if (files.isEmpty()) {
            page.addView(
                makeText(
                    "لا توجد حالات ظاهرة حالياً. شاهد الحالات في واتساب أولاً، وتأكد من اختيار مجلد .Statuses الصحيح.",
                    13f,
                    muted
                )
            )
            return
        }

        files.forEach { file ->
            val item = card()

            item.addView(
                makeText(
                    file.name ?: "ملف وسائط",
                    14f,
                    white,
                    true
                )
            )

            addSpace(item, 8)

            item.addView(
                makeText(
                    if (file.type?.startsWith("video/") == true) "فيديو" else "صورة",
                    12f,
                    green
                )
            )

            addSpace(item, 8)

            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            actions.addView(
                button("فتح") { openFile(file) },
                LinearLayout.LayoutParams(0, dp(44), 1f)
            )

            val gap = View(this)
            actions.addView(gap, LinearLayout.LayoutParams(dp(8), 1))

            actions.addView(
                button("حفظ") { saveFile(file) },
                LinearLayout.LayoutParams(0, dp(44), 1f)
            )

            item.addView(actions)
            addSpace(page, 9)
            page.addView(item)
        }
    }

    private fun showSettings(page: LinearLayout) {
        page.addView(makeText("الإعدادات", 22f, white, true))
        addSpace(page, 16)

        val box = card()
        box.addView(makeText("حالتي", 19f, green, true))
        addSpace(box, 8)
        box.addView(
            makeText("تطبيق مستقل لحفظ حالات الصور والفيديو.", 14f, white)
        )
        addSpace(box, 12)
        box.addView(
            makeText("الإصدار 1.0.0", 12f, muted)
        )
        page.addView(box)

        addSpace(page, 14)

        val note = card()
        note.addView(makeText("طريقة الاستخدام", 16f, white, true))
        addSpace(note, 8)
        note.addView(
            makeText(
                "1. افتح قسم نسخة واتساب المطلوبة.\n\n" +
                "2. اختر مجلد الحالات وامنح الإذن.\n\n" +
                "3. اعرض الملفات المتاحة.\n\n" +
                "4. افتح الملف أو احفظه في مجلد Download/Halati.",
                14f,
                muted
            )
        )
        page.addView(note)
    }

    @Deprecated("Retained for compatibility")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != 410 || resultCode != RESULT_OK) return

        val uri = data?.data ?: return

        try {
            val flags = data.flags and (
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            contentResolver.takePersistableUriPermission(uri, flags)

            prefs.edit()
                .putString(pendingKey, uri.toString())
                .apply()

            Toast.makeText(
                this,
                "تم ربط المجلد بنجاح",
                Toast.LENGTH_LONG
            ).show()

            render()

        } catch (e: Exception) {
            Toast.makeText(
                this,
                "تعذر ربط المجلد: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun openFile(file: DocumentFile) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(file.uri, file.type ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, "فتح باستخدام"))

        } catch (_: Exception) {
            Toast.makeText(
                this,
                "لا يوجد تطبيق مناسب لفتح هذا الملف",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun saveFile(file: DocumentFile) {
        var destination: Uri? = null

        try {
            val name = file.name ?: "halati_${System.currentTimeMillis()}"
            val mime = file.type ?: "application/octet-stream"

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
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

            destination = contentResolver.insert(collection, values)
                ?: throw Exception("تعذر إنشاء ملف الحفظ")

            val input = contentResolver.openInputStream(file.uri)
                ?: throw Exception("تعذر قراءة الملف الأصلي")

            val output = contentResolver.openOutputStream(destination)
                ?: throw Exception("تعذر الكتابة في مجلد التنزيلات")

            input.use { source ->
                output.use { target ->
                    source.copyTo(target)
                }
            }

            val completed = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }

            contentResolver.update(destination, completed, null, null)

            Toast.makeText(
                this,
                "تم حفظ الملف في Download/Halati",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            destination?.let {
                try {
                    contentResolver.delete(it, null, null)
                } catch (_: Exception) {
                }
            }

            Toast.makeText(
                this,
                "فشل الحفظ: ${e.message ?: "خطأ غير معروف"}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
