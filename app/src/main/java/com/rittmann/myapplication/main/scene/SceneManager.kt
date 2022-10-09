package com.rittmann.myapplication.main.scene

import android.graphics.Canvas
import android.view.MotionEvent
import com.rittmann.myapplication.main.components.Joystick
import com.rittmann.myapplication.main.entity.Player
import com.rittmann.myapplication.main.entity.Position
import com.rittmann.myapplication.main.entity.server.BulletInputsState
import com.rittmann.myapplication.main.entity.server.BulletUpdate
import com.rittmann.myapplication.main.entity.server.InputWorldState
import com.rittmann.myapplication.main.entity.server.InputsState
import com.rittmann.myapplication.main.entity.server.PlayerAim
import com.rittmann.myapplication.main.entity.server.PlayerAimInputsState
import com.rittmann.myapplication.main.entity.server.PlayerGunInputsState
import com.rittmann.myapplication.main.entity.server.PlayerGunPointer
import com.rittmann.myapplication.main.entity.server.PlayerInputsState
import com.rittmann.myapplication.main.entity.server.PlayerMovement
import com.rittmann.myapplication.main.entity.server.PlayerMovementInputsState
import com.rittmann.myapplication.main.entity.server.PlayerServer
import com.rittmann.myapplication.main.entity.server.PlayerUpdate
import com.rittmann.myapplication.main.entity.server.WorldState
import com.rittmann.myapplication.main.match.MatchEvents
import com.rittmann.myapplication.main.server.ConnectionControlListeners
import com.rittmann.myapplication.main.utils.INVALID_ID
import com.rittmann.myapplication.main.utils.Logger
import java.util.*

// KEEP IT EQUALS TO THE SERVER
private const val SERVER_TICK_RATE = 5
private const val BUFFER_SIZE = 1024
private const val ERROR_POSITION_FACTOR = 0.001

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
    private val clientInputBuffer: Array<InputsState?> = Array(BUFFER_SIZE) { null }

    //    private val clientStateMsgs: Queue<WorldState> = LinkedList()
    private val clientStateMessages: Queue<WorldState> = LinkedList()
    private var serverInputMessages = InputWorldState()

    init {
        scene = SceneMain(matchEvents)
    }

    fun receiveTouch(motionEvent: MotionEvent) {
        scene.receiveTouch(motionEvent)
    }

    fun update(deltaTime: Double) {
        if (getPlayer() == null) return

//        "update".log()
        // Get the input data and process it (update the world)
        processTheCurrentInputAndRunTheWorldUsingThem(deltaTime)

        // send input packet to server
        buildTheInputPackageAndSendToTheServer(deltaTime)

        // When there is data received from the server, process it to try the server reconciliation
        processThePackageReceivedFromTheServerAndReconcileTheData(deltaTime)

        // It will refresh and clear the variables and states
        scene.finishFrame()
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
//        worldState.forEach {
//            it.bulletUpdate?.bullets?.forEach {
//                it.position.toString().log()
//            }
//        }
    }

    fun getEnemies(): List<Player> {
        return scene.getEnemies()
    }

    private fun processTheCurrentInputAndRunTheWorldUsingThem(deltaTime: Double) {
        // Get the current tick base on the currentTick
        val bufferSlot = clientTickNumber % BUFFER_SIZE

        // Get the current data from the world, it will represents the current INPUT
        // since it was not processed it, such as the ANGLES that are importants to move the objects
        // TODO: since I need only the angle and strength to move (the mutable variable that the
        //  movement depends on) I can get the joysticks values and create a new object to be the
        //  inputs object, it will reduce the amount of data
        val currentInputs = createCurrentInputsState(clientTickNumber)

        // Get the current inputs and process the world
        currentInputs?.let { input -> scene.onWorldUpdated(input, deltaTime) }

        // Store the current world state processed that used the current inputs
        this.clientStateBuffer[bufferSlot] = createCurrentWorldState(clientTickNumber)

        // As the information about the bullet are important to send through the inputs
        // I'm going to try to get the bullets after the world evaluation, and then store the inputs
        this.clientInputBuffer[bufferSlot] = currentInputs?.copy(
            bulletInputsState = createBulletInputsState(currentTick)
        )
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

    // TODO: as I wanna to read all messages I'm going to:
    //  Go to the old message, minor tick, it should be the the first item since it is a queue
    //  Go through all and replace the client ticks
    //  When reach to the newer message continue doing as it is already doing, it means that I'll do the rewind
    private fun processThePackageReceivedFromTheServerAndReconcileTheData(deltaTime: Double) {
        // Check if there is new messages
        if (clientStateMessages.size > 0) {
            // Use the newer message
            // TODO: IT WON'T WORK FOR ME!! some messages must be read but it will be lost inside the package
            //  because it will gone faster then the server can notify
            //  example: if I shoot a near enemy the bullet will travel for few ticks and the collision
            //           will be lost in case of it isn't the last message, so I need to READ ALL

            var bufferSlot: Int

            var rewindTickNumber = verifyAllMessagesAndReplayIfNeeds(deltaTime)

            // If some state was changed, do the rewind
            if (rewindTickNumber != INVALID_ID) {
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

    private fun verifyAllMessagesAndReplayIfNeeds(deltaTime: Double): Int {
        var lastTick = INVALID_ID
//        "verifyAllMessagesAndReplayIfNeeds clientStateMessages.size=${clientStateMessages.size}".log()
        // Do it for all
        while (clientStateMessages.size > 0) {
            // Get the first message and delete it from the queue
            val stateMsg = this.clientStateMessages.remove()

//            "has bullets=${stateMsg.bulletUpdate?.bullets?.size}, clientStateMessages.size=${clientStateMessages.size}".log()

            // Get the tick to retrieve if it is the last one
            lastTick = stateMsg.tick

            // When it is the last item, save the tick
            if (clientStateMessages.size == 1) {
                // Store the message tick
                this.clientLastReceivedStateTick = stateMsg.tick
            }

            // Get the buffer slot meant to the server message
            // It can be (at least must be) a value in the past, it will be a bufferSlot previous to the
            // current bufferSlot processed at this update
            val bufferSlot = stateMsg.tick % cClientBufferSize

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

            // TODO: improve this code, I need do to less checks, and maybe update only what is necessary
            //  instead of passing everything
            // I'm letting this bullet check because if there is a bullet I need to update because the bullet
            // is updated for the server
            if (positionError > ERROR_POSITION_FACTOR
                || (stateMsg.bulletUpdate?.bullets?.size ?: 0) > 0
            ) {
//                "Correcting for error at tick ${stateMsg.tick} clientTickNumber=$clientTickNumber (rewinding ${(clientTickNumber - stateMsg.tick)} ticks) positionError=$positionError serverPosition=${serverPosition} clientPastPosition=${clientPastPosition}".log()

                // Replay, it will force the world state to be equals to the server
                scene.onWorldUpdated(stateMsg, deltaTime)
            }
        }

        return lastTick
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

            val pointer = player.getMainGunPointer()
            val playerGunPointer = PlayerGunPointer(
                position = pointer.getRotatedPosition(joystickRight.angle),
                angle = joystickRight.angle
            )

//            if (joystickRight.strength > 80.0){
//                "pointerPosition=${pointer.getRotatedPosition()}, pointer.angle=${pointer.rotationAngle}, joystickRight.angle=${joystickRight.angle}".log()
//            }

            return WorldState(
                tick = tick,
                bulletUpdate = BulletUpdate(
                    scene.getBulletsToSend(tick)
                ),
                playerUpdate = PlayerUpdate(
                    players = arrayListOf(
                        PlayerServer(
                            id = player.playerId,
                            playerMovement = playerMovementEmit,
                            playerAim = playerAimEmit,
                            playerGunPointer = playerGunPointer,
                        )
                    )
                ),
            )
        }
    }

    private fun createCurrentInputsState(tick: Int): InputsState? {
        return getPlayer()?.let { player ->
            val playerInputsState = PlayerInputsState(
                playerMovementInputsState = PlayerMovementInputsState(
                    angle = joystickLeft.angle,
                    strength = joystickLeft.strength,
                ),
                playerAimInputsState = PlayerAimInputsState(
                    angle = joystickRight.angle,
                    strength = joystickRight.strength,
                ),
                playerGunInputsState = PlayerGunInputsState(
                    position = player.getMainGunPointer().getRotatedPosition(joystickRight.angle),
                    angle = joystickRight.angle,
                ),
            )

            return InputsState(
                tick = tick,
                playerId = player.playerId,
                playerInputsState = playerInputsState,
                bulletInputsState = arrayListOf(),
            )
        }
    }

    private fun createBulletInputsState(tick: Int): List<BulletInputsState> {
        return scene.getBulletsToSend(tick).let { list ->
            if (list.isEmpty()) {
                arrayListOf()
            } else {
                val arrBullets = arrayListOf<BulletInputsState>()
                scene.getBulletsToSend(tick).forEach { bullet ->
                    arrBullets.add(
                        BulletInputsState(
                            ownerId = bullet.ownerId,
                            bulletId = bullet.bulletId,
                        )
                    )
                }
                arrBullets
            }
        }
    }
}