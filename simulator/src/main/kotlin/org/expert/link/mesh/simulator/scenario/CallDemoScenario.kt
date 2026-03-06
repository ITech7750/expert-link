package org.expert.link.mesh.simulator.scenario

import org.expert.link.mesh.contract.api.MeshCallSignalCommand
import org.expert.link.mesh.contract.api.MeshHangupCallCommand
import org.expert.link.mesh.contract.api.MeshStartCallCommand
import org.expert.link.mesh.contract.model.MeshCallSignalType

/**
 * Демо call signaling.
 *
 * Контракт:
 * - `MeshNode.startCall`
 * - `MeshNode.sendCallSignal`
 * - `MeshNode.callSessions`
 * - `MeshNode.hangupCall`
 *
 * Модели:
 * - запросы: `MeshStartCallCommand`, `MeshCallSignalCommand`, `MeshHangupCallCommand`
 * - ответы: `MeshCallSession`, `MeshCallSignal`
 */
class CallDemoScenario(
    private val basePort: Int,
) : DemoScenario {
    override val name: String = "CallDemoScenario"

    override suspend fun run(): DemoScenarioReport {
        val caller = ScenarioSupport.launchNode(name = "call-caller", port = basePort)
        val callee = ScenarioSupport.launchNode(name = "call-callee", port = basePort + 1)
        return try {
            ScenarioSupport.connectBidirectional(caller, callee)
            ScenarioSupport.pair(caller, callee)

            val conversation = caller.openConversation(callee.profile.peerId)
            val outbound = caller.startCall(
                MeshStartCallCommand(
                    targetPeerId = callee.profile.peerId,
                    offer = "v=0 demo-offer",
                    conversationId = conversation.conversationId,
                ),
            )
            ScenarioSupport.settle(500)

            val incoming = callee.callSessions().first()
            val accepted = callee.sendCallSignal(
                MeshCallSignalCommand(
                    callId = outbound.callId,
                    recipientPeerId = caller.profile.peerId,
                    signalType = MeshCallSignalType.ACCEPTED,
                    payload = "accepted-by-callee",
                ),
            )
            val quality = caller.sendCallSignal(
                MeshCallSignalCommand(
                    callId = outbound.callId,
                    recipientPeerId = callee.profile.peerId,
                    signalType = MeshCallSignalType.QUALITY,
                    payload = "rtt=42;jitter=3",
                ),
            )
            ScenarioSupport.settle(500)

            val callerState = caller.callSessions().first { it.callId == outbound.callId }
            val calleeState = callee.callSessions().first { it.callId == outbound.callId }
            val hangup = caller.hangupCall(
                MeshHangupCallCommand(
                    callId = outbound.callId,
                    recipientPeerId = callee.profile.peerId,
                    reason = "demo-complete",
                ),
            )
            ScenarioSupport.settle(300)

            DemoScenarioReport(
                name = name,
                lines = listOf(
                    "Feature: signaling звонка через публичный контракт.",
                    "Imports: MeshStartCallCommand, MeshCallSignalCommand, MeshHangupCallCommand, MeshCallSignalType.",
                    "Public API: startCall(command), sendCallSignal(command), callSessions(), hangupCall(command).",
                    "Request models: MeshStartCallCommand, MeshCallSignalCommand, MeshHangupCallCommand.",
                    "Response models: MeshCallSession, MeshCallSignal.",
                    "outbound.callId=${outbound.callId}",
                    "incoming.status=${incoming.status}",
                    "accepted.signalType=${accepted.signalType}",
                    "quality.signalType=${quality.signalType}",
                    "caller.state=${callerState.status}",
                    "callee.state=${calleeState.status}",
                    "hangup.result=${hangup?.callId}:${hangup?.status}",
                    "Mobile usage: UI вызывает startCall()/sendCallSignal()/hangupCall(), а состояние читает через callSessions().",
                ),
            )
        } finally {
            ScenarioSupport.stopAll(caller, callee)
        }
    }
}
