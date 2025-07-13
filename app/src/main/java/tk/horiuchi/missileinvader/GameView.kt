/* MissileInvaderGame - GameView（7セグ画像対応＋位置調整） */

package tk.horiuchi.missileinvader

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.graphics.BitmapFactory
import kotlin.random.Random

class GameView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paint = Paint()
    private val gridCols = 4
    private val gridRows = 8
    private var cellWidth = 0f
    private var cellHeight = 0f

    private var playerCol = 1
    private var playerRow = 7
    private var enemyCol = 1
    private var enemyRow = 2
    private var ufoCol = 0
    private var ufoDirection = 1

    private val playerMissiles = mutableListOf<Missile>()
    private val enemyMissiles = mutableListOf<Missile>()

    var score = 0
        private set

    private val handler = android.os.Handler()
    private val updateInterval: Long = 300

    private val ufoBitmap = BitmapFactory.decodeResource(resources, R.drawable.ufo)
    private val invaderBitmap = BitmapFactory.decodeResource(resources, R.drawable.invader)
    private val fighterBitmap = BitmapFactory.decodeResource(resources, R.drawable.fighter)

    private val seg7Bitmaps: List<Bitmap> = List(10) { i ->
        BitmapFactory.decodeResource(resources, resources.getIdentifier("seg7_$i", "drawable", context.packageName))
    }
    private val seg7Blank: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.seg7_blank)

    private val updateTask = object : Runnable {
        override fun run() {
            update()
            invalidate()
            handler.postDelayed(this, updateInterval)
        }
    }

    init {
        paint.isAntiAlias = true
        handler.post(updateTask)
    }

    private fun update() {
        ufoCol += ufoDirection
        if (ufoCol < 0 || ufoCol >= gridCols) {
            ufoDirection *= -1
            ufoCol += ufoDirection
        }

        if (Random.nextBoolean()) {
            enemyCol += if (Random.nextBoolean()) 1 else -1
            enemyCol = enemyCol.coerceIn(0, gridCols - 1)
        }

        playerMissiles.forEach { it.row-- }
        enemyMissiles.forEach { it.row++ }

        val toRemove = mutableListOf<Missile>()
        for (m in playerMissiles) {
            if (m.row == enemyRow && m.col == enemyCol) {
                score += 1
                toRemove.add(m)
            } else if (m.row == 0 && m.col == ufoCol) {
                score += 5
                toRemove.add(m)
            }
        }
        for (pm in playerMissiles) {
            for (em in enemyMissiles) {
                if (pm.col == em.col && pm.row == em.row) {
                    toRemove.add(pm)
                }
            }
        }
        playerMissiles.removeAll(toRemove)

        if (Random.nextInt(10) < 3 && enemyMissiles.isEmpty()) {
            enemyMissiles.add(Missile(enemyCol, enemyRow + 1, false))
        }

        playerMissiles.removeIf { it.row <= 0 }
        enemyMissiles.removeIf { it.row >= gridRows }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 1. スコア描画サイズを先に決定
        val segHeight = 160f + 10f  // drawScoreWithBitmap() と同じ値
        val gameAreaHeight = height - segHeight - 20f // スコア高さ分引く
        // 2. 各セルサイズを調整（高さ方向のみスケーリング）
        cellWidth = width / gridCols.toFloat()
        cellHeight = gameAreaHeight / gridRows.toFloat()
        //cellWidth = width / gridCols.toFloat()
        //cellHeight = height / gridRows.toFloat()

        canvas.drawColor(Color.BLACK)

        drawScoreWithBitmap(canvas, score.coerceAtMost(99))

        val ufoRect = cellRect(ufoCol, 1)
        canvas.drawBitmap(ufoBitmap, null, ufoRect, null)

        val invRect = cellRect(enemyCol, enemyRow)
        canvas.drawBitmap(invaderBitmap, null, invRect, null)

        val playerRect = cellRect(playerCol, playerRow)
        canvas.drawBitmap(fighterBitmap, null, playerRect, null)

        paint.color = Color.YELLOW
        for (m in playerMissiles) {
            drawMissile(canvas, m.col, m.row, true)
        }
        paint.color = Color.RED
        for (m in enemyMissiles) {
            drawMissile(canvas, m.col, m.row, false)
        }
    }

    private fun drawMissile(canvas: Canvas, col: Int, row: Int, isPlayer: Boolean) {
        val cx = col * cellWidth + cellWidth / 2
        val startY = row * cellHeight + if (isPlayer) cellHeight * 0.7f else cellHeight * 0.3f
        val endY = row * cellHeight + if (isPlayer) cellHeight * 0.3f else cellHeight * 0.7f
        paint.strokeWidth = 16f
        canvas.drawLine(cx, startY, cx, endY, paint)
    }

    private fun cellRect(col: Int, row: Int): RectF {
        return RectF(
            col * cellWidth + 10,
            row * cellHeight + 10,
            (col + 1) * cellWidth - 10,
            (row + 1) * cellHeight - 10
        )
    }

    private fun drawScoreWithBitmap(canvas: Canvas, value: Int) {
        val digit1 = if (value >= 10) value / 10 else -1
        val digit2 = value % 10

        val segWidth = 100f
        val segHeight = 160f

        // 上端中央にスコアを表示するよう調整
        val top = 10f
        val totalWidth = segWidth * 2 + 10f
        val left1 = (width - totalWidth) / 2
        val left2 = left1 + segWidth + 10f

        val bmp1 = if (digit1 >= 0) seg7Bitmaps[digit1] else seg7Blank
        val bmp2 = seg7Bitmaps[digit2]

        val rect1 = RectF(left1, top, left1 + segWidth, top + segHeight)
        val rect2 = RectF(left2, top, left2 + segWidth, top + segHeight)

        canvas.drawBitmap(bmp1, null, rect1, null)
        canvas.drawBitmap(bmp2, null, rect2, null)
    }

    data class Missile(var col: Int, var row: Int, val isPlayer: Boolean)

    fun moveLeft() {
        playerCol = (playerCol - 1).coerceAtLeast(0)
    }

    fun moveRight() {
        playerCol = (playerCol + 1).coerceAtMost(gridCols - 1)
    }

    fun fireMissile() {
        if (playerMissiles.none { it.row < playerRow }) {
            playerMissiles.add(Missile(playerCol, playerRow, true))
        }
    }
}
