/* MainActivity.kt */
package tk.horiuchi.missileinvader

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar


class MainActivity : AppCompatActivity() {
    private lateinit var gameView: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = "Missile Invader"
        setContentView(R.layout.activity_main)

        // ステータスバーを隠す処理を安全に呼ぶ
        window.decorView.post {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.hide(WindowInsets.Type.statusBars())
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
            }
        }
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.overflowIcon?.setTint(Color.WHITE)

        gameView = findViewById<GameView>(R.id.gameView)
        val seg1 = findViewById<ImageView>(R.id.seg1)
        val seg2 = findViewById<ImageView>(R.id.seg2)
        val missileCountText = findViewById<TextView>(R.id.missile_count_text)
        gameView.bindScoreViews(seg1, seg2)
        gameView.bindMissileCountText(missileCountText)
        val missileGauge = findViewById<ProgressBar>(R.id.missile_gauge)
        gameView.bindMissileGauge(missileGauge)

        findViewById<ImageButton>(R.id.btn_left).setOnClickListener {
            gameView.movePlayerLeft()
        }
        findViewById<ImageButton>(R.id.btn_right).setOnClickListener {
            gameView.movePlayerRight()
        }
        findViewById<ImageButton>(R.id.btn_fire).setOnClickListener {
            gameView.fireMissile()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_about -> {
                gameView.pauseGame()
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.about_title))
            .setMessage(getString(R.string.about_message))
            //.setPositiveButton(getString(R.string.about_ok), null)
            .setPositiveButton(getString(R.string.about_ok)) { dialog, _ ->
                dialog.dismiss()
                gameView.resumeGame()  // ★ ダイアログが閉じられたときにゲームを再開
            }
            .setNeutralButton(getString(R.string.about_hyperlink_name)) { _, _ ->
                val url = getString(R.string.about_hyperlink)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
            .setOnCancelListener {
                gameView.resumeGame()  // ★ 戻るボタンなどでもゲームを再開
            }
            .show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.repeatCount == 0) {
            return gameView.onKeyDown(keyCode, event)
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onPause() {
        super.onPause()
        gameView.pauseGame()
    }

    override fun onResume() {
        super.onResume()
        gameView.resumeGame()
    }
}
