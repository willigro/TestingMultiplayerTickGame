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
    private val clientStateMessages: Queue<WorldState> = LinkedList()
    private var serverInputMessages = InputWorldState()

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
        // Get the input data and process it (update the world)
        processTheCurrentInputAndRunTheWorldUsingThem(deltaTime)

        // send input packet to server
        buildTheInputPackageAndSendToTheServer(deltaTime)

        // When there is data received from the server, process it to try the server reconciliation
        processThePackageReceivedFromTheServerAndReconcileTheData(deltaTime)
    }

    fun draw(canvas: Canvas) {
        scene.draw(canvas)
        matchEvents.draw()
    }

    fun ownPlayerCreated(player: Player) {
        currentTick.toString().log()
        if (player.playerId == getPlayer()?.playerId) {
            timer = 0.0
            currentTick = 0
            clientTickNumber = 0
            clientLastReceivedStateTick = 0
            clientStateMessages.clear()
            serverInputMessages.inputs.clear()
        }

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

    fun onPlayerUpdate(worldState: List<WorldState>) {
        clientStateMessages.addAll(worldState)
    }

    fun getEnemies(): List<Player> {
        return scene.getEnemies()
    }

    private fun processThePackageReceivedFromTheServerAndReconcileTheData(deltaTime: Double) {
        // Check if there is new messages
        if (clientStateMessages.size > 0) {
            // Use the newer message
            val stateMsg = this.clientStateMessages.last()

            // Clear the list since we don't need to handle it at the next update
            this.clientStateMessages.clear()

            // Store the message tick
            this.clientLastReceivedStateTick = stateMsg.tick

            // Get the buffer slot meant to the server message
            // It can be (at least must be) a value in the past, it will be a bufferSlot previous to the
            // current bufferSlot processed at this update
            var bufferSlot = stateMsg.tick % cClientBufferSize

            // Check if there is some inconsistency on the data, to do so get the server data
            // TODO: using only the first player position because I'm testing the change position
            //  using only one player connected
            val serverPosition =
                stateMsg.playerUpdate.players.firstOrNull()?.playerMovement?.position ?: Position()

            // Get the client data
            val clientPastPosition =
                this.clientStateBuffer[bufferSlot]?.playerUpdate?.players?.firstOrNull()?.playerMovement?.position
                    ?: Position()

            // Calculate the position error
            val positionError = serverPosition.distance(clientPastPosition)

            // If there is error, try the reconciliation
            if (positionError > 0.001) {
                "Correcting for error at tick ${stateMsg.tick} clientTickNumber=$clientTickNumber (rewinding ${(clientTickNumber - stateMsg.tick)} ticks) positionError=$positionError serverPosition=${serverPosition} clientPastPosition=${clientPastPosition}".log()

                // Replay, it will force the world state to be equals to the server
                scene.onWorldUpdated(stateMsg, deltaTime, true)

                // Rewind, it will process the world starting of the server data, so
                // the world will be equals or at least it will looks like with the server data
                var rewindTickNumber = stateMsg.tick
                while (rewindTickNumber < clientTickNumber) {
                    // Buffer slot to rewind
                    bufferSlot = rewindTickNumber % cClientBufferSize

                    // Input that was processed at this slot
                    val input = this.clientInputBuffer[bufferSlot]

                    // Reprocess the data
                    input?.let { scene.onWorldUpdated(input, deltaTime) }

                    // Store the new world state
                    this.clientStateBuffer[bufferSlot] = createCurrentWorldState(rewindTickNumber)

                    rewindTickNumber++
                }
            }
        }
    }

    private fun processTheCurrentInputAndRunTheWorldUsingThem(deltaTime: Double) {
        // Get the current tick base on the currentTick
        val bufferSlot = clientTickNumber % BUFFER_SIZE

        // Get the current data from the world, it will represents the current INPUT
        // since it was not processed it, such as the ANGLES that are importants to move the objects
        // TODO: since I need only the angle and strength to move (the mutable variable that the
        //  movement depends on) I can get the joysticks values and create a new object to be the
        //  inputs object, it will reduce the amount of data
        val currentInputs = createCurrentWorldState(clientTickNumber)

        // Store the current inputs according of the current bufferSlot
        this.clientInputBuffer[bufferSlot] = currentInputs

        // Get the current inputs and process the world
        currentInputs?.let { scene.onWorldUpdated(it, deltaTime) }

        // Store the current world state processed that used the current inputs
        this.clientStateBuffer[bufferSlot] = createCurrentWorldState(clientTickNumber)
    }

    private fun buildTheInputPackageAndSendToTheServer(deltaTime: Double) {
        val startTickNumber = this.clientLastReceivedStateTick

        for (tick in startTickNumber until clientTickNumber) {
            this.serverInputMessages.inputs.add(this.clientInputBuffer[tick % cClientBufferSize])
        }

        this.serverInputMessages.start_tick_number = startTickNumber

        timer += deltaTime
        if (timer >= minTimeBetweenTicks) {
            matchEvents.sendTheUpdatedState(this.serverInputMessages.copy())
            serverInputMessages.inputs.clear()
            serverInputMessages = InputWorldState()
        }

        this.clientTickNumber++
    }

    /**
     * TODO: at this moment I'm creating the current state to get both the inputs and the results
     *  By what I was seeing, I guess that the joysticks values are the only data that I need to
     *  represent the inputs, so I need to create a new method and a new object representing them
     *  of course I'll need to change the server, by the way, I'm going to let at this way for a while
     * */
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
}