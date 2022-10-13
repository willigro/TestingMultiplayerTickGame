package com.rittmann.myapplication.main.entityimport android.graphics.Canvasimport android.graphics.Colorimport android.graphics.Paintimport com.rittmann.myapplication.main.draw.DrawObjectimport com.rittmann.myapplication.main.entity.body.Bodyimport com.rittmann.myapplication.main.entity.collisor.Collidableimport com.rittmann.myapplication.main.entity.collisor.Colliderimport com.rittmann.myapplication.main.entity.server.PlayerServerimport com.rittmann.myapplication.main.scene.SceneManagerimport com.rittmann.myapplication.main.utils.Loggerconst val BODY_WIDTH = 40const val BODY_HEIGHT = 40data class Player(    val playerId: String = "",    val position: Position = Position(),    val color: String = "",) : DrawObject, Collidable, Logger {    /**     * Physic values     * */    private val body: Body = Body(position.copy(), BODY_WIDTH, BODY_HEIGHT)    private val mainGunPointer: Pointer = Pointer(        position.copy(),        BODY_WIDTH.toDouble(),        BODY_HEIGHT.toDouble(),    )    private val collider: Collider = Collider(position.copy(), BODY_WIDTH, BODY_HEIGHT, this)    var angle: Double = 0.0    var strength: Double = 0.0    /**     * Check if I can use the mainGunPointer or body instead of it, I know that I can     * replace the angle, but for consistence I'm letting both the angle and strength here     * */    var aimAngle: Double = 0.0    var aimStrength: Double = 0.0    /**     * Server values     * */    private var playerServer: PlayerServer? = null    /**     * Stats values     * */    private var maxHealthPoints: Double = 10293.0    private var currentHealthPoints: Double = maxHealthPoints    /**     * Draw     * */    private val paint = Paint()    private val paintText = Paint()    private val paintHp = Paint()    init {        paint.color = Color.parseColor(color.ifEmpty { "#FFFFFF" })        paintText.color = Color.WHITE        paintText.textAlign = Paint.Align.CENTER        paintText.textSize = 30f        paintHp.color = Color.RED    }    override fun update(deltaTime: Double) {        updateBodyPosition()        updateColliderPosition()    }    override fun draw(canvas: Canvas) {        // rotate and in its own axis        drawBody(canvas)        mainGunPointer.draw(canvas)        drawPlayerName(canvas)        drawPlayerHp(canvas)    }    override fun free() {        retrieveCollider().free()    }    override fun retrieveCollider(): Collider {        return collider    }    override fun collidingWith(collidable: Collidable) {//        when (collidable) {//            is Bullet -> {//                "Player $playerId is Touching a bullet".log()//            }//            is Player -> {//                "Player $playerId is Touching a player".log()//            }//        }    }    private fun drawBody(canvas: Canvas) {        canvas.save()        canvas.rotate(            -body.rotationAngle.toFloat(),            (position.x).toFloat(),            (position.y).toFloat()        )        canvas.drawRect(body.rect, paint)        canvas.restore()    }    private fun drawPlayerName(canvas: Canvas) {        val xPos = position.x        val yPos = position.y - 40        canvas.drawText(playerId, xPos.toFloat(), yPos.toFloat(), paintText)    }    private fun drawPlayerHp(canvas: Canvas) {        val left = position.x.toFloat() - body.width - 50        val diffPercentage = (maxHealthPoints - currentHealthPoints).toFloat()        val totalRight = (body.width).toFloat() + 50        val right = totalRight - (diffPercentage * (totalRight / 100f))        val top = (position.y + body.height + 30f).toFloat()        val bottom = top + 10f        canvas.drawRect(            left,            top,            (position.x + right).toFloat(),            bottom,            paintHp        )    }    private fun updateColliderPosition() {        collider.move(position)    }    private fun updateBodyPosition() {        body.move(position)    }    fun move(deltaTime: Double, angle: Double, strength: Double) {        this.angle = angle        this.strength = strength        playerServer?.playerMovement?.resetPositionWasApplied()//        val c = cos(angle * Math.PI / 180f) * strength//        val s = -sin(angle * Math.PI / 180f) * strength//        val ps1 = Position(//            c,//            s, // Is negative to invert the direction//        )//        val normalizedPosition = ps1.normalize()        val normalizedPosition = Position.calculateNormalizedPosition(angle, strength)        val newX = this.position.x + (normalizedPosition.x * VELOCITY) * deltaTime        val newY = this.position.y + (normalizedPosition.y * VELOCITY) * deltaTime        (                "Tick " + SceneManager.clientTickNumber +                        " Normalized " + normalizedPosition +                        " Delta " + deltaTime +//                " Cos " + c +//                " Sin " + s +                        " Angle " + angle +                        " Strength " + strength +                        " Velocity " + VELOCITY +                        " New X " + newX +                        " New Y " + newY                ).log()        this.position.set(newX, newY)        mainGunPointer.setMoveAndRotate(newX, newY)        update(deltaTime)    }    fun move(deltaTime: Double, angle: Double, strength: Double, position: Position) {        this.angle = angle        this.strength = strength        this.position.set(position)        mainGunPointer.setMoveAndRotate(position.x, position.y)        update(deltaTime)    }    fun aim(angle: Double, strength: Double) {        body.setRotation(angle)        mainGunPointer.setRotation(angle)        aimStrength = strength        aimAngle = angle    }    private var lastTime = 0L    fun shoot(): Bullet? {        // TODO: this controller also must be done from the server, I need to recheck if the player can shoot        val currentTime = System.currentTimeMillis()        if (lastTime > 0L && currentTime - lastTime < 500) return null        lastTime = System.currentTimeMillis()        return createBullet()    }    private fun createBullet(): Bullet {        val pointerPosition = mainGunPointer.getRotatedPosition()//        "pointerPosition=$pointerPosition, player.angle=${body.rotationAngle}, pointer.angle=${mainGunPointer.rotationAngle}".log()        val bullet = Bullet(            bulletId = "${playerId}_${System.nanoTime()}",            ownerId = playerId,            position = Position(                x = pointerPosition.x,                y = pointerPosition.y            ),            angle = body.rotationAngle,            velocity = BULLET_DEFAULT_VELOCITY,            maxDistance = BULLET_DEFAULT_MAX_DISTANCE,        )        bullet.retrieveCollider().enable()        return bullet    }    fun getMainGunPointer() = mainGunPointer    fun getCurrentHp(): Double {        return currentHealthPoints    }    companion object {        // TODO get it from the server        const val VELOCITY = 300.0    }}