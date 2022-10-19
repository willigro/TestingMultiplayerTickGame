package com.rittmann.myapplication.main.match.screen

import android.content.res.Resources
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.rittmann.myapplication.R
import com.rittmann.myapplication.main.components.JoystickView
import com.rittmann.myapplication.main.core.GamePanel
import com.rittmann.myapplication.main.entity.Bullet
import com.rittmann.myapplication.main.entity.server.InputWorldState
import com.rittmann.myapplication.main.entity.server.WorldState
import com.rittmann.myapplication.main.match.MatchEvents
import com.rittmann.myapplication.main.server.ConnectionControl
import com.rittmann.myapplication.main.server.ConnectionControlEvents
import com.rittmann.myapplication.main.server.ConnectionControlListeners
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

    /**
     * Think about use BINDING
     * */
    private val joystickLeft: JoystickView by lazy {
        findViewById(R.id.joystick_left)
    }

    private val joystickRight: JoystickView by lazy {
        findViewById(R.id.joystick_right)
    }

    private val txtValueHp: TextView by lazy {
        findViewById(R.id.txt_value_hp)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_match)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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

        gamePanel?.also {
            findViewById<ViewGroup>(R.id.container).addView(it)
        }

        /**
         * TODO: check later a better way to send these information, since I'm going to send the
         *  movement and aim every time it changes, and I guess it isn't a good idea when there are
         *  several requests
         *  - Maybe creating a queue of requests and send X requests per T time
         * */
        joystickLeft.setOnMoveListener { angle, strength ->
            gamePanel?.apply {
                onJoystickMovementChanged(angle, strength)
            }
        }

        joystickRight.setOnMoveListener { angle, strength ->
            gamePanel?.apply {
                onJoystickAimChanged(angle, strength)
            }
        }
    }

    private fun configureListViewLog() {
        val listView = findViewById<ListView>(R.id.container_log)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayListOf())
        listView.adapter = adapter
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

    override fun connectionCreated(player: ConnectionControlListeners.NewPlayerConnected) {
        gamePanel?.ownPlayerCreated(player)
    }

    override fun newPlayerConnected(player: ConnectionControlListeners.NewPlayerConnected) {
        gamePanel?.newPlayerConnected(player)
    }

    override fun onGameStarted(tick: Int) {
        gamePanel?.onGameStarted(tick)
    }

    override fun onGameDisconnected() {
        gamePanel?.onGameDisconnected()
        matchController.disconnect()
        finish()
    }

    override fun playerDisconnected(id: String) {
        gamePanel?.playerDisconnected(id)
    }

    override fun onPlayerUpdate(worldState: List<WorldState>) {
        gamePanel?.onPlayerUpdate(worldState)
    }

    override fun shoot(bullet: Bullet) {
        matchController.shoot(bullet)
    }

    override fun sendTheUpdatedState(deltaTime: Double, tick: Int, worldState: WorldState?) {
        worldState?.also { matchController.update(it) }
    }

    override fun sendTheUpdatedState(inputWorldState: InputWorldState) {
        matchController.update(inputWorldState)
    }

    override fun draw() {
//        gamePanel?.apply {
//            getPlayer()?.also { player ->
//                txtValueHp.text = player.getCurrentHp().toString()
//            }
//        }
    }

    override fun gameMustStop() {
        matchController.gameMustStop()
    }

    companion object {
        var SCREEN_WIDTH = 0
        var SCREEN_HEIGHT = 0
        var SCREEN_DENSITY = 0f
    }
}