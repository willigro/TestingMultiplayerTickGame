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

// KEEP IT EQUALS TO THE SERVER
private const val SERVER_TICK_RATE = 2
private const val BUFFER_SIZE = 1024
private const val ERROR_POSITION_FACTOR = 0.001
private const val COUNT_TO_SEND = 2

class SceneManager(
    private val matchEvents: MatchEvents,
) : Logger {

    // sharing to use on logs
    companion object {
        var clientTickNumber = 0
        var clientLastReceivedStateTick = 0
        var clientStateMessagesSize = 0
        var serverInputMessagesSize = 0
    }

    private val scene: Scene

    private var joystickLeft: Joystick = Joystick()
    private var joystickRight: Joystick = Joystick()

    private var timer = 0.0
    private var minTimeBetweenTicks = 1.0 / SERVER_TICK_RATE
    private var countToSend = 0
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
        if (getPlayer() == null || matchInitiliazed.not()) return

//        if (clientTickNumber - clientLastReceivedStateTick < -(MAX_FPS)) {
//            "currentTick=${clientTickNumber}, clientLastReceivedStateTick=${clientLastReceivedStateTick}".log()
//            clientTickNumber = clientLastReceivedStateTick
//        }

        // Get the input data and process it (update the world)
        processTheCurrentInputAndRunTheWorldUsingThem(deltaTime)

        // send input packet to server
        buildTheInputPackageAndSendToTheServer(deltaTime)

//        clientStateMessages.lastOrNull()?.also {
//            clientLastReceivedStateTick = it.tick
//        }

//        if (clientLastReceivedStateTick > clientTickNumber) {
//            "forward from $clientTickNumber to $clientLastReceivedStateTick".log()
//            for (tick in clientTickNumber until clientLastReceivedStateTick) {
//                clientStateMessages.firstOrNull { it.tick == tick }?.also {
//                    scene.onWorldUpdated(it, deltaTime)
//                    clientTickNumber++
//                    "forward updating tick=${it.tick}, new clientTickNumber=$clientTickNumber".log()
//                }
//            }
//        } else {
//            processThePackageReceivedFromTheServerAndReconcileTheData(deltaTime)
//        }

        processThePackageReceivedFromTheServerAndReconcileTheData(deltaTime)

        // It will refresh and clear the variables and states
        scene.finishFrame()

        clientTickNumber++
    }

    fun draw(canvas: Canvas) {
        scene.draw(canvas)
        matchEvents.draw()
    }

    fun onGameStarted(tick: Int) {
        timer = 0.0
        countToSend = 0
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
//        reset(player)

        scene.ownPlayerCreated(player.player)
    }

    fun newPlayerConnected(player: ConnectionControlListeners.NewPlayerConnected) {
//        reset(player)

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
        val arr = arrayListOf<WorldState>()
        arr.addAll(clientStateMessages)
        arr.addAll(worldState)
        arr.sortBy { it.tick }

        clientStateMessages.clear()
        clientStateMessages.addAll(arr)
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
        // Get the current tick base on the currentTick
        val bufferSlot = clientTickNumber % BUFFER_SIZE

        // Get the current data from the world, it will represents the current INPUT
        // since it was not processed it, such as the ANGLES that are importants to move the objects
        // TODO: since I need only the angle and strength to move (the mutable variable that the
        //  movement depends on) I can get the joysticks values and create a new object to be the
        //  inputs object, it will reduce the amount of data
        val currentInputs = createCurrentInputsState(clientTickNumber)

        // Store the current world state processed that used the current inputs "
        this.clientStatePreBuffer[bufferSlot] = createCurrentWorldState(clientTickNumber).apply {
            "\nCurrent PRE world at tick=$clientTickNumber, on bufferSlot=$bufferSlot -> ${this?.printPlayers()}".log()
        }

        // Get the current inputs and process the world
        currentInputs?.let { input -> scene.onWorldUpdated(input, deltaTime) }

        // Store the current world state processed that used the current inputs
        this.clientStatePostBuffer[bufferSlot] = createCurrentWorldState(clientTickNumber).apply {
            "Current POST world at tick=$clientTickNumber -> ${this?.printPlayers()}".log()
        }

        // TODO!! I guess that I need to store the current state without proccess it!!!!
        // Store the current world state processed that used the current inputs
//        this.clientStateBuffer[bufferSlot] = createCurrentWorldState(clientTickNumber)

        // As the information about the bullet are important to send through the inputs
        // I'm going to try to get the bullets after the world evaluation, and then store the inputs
        this.clientInputBuffer[bufferSlot] = currentInputs?.copy(
            bulletInputsState = createBulletInputsState(clientTickNumber)
        )?.resetCanSend()

//        clientTickNumber++
    }

    private fun buildTheInputPackageAndSendToTheServer(deltaTime: Double) {
//        ("clientLastReceivedStateTick=$clientLastReceivedStateTick, " +
//                "clientTickNumber=$clientTickNumber, " +
//                "first.clientStateMessages.tick=${clientStateMessages.firstOrNull()?.tick}, " +
//                "last.clientStateMessages.tick=${clientStateMessages.lastOrNull()?.tick}.").log()
        // TODO: I'm forcing the last message, if it is null, send the last tick until the current
        /*clientLastReceivedStateTick =
            clientStateMessages.lastOrNull()?.tick ?: (clientTickNumber - 1)
        if (clientLastReceivedStateTick > clientTickNumber) {
            ("Server is ahead clientLastReceivedStateTick=$clientLastReceivedStateTick " +
                    "clientTickNumber=$clientTickNumber").log()
        }*/
        val startTickNumber = lastSent + 1
//        val startTickNumber = lastSent

//        "clientLastReceivedStateTick=$clientLastReceivedStateTick, clientTickNumber=$clientTickNumber".log()
        // until -> tick -1
        for (tick in startTickNumber until clientTickNumber) {
            val input = this.clientInputBuffer[tick % BUFFER_SIZE]
            if (input?.canSend() == true) {
                serverInputMessages.inputs.add(input)
            }
        }

        serverInputMessages.start_tick_number = startTickNumber

        timer += deltaTime
//        if (timer >= minTimeBetweenTicks) {
        if (countToSend >= COUNT_TO_SEND) {
            countToSend = 0
            serverInputMessagesSize = serverInputMessages.inputs.size
            serverInputMessages.inputs.lastOrNull()?.also {
                lastSent = it.tick
            }
            "sending $startTickNumber".log()
            serverInputMessages.inputs.forEach {
                it?.toString()?.log()
            }
//            "sending=${serverInputMessagesSize}, lastSent=$lastSent, clientLastReceivedStateTick=$clientLastReceivedStateTick, clientTickNumber=$clientTickNumber".log()
            matchEvents.sendTheUpdatedState(serverInputMessages)
            serverInputMessages.inputs.clear()
            serverInputMessages = InputWorldState()
        }
        countToSend++
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

                while (rewindTickNumber <= clientTickNumber) {
                    // Buffer slot to rewind
                    bufferSlot = rewindTickNumber % BUFFER_SIZE

                    "rewind tick=$rewindTickNumber, on bufferSlot=$bufferSlot".log()

                    // Input that was processed at this slot
                    val input = this.clientInputBuffer[bufferSlot]

                    // As the world was already replayed, I can create a current state (getting the replayed state)
                    // Store the world state
                    this.clientStatePreBuffer[bufferSlot] =
                        createCurrentWorldState(rewindTickNumber).apply {
                            "Current PRE (rewinded) world at tick=$clientTickNumber -> ${this?.printPlayers()}".log()
                        }

                    // Reprocess the data
                    input?.let { scene.onWorldUpdated(input, deltaTime) }

                    // Store the world state
                    this.clientStatePostBuffer[bufferSlot] =
                        createCurrentWorldState(rewindTickNumber).apply {
                            "Current POST (rewinded) world at tick=$clientTickNumber -> ${this?.printPlayers()}".log()
                        }

                    // Store the new world state
//                    this.clientStateBuffer[bufferSlot] = createCurrentWorldState(rewindTickNumber)

                    rewindTickNumber++
                }
            }
        }
    }

    private fun verifyAllMessagesAndReplayIfNeeds(deltaTime: Double): Int {
        var lastTick = INVALID_ID
        val possibleLastTick = clientStateMessages.lastOrNull()?.tick ?: INVALID_ID

        // Do it for all
        while (clientStateMessages.size > 0) {
            // Get the first message and delete it from the queue
            val stateMsg = clientStateMessages.remove()

            val bufferSlot = stateMsg.tick % BUFFER_SIZE

            // When the stick is bigger than the current tick, just replace it and ignore the rewind
            if (stateMsg.tick > clientTickNumber) {
                "Move forward on tick=${stateMsg.tick}, on bufferSlot=$bufferSlot, state=$stateMsg".log()

//                // Store the world state, as I'm forcing a new one, I guess that it need to be get the "new"
                this.clientStatePreBuffer[bufferSlot] =
                    createCurrentWorldState(stateMsg.tick).apply {
                    "Current PRE (replayed) world at tick=$clientTickNumber -> ${this?.printPlayers()}".log()
                    }

                scene.onWorldUpdated(stateMsg, deltaTime)

                // Store the world state, as I'm forcing a new one, I guess that it need to be get the "new"
                this.clientStatePostBuffer[bufferSlot] =
                    createCurrentWorldState(stateMsg.tick).apply {
                        "Current POST (replayed) world at tick=$clientTickNumber -> ${this?.printPlayers()}".log()
                    }

                // Get the tick of the last message processed
                lastTick = INVALID_ID

                // Increase the current tick, because I'm moving the game forward
                clientTickNumber++
            } else {
                // Check if there is some inconsistency on the data, to do so get the server data
                // Calculate the position error
                val thereIsError = calculateError(stateMsg, bufferSlot)

                // I'm letting this bullet check because if there is a bullet I need to update because the bullet
                // is updated for the server
                if (thereIsError || (stateMsg.bulletUpdate?.bullets?.size ?: 0) > 0
                ) {
                    ("replay clientTickNumber=$clientTickNumber, " +
                            "stateMsg.tick=${stateMsg.tick}, " +
                            "last.tick=${possibleLastTick}, " +
                            "on bufferSlot=$bufferSlot, " +
                            "state=$stateMsg").log()
//                "Correcting for error at tick ${stateMsg.tick} clientTickNumber=$clientTickNumber (rewinding ${(clientTickNumber - stateMsg.tick)} ticks) positionError=$positionError serverPosition=${serverPosition} clientPastPosition=${clientPastPosition}".log()

                    // The pre world don't need to be changed, only the post, since I'm forcing a new state
                    // unless I start to send the pre state from the server, as I'm not, I cannot know the
                    // world state when I'm replaying because the world state can be built from a different state
//                    this.clientStatePreBuffer[bufferSlot] =
//                        createCurrentWorldState(stateMsg.tick).apply {
//                            "Current PRE (replayed) world at tick=$clientTickNumber -> ${this?.printPlayers()}".log()
//                        }

                    // Replay, it will force the world state to be equals to the server
                    scene.onWorldUpdated(stateMsg, deltaTime)

                    // Store the world state, as I'm forcing a new one, I guess that it need to be get the "new"
                    this.clientStatePostBuffer[bufferSlot] =
                        createCurrentWorldState(stateMsg.tick).apply {
                            "Current POST (replayed) world at tick=$clientTickNumber -> ${this?.printPlayers()}".log()
                        }

                    // Get the tick of the last message processed
                    lastTick = possibleLastTick
                }
            }
        }

        return lastTick
    }

    private fun calculateError(stateMsg: WorldState, bufferSlot: Int): Boolean {
        // Force a forward in case of the server is ahead the client
//        if (stateMsg.tick >= clientTickNumber) {
//            clientTickNumber++
//            "go forward".log()
//            return true
//        }

        // Get the buffer slot meant to the server message
        // It can be (at least must be) a value in the past, it will be a bufferSlot previous to the
        // current bufferSlot processed at this update

//        val players  = scene.getEnemies()
//        scene.getPlayer()?.also {
//            players.add(it)
//        }
        val localStatePost = this.clientStatePostBuffer[bufferSlot]
        val localStatePre = this.clientStatePreBuffer[bufferSlot]
        val localState = localStatePost
//        "localState PRE used to compare the error=$localStatePre".log()
        "localState POST used to compare the error=$localStatePost".log()

        if (stateMsg.tick != localState?.tick) return false

        val localPlayers = localState.playerUpdate.players

        stateMsg.playerUpdate.players.forEach { remotePlayer ->
            for (player in localPlayers) {
                if (remotePlayer.id == player.id) {
                    val distance = player.playerMovement.position.distance(
                        remotePlayer.playerMovement.position
                    )
                    if (distance > ERROR_POSITION_FACTOR
                    ) {
                        ("distance error $distance, " +
                                "player=${player.id}, " +
                                "current tick=${clientTickNumber}, " +
                                "stateMsg.tick=${stateMsg.tick}, " +
                                "on bufferSlot=$bufferSlot, " +
                                "localState.tick=${localState.tick}, " +
                                "positionLocal=${player.playerMovement.position}, " +
                                "angleLocal=${player.playerMovement.angle}, " +
                                "positionRemote=${remotePlayer.playerMovement.position} " +
                                "angleRemote=${remotePlayer.playerMovement.angle}, "
                                ).log()

//                        stopGame(player.id)
                        return true
                    }

                    if (player.playerAim.angle != remotePlayer.playerAim.angle
                        || player.playerAim.strength != remotePlayer.playerAim.strength
                        || player.playerGunPointer.angle != remotePlayer.playerGunPointer.angle
                        || player.playerMovement.angle != remotePlayer.playerMovement.angle
                        || player.playerMovement.strength != remotePlayer.playerMovement.strength
                    ) {
                        "angles error, player=${player.id}".log()
                        return true
                    }

                    break
                }
            }
        }

        return false
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