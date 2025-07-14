package tk.horiuchi.missileinvader

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import kotlin.random.Random

class GameView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paint = Paint()
    private var cellWidth = 0f
    private var cellHeight = 0f

    private val gridCols = 4
    private val gridRows = 7

    private val background: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.missileinvader_background)

    private var score = 0
    private var scoreOverflow = false
    private var blinkCount = 0
    private var blinking = false
    private var scoreSeg1: ImageView? = null
    private var scoreSeg2: ImageView? = null
    private val segMap = mutableMapOf<Char, Bitmap>()

    private val playerBitmap = BitmapFactory.decodeResource(resources, R.drawable.fighter)
    private var playerCol = 1
    private val playerRow = 6
    private var playerMissileCount = 150
    private var playerFlashing = false
    private var playerFlashCount = 0

    private val ufoBitmap = BitmapFactory.decodeResource(resources, R.drawable.ufo)
    private val invaderBitmap = BitmapFactory.decodeResource(resources, R.drawable.invader)

    private var ufoCol = 0
    private var ufoRow = 0
    private var ufoDirection = 1
    private var ufoVisible = true
    private var ufoFlashing = false
    private var ufoFlashCount = 0
    private var ufoNextSide = 1

    private var invaderCol = 1
    private var invaderRow = 1
    private var invaderVisible = true
    private var invaderFlashing = false
    private var invaderFlashCount = 0

    private val playerMissiles = mutableListOf<Missile>()
    private val enemyMissiles = mutableListOf<Missile>()
    private val missileSpeed = 1
    private val missileInterval = 100L
    private val missilePaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
    }

    private var isGameOver = false
    private var showGameOver = false
    private var canRestart = false

    private val missileRunnable = object : Runnable {
        override fun run() {
            if (isGameOver) return
            val toRemove = mutableListOf<Missile>()
            for (m in playerMissiles) {
                m.prevRow = m.row
                m.row -= missileSpeed
                if (m.row < 0) toRemove.add(m)
            }
            playerMissiles.removeAll(toRemove)
            checkCollisions()  // ← ★追加：移動直後に衝突判定

            if (!isGameOver &&
                playerMissileCount == 0 &&
                playerMissiles.isEmpty() &&
                !playerFlashing &&
                !ufoFlashing &&
                !invaderFlashing) {
                triggerGameOver()
                return
            }

            invalidate()
            postDelayed(this, missileInterval)
        }
    }

    private val enemyRunnable = object : Runnable {
        override fun run() {
            if (isGameOver) return
            if (ufoVisible && !ufoFlashing) {
                ufoCol += ufoDirection
                if (ufoCol < 0 || ufoCol >= gridCols) {
                    ufoVisible = false

                    // 撃破されていなかった場合は、次の出現方向をランダムに決定
                    if (!ufoFlashing) {
                        ufoNextSide = if (Random.nextBoolean()) 1 else -1
                    }

                    postDelayed({
                        ufoCol = if (ufoNextSide == -1) gridCols - 1 else 0
                        ufoDirection = ufoNextSide
                        ufoVisible = true
                    }, 1000)
                }
            }
            if (invaderVisible && !invaderFlashing) {
                val move = if (Random.nextBoolean()) -1 else 1
                invaderCol = (invaderCol + move).coerceIn(0, gridCols - 1)
                if (enemyMissiles.isEmpty() && Random.nextBoolean()) {
                    enemyMissiles.add(Missile(invaderCol, invaderRow, false))
                }
            }
            moveEnemyMissiles()
            checkCollisions()
            invalidate()
            postDelayed(this, 400)
        }
    }

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        load7SegImages()
        post(missileRunnable)
        post(enemyRunnable)
    }

    fun bindScoreViews(seg1: ImageView, seg2: ImageView) {
        scoreSeg1 = seg1
        scoreSeg2 = seg2
        updateScoreDisplay()
    }

    private fun load7SegImages() {
        val chars = "0123456789ABCDEF "
        val ids = listOf(
            R.drawable.seg7_0, R.drawable.seg7_1, R.drawable.seg7_2, R.drawable.seg7_3,
            R.drawable.seg7_4, R.drawable.seg7_5, R.drawable.seg7_6, R.drawable.seg7_7,
            R.drawable.seg7_8, R.drawable.seg7_9, R.drawable.seg7_a, R.drawable.seg7_b,
            R.drawable.seg7_c, R.drawable.seg7_d, R.drawable.seg7_e, R.drawable.seg7_f,
            R.drawable.seg7_blank
        )
        chars.forEachIndexed { i, c ->
            segMap[c] = BitmapFactory.decodeResource(resources, ids[i])
        }
    }

    private fun updateScoreDisplay() {
        if (score >= 160) {
            score = 0
            scoreOverflow = true
        }
        // オーバーフロー状態中は点滅し続ける
        if (scoreOverflow && !blinking) {
            blinkCount = 6
            blinking = true
            post(blinkRunnable)
            return
        }
        if (blinking) return  // 点滅中は表示切り替えを制御

        val tens = (score / 10)
        val ones = (score % 10)

        val tensChar = "0123456789ABCDEF"[tens.coerceIn(0, 15)]
        val onesChar = '0' + ones

        scoreSeg1?.setImageBitmap(segMap[tensChar] ?: segMap[' ']!!)
        scoreSeg2?.setImageBitmap(segMap[onesChar] ?: segMap[' ']!!)
    }

    private val blinkRunnable = object : Runnable {
        override fun run() {
            if (blinkCount > 0) {
                val visible = blinkCount % 2 == 0

                val tens = score / 10
                val ones = score % 10
                val tensChar = "0123456789ABCDEF"[tens.coerceIn(0, 15)]
                val onesChar = '0' + ones

                scoreSeg1?.setImageBitmap(if (visible) segMap[tensChar] else segMap[' ']!!)
                scoreSeg2?.setImageBitmap(if (visible) segMap[onesChar] else segMap[' ']!!)

                blinkCount--
                postDelayed(this, 300)
            } else {
                blinking = false
                if (scoreOverflow) {
                    updateScoreDisplay() // 再点滅開始（再帰的にループ）
                } else {
                    updateScoreDisplay() // 通常表示に戻す
                }
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        cellWidth = w / gridCols.toFloat()
        cellHeight = h / gridRows.toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(
            Bitmap.createScaledBitmap(background, width, height, false),
            0f, 0f, paint
        )

        // 自機
        if (!playerFlashing || (playerFlashCount % 2 == 0)) {
            val px = playerCol * cellWidth
            val py = playerRow * cellHeight
            canvas.drawBitmap(
                Bitmap.createScaledBitmap(
                    playerBitmap,
                    cellWidth.toInt(),
                    cellHeight.toInt(),
                    false
                ),
                px, py, paint
            )
        }
        if (playerFlashing) {
            playerFlashCount--
            if (playerFlashCount <= 0) {
                playerFlashing = false
                // ★点滅終了後に残弾ゼロならゲームオーバー発動
                if (playerMissileCount <= 0) {
                    triggerGameOver()
                }
            }
        }

        // UFO
        if (ufoVisible) {
            if (!ufoFlashing || (ufoFlashCount % 2 == 0)) {
                val x = ufoCol * cellWidth
                val y = ufoRow * cellHeight
                canvas.drawBitmap(
                    Bitmap.createScaledBitmap(ufoBitmap, cellWidth.toInt(), cellHeight.toInt(), false),
                    x, y, paint
                )
            }
            if (ufoFlashing) {
                ufoFlashCount--
                if (ufoFlashCount <= 0) {
                    ufoVisible = false
                    ufoFlashing = false
                    postDelayed({
                        ufoCol = if (ufoNextSide == -1) gridCols - 1 else 0
                        ufoDirection = ufoNextSide
                        ufoVisible = true
                    }, 1000)
                }
            }
        }

        // Invader
        if (invaderVisible) {
            if (!invaderFlashing || (invaderFlashCount % 2 == 0)) {
                val x = invaderCol * cellWidth
                val y = invaderRow * cellHeight
                canvas.drawBitmap(
                    Bitmap.createScaledBitmap(invaderBitmap, cellWidth.toInt(), cellHeight.toInt(), false),
                    x, y, paint
                )
            }
            if (invaderFlashing) {
                invaderFlashCount--
                if (invaderFlashCount <= 0) {
                    invaderVisible = false
                    invaderFlashing = false
                    postDelayed({
                        invaderCol = Random.nextInt(0, gridCols)
                        invaderVisible = true
                    }, 1000)
                }
            }
        }

        // ミサイル描画
        playerMissiles.forEach {
            val x = it.col * cellWidth + cellWidth / 2
            val y = it.row * cellHeight + cellHeight / 2
            canvas.drawCircle(x, y, cellWidth * 0.1f, missilePaint)
        }
        enemyMissiles.forEach {
            val x = it.col * cellWidth + cellWidth / 2
            val y = it.row * cellHeight + cellHeight / 2
            canvas.drawCircle(x, y, cellWidth * 0.1f, Color.RED.paint())
        }

        if (showGameOver) {
            paint.color = Color.RED
            paint.textSize = cellHeight * 0.5f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("GAME OVER", width / 2f, height / 2f, paint)
        }
    }

    fun movePlayerLeft() {
        if (isGameOver) return
        if (playerCol > 0) {
            playerCol--
            invalidate()
        }
    }

    fun movePlayerRight() {
        if (isGameOver) return
        if (playerCol < gridCols - 1) {
            playerCol++
            invalidate()
        }
    }

    fun fireMissile() {
        if (isGameOver) {
            if (canRestart) {
                restartGame()
            }
            return
        }
        if (playerMissileCount <= 0) return
        if (playerMissiles.isEmpty()) {
            playerMissiles.add(Missile(playerCol, playerRow - 1, true))
            playerMissileCount--
            updateMissileCountDisplay()
            invalidate()
        }
    }


    private fun moveEnemyMissiles() {
        val toRemove = mutableListOf<Missile>()
        for (m in enemyMissiles) {
            m.prevRow = m.row
            m.row += 1
            if (m.row >= gridRows) toRemove.add(m)
        }
        enemyMissiles.removeAll(toRemove)
    }

    // 柔らかい当たり判定：ミサイルとターゲットの中心距離が一定以下ならヒット
    private fun isHit(missile: Missile, targetCol: Int, targetRow: Int): Boolean {
        val missileX = missile.col * cellWidth + cellWidth / 2
        val missileY = missile.row * cellHeight + cellHeight / 2

        val targetX = targetCol * cellWidth + cellWidth / 2
        val targetY = targetRow * cellHeight + cellHeight / 2

        val dx = missileX - targetX
        val dy = missileY - targetY
        val distance = Math.sqrt((dx * dx + dy * dy).toDouble())

        return distance < cellWidth * 0.9 // 判定を緩めたい場合は0.7くらいにしてもOK
    }


    private fun checkCollisions() {
        // ミサイル同士の衝突（すれ違い含む）：自機のミサイルのみ消す
        val playerToRemove = mutableListOf<Missile>()
        for (pm in playerMissiles) {
            for (em in enemyMissiles) {
                val sameSpot = pm.col == em.col && pm.row == em.row
                val crossedOver = pm.col == em.col &&
                        pm.prevRow == em.row &&
                        pm.row == em.prevRow
                if (sameSpot || crossedOver) {
                    playerToRemove.add(pm)
                    break
                }
            }
        }
        playerMissiles.removeAll(playerToRemove)

        val toRemove = mutableListOf<Missile>()
        for (missile in playerMissiles) {
            if (missile.row == ufoRow && missile.col == ufoCol && ufoVisible && !ufoFlashing) {
                ufoFlashing = true
                ufoFlashCount = 6
                score += 5
                toRemove.add(missile)
                ufoNextSide = ufoDirection
            } else if (missile.row == invaderRow && missile.col == invaderCol && invaderVisible && !invaderFlashing) {
                invaderFlashing = true
                invaderFlashCount = 6
                score += 1
                toRemove.add(missile)
            }
        }
        playerMissiles.removeAll(toRemove)
        updateScoreDisplay()

        val hitPlayer = enemyMissiles.any {
            it.row == playerRow && it.col == playerCol
        }
        if (hitPlayer) {
            //Log.w("hitPlayer", "HIT!!!")
            playerMissileCount = (playerMissileCount - 5).coerceAtLeast(0)
            enemyMissiles.clear()
            updateMissileCountDisplay()
            // ★ 自機点滅開始
            playerFlashing = true
            playerFlashCount = 6
        }
    }

    private fun restartGame() {
        score = 0
        playerMissileCount = 150
        playerCol = 1
        ufoCol = 0
        ufoVisible = true
        invaderCol = 1
        invaderVisible = true
        playerMissiles.clear()
        enemyMissiles.clear()
        isGameOver = false
        showGameOver = false
        scoreOverflow = false
        playerFlashing = false
        playerFlashCount = 0
        ufoFlashing = false
        ufoFlashCount = 0
        invaderFlashing = false
        invaderFlashCount = 0
        updateScoreDisplay()
        invalidate()

        // Runnable 再起動
        handler.post(missileRunnable)
        handler.post(enemyRunnable)
    }

    override fun performClick(): Boolean {
        if (isGameOver) {
            restartGame()
            return true
        }
        return super.performClick()
    }

    data class Missile(var col: Int, var row: Int, val fromPlayer: Boolean) {
        var prevRow: Int = row
    }

    private fun Int.paint(): Paint = Paint().apply {
        color = this@paint
        style = Paint.Style.FILL
    }

    private fun triggerGameOver() {
        if (isGameOver) return // 2重実行防止
        isGameOver = true
        showGameOver = true
        canRestart = false
        invalidate()

        postDelayed({
            canRestart = true
        }, 2000)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isGameOver && canRestart) {
            restartGame()
            return true
        }
        return super.onTouchEvent(event)
    }

    private var missileCountTextView: TextView? = null

    fun bindMissileCountText(view: TextView) {
        missileCountTextView = view
        updateMissileCountDisplay()
    }

    private fun updateMissileCountDisplay() {
        missileCountTextView?.text = "$playerMissileCount"
    }

}
