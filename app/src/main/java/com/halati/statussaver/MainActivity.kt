
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
import android.view.WindowInsets
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.util.Size
import androidx.documentfile.provider.DocumentFile

class MainActivity : Activity() {

    private val prefs by lazy {
        getSharedPreferences("halati_folders", MODE_PRIVATE)
    }

    private val bg = Color.rgb(12, 17, 19)
    private val panel = Color.rgb(25, 33, 35)
    private val green = Color.rgb(27, 190, 157)
    private val white = Color.WHITE
    private val muted = Color.LTGRAY

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
        window.navigationBarColor = Color.rgb(20, 26, 28)

        render()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int = 16) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun text(
        value: String,
        size: Float = 14f,
        color: Int = white,
        bold: Boolean = false
    ) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        gravity = Gravity.CENTER_VERTICAL
        if (bold) setTypeface(null, Typeface.BOLD)
    }

    private fun button(
        value: String,
        action: () -> Unit
    ) = TextView(this).apply {
        text = value
        textSize = 14f
        gravity = Gravity.CENTER
        setTextColor(Color.BLACK)
        typeface = Typeface.DEFAULT_BOLD
        setPadding(dp(15), dp(12), dp(15), dp(12))
        background = rounded(green, 12)
        isClickable = true
        setOnClickListener {
            try {
                action()
            } catch (e: Exception) {
                toast("حدث خطأ: ${e.message}")
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun space(parent: LinearLayout, height: Int) {
        parent.addView(
            View(this),
            LinearLayout.LayoutParams(1, dp(height))
        )
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(15), dp(14), dp(15), dp(14))
        background = rounded(panel)
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        // معالجة المساحة الآمنة أعلى وأسفل الشاشة
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)

            root.setOnApplyWindowInsetsListener { view, insets ->
                val bars = insets.getInsets(
                    WindowInsets.Type.systemBars()
                )
                view.setPadding(0, bars.top, 0, bars.bottom)
                insets
            }
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(15), dp(18), dp(15))
        }

        val logo = text("ح", 25f, Color.BLACK, true).apply {
            gravity = Gravity.CENTER
            background = rounded(green, 14)
        }

        header.addView(
            logo,
            LinearLayout.LayoutParams(dp(46), dp(46))
        )

        val titleBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }

        titleBox.addView(text("حالتي", 23f, white, true))
        titleBox.addView(text("حالاتك ووسائطك في مكان واحد", 11f, muted))
        header.addView(titleBox)

        root.addView(header)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(17), dp(10), dp(17), dp(20))
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
            setPadding(dp(2), dp(5), dp(2), dp(5))
            setBackgroundColor(Color.rgb(20, 26, 28))
        }

        val names = listOf("الرئيسية", "واتساب", "الأعمال", "GB", "الإعدادات")
        val icons = listOf("⌂", "◉", "▣", "◆", "⚙")

        names.forEachIndexed { index, name ->
            val color = if (currentTab == index) green else muted

            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                isClickable = true
                setPadding(dp(1), dp(5), dp(1), dp(5))

                setOnClickListener {
                    currentTab = index
                    render()
                }
            }

            item.addView(
                text(icons[index], 19f, color, true).apply {
                    gravity = Gravity.CENTER
                }
            )

            item.addView(
                text(name, 9f, color, currentTab == index).apply {
                    gravity = Gravity.CENTER
                }
            )

            nav.addView(
                item,
                LinearLayout.LayoutParams(0, dp(52), 1f)
            )
        }

        root.addView(nav)
        setContentView(root)

        if (android.os.Build.VERSION.SDK_INT >= 30) {
            root.requestApplyInsets()
        }
    }

    private fun showHome(page: LinearLayout) {
        page.addView(text("أهلاً بك في حالتي 👋", 22f, white, true))
        space(page, 5)
        page.addView(text("احفظ حالات الصور والفيديو بسهولة", 13f, muted))
        space(page, 18)

        val welcome = card()
        welcome.addView(text("كل حالاتك في مكان واحد", 19f, green, true))
        space(welcome, 8)
        welcome.addView(
            text(
                "اربط مجلد الحالات مرة واحدة، وسيحتفظ التطبيق بإذن الوصول.",
                13f
            )
        )
        page.addView(welcome)

        space(page, 20)
        page.addView(text("اختر نسخة واتساب", 18f, white, true))
        space(page, 10)

        sources.forEachIndexed { index, source ->
            val row = card()

            val line = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            line.addView(
                text(
                    "${source.third}   ${source.second}",
                    15f,
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

            space(row, 5)
            row.addView(
                text(
                    if (saved == null) "يحتاج ربط مجلد الحالات"
                    else "✓ مجلد الحالات مرتبط",
                    12f,
                    if (saved == null) muted else green
                )
            )

            page.addView(row)
            space(page, 10)
        }
    }

    private fun showSource(page: LinearLayout, key: String) {
        val source = sources.first { it.first == key }

        page.addView(text(source.second, 21f, white, true))
        space(page, 5)
        page.addView(text("معرض الصور والفيديوهات", 13f, muted))
        space(page, 15)

        val saved = prefs.getString(key, null)

        val folderCard = card()

        folderCard.addView(
            text(
                if (saved == null) "لم يتم ربط مجلد الحالات"
                else "✓ تم ربط مجلد الحالات",
                15f,
                if (saved == null) muted else green,
                true
            )
        )

        space(folderCard, 10)

        folderCard.addView(
            button(
                if (saved == null) "ربط مجلد الحالات"
                else "تغيير مجلد الحالات"
            ) {
                chooseFolder(key)
            }
        )

        page.addView(folderCard)
        space(page, 12)

        if (saved == null) {
            val help = card()
            help.addView(text("كيف تربط المجلد؟", 16f, green, true))
            space(help, 8)
            help.addView(
                text(
                    "اضغط ربط مجلد الحالات، ثم افتح مجلد واتساب المناسب، واختر مجلد .Statuses واضغط استخدام هذا المجلد.",
                    13f,
                    muted
                )
            )
            page.addView(help)
            return
        }

        val refresh = button("↻   تحديث الحالات") {
            render()
            toast("تم تحديث قائمة الحالات")
        }

        page.addView(refresh)
        space(page, 17)

        page.addView(text("الحالات المتاحة", 18f, white, true))
        space(page, 10)

        val folder = try {
            DocumentFile.fromTreeUri(this, Uri.parse(saved))
        } catch (_: Exception) {
            null
        }

        if (folder == null || !folder.canRead()) {
            page.addView(
                text("تعذر الوصول إلى المجلد. اربطه من جديد.", 13f, muted)
            )
            return
        }

        val files = try {
            folder.listFiles()
                .filter {
                    it.isFile &&
                    (
                        it.type?.startsWith("image/") == true ||
                        it.type?.startsWith("video/") == true
                    )
                }
                .sortedByDescending { it.lastModified() }
        } catch (_: Exception) {
            emptyList()
        }

        if (files.isEmpty()) {
            val empty = card()
            empty.addView(text("لا توجد حالات ظاهرة حالياً", 16f, white, true))
            space(empty, 8)
            empty.addView(
                text(
                    "شاهد الحالة في واتساب أولاً، وتأكد من اختيار مجلد .Statuses الصحيح.",
                    13f,
                    muted
                )
            )
            page.addView(empty)
            return
        }

        page.addView(
            text("عدد الملفات: ${files.size}", 12f, green, true)
        )
        space(page, 10)

        files.forEach { file ->
            addMediaItem(page, file)
            space(page, 10)
        }
    }

    private fun addMediaItem(
        page: LinearLayout,
        file: DocumentFile
    ) {
        val item = card()

        val line = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val image = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = rounded(Color.rgb(37, 47, 49), 10)
        }

        line.addView(
            image,
            LinearLayout.LayoutParams(dp(92), dp(92))
        )

        val isVideo = file.type?.startsWith("video/") == true

        if (!isVideo) {
            try {
                val thumbnail = contentResolver.loadThumbnail(
                    file.uri,
                    Size(dp(180), dp(180)),
                    null
                )
                image.setImageBitmap(thumbnail)
            } catch (_: Exception) {
                image.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        } else {
            image.setImageResource(android.R.drawable.ic_media_play)
        }

        val details = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }

        details.addView(
            text(
                file.name ?: "وسائط واتساب",
                13f,
                white,
                true
            )
        )

        space(details, 8)

        details.addView(
            text(
                if (isVideo) "فيديو" else "صورة",
                12f,
                green
            )
        )

        space(details, 8)

        details.addView(
            text("اضغط فتح للمعاينة", 11f, muted)
        )

        line.addView(
            details,
            LinearLayout.LayoutParams(0, dp(92), 1f)
        )

        item.addView(line)
        space(item, 12)

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        actions.addView(
            button("فتح") {
                openFile(file)
            },
            LinearLayout.LayoutParams(0, dp(44), 1f)
        )

        actions.addView(
            View(this),
            LinearLayout.LayoutParams(dp(8), 1)
        )

        actions.addView(
            button("حفظ") {
                saveFile(file)
            },
            LinearLayout.LayoutParams(0, dp(44), 1f)
        )

        item.addView(actions)
        page.addView(item)
    }

    private fun chooseFolder(key: String) {
        pendingKey = key

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }

        @Suppress("DEPRECATION")
        startActivityForResult(intent, 410)
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

            currentTab = when (pendingKey) {
                "wa" -> 1
                "business" -> 2
                else -> 3
            }

            toast("تم ربط المجلد بنجاح")
            render()

        } catch (e: Exception) {
            toast("تعذر ربط المجلد: ${e.message}")
        }
    }

    private fun openFile(file: DocumentFile) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(file.uri, file.type ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(
                Intent.createChooser(intent, "فتح باستخدام")
            )

        } catch (e: Exception) {
            toast("تعذر فتح الملف: ${e.message}")
        }
    }

    private fun saveFile(file: DocumentFile) {
        var destination: Uri? = null

        try {
            val name = file.name
                ?: "halati_${System.currentTimeMillis()}"

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
                ?: throw Exception("تعذر قراءة الملف")

            val output = contentResolver.openOutputStream(destination)
                ?: throw Exception("تعذر حفظ الملف")

            input.use { source ->
                output.use { target ->
                    source.copyTo(target)
                }
            }

            val completed = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }

            contentResolver.update(destination, completed, null, null)

            toast("تم الحفظ في Download/Halati")

        } catch (e: Exception) {
            destination?.let {
                try {
                    contentResolver.delete(it, null, null)
                } catch (_: Exception) {
                }
            }

            toast("فشل الحفظ: ${e.message}")
        }
    }

    private fun showSettings(page: LinearLayout) {
        page.addView(text("الإعدادات", 22f, white, true))
        space(page, 15)

        val box = card()
        box.addView(text("حالتي", 20f, green, true))
        space(box, 8)
        box.addView(
            text(
                "تطبيق لحفظ حالات الصور والفيديو.",
                14f
            )
        )
        space(box, 10)
        box.addView(text("الإصدار 1.0.0", 12f, muted))
        page.addView(box)

        space(page, 12)

        val info = card()
        info.addView(text("إدارة المجلدات", 16f, white, true))
        space(info, 8)

        sources.forEach { source ->
            info.addView(
                button("إعادة ربط ${source.second}") {
                    chooseFolder(source.first)
                }
            )
            space(info, 8)
        }

        page.addView(info)
    }
}
