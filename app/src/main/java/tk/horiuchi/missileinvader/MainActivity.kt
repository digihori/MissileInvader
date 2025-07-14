/* MainActivity.kt */
package tk.horiuchi.missileinvader

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class MainActivity : AppCompatActivity() {
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

        val gameView = findViewById<GameView>(R.id.gameView)
        val seg1 = findViewById<ImageView>(R.id.seg1)
        val seg2 = findViewById<ImageView>(R.id.seg2)
        val missileCountText = findViewById<TextView>(R.id.missile_count_text)
        gameView.bindScoreViews(seg1, seg2)
        gameView.bindMissileCountText(missileCountText)

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
                AlertDialog.Builder(this)
                    .setTitle("About")
                    .setMessage("Missile Invader\n\nRetro-inspired game based on LSI gameplay of the 1970s.")
                    .setPositiveButton("OK", null)
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
