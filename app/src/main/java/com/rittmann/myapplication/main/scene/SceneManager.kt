package com.rittmann.myapplication.main.scene

import android.graphics.Canvas
import android.view.MotionEvent
import com.rittmann.myapplication.main.components.Joystick
import com.rittmann.myapplication.main.entity.Player
import com.rittmann.myapplication.main.entity.Position
import com.rittmann.myapplication.main.entity.server.PlayerAim
import com.rittmann.myapplication.main.entity.server.PlayerMovement
import com.rittmann.myapplication.main.entity.server.PlayerServer
import com.rittmann.myapplication.main.entity.server.PlayerUpdate
import com.rittmann.myapplication.main.entity.server.WorldState
import com.rittmann.myapplication.main.match.MatchEvents
import com.rittmann.myapplication.main.server.ConnectionControlListeners
import com.rittmann.myapplication.main.utils.Logger
import java.util.*
import kotlin.collections.ArrayList

// KEEP IT EQUALS TO THE SERVER
private const val SERVER_TICK_RATE = 2
private const val BUFFER_SIZE = 1024

class SceneManager(
    private val matchEvents: MatchEvents,
) : Logger {
    private val scene: Scene

    private var joystickLeft: Joystick = Joystick()
    private var joystickRight: Joystick = Joystick()

    private var timer = 0.0
    private var currentTick = 0
    private var minTimeBetweenTicks = 1.0 / SERVER_TICK_RATE
    private var clientTickNumber = 0
    private var clientLastReceivedStateTick = 0
    private val cClientBufferSize = 1024
    private val clientStateBuffer: Array<WorldState?> = Array(BUFFER_SIZE) { null }
    private val clientInputBuffer: Array<WorldState?> = Array(BUFFER_SIZE) { null }

    //    private val clientStateMsgs: Queue<WorldState> = LinkedList()
    private val clientStateMsgs: Queue<WorldState> = LinkedList()
    private var serverInputMsgs = InputWorldState()

    data class InputWorldState(
        val inputs: ArrayList<WorldState?> = arrayListOf(),
        var start_tick_number: Int = 0,
    )

    init {
        scene = SceneMain(matchEvents)
    }

    fun receiveTouch(motionEvent: MotionEvent) {
        scene.receiveTouch(motionEvent)
    }

    fun update(deltaTime: Double) {

        var bufferSlot = clientTickNumber % BUFFER_SIZE

        // sample and store inputs for this tick
        val currentInputs = createCurrentWorldState(clientTickNumber)
        this.clientInputBuffer[bufferSlot] = currentInputs

        // store state for this tick, then use current state + input to step simulation
        currentInputs?.let { scene.onPlayerUpdate(it, deltaTime) }

        this.clientStateBuffer[bufferSlot] = createCurrentWorldState(clientTickNumber)

        // send input packet to server
        // limiting the package
        val startTickNumber =
//            if (clientTickNumber - this.clientLastReceivedStateTick > 5) {
//                clientTickNumber - 5
//            } else
                this.clientLastReceivedStateTick

        for (tick in startTickNumber until clientTickNumber) {
            this.serverInputMsgs.inputs.add(this.clientInputBuffer[tick % cClientBufferSize])
        }

        this.serverInputMsgs.start_tick_number = startTickNumber

        timer += deltaTime
        if (timer >= minTimeBetweenTicks) {
//            "size=${this.serverInputMsgs.inputs.size}, clientLastReceivedStateTick=$clientLastReceivedStateTick".log()
            matchEvents.sendTheUpdatedState(this.serverInputMsgs.copy())
            serverInputMsgs.inputs.clear()
            serverInputMsgs = InputWorldState()
        }

        this.clientTickNumber++

        if (clientStateMsgs.size > 0) {
//            var stateMsg = this.clientStateMsgs.remove()
            val stateMsg = this.clientStateMsgs.last()
            this.clientStateMsgs.clear()
            // make sure if there are any newer state messages available, we use those instead
//            while (clientStateMsgs.size > 0) {
//                stateMsg = this.clientStateMsgs.remove()
//            }

            this.clientLastReceivedStateTick = stateMsg.tick

            bufferSlot = stateMsg.tick % cClientBufferSize

            val serverPosition =
                stateMsg.playerUpdate.players.firstOrNull()?.playerMovement?.position ?: Position()

            val clientPastPosition =
                this.clientStateBuffer[bufferSlot]?.playerUpdate?.players?.firstOrNull()?.playerMovement?.position
                    ?: Position()

            val positionError = serverPosition.distance(clientPastPosition)

            if (positionError > 0.001) {
                "Correcting for error at tick ${stateMsg.tick} clientTickNumber=$clientTickNumber (rewinding ${(clientTickNumber - stateMsg.tick)} ticks) positionError=$positionError serverPosition=${serverPosition} clientPastPosition=${clientPastPosition}".log()

                // rewind & replay
                scene.onPlayerUpdate(stateMsg, deltaTime, true)

                var rewindTickNumber = stateMsg.tick
                while (rewindTickNumber < clientTickNumber) {
                    bufferSlot = rewindTickNumber % cClientBufferSize

                    val input = this.clientInputBuffer[bufferSlot]

                    input?.let { scene.onPlayerUpdate(input, deltaTime) }

                    this.clientStateBuffer[bufferSlot] = input

                    rewindTickNumber++
                }
            }
        }
    }

    private fun createCurrentWorldState(tick: Int): WorldState? {
        return getPlayer()?.let { player ->
            val playerMovementEmit = PlayerMovement(
                position = player.position,
                angle = joystickLeft.angle,
                strength = joystickLeft.strength,
                velocity = Player.VELOCITY,
            )

            val playerAimEmit = PlayerAim(
                angle = joystickRight.angle,
                strength = joystickRight.strength,
            )

            return WorldState(
                tick = tick,
                bulletUpdate = null,
                playerUpdate = PlayerUpdate(
                    players = arrayListOf(
                        PlayerServer(
                            id = player.playerId,
                            playerMovement = playerMovementEmit,
                            playerAim = playerAimEmit,
                        )
                    )
                )
            )
        }
    }

    fun draw(canvas: Canvas) {
        scene.draw(canvas)
        matchEvents.draw()
    }

    fun ownPlayerCreated(player: Player) {
        scene.ownPlayerCreated(player)
    }

    fun newPlayerConnected(player: ConnectionControlListeners.NewPlayerConnected) {
        scene.newPlayerConnected(player.player)
        currentTick = player.tick
    }

    fun playerDisconnected(id: String) {
        scene.playerDisconnected(id)
    }

    fun getPlayer(): Player? = scene.getPlayer()

    fun onJoystickMovementChanged(angle: Double, strength: Double) {
        joystickLeft.set(angle, strength)
        scene.onJoystickMovementChanged(angle, strength)
    }

    fun onJoystickAimChanged(angle: Double, strength: Double) {
        joystickRight.set(angle, strength)
        scene.onJoystickAimChanged(angle, strength)
    }

    fun onPlayerUpdate(worldState: WorldState) {
        // scene.onPlayerUpdate(worldState)
//        if (tickStated.not()) {
//            currentTick = worldState.tick
//        }
        "onPlayerUpdate, at tick=$currentTick, worldTick=${worldState.tick} updating latest ${worldState.playerUpdate.players.firstOrNull()?.playerMovement?.position}".log()
//        latestServerState = worldState
//        client_state_msgs.add(worldState)
    }

    fun onPlayerUpdate(worldState: List<WorldState>) {
        // scene.onPlayerUpdate(worldState)
//        if (tickStated.not()) {
//            currentTick = worldState.tick
//        }
//        "onPlayerUpdate, at tick=$currentTick, worldTick=${worldState.tick} updating latest ${worldState.playerUpdate.players.firstOrNull()?.playerMovement?.position}".log()
//        latestServerState = worldState
        clientStateMsgs.addAll(worldState)
        "client_state_msgs.size=${clientStateMsgs.size}".log()
    }

    fun getEnemies(): List<Player> {
        return scene.getEnemies()
    }
}