package com.rittmann.myapplication.main.scene

import android.graphics.Canvas
import android.view.MotionEvent
import com.rittmann.myapplication.main.components.Joystick
import com.rittmann.myapplication.main.entity.Player
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
import kotlin.collections.ArrayList

// KEEP IT EQUALS TO THE SERVER
private const val BUFFER_SIZE = 1024
private const val ERROR_POSITION_FACTOR = 0.001
private const val COUNT_OF_TICKS_TO_SEND = 2

enum class ProcessState {
    NORMAL, REPLAY, REWIND, FORWARD
}

class SceneManager(
    private val matchEvents: MatchEvents,
) : Logger {

    // sharing to use on logs
    companion object {
        var clientTickNumber = 0
        var tickNumberBeenProcessed = 0
        var clientLastReceivedStateTick = 0
        var clientStateMessagesSize = 0
        var serverInputMessagesSize = 0
        var processState: ProcessState = ProcessState.NORMAL
    }

    private val scene: Scene

    private var joystickLeft: Joystick = Joystick()
    private var joystickRight: Joystick = Joystick()

    private var ticksLoadedToSend = 0
    private var lastSent = -1

    private val clientStatePreBuffer: Array<WorldState?> = Array(BUFFER_SIZE) { null }
    private val clientStatePostBuffer: Array<WorldState?> = Array(BUFFER_SIZE) { null }
    private val clientInputBuffer: Array<InputsState?> = Array(BUFFER_SIZE) { null }
    private var clientStateMessages: Queue<WorldState> = LinkedList()
    private var serverInputMessages = InputWorldState()
    private var matchInitiliazed = false

    init {
        scene = SceneMain(matchEvents)
    }

    fun receiveTouch(motionEvent: MotionEvent) {
        scene.receiveTouch(motionEvent)
    }

    fun update(deltaTime: Double) {
        // If there the player is not connected or the match is not initialized then don't update the game
        if (getPlayer() == null || matchInitiliazed.not()) return

        // Get the input data and process it (update the world)
        processTheCurrentInputAndRunTheWorldUsingThem(deltaTime)

        // Send input packet to server
        buildTheInputPackageAndSendToTheServer(deltaTime)

        // Get the data from the server and handle it (replay, rewind, calculate error, etc)
        processThePackageReceivedFromTheServerAndReconcileTheData(deltaTime)

        // It will refresh and clear the variables and states
        scene.finishFrame()

        // Update the tick, I'm updating it only after update all the world state, reconciliation, etc
        clientTickNumber++
    }

    fun draw(canvas: Canvas) {
        scene.draw(canvas)
        matchEvents.draw()
    }

    fun onGameStarted(tick: Int) {
        ticksLoadedToSend = 0
        clientTickNumber = tick
        clientLastReceivedStateTick = tick
        clientStateMessages.clear()
        serverInputMessages.inputs.clear()

        matchInitiliazed = true
    }

    fun onGameDisconnected() {
        clientTickNumber = 0
        clientLastReceivedStateTick = 0
        clientStateMessagesSize = 0
        serverInputMessagesSize = 0
        matchInitiliazed = false
    }

    fun ownPlayerCreated(player: ConnectionControlListeners.NewPlayerConnected) {
        scene.ownPlayerCreated(player.player)
    }

    fun newPlayerConnected(player: ConnectionControlListeners.NewPlayerConnected) {
        scene.newPlayerConnected(player.player)
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
        // I wanna to sort the list to always handle the messages by the tick ordering
        val arr = arrayListOf<WorldState>()
        arr.addAll(clientStateMessages)
        arr.addAll(worldState)
        arr.sortBy { it.tick }

        // Clear and add the items
        clientStateMessages.clear()
        clientStateMessages.addAll(arr)

        // Get the size and print the results for debug
        clientStateMessagesSize = worldState.size
        "clientStateMessages.size=${clientStateMessages.size}, currentTick=$clientTickNumber".log()
        worldState.forEach { w ->
            w.playerUpdate.players.forEach {
                "tick=${w.tick} ${it.id} ${it.playerMovement}".log()
            }
        }
    }

    fun getEnemies(): List<Player> {
        return scene.getEnemies()
    }

    private fun processTheCurrentInputAndRunTheWorldUsingThem(deltaTime: Double) {
        processState = ProcessState.NORMAL
        tickNumberBeenProcessed = clientTickNumber

        // Get the current buffer base on the current tick
        val bufferSlot = clientTickNumber % BUFFER_SIZE

        // Get the current data from the world, it will represents the current INPUT
        // since it was not processed it, like the ANGLES that are important to move the objects
        val currentInputs = createCurrentInputsState(clientTickNumber)

        // First I'm going to store the current state as the PRE world, because I wanna to store
        // the world the way is was before process the inputs
        this.clientStatePreBuffer[bufferSlot] = createCurrentWorldState(clientTickNumber).apply {
            "\nCurrent PRE world at tick=$clientTickNumber, on bufferSlot=$bufferSlot -> ${this?.printPlayers()}".log()
        }

        // Process the world with the current inputs
        currentInputs?.let { input -> scene.onWorldUpdated(input, deltaTime, clientTickNumber) }

        // Store the current world state processed that used the current inputs
        // This way I can know the result of the PRE loaded world + the current inputs and keep the
        // reference to the current tick
        this.clientStatePostBuffer[bufferSlot] = createCurrentWorldState(clientTickNumber).apply {
            "Current POST world at tick=$clientTickNumber -> ${this?.printPlayers()}".log()
        }

        // As the information about the bullet are important to send through the inputs
        // I'm going to try to get the bullets after the world evaluation, and then store the inputs
        // The reason why I'm doing it is because the "inputs of the bullets" are created using the
        // data about the already created bullet, that's why I need to run it after process the world
        this.clientInputBuffer[bufferSlot] = currentInputs?.copy(
            bulletInputsState = createBulletInputsState(clientTickNumber)
        )?.resetCanSend()
    }

    private fun buildTheInputPackageAndSendToTheServer(deltaTime: Double) {
        // Keep the number of the first tick that will be sent
        val startTickNumber = lastSent + 1

        // It goes from the start tick until the current tick getting all processed ticks
        // since the last package sent
        for (tick in startTickNumber until clientTickNumber) {
            val input = this.clientInputBuffer[tick % BUFFER_SIZE]

            // Verify if it was already sent
            // I don't wanna send duplicated inputs for the same tick
            if (input?.canSend() == true) {
                serverInputMessages.inputs.add(input)
            }
        }

        // Informs the start tick to the package
        serverInputMessages.start_tick_number = startTickNumber

        // It will trigger to send only when a specific numbers of tick is processed
        if (ticksLoadedToSend >= COUNT_OF_TICKS_TO_SEND) {
            // Reset the ticks to send
            ticksLoadedToSend = 0

            // Get the size of the message, for debug
            serverInputMessagesSize = serverInputMessages.inputs.size

            // Keep the last sent tick
            serverInputMessages.inputs.lastOrNull()?.also {
                lastSent = it.tick
            }

            // Log
            "sending $startTickNumber".log()
            serverInputMessages.inputs.forEach {
                it?.toString()?.log()
            }

            // Send the ticks
            matchEvents.sendTheUpdatedState(serverInputMessages)

            // Clear the list and the current inputs
            serverInputMessages.inputs.clear()
            serverInputMessages = InputWorldState()
        }

        // For each tick updates its value
        ticksLoadedToSend++
    }

    private fun processThePackageReceivedFromTheServerAndReconcileTheData(deltaTime: Double) {
        // Check if there is new messages
        if (clientStateMessages.size > 0) {
            // Use the newer message

            var bufferSlot: Int

            // Replay the world state if necessary, then rewind the messages
            var rewindTickNumber = verifyAllMessagesAndReplayIfNeeds(deltaTime)

            // If some state was changed, do the rewind
            if (rewindTickNumber != INVALID_ID) {

                rewindTickNumber += 1

                while (rewindTickNumber <= clientTickNumber) {
                    processState = ProcessState.REWIND
                    tickNumberBeenProcessed = rewindTickNumber

                    // Buffer slot to rewind
                    bufferSlot = rewindTickNumber % BUFFER_SIZE

                    "rewind tick=$rewindTickNumber, on bufferSlot=$bufferSlot".log()

                    // Get the inputs of the current buffer
                    val input = this.clientInputBuffer[bufferSlot]

                    // As the world was already replayed, I can create a current state (getting the replayed state)
                    // Store the world state
                    this.clientStatePreBuffer[bufferSlot] =
                        createCurrentWorldState(rewindTickNumber).apply {
                            "Current PRE (rewinded) world at tick=$clientTickNumber, rewinded tick=${rewindTickNumber} -> ${this?.printPlayers()}".log()
                        }

                    // Reprocess the data
                    input?.let { scene.onWorldUpdated(input, deltaTime, clientTickNumber) }

                    // Store the new world state
                    this.clientStatePostBuffer[bufferSlot] =
                        createCurrentWorldState(rewindTickNumber).apply {
                            "Current POST (rewinded) world at tick=$clientTickNumber, rewinded tick=${rewindTickNumber} -> ${this?.printPlayers()}".log()
                        }

                    rewindTickNumber++
                }
            }
        }
    }

    private fun verifyAllMessagesAndReplayIfNeeds(deltaTime: Double): Int {
        var lastTick = INVALID_ID

        // Useful to log
        val possibleLastTick = clientStateMessages.lastOrNull()?.tick ?: INVALID_ID

        // Do it for all
        while (clientStateMessages.size > 0) {
            // Get the first message and delete it from the queue
            val stateMsg = clientStateMessages.remove()

            // Calculate the current buffer
            val bufferSlot = stateMsg.tick % BUFFER_SIZE

            // When the stick is bigger than the current tick, do the FORWARD
            if (stateMsg.tick > clientTickNumber) {
                processState = ProcessState.FORWARD
                tickNumberBeenProcessed = stateMsg.tick

                "Move forward on tick=${stateMsg.tick}, on bufferSlot=$bufferSlot, state=$stateMsg".log()

                // Store the current state
                this.clientStatePreBuffer[bufferSlot] =
                    createCurrentWorldState(stateMsg.tick).apply {
                        "Current PRE (replayed) world at tick=$clientTickNumber, forward tick=${stateMsg.tick} -> ${this?.printPlayers()}".log()
                    }

                // Move the world ahead updating it
                scene.onWorldUpdated(stateMsg, deltaTime)

                // Store the new world state
                this.clientStatePostBuffer[bufferSlot] =
                    createCurrentWorldState(stateMsg.tick).apply {
                        "Current POST (replayed) world at tick=$clientTickNumber, forward tick=${stateMsg.tick} -> ${this?.printPlayers()}".log()
                    }

                // I'm not going to do the rewind, so I can returns the INVALID_ID
                lastTick = INVALID_ID

                // Increase the current tick, because I'm moving the game forward
                clientTickNumber++
            } else {
                // Check if there is some inconsistency on the data, to do so
                // get the server data and calculate the errors
                val errors = calculateError(stateMsg, bufferSlot)

                // If there is an error, replay the world
                if (errors.isNotEmpty()) {
                    processState = ProcessState.REPLAY
                    tickNumberBeenProcessed = stateMsg.tick

                    // FIXME: I need to join the world states locally and remote, because the remote can be changing only one player
                    //  - First: I'm going to change only what is wrong
                    //  - Second: Test using 3 players, because I guess that I need to join the state to guarantee that the replay will
                    //            consider all states

                    val combinedState = combineState(stateMsg, bufferSlot) ?: continue

                    ("replay clientTickNumber=$clientTickNumber, " +
                            "stateMsg.tick=${stateMsg.tick}, " +
                            "last.tick=${possibleLastTick}, " +
                            "on bufferSlot=$bufferSlot, " +
                            "\ninputs=${clientInputBuffer[bufferSlot]}, " +
                            "\nlocal state POST=${clientStatePostBuffer[bufferSlot]}, " +
                            "\nremote state=$stateMsg, " +
                            "\ncombined state=$combinedState, " +
                            "\nerrors=$errors").log()

                    // Replay, it will force the world state to be equals to the server
                    scene.onWorldUpdated(combinedState, deltaTime)

                    // Store the new world state
                    this.clientStatePostBuffer[bufferSlot] =
                        createCurrentWorldState(combinedState.tick).apply {
                            "Current POST (replayed) world at tick=$clientTickNumber, replayed tick=${combinedState.tick}-> ${this?.printPlayers()}".log()
                        }

                    // Get the tick of the last message processed
                    lastTick = possibleLastTick
                }
            }
        }

        return lastTick
    }

    private fun combineState(stateMsg: WorldState?, bufferSlot: Int): WorldState? {
        if (stateMsg == null) return null

        val localState = clientStatePostBuffer[bufferSlot]

        if (localState == null || localState.tick != stateMsg.tick) return null

        val remotePLayers = stateMsg.playerUpdate.players
        val localPlayers = localState.playerUpdate.players

        val newPlayers = arrayListOf<PlayerServer>()
        newPlayers.addAll(remotePLayers)

        // added the states that are lacking
        for (localPlayer in localPlayers) {
            var found = false
            for (remotePlayer in remotePLayers) {
                if (remotePlayer.id == localPlayer.id) {
                    found = true
                }
            }

            if (found.not()) {
                newPlayers.add(localPlayer.copy())
            }
        }

        return WorldState(
            tick = stateMsg.tick,
            bulletUpdate = stateMsg.bulletUpdate,
            playerUpdate = PlayerUpdate(players = newPlayers),
        )
    }

    sealed class Error(val id: String) {
        data class PlayerPositionError(val playerId: String) : Error(playerId)
        data class PlayerAngleError(val playerId: String) : Error(playerId)
        data class BulletPositionError(val bulletId: String) : Error(bulletId)
        data class BulletNotFoundError(val bulletId: String) : Error(bulletId)
    }

    private fun calculateError(stateMsg: WorldState, bufferSlot: Int): List<Error> {
        // Get the result of the world of the current buffer
        val localState = this.clientStatePostBuffer[bufferSlot]
        "\nlocalState POST used to compare the error=$localState".log()

        // If they have a different tick, ignores it
        if (stateMsg.tick != localState?.tick) return arrayListOf()

        val errors = arrayListOf<Error>()

        // Retrieve the local players
        val localPlayers = localState.playerUpdate.players

        // Retrieve the local bullets
        val localBullets = localState.bulletUpdate?.bullets ?: arrayListOf()

        // Do it to all players
        stateMsg.playerUpdate.players.forEach { remotePlayer ->

            for (player in localPlayers) {

                // Check if the player of the server is equals to the local player
                if (remotePlayer.id == player.id) {

                    // Calculate the distance between them
                    val distance = player.playerMovement.position.distance(
                        remotePlayer.playerMovement.position
                    )

                    // Check if the error is tolerable
                    if (distance > ERROR_POSITION_FACTOR
                    ) {
                        ("Error on: player distance $distance, " +
                                "\nplayer=${player.id}, " +
                                "\ncurrent tick=${clientTickNumber}, " +
                                "\nstateMsg.tick=${stateMsg.tick}, " +
                                "\non bufferSlot=$bufferSlot, " +
                                "\nlocalState.tick=${localState.tick}, " +
                                "\npositionLocal=${player.playerMovement.position}, " +
                                "\nangleLocal=${player.playerMovement.angle}, " +
                                "\npositionRemote=${remotePlayer.playerMovement.position} " +
                                "\nangleRemote=${remotePlayer.playerMovement.angle}, "
                                ).log()

//                        stopGame(player.id)
                        errors.add(Error.PlayerPositionError(player.id))
                    }

                    // Calculate if the angles are different
                    // TODO: if I start to use delta or interpolation, do something like the distance
                    if (player.playerAim.angle != remotePlayer.playerAim.angle
                        || player.playerAim.strength != remotePlayer.playerAim.strength
                        || player.playerGunPointer.angle != remotePlayer.playerGunPointer.angle
                        || player.playerMovement.angle != remotePlayer.playerMovement.angle
                        || player.playerMovement.strength != remotePlayer.playerMovement.strength
                    ) {
                        ("Error on: player angles, player=${player.id}, " +
                                "current tick=${clientTickNumber}, " +
                                "stateMsg.tick=${stateMsg.tick}").log()
                        errors.add(Error.PlayerAngleError(player.id))
                    }
                }
            }
        }

        if (localBullets.isNotEmpty()) {
            // If there is any bullet to update, then force the new state
            stateMsg.bulletUpdate?.bullets?.forEach { remoteBullet ->
                val localBullet = localBullets.firstOrNull { it.bulletId == remoteBullet.bulletId }

                if (localBullet == null) {
                    ("Error on: bullet not found, " +
                            "current tick=${clientTickNumber}, " +
                            "stateMsg.tick=${stateMsg.tick}, " +
                            "bullet=${remoteBullet.bulletId}, " +
                            "remoteBullet.position=${remoteBullet.position}").log()
                    errors.add(Error.BulletNotFoundError(remoteBullet.bulletId))
                } else {
                    val distance = localBullet.position.distance(remoteBullet.position)
                    if (distance > ERROR_POSITION_FACTOR) {
                        ("Error on: bullet distance, " +
                                "current tick=${clientTickNumber}, " +
                                "stateMsg.tick=${stateMsg.tick}, " +
                                "distance=$distance, " +
                                "bullet=${remoteBullet.bulletId}, " +
                                "localBullet.position=${localBullet.position}, " +
                                "remoteBullet.position=${remoteBullet.position}").log()
                        errors.add(Error.BulletPositionError(localBullet.bulletId))
                    }
                }
            }
        }

        return errors
    }

    private fun stopGame(id: String) {
        if (id == getPlayer()?.playerId) {
            matchEvents.gameMustStop()
            matchInitiliazed = false
        }
    }

    /**
     * TODO: at this moment I'm creating the current state to get both the inputs and the results
     *  By what I was seeing, I guess that the joysticks values are the only data that I need to
     *  represent the inputs, so I need to create a new method and a new object representing them
     *  of course I'll need to change the server, by the way, I'm going to let at this way for a while
     * */
    private fun createCurrentWorldState(tick: Int): WorldState? {
        val host = getPlayer() ?: return null

        val players = scene.getEnemies().toMutableList()
        players.add(host)

        val playersServer = arrayListOf<PlayerServer>()

        players.forEach { player ->
            val playerMovementEmit = PlayerMovement(
                position = player.position.copy(),
                angle = player.angle,
                strength = player.strength,
                velocity = Player.VELOCITY,
            )

            val playerAimEmit = PlayerAim(
                angle = player.aimAngle,
                strength = player.aimStrength,
            )

            val pointer = player.getMainGunPointer()
            val playerGunPointer = PlayerGunPointer(
                position = pointer.getRotatedPosition(player.aimAngle),
                angle = player.aimAngle,
            )

            playersServer.add(
                PlayerServer(
                    id = player.playerId,
                    playerMovement = playerMovementEmit,
                    playerAim = playerAimEmit,
                    playerGunPointer = playerGunPointer,
                )
            )
        }

        return WorldState(
            tick = tick,
            bulletUpdate = BulletUpdate(
                scene.getBulletsToSend(tick),
            ),
            playerUpdate = PlayerUpdate(
                players = playersServer,
            ),
        )
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