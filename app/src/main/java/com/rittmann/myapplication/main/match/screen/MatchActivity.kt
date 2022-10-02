package com.rittmann.myapplication.main.match.screen

import android.content.res.Resources
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.rittmann.myapplication.R
import com.rittmann.myapplication.main.components.JoystickView
import com.rittmann.myapplication.main.core.GamePanel
import com.rittmann.myapplication.main.entity.Bullet
import com.rittmann.myapplication.main.entity.Player
import com.rittmann.myapplication.main.entity.server.PlayerAimEmit
import com.rittmann.myapplication.main.entity.server.PlayerMovementEmit
import com.rittmann.myapplication.main.entity.server.PlayerMovementWrapResult
import com.rittmann.myapplication.main.entity.server.PlayerShootingResponseWrap
import com.rittmann.myapplication.main.match.MatchEvents
import com.rittmann.myapplication.main.server.ConnectionControl
import com.rittmann.myapplication.main.server.ConnectionControlEvents
import com.rittmann.myapplication.main.utils.Logger

const val GLOBAL_TAG = "TAGGING"

class MatchActivity : AppCompatActivity(), ConnectionControlEvents, MatchEvents, Logger {

    private val matchController: MatchController by lazy {
        MatchController(
            ConnectionControl(connectionControlEvents = this)
        )
    }
    private var gamePanel: GamePanel? = null
    private var adapter: ArrayAdapter<String?>? = null

    private val joystickLeft: JoystickView by lazy {
        findViewById(R.id.joystick_left)
    }

    private val joystickRight: JoystickView by lazy {
        findViewById(R.id.joystick_right)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_match)

        matchController.connect()

        fetchScreenValues()

        gamePanel = GamePanel(this).build(this)

        setupUi()
    }

    override fun onDestroy() {
        matchController.disconnect()
        super.onDestroy()
    }

    private fun fetchScreenValues() {
        SCREEN_WIDTH = Resources.getSystem().displayMetrics.widthPixels
        SCREEN_HEIGHT = Resources.getSystem().displayMetrics.heightPixels
        SCREEN_DENSITY = Resources.getSystem().displayMetrics.density
    }

    private fun setupUi() {
        configureListViewLog()

        findViewById<ViewGroup>(R.id.container).addView(gamePanel)

        /**
         * TODO: check later a better way to send these information, since I'm going to send the
         *  movement and aim every time it changes, and I guess it isn't a good idea when there are
         *  several requests
         *  - Maybe creating a queue of requests and send X requests per T time
         * */
        joystickLeft.setOnMoveListener { angle, strength ->
            gamePanel?.apply {
                onJoystickMovementChanged(angle, strength)
                sendPlayerMovement()
            }
        }

        joystickRight.setOnMoveListener { angle, strength ->
            gamePanel?.apply {
                onJoystickAimChanged(angle, strength)
                sendPlayerMovement()
            }
        }
    }

    private fun sendPlayerMovement() {
        gamePanel?.apply {
            val playerPosition = getPlayerPosition()

            matchController.sendPlayerPosition(
                playerMovementEmit = PlayerMovementEmit(
                    x = playerPosition.x,
                    y = playerPosition.y,
                    angle = joystickLeft.angle,
                    strength = joystickLeft.strength,
                    velocity = Player.VELOCITY,
                ),

                playerAimEmit = PlayerAimEmit(
                    angle = joystickRight.angle,
                    strength = joystickRight.strength,
                )
            )
        }
    }

    private fun configureListViewLog() {
        val listView = findViewById<ListView>(R.id.container_log)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayListOf())
        listView.adapter = adapter
    }

    companion object {
        var SCREEN_WIDTH = 0
        var SCREEN_HEIGHT = 0
        var SCREEN_DENSITY = 0f
    }

    override fun logCallback(log: String) {
        runOnUiThread {
            adapter?.apply {
                add(log)
                val listView = findViewById<ListView>(R.id.container_log)
                listView.setSelection(count - 1)
            }
        }
    }

    override fun connectionCreated(player: Player) {
        gamePanel?.ownPlayerCreated(player)
    }

    override fun newPlayerConnected(player: Player) {
        gamePanel?.newPlayerConnected(player)
    }

    override fun playerMovementWrapResult(playerMovementWrapResult: PlayerMovementWrapResult) {
        gamePanel?.playerMovement(playerMovementWrapResult)
    }

    override fun playerDisconnected(id: String) {
        gamePanel?.playerDisconnected(id)
    }

    override fun onPlayerEnemyShooting(shootingResponseWrap: PlayerShootingResponseWrap) {
        gamePanel?.onPlayerEnemyShooting(shootingResponseWrap)
    }

    override fun shoot(player: Player, bullet: Bullet) {
        matchController.shoot(player, bullet)
    }
}