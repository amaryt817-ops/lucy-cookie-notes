package com.lucy.cookienotes

import android.app.*
import android.os.Bundle
import android.content.*
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class Account(val id: Long, var username: String, var cookie: String, var label: String, var note: String)

class MainActivity : Activity() {
    private val bg = Color.rgb(16, 16, 20)
    private val panel = Color.rgb(29, 29, 37)
    private val panel2 = Color.rgb(38, 38, 48)
    private val purple = Color.rgb(167, 139, 250)
    private val white = Color.rgb(244, 242, 250)
    private val muted = Color.rgb(157, 154, 171)
    private val prefs by lazy { getSharedPreferences("vault", MODE_PRIVATE) }
    private val accounts = mutableListOf<Account>()
    private var labels = mutableListOf("A", "B", "C")
    private var selectedLabel = "Semua"
    private lateinit var list: LinearLayout
    private lateinit var search: EditText
    private lateinit var count: TextView
    private var revealIds = mutableSetOf<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        loadVault()
        buildScreen()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun rounded(color: Int, radius: Int = 18): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun text(value: String, size: Float, color: Int = white, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value; textSize = size; setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }
    private fun button(value: String, action: () -> Unit, filled: Boolean = false): TextView =
        text(value, 14f, if (filled) bg else purple, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(11), dp(14), dp(11))
            background = rounded(if (filled) purple else panel2, 14)
            setOnClickListener { action() }
        }
    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(bg)
            setPadding(dp(18), dp(20), dp(18), dp(12))
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val heading = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        heading.addView(text("LUCY", 11f, purple, true))
        heading.addView(text("Cookie Notes", 27f, white, true))
        heading.addView(text("Simpan dan kelompokkan akun Roblox", 12f, muted))
        header.addView(heading)
        header.addView(button("+ Akun", { showAccountDialog(null) }, true))
        root.addView(header)
        val stats = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            background = rounded(panel); setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        count = text("${accounts.size} akun tersimpan", 13f, white, true)
        stats.addView(count)
        stats.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        stats.addView(button("Kelola label", { showLabelDialog() }))
        val statLp = LinearLayout.LayoutParams(-1, -2); statLp.topMargin = dp(18)
        root.addView(stats, statLp)

        search = EditText(this).apply {
            hint = "Cari username atau catatan..."
            setHintTextColor(muted); setTextColor(white); textSize = 14f
            setSingleLine(true); setPadding(dp(14), 0, dp(14), 0)
            background = rounded(panel2, 14)
            addTextChangedListener(SimpleWatcher { renderAccounts() })
        }
        val searchLp = LinearLayout.LayoutParams(-1, dp(48)); searchLp.topMargin = dp(14)
        root.addView(search, searchLp)

        val labelScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val labelRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; tag = "labels" }
        labelScroll.addView(labelRow)
        val labelLp = LinearLayout.LayoutParams(-1, dp(48)); labelLp.topMargin = dp(10)
        root.addView(labelScroll, labelLp)

        val scroll = ScrollView(this)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        renderLabels(labelRow)
        renderAccounts()
    }

    private fun renderLabels(row: LinearLayout) {
        row.removeAllViews()
        val all = listOf("Semua") + labels
        all.forEach { label ->
            val chip = text(label, 13f, if (selectedLabel == label) bg else white, true).apply {
                gravity = Gravity.CENTER
                setPadding(dp(16), dp(9), dp(16), dp(9))
                background = rounded(if (selectedLabel == label) purple else panel2, 22)
                setOnClickListener { selectedLabel = label; renderLabels(row); renderAccounts() }
            }
            val lp = LinearLayout.LayoutParams(-2, dp(36)); lp.marginEnd = dp(8); lp.topMargin = dp(5)
            row.addView(chip, lp)
        }
    }

    private fun renderAccounts() {
        if (!::list.isInitialized) return
        list.removeAllViews()
        count.text = "${accounts.size} akun tersimpan"
        val q = if (::search.isInitialized) search.text.toString().trim().lowercase() else ""
        val filtered = accounts.filter {
            (selectedLabel == "Semua" || it.label == selectedLabel) &&
            (it.username.lowercase().contains(q) || it.note.lowercase().contains(q))
        }
        if (filtered.isEmpty()) {
            val empty = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(0, dp(54), 0, dp(54)) }
            empty.addView(text(if (accounts.isEmpty()) "Belum ada akun" else "Akun tidak ditemukan", 18f, white, true))
            empty.addView(text("Tekan + Akun untuk menambahkan catatan cookie.", 12f, muted))
            list.addView(empty); return
        }
        filtered.forEach { account ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; background = rounded(panel)
                setPadding(dp(15), dp(14), dp(15), dp(14))
            }
            val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            val nameCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
            nameCol.addView(text(account.username, 16f, white, true))
            nameCol.addView(text("LABEL  ${account.label}", 10f, purple, true))
            top.addView(nameCol)
            top.addView(button("⋮", { showActions(account) }))
            card.addView(top)
            if (account.note.isNotBlank()) {
                val note = text(account.note, 12f, muted)
                val noteLp = LinearLayout.LayoutParams(-1, -2); noteLp.topMargin = dp(8)
                card.addView(note, noteLp)
            }
            val cookieLine = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                background = rounded(panel2, 12); setPadding(dp(10), dp(4), dp(5), dp(4))
            }
            val shown = if (revealIds.contains(account.id)) account.cookie else "•".repeat(minOf(24, account.cookie.length.coerceAtLeast(8)))
            val cookieText = text(shown, 11f, muted).apply {
                maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            }
            cookieLine.addView(cookieText)
            cookieLine.addView(button(if (revealIds.contains(account.id)) "Sembunyi" else "Lihat", {
                if (revealIds.contains(account.id)) revealIds.remove(account.id) else revealIds.add(account.id)
                renderAccounts()
            }))
            val cookieLp = LinearLayout.LayoutParams(-1, dp(48)); cookieLp.topMargin = dp(12)
            card.addView(cookieLine, cookieLp)
            val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END }
            actions.addView(button("Salin cookie", {
                val clip = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clip.setPrimaryClip(ClipData.newPlainText("Roblox cookie", account.cookie))
                Toast.makeText(this@MainActivity, "Cookie disalin. Jangan bagikan kepada siapa pun.", Toast.LENGTH_SHORT).show()
            }, true))
            val actLp = LinearLayout.LayoutParams(-1, -2); actLp.topMargin = dp(10)
            actions.gravity = Gravity.END
            card.addView(actions, actLp)
            val lp = LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = dp(12)
            list.addView(card, lp)
        }
    }

    private fun showActions(a: Account) {
        AlertDialog.Builder(this).setTitle(a.username)
            .setItems(arrayOf("Edit akun", "Pindahkan label", "Hapus akun")) { _, which ->
                when (which) {
                    0 -> showAccountDialog(a)
                    1 -> showMoveDialog(a)
                    2 -> AlertDialog.Builder(this).setMessage("Hapus catatan akun ${a.username}?")
                        .setNegativeButton("Batal", null).setPositiveButton("Hapus") { _, _ ->
                            accounts.removeAll { it.id == a.id }; revealIds.remove(a.id); saveVault(); renderAccounts()
                        }.show()
                }
            }.show()
    }

    private fun showAccountDialog(existing: Account?) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), 0) }
        fun field(hintText: String, value: String = "", secret: Boolean = false): EditText {
            val e = EditText(this).apply {
                hint = hintText; setHintTextColor(muted); setTextColor(white); textSize = 14f
                setSingleLine(!secret); setPadding(dp(12), dp(8), dp(12), dp(8)); background = rounded(panel2, 12)
                setText(value)
                if (secret) { inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD; minLines = 2; gravity = Gravity.TOP }
            }
            val lp = LinearLayout.LayoutParams(-1, if (secret) dp(90) else dp(48)); lp.bottomMargin = dp(10)
            box.addView(e, lp); return e
        }
        val user = field("Username Roblox", existing?.username ?: "")
        val cookie = field("Cookie Roblox", existing?.cookie ?: "", true)
        val label = field("Label (contoh: A, B, Farming)", existing?.label ?: labels.firstOrNull().orEmpty())
        val note = field("Catatan (opsional)", existing?.note ?: "")
        AlertDialog.Builder(this).setTitle(if (existing == null) "Tambah akun" else "Edit akun")
            .setView(box).setNegativeButton("Batal", null)
            .setPositiveButton("Simpan", null).create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(-1).setOnClickListener {
                        val u = user.text.toString().trim()
                        val c = cookie.text.toString().trim()
                        val l = label.text.toString().trim().ifBlank { "Lainnya" }
                        if (u.isBlank() || c.isBlank()) {
                            Toast.makeText(this, "Username dan cookie wajib diisi.", Toast.LENGTH_SHORT).show(); return@setOnClickListener
                        }
                        if (!labels.contains(l)) labels.add(l)
                        if (existing == null) accounts.add(Account(System.currentTimeMillis(), u, c, l, note.text.toString().trim()))
                        else { existing.username = u; existing.cookie = c; existing.label = l; existing.note = note.text.toString().trim() }
                        saveVault(); dialog.dismiss(); selectedLabel = "Semua"
                        val row = (search.parent as? ViewGroup)?.parent
                        val labelRow = findViewById<ViewGroup>(android.R.id.content).findViewWithTag<View>("labels")
                        if (labelRow is LinearLayout) renderLabels(labelRow)
                        renderAccounts()
                    }
                }
                dialog.show()
            }
    }

    private fun showMoveDialog(a: Account) {
        val choices = (labels + "Buat label baru").distinct().toTypedArray()
        AlertDialog.Builder(this).setTitle("Pindahkan ${a.username}").setItems(choices) { _, which ->
            if (choices[which] == "Buat label baru") showLabelDialog()
            else { a.label = choices[which]; saveVault(); renderAccounts() }
        }.show()
    }

    private fun showLabelDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), 0) }
        val input = EditText(this).apply { hint = "Nama label baru"; setTextColor(white); setHintTextColor(muted); background = rounded(panel2); setPadding(dp(12), 0, dp(12), 0) }
        box.addView(input, LinearLayout.LayoutParams(-1, dp(48)))
        val options = labels.toTypedArray()
        AlertDialog.Builder(this).setTitle("Kelola label").setView(box)
            .setPositiveButton("Tambah", null).setNeutralButton("Ubah / hapus", null).setNegativeButton("Tutup", null)
            .create().also { d ->
                d.setOnShowListener {
                    d.getButton(-1).setOnClickListener {
                        val v = input.text.toString().trim()
                        if (v.isBlank()) return@setOnClickListener
                        if (labels.contains(v)) { Toast.makeText(this, "Label sudah ada.", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                        labels.add(v); saveVault(); d.dismiss(); refreshLabels()
                    }
                    d.getButton(-3).setOnClickListener {
                        if (labels.isEmpty()) return@setOnClickListener
                        AlertDialog.Builder(this).setTitle("Pilih label").setItems(labels.toTypedArray()) { _, index ->
                            val old = labels[index]
                            AlertDialog.Builder(this).setTitle(old).setItems(arrayOf("Ubah nama", "Hapus label")) { _, action ->
                                if (action == 0) {
                                    val edit = EditText(this).apply { setText(old); setTextColor(white) }
                                    AlertDialog.Builder(this).setTitle("Ubah label").setView(edit)
                                        .setNegativeButton("Batal", null).setPositiveButton("Simpan") { _, _ ->
                                            val new = edit.text.toString().trim()
                                            if (new.isNotBlank() && !labels.contains(new)) {
                                                labels[index] = new; accounts.filter { it.label == old }.forEach { it.label = new }
                                                saveVault(); refreshLabels()
                                            }
                                        }.show()
                                } else {
                                    AlertDialog.Builder(this).setMessage("Akun pada label ini akan dipindahkan ke 'Lainnya'.")
                                        .setNegativeButton("Batal", null).setPositiveButton("Hapus") { _, _ ->
                                            labels.remove(old); accounts.filter { it.label == old }.forEach { it.label = "Lainnya" }
                                            saveVault(); refreshLabels()
                                        }.show()
                                }
                            }.show()
                        }.show()
                    }
                }
                d.show()
            }
    }

    private fun refreshLabels() {
        selectedLabel = "Semua"
        val row = findViewById<ViewGroup>(android.R.id.content).findViewWithTag<View>("labels")
        if (row is LinearLayout) renderLabels(row)
        renderAccounts()
    }

    private fun loadVault() {
        try {
            val encrypted = prefs.getString("data", null) ?: return
            val raw = Base64.decode(encrypted, Base64.DEFAULT)
            val buffer = ByteBuffer.wrap(raw)
            val iv = ByteArray(buffer.int).also { buffer.get(it) }
            val cipherText = ByteArray(buffer.remaining()).also { buffer.get(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(128, iv))
            val json = JSONObject(String(cipher.doFinal(cipherText), Charsets.UTF_8))
            labels = mutableListOf<String>().apply {
                val arr = json.optJSONArray("labels") ?: JSONArray()
                for (i in 0 until arr.length()) add(arr.getString(i))
            }.ifEmpty { mutableListOf("A", "B", "C") }
            accounts.clear()
            val arr = json.optJSONArray("accounts") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                accounts.add(Account(o.getLong("id"), o.getString("username"), o.getString("cookie"), o.optString("label", "A"), o.optString("note", "")))
            }
        } catch (_: Exception) {
            Toast.makeText(this, "Data vault tidak dapat dibuka. Data tidak dihapus.", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveVault() {
        try {
            val json = JSONObject()
            json.put("labels", JSONArray(labels))
            val arr = JSONArray()
            accounts.forEach { a ->
                arr.put(JSONObject().put("id", a.id).put("username", a.username).put("cookie", a.cookie).put("label", a.label).put("note", a.note))
            }
            json.put("accounts", arr)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getKey())
            val iv = cipher.iv
            val encrypted = cipher.doFinal(json.toString().toByteArray(Charsets.UTF_8))
            val packed = ByteBuffer.allocate(4 + iv.size + encrypted.size).putInt(iv.size).put(iv).put(encrypted).array()
            prefs.edit().putString("data", Base64.encodeToString(packed, Base64.NO_WRAP)).apply()
        } catch (_: Exception) {
            Toast.makeText(this, "Gagal menyimpan vault.", Toast.LENGTH_LONG).show()
        }
    }

    private fun getKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey("lucy_cookie_vault", null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder("lucy_cookie_vault",
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true).build())
        return generator.generateKey()
    }
}

class SimpleWatcher(val changed: () -> Unit) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { changed() }
    override fun afterTextChanged(s: android.text.Editable?) {}
}
