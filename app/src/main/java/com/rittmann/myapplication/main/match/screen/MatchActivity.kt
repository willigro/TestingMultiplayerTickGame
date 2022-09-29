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
import com.rittmann.myapplication.main.entity.Player
import com.rittmann.myapplication.main.server.ConnectionControl
import com.rittmann.myapplication.main.server.ConnectionControlEvents

const val GLOBAL_TAG = "TAGGING"

class MatchActivity : AppCompatActivity(), ConnectionControlEvents {

    private var gamePanel: GamePanel? = null
    private var adapter: ArrayAdapter<String?>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SCREEN_WIDTH = Resources.getSystem().displayMetrics.widthPixels
        SCREEN_HEIGHT = Resources.getSystem().displayMetrics.heightPixels
        SCREEN_DENSITY = Resources.getSystem().displayMetrics.density

        setContentView(R.layout.activity_match)

        ConnectionControl(this, this).connect()

        gamePanel = GamePanel(this)

        configureListViewLog()

        findViewById<ViewGroup>(R.id.container).apply {

            // mock
            SCREEN_HEIGHT /= 2

            addView(gamePanel)
        }

        findViewById<JoystickView>(R.id.joystick_left).setOnMoveListener { angle, strength ->
            gamePanel?.setJoystickLeftValues(angle, strength)
        }
    }

    fun configureListViewLog() {
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
        runOnUiThread { adapter?.add(log) }
    }

    override fun connectionCreated(player: Player) {
        gamePanel?.ownPlayerCreated(player)
    }

    override fun newPlayerConnected(player: Player) {
        gamePanel?.newPlayerConnected(player)
    }
}