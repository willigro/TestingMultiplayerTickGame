package com.rittmann.myapplication.main.scene

import android.graphics.Canvas
import android.view.MotionEvent
import com.rittmann.myapplication.main.components.Joystick
import com.rittmann.myapplication.main.entity.Player
import com.rittmann.myapplication.main.entity.server.PlayerAim
import com.rittmann.myapplication.main.entity.server.PlayerMovement
import com.rittmann.myapplication.main.entity.server.PlayerServer
import com.rittmann.myapplication.main.entity.server.PlayerUpdate
import com.rittmann.myapplication.main.entity.server.WorldState
import com.rittmann.myapplication.main.match.MatchEvents
import com.rittmann.myapplication.main.server.ConnectionControlListeners
import com.rittmann.myapplication.main.utils.Logger

// KEEP IT EQUALS TO THE SERVER
private const val SERVER_TICK_RATE = 5
private const val BUFFER_SIZE = 1024

class SceneManager(
    private val matchEvents: MatchEvents,
) : Logger {
    private var tickStated: Boolean = false
    private val scene: Scene

    private var timer = 0.0
    private var currentTick = 0
    private var minTimeBetweenTicks = 1.0 / SERVER_TICK_RATE

    private val stateBuffer: Array<WorldState?> = Array(BUFFER_SIZE) { null }
    private val inputBuffer: Array<WorldState?> = Array(BUFFER_SIZE) { null }

    //    private val inputBuffer: Array<WorldState?> = Array(BUFFER_SIZE) { null }
    private var latestServerState: WorldState? = null
    private var lastProcessedState: WorldState? = null

    private var joystickLeft: Joystick = Joystick()
    private var joystickRight: Joystick = Joystick()

    init {
        scene = SceneMain(matchEvents)
    }

    fun receiveTouch(motionEvent: MotionEvent) {
        scene.receiveTouch(motionEvent)
    }

    fun update(deltaTime: Double) {
//        "update".log()
        // update the game
        // for a while I'm letting it out of the the handleTick because I wanna to test
        // it running only once, cause the handleTick can be called twice or more
        val bufferIndex = currentTick % BUFFER_SIZE
//        "Before - update - currentPosition=${getPlayer()?.position}".log()
        inputBuffer[bufferIndex] = createCurrentWorldState()
//        "After - update - currentPosition=${getPlayer()?.position}".log()

        scene.update(deltaTime, currentTick)
        val firstWorldState = createCurrentWorldState()
        lastProcessedState = firstWorldState
        currentTick++
        tickStated = true

        // TODO make a more elegant way of testing if the state was already processed
        if (latestServerState != null && latestServerState!!.alreadyProcessed.compareAndSet(
                false,
                true
            ) && latestServerState!! != lastProcessedState
        ) {
            handleServerReconciliation(deltaTime)

            // TODO: return a boolean to update it only when it needs and reduce the code
            // Create a new worldState representing the new updated state
            val worldState = createCurrentWorldState()
            stateBuffer[bufferIndex] = worldState
            lastProcessedState = worldState
        } else {
            stateBuffer[bufferIndex] = firstWorldState
        }
//        "SERVER = ${latestServerState?.playerUpdate?.players?.firstOrNull().toString()}".log()
//        "LOCAL = ${worldState?.playerUpdate?.players?.firstOrNull().toString()}".log()
//        inputBuffer[bufferIndex] = worldState
        timer += deltaTime
//        "deltaTime=$deltaTime, minTimeBetweenTicks=$minTimeBetweenTicks, timer=$timer".log()
//        "currentTick=$currentTick".log()


//        var a = 1
//        while (timer >= minTimeBetweenTicks) {

        // TODO time to send
        if (timer >= minTimeBetweenTicks) {
//            currentTick.toString().log()
            timer = 0.0
//            timer -= minTimeBetweenTicks
            // the update function
//            handleTick()

            // send the updated data
            matchEvents.sendTheUpdatedState(deltaTime, currentTick, lastProcessedState?.copy())

//            currentTick++
//            a++
        }
//        "a=$a".log()
    }

    private fun handleTick() {
//        if (latestServerState?.equals(lastProcessedState) == false) {
//            handleServerReconciliation()
//        }

        val bufferIndex = currentTick % BUFFER_SIZE

        val worldState = createCurrentWorldState()
//        inputBuffer[bufferIndex] = worldState
        stateBuffer[bufferIndex] = worldState
    }

    private fun createCurrentWorldState(): WorldState? {
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
                tick = currentTick,
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

    private fun handleServerReconciliation(deltaTime: Double) {
//        lastProcessedState = latestServerState
        latestServerState?.also { latestServerState ->
            val serverStateBufferIndex = latestServerState.tick % BUFFER_SIZE
            lastProcessedState?.let {
                "serverTick=${latestServerState.tick}, currentTick=$currentTick".log()
                when {
                    latestServerState.tick > currentTick -> moveForward(
                        deltaTime,
                        latestServerState,
                        serverStateBufferIndex,
                        it
                    )
                    latestServerState.tick < currentTick -> rewindAndReplay(
                        deltaTime,
                        latestServerState,
                        serverStateBufferIndex,
                        it
                    )
                }
            }
        }
    }

    private fun rewindAndReplay(
        deltaTime: Double,
        latestServerState: WorldState,
        serverStateBufferIndex: Int,
        lastProcessedState: WorldState,
    ) {

        // TODO: I need to check all item later
        val serverPosition =
            latestServerState.playerUpdate.players.firstOrNull()?.playerMovement?.position
                ?: return

        val positionError =
            lastProcessedState.playerUpdate.players.firstOrNull()?.playerMovement?.position?.distance(
                serverPosition
            ) ?: return

        if (positionError > 0.001) {
            "rewindAndReplay $positionError".log()

            // TODO: Rewind & Replay
//                    "LATEST = ${
//                        latestServerState.playerUpdate.players.firstOrNull().toString()
//                    }".log()
            "Before - rewindAndReplay - currentPosition=${getPlayer()?.position}".log()
            scene.onPlayerUpdate(latestServerState, deltaTime)
            "After - rewindAndReplay - currentPosition=${getPlayer()?.position}".log()
            // Update buffer at index of latest server state
            stateBuffer[serverStateBufferIndex] = latestServerState

            // como o ultimo state do servidor foi refeito, o jogador pode ter feito diversas açoes que nao serao processadas
            // entao posso da um rewind em todos os stateBuffer desse indice em diante para tentar manter na mesma posicao

            // Now re-simulate the rest of the ticks up to the current tick on the client
            var tickToProcess = latestServerState.tick + 1
//            "TICKS TO PROCESS = ${currentTick - tickToProcess} | currentTick=$currentTick, tickToProcess=$tickToProcess".log()
            while (tickToProcess < currentTick) {
                val bufferIndex = tickToProcess % BUFFER_SIZE

                // Process new movement with reconciled state
                // TODO: reprocess it
//                        val statePayload: WorldState? = inputBuffer[bufferIndex]
//
//                        // Update buffer with recalculated state
//                        stateBuffer[bufferIndex] = statePayload
                inputBuffer[bufferIndex]?.let { scene.onPlayerUpdate(it, deltaTime) }

                tickToProcess++
            }
        }
    }

    private fun moveForward(
        deltaTime: Double,
        latestServerState: WorldState,
        serverStateBufferIndex: Int,
        lastProcessedState: WorldState,
    ) {

//        val lastClientStateBufferIndex = lastProcessedState.tick % BUFFER_SIZE

//        val lastClientPosition = stateBuffer[lastClientStateBufferIndex]?.playerUpdate?.players?.firstOrNull()?.playerMovement?.position
//            ?: return

        val serverPosition =
            latestServerState.playerUpdate.players.firstOrNull()?.playerMovement?.position
                ?: return

        val positionError =
            lastProcessedState.playerUpdate.players.firstOrNull()?.playerMovement?.position?.distance(
                serverPosition
            ) ?: return

        if (positionError > 0.001) {
            "moveForward $positionError".log()

            scene.onPlayerUpdate(latestServerState, deltaTime)

            // Update buffer at the current client index until the server tick
            // todo: I can bring more data from the server and not only one item, this way I could
            //  make a proper replace
            for (i in currentTick until latestServerState.tick) {
                stateBuffer[i % BUFFER_SIZE] = latestServerState
            }

            // Replace the current tick with the latestServer because I wanna to forward the game forcing a new value
            currentTick =
                latestServerState.tick  // - 1 // TODO: check if it will have impact since the currentTick is increase after this function is executed

            // como o ultimo state do servidor foi refeito, o jogador pode ter feito diversas açoes que nao serao processadas
            // entao posso da um rewind em todos os stateBuffer desse indice em diante para tentar manter na mesma posicao

            // Now re-simulate the rest of the ticks up to the current tick on the client
//            var tickToProcess = latestServerState.tick + 1
//            "TICKS TO PROCESS = ${currentTick - tickToProcess} | currentTick=$currentTick, tickToProcess=$tickToProcess".log()
//            while (tickToProcess < currentTick) {
//                val bufferIndex = tickToProcess % BUFFER_SIZE
//
//                // Process new movement with reconciled state
//                // TODO: reprocess it
////                        val statePayload: WorldState? = inputBuffer[bufferIndex]
////
////                        // Update buffer with recalculated state
////                        stateBuffer[bufferIndex] = statePayload
//                stateBuffer[bufferIndex]?.let { scene.onPlayerUpdate(it, deltaTime) }
//
//                tickToProcess++
//            }
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
        if (tickStated.not()) {
            currentTick = worldState.tick
        }
        "onPlayerUpdate, at tick=$currentTick, worldTick=${worldState.tick} updating latest ${worldState.playerUpdate.players.firstOrNull()?.playerMovement?.position}".log()
        latestServerState = worldState
    }

    fun getEnemies(): List<Player> {
        return scene.getEnemies()
    }
}