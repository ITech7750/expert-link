package org.expert.link.mesh.simulator.scenario

import org.expert.link.mesh.contract.api.MeshAcceptCallCommand
import org.expert.link.mesh.contract.api.MeshCallSignalCommand
import org.expert.link.mesh.contract.api.MeshEndCallCommand
import org.expert.link.mesh.contract.api.MeshJoinCallCommand
import org.expert.link.mesh.contract.api.MeshLeaveCallCommand
import org.expert.link.mesh.contract.api.MeshRejectCallCommand
import org.expert.link.mesh.contract.api.MeshStartCallCommand
import org.expert.link.mesh.contract.api.MeshStartGroupCallCommand
import org.expert.link.mesh.contract.api.MeshToggleCameraCommand
import org.expert.link.mesh.contract.api.MeshToggleMicrophoneCommand
import org.expert.link.mesh.contract.model.MeshCallSignalType

/**
 * Демо сценарий call subsystem v2 через публичный контракт `MeshNode`.
 *
 * Покрывает:
 * - 1:1 аудиозвонок (invite -> accept -> active -> end);
 * - 1:1 видеозвонок (invite -> reject);
 * - групповой видеозвонок (invite -> join -> leave -> end).
 */
class CallDemoScenario(
    private val basePort: Int,
) : DemoScenario {
    override val name: String = "CallDemoScenario"

    override suspend fun run(): DemoScenarioReport {
        val caller = ScenarioSupport.launchNode(name = "call-caller", port = basePort)
        val callee = ScenarioSupport.launchNode(name = "call-callee", port = basePort + 1)
        val third = ScenarioSupport.launchNode(name = "call-third", port = basePort + 2)
        return try {
            ScenarioSupport.connectBidirectional(caller, callee)
            ScenarioSupport.connectBidirectional(caller, third)
            ScenarioSupport.connectBidirectional(callee, third)
            ScenarioSupport.pair(caller, callee)
            ScenarioSupport.pair(caller, third)
            ScenarioSupport.pair(callee, third)

            val directConversation = caller.openConversation(callee.profile.peerId)

            val audio = caller.startAudioCall(
                MeshStartCallCommand(
                    targetPeerId = callee.profile.peerId,
                    offer = "v=0 audio-offer",
                    conversationId = directConversation.conversationId,
                ),
            )
            ScenarioSupport.settle(350)
            val incomingAudio = callee.observeIncomingCalls().first { it.callId == audio.callId }
            val accept = callee.acceptCall(
                MeshAcceptCallCommand(
                    callId = audio.callId,
                    recipientPeerId = caller.profile.peerId,
                    answer = "v=0 audio-answer",
                ),
            )
            ScenarioSupport.settle(350)
            @Suppress("DEPRECATION")
            val quality = caller.sendCallSignal(
                MeshCallSignalCommand(
                    callId = audio.callId,
                    recipientPeerId = callee.profile.peerId,
                    signalType = MeshCallSignalType.QUALITY,
                    payload = "rtt=38;jitter=4",
                ),
            )
            val activeAudio = caller.observeActiveCall().first { it.callId == audio.callId }
            val mediaStateBefore = caller.observeMediaState(audio.callId)
            val mediaStatsBefore = caller.observeMediaStats(audio.callId)
            val toggledMic = caller.toggleMicrophone(MeshToggleMicrophoneCommand(audio.callId, enabled = false))
            val toggledCam = caller.toggleCamera(MeshToggleCameraCommand(audio.callId, enabled = false))
            val switchedCamera = caller.switchCamera(audio.callId)
            val endAudio = caller.endCall(MeshEndCallCommand(audio.callId, "audio-demo-complete"))
            ScenarioSupport.settle(350)

            val video = caller.startVideoCall(
                MeshStartCallCommand(
                    targetPeerId = callee.profile.peerId,
                    offer = "v=0 video-offer",
                    conversationId = directConversation.conversationId,
                ),
            )
            ScenarioSupport.settle(300)
            val reject = callee.rejectCall(
                MeshRejectCallCommand(
                    callId = video.callId,
                    recipientPeerId = caller.profile.peerId,
                    reason = "busy",
                ),
            )
            ScenarioSupport.settle(300)

            val group = caller.startGroupVideoCall(
                MeshStartGroupCallCommand(
                    targetPeerIds = setOf(callee.profile.peerId, third.profile.peerId),
                    offer = "v=0 group-video-offer",
                    roomTitle = "Команда",
                ),
            )
            ScenarioSupport.settle(350)
            val joinB = callee.joinCall(
                MeshJoinCallCommand(
                    callId = group.callId,
                    recipientPeerId = caller.profile.peerId,
                    answer = "v=0 join-b",
                ),
            )
            val joinC = third.joinCall(
                MeshJoinCallCommand(
                    callId = group.callId,
                    recipientPeerId = caller.profile.peerId,
                    answer = "v=0 join-c",
                ),
            )
            ScenarioSupport.settle(350)
            val participantsAfterJoin = caller.observeCallParticipants(group.callId)
            val leaveC = third.leaveCall(
                MeshLeaveCallCommand(
                    callId = group.callId,
                    recipientPeerId = caller.profile.peerId,
                    reason = "left-demo",
                ),
            )
            ScenarioSupport.settle(250)
            val events = caller.observeCallEvents(group.callId, limit = 32)
            val endGroup = caller.endCall(MeshEndCallCommand(group.callId, "group-demo-complete"))
            ScenarioSupport.settle(350)

            DemoScenarioReport(
                name = name,
                lines = listOf(
                    "Feature: call subsystem v2 через публичный контракт.",
                    "Imports: MeshStartCallCommand, MeshStartGroupCallCommand, MeshAcceptCallCommand, MeshRejectCallCommand, MeshJoinCallCommand, MeshLeaveCallCommand, MeshEndCallCommand.",
                    "Public API: startAudioCall(), startVideoCall(), startGroupVideoCall(), acceptCall(), rejectCall(), joinCall(), leaveCall(), endCall(), observeIncomingCalls(), observeActiveCall(), observeCallParticipants(), observeCallEvents().",
                    "Request models: MeshStartCallCommand, MeshStartGroupCallCommand, MeshAcceptCallCommand, MeshRejectCallCommand, MeshJoinCallCommand, MeshLeaveCallCommand, MeshEndCallCommand.",
                    "Response models: MeshCallSession, MeshCallSignal, MeshCallParticipant, MeshCallEvent.",
                    "audio.callId=${audio.callId}",
                    "audio.incoming=${incomingAudio.status}",
                    "audio.accept.signal=${accept.signalType}",
                    "audio.quality.signal=${quality.signalType}",
                    "audio.active=${activeAudio.status}",
                    "audio.media.before=${mediaStateBefore?.connectionState ?: "UNAVAILABLE"}",
                    "audio.media.stats=${mediaStatsBefore?.rttMs?.toString() ?: "UNAVAILABLE"}",
                    "audio.media.toggle.mic=${toggledMic?.localAudioEnabled?.toString() ?: "UNAVAILABLE"}",
                    "audio.media.toggle.cam=${toggledCam?.localVideoEnabled?.toString() ?: "UNAVAILABLE"}",
                    "audio.media.switch.camera=${switchedCamera?.cameraFacing?.name ?: "UNAVAILABLE"}",
                    "audio.end=${endAudio?.status}",
                    "video.callId=${video.callId}",
                    "video.reject.signal=${reject.signalType}",
                    "group.callId=${group.callId}",
                    "group.join.b=${joinB.signalType}",
                    "group.join.c=${joinC.signalType}",
                    "group.participants=${participantsAfterJoin.size}",
                    "group.leave.c=${leaveC.signalType}",
                    "group.events=${events.size}",
                    "group.end=${endGroup?.status}",
                    "Mobile usage: UI работает только через MeshNode call v2 методы и читает состояния polling-вызовами observe*.",
                ),
            )
        } finally {
            ScenarioSupport.stopAll(caller, callee, third)
        }
    }
}
