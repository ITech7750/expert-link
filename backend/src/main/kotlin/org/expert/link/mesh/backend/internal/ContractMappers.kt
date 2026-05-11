package org.expert.link.mesh.backend.internal

import org.expert.link.mesh.application.service.RouteHop
import org.expert.link.mesh.application.service.RoutingPlan
import org.expert.link.mesh.application.service.InventorySyncResult
import org.expert.link.mesh.bootstrap.config.FeatureFlags
import org.expert.link.mesh.bootstrap.config.FileTransferSettings
import org.expert.link.mesh.bootstrap.config.NodeConfiguration
import org.expert.link.mesh.bootstrap.config.NodeMode
import org.expert.link.mesh.bootstrap.config.RelayClientSettings
import org.expert.link.mesh.bootstrap.config.RetrySettings
import org.expert.link.mesh.bootstrap.config.StaticPeerConfig
import org.expert.link.mesh.contract.config.MeshFeatureFlags
import org.expert.link.mesh.contract.config.MeshFileTransferConfig
import org.expert.link.mesh.contract.config.MeshNodeConfig
import org.expert.link.mesh.contract.config.MeshNodeMode
import org.expert.link.mesh.contract.config.MeshRelayConfig
import org.expert.link.mesh.contract.config.MeshRetryConfig
import org.expert.link.mesh.contract.config.MeshStaticPeer
import org.expert.link.mesh.contract.model.MeshCentralConfig
import org.expert.link.mesh.contract.model.MeshBlockedPeer
import org.expert.link.mesh.contract.model.MeshCallEvent
import org.expert.link.mesh.contract.model.MeshCallEventType
import org.expert.link.mesh.contract.model.MeshCallInvitation
import org.expert.link.mesh.contract.model.MeshCallParticipant
import org.expert.link.mesh.contract.model.MeshCallParticipantState
import org.expert.link.mesh.contract.model.MeshCallRoom
import org.expert.link.mesh.contract.model.MeshCallScope
import org.expert.link.mesh.contract.model.MeshCallSession
import org.expert.link.mesh.contract.model.MeshCallSignal
import org.expert.link.mesh.contract.model.MeshCallSignalType
import org.expert.link.mesh.contract.model.MeshCallState
import org.expert.link.mesh.contract.model.MeshCallType
import org.expert.link.mesh.contract.model.MeshCallMediaState
import org.expert.link.mesh.contract.model.MeshMediaStats
import org.expert.link.mesh.contract.model.MeshPeerMediaState
import org.expert.link.mesh.contract.model.MeshSessionDescription
import org.expert.link.mesh.contract.model.MeshIceCandidate
import org.expert.link.mesh.contract.model.MeshWebRtcSignalEvent
import org.expert.link.mesh.contract.model.MeshMediaConnectionState
import org.expert.link.mesh.contract.model.MeshCameraFacing
import org.expert.link.mesh.contract.model.MeshSdpType
import org.expert.link.mesh.contract.model.MeshGroupCallRoom
import org.expert.link.mesh.contract.model.MeshChatMessage
import org.expert.link.mesh.contract.model.MeshChatMember
import org.expert.link.mesh.contract.model.MeshChatMemberRole
import org.expert.link.mesh.contract.model.MeshChatSummary
import org.expert.link.mesh.contract.model.MeshChatType
import org.expert.link.mesh.contract.model.MeshConversation
import org.expert.link.mesh.contract.model.MeshEventCategory
import org.expert.link.mesh.contract.model.MeshEventLevel
import org.expert.link.mesh.contract.model.MeshEventLogEntry
import org.expert.link.mesh.contract.model.MeshFileDescriptor
import org.expert.link.mesh.contract.model.MeshFileTransferSession
import org.expert.link.mesh.contract.model.MeshFileTransferStatus
import org.expert.link.mesh.contract.model.MeshGroupChat
import org.expert.link.mesh.contract.model.MeshGroupEvent
import org.expert.link.mesh.contract.model.MeshGroupEventType
import org.expert.link.mesh.contract.model.MeshOrganization
import org.expert.link.mesh.contract.model.MeshOrganizationMember
import org.expert.link.mesh.contract.model.MeshOrganizationMemberStatus
import org.expert.link.mesh.contract.model.MeshRole
import org.expert.link.mesh.contract.model.MeshInventoryAttachment
import org.expert.link.mesh.contract.model.MeshInventoryAttachmentPreviewMetadata
import org.expert.link.mesh.contract.model.MeshInventoryAttachmentStatus
import org.expert.link.mesh.contract.model.MeshInventoryAttachmentSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryAttachmentType
import org.expert.link.mesh.contract.model.MeshInventoryAttributeDefinition
import org.expert.link.mesh.contract.model.MeshInventoryAttributeDefinitionSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryAttributeType
import org.expert.link.mesh.contract.model.MeshInventoryAttributeValue
import org.expert.link.mesh.contract.model.MeshInventoryCategory
import org.expert.link.mesh.contract.model.MeshInventoryCategoryTemplate
import org.expert.link.mesh.contract.model.MeshInventoryCategorySnapshot
import org.expert.link.mesh.contract.model.MeshInventoryCategoryTemplateSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryChangeLog
import org.expert.link.mesh.contract.model.MeshInventoryChangeLogSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryBarcodeFormat
import org.expert.link.mesh.contract.model.MeshInventoryCode
import org.expert.link.mesh.contract.model.MeshInventoryCodeBinding
import org.expert.link.mesh.contract.model.MeshInventoryCodeSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryCodeType
import org.expert.link.mesh.contract.model.MeshInventoryCodeBindingSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryComment
import org.expert.link.mesh.contract.model.MeshInventoryCommentSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryCondition
import org.expert.link.mesh.contract.model.MeshInventoryConfirmationStatus
import org.expert.link.mesh.contract.model.MeshInventoryConflict
import org.expert.link.mesh.contract.model.MeshInventoryConflictSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryCostCenter
import org.expert.link.mesh.contract.model.MeshInventoryCostCenterSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryDeadlineRule
import org.expert.link.mesh.contract.model.MeshInventoryDeadlineRuleSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryDeadlineTarget
import org.expert.link.mesh.contract.model.MeshInventoryDepartment
import org.expert.link.mesh.contract.model.MeshInventoryDepartmentSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryFieldChange
import org.expert.link.mesh.contract.model.MeshInventoryFieldTemplate
import org.expert.link.mesh.contract.model.MeshInventoryFilterSet
import org.expert.link.mesh.contract.model.MeshInventoryEntityType
import org.expert.link.mesh.contract.model.MeshInventoryEvent
import org.expert.link.mesh.contract.model.MeshInventoryEventPayload
import org.expert.link.mesh.contract.model.MeshInventoryEventType
import org.expert.link.mesh.contract.model.MeshInventoryExportFormat
import org.expert.link.mesh.contract.model.MeshInventoryExportSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryExportStatus
import org.expert.link.mesh.contract.model.MeshInventoryExportTask
import org.expert.link.mesh.contract.model.MeshInventoryFundingSource
import org.expert.link.mesh.contract.model.MeshInventoryFundingSourceSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryIncident
import org.expert.link.mesh.contract.model.MeshInventoryIncidentSeverity
import org.expert.link.mesh.contract.model.MeshInventoryIncidentStatus
import org.expert.link.mesh.contract.model.MeshInventoryIncidentType
import org.expert.link.mesh.contract.model.MeshInventoryIncidentSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryItem
import org.expert.link.mesh.contract.model.MeshInventoryItemType
import org.expert.link.mesh.contract.model.MeshInventoryItemSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryLabel
import org.expert.link.mesh.contract.model.MeshInventoryLabelField
import org.expert.link.mesh.contract.model.MeshInventoryLabelFieldKey
import org.expert.link.mesh.contract.model.MeshInventoryLabelSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryLabelTemplate
import org.expert.link.mesh.contract.model.MeshInventoryLabelTemplateSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryLabelTemplateType
import org.expert.link.mesh.contract.model.MeshInventoryLegalHolder
import org.expert.link.mesh.contract.model.MeshInventoryLegalHolderSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryLocation
import org.expert.link.mesh.contract.model.MeshInventoryLocationType
import org.expert.link.mesh.contract.model.MeshInventoryLocationSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryAcceptanceStatus
import org.expert.link.mesh.contract.model.MeshInventoryOwner
import org.expert.link.mesh.contract.model.MeshInventoryOwnerType
import org.expert.link.mesh.contract.model.MeshInventoryOwnerSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryMemberSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryMergeResult
import org.expert.link.mesh.contract.model.MeshInventoryOrganizationSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryPermission
import org.expert.link.mesh.contract.model.MeshInventoryPresenceStatus
import org.expert.link.mesh.contract.model.MeshInventoryPrintStatus
import org.expert.link.mesh.contract.model.MeshInventoryPrintTask
import org.expert.link.mesh.contract.model.MeshInventoryPrintTaskSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryQrCode
import org.expert.link.mesh.contract.model.MeshInventoryQrSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryReminder
import org.expert.link.mesh.contract.model.MeshInventoryReminderStatus
import org.expert.link.mesh.contract.model.MeshInventoryReminderSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryRequiredFieldRule
import org.expert.link.mesh.contract.model.MeshInventoryReview
import org.expert.link.mesh.contract.model.MeshInventoryReviewSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryReviewStatus
import org.expert.link.mesh.contract.model.MeshInventoryRoleSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryRuleThreshold
import org.expert.link.mesh.contract.model.MeshInventoryRuleThresholdSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryRuleType
import org.expert.link.mesh.contract.model.MeshInventoryScanEvent
import org.expert.link.mesh.contract.model.MeshInventoryScanEventSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryScanResultStatus
import org.expert.link.mesh.contract.model.MeshInventorySearchQuery
import org.expert.link.mesh.contract.model.MeshInventorySearchResult
import org.expert.link.mesh.contract.model.MeshInventorySession
import org.expert.link.mesh.contract.model.MeshInventorySessionMember
import org.expert.link.mesh.contract.model.MeshInventorySessionResult
import org.expert.link.mesh.contract.model.MeshInventorySessionReviewStatus
import org.expert.link.mesh.contract.model.MeshInventorySessionRole
import org.expert.link.mesh.contract.model.MeshInventorySessionSnapshot
import org.expert.link.mesh.contract.model.MeshInventorySessionStatus
import org.expert.link.mesh.contract.model.MeshInventoryStatus
import org.expert.link.mesh.contract.model.MeshInventorySubcategory
import org.expert.link.mesh.contract.model.MeshInventorySubcategorySnapshot
import org.expert.link.mesh.contract.model.MeshInventorySupplier
import org.expert.link.mesh.contract.model.MeshInventorySupplierSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryTag
import org.expert.link.mesh.contract.model.MeshInventoryTagSnapshot
import org.expert.link.mesh.contract.model.MeshInventorySortMode
import org.expert.link.mesh.contract.model.MeshInventorySyncResult
import org.expert.link.mesh.contract.model.MeshInventorySyncStatus
import org.expert.link.mesh.contract.model.MeshInventoryWorkflowStatus
import org.expert.link.mesh.contract.model.MeshInventoryValidationRule
import org.expert.link.mesh.contract.model.MeshInventoryValidationRuleType
import org.expert.link.mesh.contract.model.MeshInventoryDashboardSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryDashboardSnapshotPayload
import org.expert.link.mesh.contract.model.MeshInventoryAlertEvent
import org.expert.link.mesh.contract.model.MeshInventoryAlertSnapshot
import org.expert.link.mesh.contract.model.MeshInventoryRevision
import org.expert.link.mesh.contract.model.MeshInventoryRevisionSnapshot
import org.expert.link.mesh.contract.model.MeshLocalProfile
import org.expert.link.mesh.contract.model.MeshMediaQualitySnapshot
import org.expert.link.mesh.contract.model.MeshMessageDeliveryStatus
import org.expert.link.mesh.contract.model.MeshMessageType
import org.expert.link.mesh.contract.model.MeshMessageReceipt
import org.expert.link.mesh.contract.model.MeshPairingRole
import org.expert.link.mesh.contract.model.MeshPairingSession
import org.expert.link.mesh.contract.model.MeshPairedPeer
import org.expert.link.mesh.contract.model.MeshPeerEndpoint
import org.expert.link.mesh.contract.model.MeshPeerIdentity
import org.expert.link.mesh.contract.model.MeshMetricSnapshot
import org.expert.link.mesh.contract.model.MeshNearbyPeer
import org.expert.link.mesh.contract.model.MeshEndpointSource
import org.expert.link.mesh.contract.model.MeshTransferDirection
import org.expert.link.mesh.contract.model.MeshTrustState
import org.expert.link.mesh.contract.model.MeshThreadSummary
import org.expert.link.mesh.contract.model.MeshThread
import org.expert.link.mesh.contract.model.MeshThreadMessage
import org.expert.link.mesh.contract.model.MeshRouteHop
import org.expert.link.mesh.contract.model.MeshRouteInfo
import org.expert.link.mesh.contract.model.MeshRouteHealth
import org.expert.link.mesh.contract.model.MeshRouteHealthState
import org.expert.link.mesh.contract.model.MeshRouteMode
import org.expert.link.mesh.contract.model.MeshRoutingPlan
import org.expert.link.mesh.contract.model.MeshConnectivityMode
import org.expert.link.mesh.contract.model.MeshConnectivityStrategy
import org.expert.link.mesh.contract.model.MeshHostCandidate
import org.expert.link.mesh.contract.model.MeshHostRole
import org.expert.link.mesh.contract.model.MeshNetworkRoleState
import org.expert.link.mesh.contract.model.MeshRelayMode
import org.expert.link.mesh.contract.model.MeshTopologyEvent
import org.expert.link.mesh.contract.model.MeshTopologyEventType
import org.expert.link.mesh.contract.model.MeshTopologyState
import org.expert.link.mesh.domain.model.call.CallEvent
import org.expert.link.mesh.domain.model.call.CallEventType
import org.expert.link.mesh.domain.model.call.CallInvitation
import org.expert.link.mesh.domain.model.call.CallParticipant
import org.expert.link.mesh.domain.model.call.CallParticipantState
import org.expert.link.mesh.domain.model.call.CallRoom
import org.expert.link.mesh.domain.model.call.CallScope
import org.expert.link.mesh.domain.model.call.CallSession
import org.expert.link.mesh.domain.model.call.CallSignal
import org.expert.link.mesh.domain.model.call.CallSignalType
import org.expert.link.mesh.domain.model.call.CallState
import org.expert.link.mesh.domain.model.call.CallType
import org.expert.link.mesh.domain.model.call.GroupCallRoom
import org.expert.link.mesh.domain.model.call.MediaQualitySnapshot
import org.expert.link.mesh.domain.model.call.CallMediaState
import org.expert.link.mesh.domain.model.call.CallMediaStats
import org.expert.link.mesh.domain.model.call.PeerMediaState
import org.expert.link.mesh.domain.model.call.SessionDescription
import org.expert.link.mesh.domain.model.call.IceCandidate
import org.expert.link.mesh.domain.model.call.WebRtcSignalEvent
import org.expert.link.mesh.domain.model.call.MediaConnectionState
import org.expert.link.mesh.domain.model.call.CameraFacing
import org.expert.link.mesh.domain.model.call.SdpType
import org.expert.link.mesh.domain.model.diagnostics.EventCategory
import org.expert.link.mesh.domain.model.diagnostics.EventLevel
import org.expert.link.mesh.domain.model.diagnostics.EventLogEntry
import org.expert.link.mesh.domain.model.diagnostics.MetricSnapshot
import org.expert.link.mesh.domain.model.filetransfer.FileDescriptor
import org.expert.link.mesh.domain.model.filetransfer.FileTransferSession
import org.expert.link.mesh.domain.model.filetransfer.FileTransferStatus
import org.expert.link.mesh.domain.model.filetransfer.TransferDirection
import org.expert.link.mesh.domain.model.identity.LocalProfile
import org.expert.link.mesh.domain.model.identity.PairedPeer
import org.expert.link.mesh.domain.model.identity.PairingSession
import org.expert.link.mesh.domain.model.identity.PairingSessionRole
import org.expert.link.mesh.domain.model.identity.PeerIdentity
import org.expert.link.mesh.domain.model.identity.TrustState
import org.expert.link.mesh.domain.model.messaging.ChatMessage
import org.expert.link.mesh.domain.model.messaging.ChatMember
import org.expert.link.mesh.domain.model.messaging.ChatMemberRole
import org.expert.link.mesh.domain.model.messaging.ChatSummary
import org.expert.link.mesh.domain.model.messaging.ChatThread
import org.expert.link.mesh.domain.model.messaging.ChatType
import org.expert.link.mesh.domain.model.messaging.Conversation
import org.expert.link.mesh.domain.model.messaging.GroupChat
import org.expert.link.mesh.domain.model.messaging.GroupChatEvent
import org.expert.link.mesh.domain.model.messaging.GroupEventType
import org.expert.link.mesh.domain.model.messaging.MessageDeliveryStatus
import org.expert.link.mesh.domain.model.messaging.MessageType
import org.expert.link.mesh.domain.model.messaging.MessageReceipt
import org.expert.link.mesh.domain.model.messaging.ThreadMessage
import org.expert.link.mesh.domain.model.messaging.ThreadSummary
import org.expert.link.mesh.domain.model.inventory.InventoryAttachment
import org.expert.link.mesh.domain.model.inventory.InventoryAttachmentPreviewMetadata
import org.expert.link.mesh.domain.model.inventory.InventoryAttachmentStatus
import org.expert.link.mesh.domain.model.inventory.InventoryAttachmentSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryAttachmentType
import org.expert.link.mesh.domain.model.inventory.InventoryAttributeDefinition
import org.expert.link.mesh.domain.model.inventory.InventoryAttributeDefinitionSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryAttributeType
import org.expert.link.mesh.domain.model.inventory.InventoryAttributeValue
import org.expert.link.mesh.domain.model.inventory.InventoryCategory
import org.expert.link.mesh.domain.model.inventory.InventoryCategoryTemplate
import org.expert.link.mesh.domain.model.inventory.InventoryCategorySnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryCategoryTemplateSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryChangeLog
import org.expert.link.mesh.domain.model.inventory.InventoryChangeLogSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryBarcodeFormat
import org.expert.link.mesh.domain.model.inventory.InventoryCode
import org.expert.link.mesh.domain.model.inventory.InventoryCodeSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryCodeBinding
import org.expert.link.mesh.domain.model.inventory.InventoryCodeBindingSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryCodeType
import org.expert.link.mesh.domain.model.inventory.InventoryComment
import org.expert.link.mesh.domain.model.inventory.InventoryCommentSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryCondition
import org.expert.link.mesh.domain.model.inventory.InventoryConfirmationStatus
import org.expert.link.mesh.domain.model.inventory.InventoryConflict
import org.expert.link.mesh.domain.model.inventory.InventoryConflictSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryCostCenter
import org.expert.link.mesh.domain.model.inventory.InventoryCostCenterSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryDeadlineRule
import org.expert.link.mesh.domain.model.inventory.InventoryDeadlineRuleSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryDeadlineTarget
import org.expert.link.mesh.domain.model.inventory.InventoryDepartment
import org.expert.link.mesh.domain.model.inventory.InventoryDepartmentSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryFieldChange
import org.expert.link.mesh.domain.model.inventory.InventoryFieldTemplate
import org.expert.link.mesh.domain.model.inventory.InventoryFilterSet
import org.expert.link.mesh.domain.model.inventory.InventoryEvent
import org.expert.link.mesh.domain.model.inventory.InventoryEventPayload
import org.expert.link.mesh.domain.model.inventory.InventoryEventType
import org.expert.link.mesh.domain.model.inventory.InventoryExportFormat
import org.expert.link.mesh.domain.model.inventory.InventoryExportSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryExportStatus
import org.expert.link.mesh.domain.model.inventory.InventoryExportTask
import org.expert.link.mesh.domain.model.inventory.InventoryFundingSource
import org.expert.link.mesh.domain.model.inventory.InventoryFundingSourceSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryIncident
import org.expert.link.mesh.domain.model.inventory.InventoryIncidentSeverity
import org.expert.link.mesh.domain.model.inventory.InventoryIncidentStatus
import org.expert.link.mesh.domain.model.inventory.InventoryIncidentType
import org.expert.link.mesh.domain.model.inventory.InventoryIncidentSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryItem
import org.expert.link.mesh.domain.model.inventory.InventoryItemType
import org.expert.link.mesh.domain.model.inventory.InventoryItemSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryLabel
import org.expert.link.mesh.domain.model.inventory.InventoryLabelField
import org.expert.link.mesh.domain.model.inventory.InventoryLabelFieldKey
import org.expert.link.mesh.domain.model.inventory.InventoryLabelSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryLabelTemplate
import org.expert.link.mesh.domain.model.inventory.InventoryLabelTemplateSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryLabelTemplateType
import org.expert.link.mesh.domain.model.inventory.InventoryLegalHolder
import org.expert.link.mesh.domain.model.inventory.InventoryLegalHolderSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryLocation
import org.expert.link.mesh.domain.model.inventory.InventoryLocationType
import org.expert.link.mesh.domain.model.inventory.InventoryLocationSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryMergeResult
import org.expert.link.mesh.domain.model.inventory.InventoryMemberSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryOrganizationSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryAcceptanceStatus
import org.expert.link.mesh.domain.model.inventory.InventoryOwner
import org.expert.link.mesh.domain.model.inventory.InventoryOwnerType
import org.expert.link.mesh.domain.model.inventory.InventoryOwnerSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryPermission
import org.expert.link.mesh.domain.model.inventory.InventoryPresenceStatus
import org.expert.link.mesh.domain.model.inventory.InventoryPrintStatus
import org.expert.link.mesh.domain.model.inventory.InventoryPrintTask
import org.expert.link.mesh.domain.model.inventory.InventoryPrintTaskSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryQrCode
import org.expert.link.mesh.domain.model.inventory.InventoryQrSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryReminder
import org.expert.link.mesh.domain.model.inventory.InventoryReminderStatus
import org.expert.link.mesh.domain.model.inventory.InventoryReminderSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryRequiredFieldRule
import org.expert.link.mesh.domain.model.inventory.InventoryReview
import org.expert.link.mesh.domain.model.inventory.InventoryReviewSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryReviewStatus
import org.expert.link.mesh.domain.model.inventory.InventoryRoleSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryRuleThreshold
import org.expert.link.mesh.domain.model.inventory.InventoryRuleThresholdSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryRuleType
import org.expert.link.mesh.domain.model.inventory.InventoryScanEvent
import org.expert.link.mesh.domain.model.inventory.InventoryScanEventSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryScanResultStatus
import org.expert.link.mesh.domain.model.inventory.InventorySearchQuery
import org.expert.link.mesh.domain.model.inventory.InventorySearchResult
import org.expert.link.mesh.domain.model.inventory.InventorySortMode
import org.expert.link.mesh.domain.model.inventory.InventorySession
import org.expert.link.mesh.domain.model.inventory.InventorySessionMember
import org.expert.link.mesh.domain.model.inventory.InventorySessionResult
import org.expert.link.mesh.domain.model.inventory.InventorySessionReviewStatus
import org.expert.link.mesh.domain.model.inventory.InventorySessionRole
import org.expert.link.mesh.domain.model.inventory.InventorySessionSnapshot
import org.expert.link.mesh.domain.model.inventory.InventorySessionStatus
import org.expert.link.mesh.domain.model.inventory.InventoryStatus
import org.expert.link.mesh.domain.model.inventory.InventorySubcategory
import org.expert.link.mesh.domain.model.inventory.InventorySubcategorySnapshot
import org.expert.link.mesh.domain.model.inventory.InventorySupplier
import org.expert.link.mesh.domain.model.inventory.InventorySupplierSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryTag
import org.expert.link.mesh.domain.model.inventory.InventoryTagSnapshot
import org.expert.link.mesh.domain.model.inventory.Organization
import org.expert.link.mesh.domain.model.inventory.OrganizationMember
import org.expert.link.mesh.domain.model.inventory.OrganizationMemberStatus
import org.expert.link.mesh.domain.model.inventory.Role
import org.expert.link.mesh.domain.model.inventory.InventorySyncStatus
import org.expert.link.mesh.domain.model.inventory.InventoryWorkflowStatus
import org.expert.link.mesh.domain.model.inventory.InventoryDashboardSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryDashboardSnapshotPayload
import org.expert.link.mesh.domain.model.inventory.InventoryAlertEvent
import org.expert.link.mesh.domain.model.inventory.InventoryAlertSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryRevision
import org.expert.link.mesh.domain.model.inventory.InventoryRevisionSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryValidationRule
import org.expert.link.mesh.domain.model.inventory.InventoryValidationRuleType
import org.expert.link.mesh.domain.model.network.ConnectivityMode
import org.expert.link.mesh.domain.model.network.ConnectivityStrategy
import org.expert.link.mesh.domain.model.network.HostCandidate
import org.expert.link.mesh.domain.model.network.HostRole
import org.expert.link.mesh.domain.model.network.NetworkRoleState
import org.expert.link.mesh.domain.model.network.NetworkTopologyState
import org.expert.link.mesh.domain.model.network.RelayMode
import org.expert.link.mesh.domain.model.network.PeerEndpoint
import org.expert.link.mesh.domain.model.network.PeerEndpointCandidate
import org.expert.link.mesh.domain.model.network.RouteHealth
import org.expert.link.mesh.domain.model.network.RouteHealthState
import org.expert.link.mesh.domain.model.network.RouteEntry
import org.expert.link.mesh.domain.model.network.RouteMode
import org.expert.link.mesh.domain.model.network.TopologyEvent
import org.expert.link.mesh.domain.model.network.TopologyEventType
import org.expert.link.mesh.domain.model.security.BlockedPeer
import org.expert.link.mesh.domain.model.network.EndpointSource
import org.expert.link.mesh.domain.model.hybrid.CentralNodeConfiguration

internal fun MeshNodeConfig.toRuntime(): NodeConfiguration = NodeConfiguration(
    displayName = displayName,
    bindHost = bindHost,
    httpPort = httpPort,
    discoveryPort = discoveryPort,
    multicastGroup = multicastGroup,
    nodeMode = NodeMode.valueOf(nodeMode.name),
    featureFlags = featureFlags.toRuntime(),
    retrySettings = retry.toRuntime(),
    fileTransferSettings = fileTransfer.toRuntime(),
    relayClientSettings = relay?.toRuntime(),
    centralConfiguration = central?.toRuntime(),
    capabilities = capabilities,
    staticPeers = staticPeers.map(MeshStaticPeer::toRuntime),
)

private fun MeshFeatureFlags.toRuntime(): FeatureFlags = FeatureFlags(
    discoveryEnabled = discoveryEnabled,
    relayEnabled = relayEnabled,
    inMemoryTransport = inMemoryTransport,
    inMemoryDiscovery = inMemoryDiscovery,
)

private fun MeshRetryConfig.toRuntime(): RetrySettings = RetrySettings(
    pollIntervalMillis = pollIntervalMillis,
)

private fun MeshFileTransferConfig.toRuntime(): FileTransferSettings = FileTransferSettings(
    chunkSizeBytes = chunkSizeBytes,
    downloadDirectory = downloadDirectory,
)

private fun MeshRelayConfig.toRuntime(): RelayClientSettings = RelayClientSettings(
    enabled = enabled,
    forceRelayLookup = forceRelayLookup,
    relayEligible = relayEligible,
)

private fun MeshCentralConfig.toRuntime(): CentralNodeConfiguration = CentralNodeConfiguration(
    baseUrl = baseUrl,
    authPath = authPath,
    requestTimeoutMillis = requestTimeoutMillis,
    deviceId = deviceId,
    allowMeteredSync = allowMeteredSync,
    syncOnStart = syncOnStart,
    syncOnResume = syncOnResume,
    backgroundSyncIntervalSeconds = backgroundSyncIntervalSeconds,
)

private fun MeshStaticPeer.toRuntime(): StaticPeerConfig = StaticPeerConfig(
    peerId = peerId,
    host = host,
    port = port,
)

internal fun PeerEndpoint.toContract(): MeshPeerEndpoint = MeshPeerEndpoint(
    scheme = scheme,
    host = host,
    port = port,
    path = path,
    announcedPeerId = announcedPeerId,
    announcedAt = announcedAt,
    expiresAt = expiresAt,
)

internal fun MeshPeerEndpoint.toDomain(): PeerEndpoint = PeerEndpoint(
    scheme = scheme,
    host = host,
    port = port,
    path = path,
    announcedPeerId = announcedPeerId,
    announcedAt = announcedAt,
    expiresAt = expiresAt,
)

internal fun LocalProfile.toContract(): MeshLocalProfile = MeshLocalProfile(
    peerId = peerId,
    displayName = displayName,
    publicKey = publicKey,
    capabilities = capabilities,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun PeerIdentity.toContract(): MeshPeerIdentity = MeshPeerIdentity(
    peerId = peerId,
    displayName = displayName,
    publicKey = publicKey,
    capabilities = capabilities,
)

internal fun PairedPeer.toContract(): MeshPairedPeer = MeshPairedPeer(
    identity = peerIdentity.toContract(),
    trustState = MeshTrustState.valueOf(trustState.name),
    pairedAt = pairedAt,
    lastSeenAt = lastSeenAt,
    endpointHint = endpointHint?.toContract(),
    metadata = metadata,
)

internal fun PairingSession.toContract(): MeshPairingSession = MeshPairingSession(
    sessionId = sessionId,
    localPeerId = localPeerId,
    remotePeerId = remotePeerId,
    role = MeshPairingRole.valueOf(role.name),
    createdAt = createdAt,
    expiresAt = expiresAt,
    trustState = MeshTrustState.valueOf(trustState.name),
    used = used,
    endpointHint = endpointHint?.toContract(),
    remotePublicKey = remotePublicKey,
    remoteDisplayName = remoteDisplayName,
)

internal fun BlockedPeer.toContract(): MeshBlockedPeer = MeshBlockedPeer(
    peerId = peerId,
    reason = reason,
    blockedAt = blockedAt,
    expiresAt = expiresAt,
)

internal fun Conversation.toContract(): MeshConversation = MeshConversation(
    conversationId = conversationId,
    chatType = MeshChatType.valueOf(chatType.name),
    title = title,
    description = description,
    createdByPeerId = createdByPeerId,
    participantPeerIds = participantPeerIds,
    members = members.map(ChatMember::toContract),
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastMessageId = lastMessageId,
    unreadCount = unreadCount,
    pinned = pinned,
    archived = archived,
)

internal fun ChatMember.toContract(): MeshChatMember = MeshChatMember(
    peerId = peerId,
    displayName = displayName,
    role = MeshChatMemberRole.valueOf(role.name),
    joinedAt = joinedAt,
)

internal fun ChatMessage.toContract(): MeshChatMessage = MeshChatMessage(
    messageId = messageId,
    conversationId = conversationId,
    senderPeerId = senderPeerId,
    recipientPeerId = recipientPeerId,
    body = body,
    messageType = MeshMessageType.valueOf(messageType.name),
    threadRootMessageId = threadRootMessageId,
    parentMessageId = parentMessageId,
    replyToMessageId = replyToMessageId,
    threadReplyCount = threadReplyCount,
    deliveryStatus = MeshMessageDeliveryStatus.valueOf(deliveryStatus.name),
    createdAt = createdAt,
    deliveredAt = deliveredAt,
    failedAt = failedAt,
)

internal fun GroupChat.toContract(): MeshGroupChat = MeshGroupChat(
    chatId = chatId,
    title = title,
    description = description,
    createdByPeerId = createdByPeerId,
    members = members.map(ChatMember::toContract),
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastMessageId = lastMessageId,
    pinned = pinned,
    archived = archived,
)

internal fun GroupChatEvent.toContract(): MeshGroupEvent = MeshGroupEvent(
    eventId = eventId,
    chatId = chatId,
    eventType = MeshGroupEventType.valueOf(eventType.name),
    actorPeerId = actorPeerId,
    subjectPeerId = subjectPeerId,
    text = text,
    attributes = attributes,
    createdAt = createdAt,
)

internal fun ChatSummary.toContract(): MeshChatSummary = MeshChatSummary(
    chatId = chatId,
    chatType = MeshChatType.valueOf(chatType.name),
    title = title,
    lastMessageId = lastMessageId,
    lastMessagePreview = lastMessagePreview,
    unreadCount = unreadCount,
    participantCount = participantCount,
    updatedAt = updatedAt,
)

internal fun ChatThread.toContract(): MeshThread = MeshThread(
    threadId = threadId,
    chatId = chatId,
    rootMessageId = rootMessageId,
    rootSenderPeerId = rootSenderPeerId,
    createdByPeerId = createdByPeerId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    replyCount = replyCount,
    lastReplyMessageId = lastReplyMessageId,
    participantPeerIds = participantPeerIds,
)

internal fun ThreadMessage.toContract(): MeshThreadMessage = MeshThreadMessage(
    threadId = threadId,
    chatId = chatId,
    rootMessageId = rootMessageId,
    messageId = messageId,
    senderPeerId = senderPeerId,
    body = body,
    parentMessageId = parentMessageId,
    replyToMessageId = replyToMessageId,
    deliveryStatus = MeshMessageDeliveryStatus.valueOf(deliveryStatus.name),
    createdAt = createdAt,
    deliveredAt = deliveredAt,
    failedAt = failedAt,
)

internal fun ThreadSummary.toContract(): MeshThreadSummary = MeshThreadSummary(
    threadId = threadId,
    chatId = chatId,
    rootMessageId = rootMessageId,
    replyCount = replyCount,
    lastReplyAt = lastReplyAt,
    participantPeerIds = participantPeerIds,
)

internal fun MessageReceipt.toContract(): MeshMessageReceipt = MeshMessageReceipt(
    messageId = messageId,
    packetId = packetId,
    conversationId = conversationId,
    receivedAt = receivedAt,
    deliveryStatus = MeshMessageDeliveryStatus.valueOf(deliveryStatus.name),
)

internal fun FileDescriptor.toContract(): MeshFileDescriptor = MeshFileDescriptor(
    fileId = fileId,
    fileName = fileName,
    sizeBytes = sizeBytes,
    sha256 = sha256,
    contentType = contentType,
)

internal fun MeshFileDescriptor.toDomain(): FileDescriptor = FileDescriptor(
    fileId = fileId,
    fileName = fileName,
    sizeBytes = sizeBytes,
    sha256 = sha256,
    contentType = contentType,
)

internal fun FileTransferSession.toContract(): MeshFileTransferSession = MeshFileTransferSession(
    transferId = transferId,
    conversationId = conversationId,
    descriptor = descriptor.toContract(),
    senderPeerId = senderPeerId,
    recipientPeerId = recipientPeerId,
    direction = MeshTransferDirection.valueOf(direction.name),
    status = MeshFileTransferStatus.valueOf(status.name),
    chunkSizeBytes = chunkSizeBytes,
    totalChunks = totalChunks,
    acknowledgedChunks = acknowledgedChunks,
    receivedChunks = receivedChunks,
    localPath = localPath,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun MediaQualitySnapshot.toContract(): MeshMediaQualitySnapshot = MeshMediaQualitySnapshot(
    rttMs = rttMs,
    packetLossPercent = packetLossPercent,
    jitterMs = jitterMs,
    bitrateKbps = bitrateKbps,
    capturedAt = capturedAt,
)

internal fun CallParticipant.toContract(): MeshCallParticipant = MeshCallParticipant(
    peerId = peerId,
    displayName = displayName,
    state = MeshCallParticipantState.valueOf(state.name),
    muted = muted,
    videoEnabled = videoEnabled,
    joinedAt = joinedAt,
    updatedAt = updatedAt,
)

private fun CallInvitation.toContract(): MeshCallInvitation = MeshCallInvitation(
    callId = callId,
    roomId = roomId,
    conversationId = conversationId,
    initiatorPeerId = initiatorPeerId,
    targetPeerIds = targetPeerIds,
    callType = MeshCallType.valueOf(callType.name),
    callScope = MeshCallScope.valueOf(callScope.name),
    offer = offer,
    createdAt = createdAt,
)

internal fun CallRoom.toContract(): MeshCallRoom = MeshCallRoom(
    roomId = roomId,
    conversationId = conversationId,
    scope = MeshCallScope.valueOf(scope.name),
    title = title,
    createdByPeerId = createdByPeerId,
    participantPeerIds = participantPeerIds,
    activeCallId = activeCallId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun GroupCallRoom.toContract(): MeshGroupCallRoom = MeshGroupCallRoom(
    roomId = roomId,
    conversationId = conversationId,
    title = title,
    ownerPeerId = ownerPeerId,
    participantPeerIds = participantPeerIds,
    activeCallId = activeCallId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun CallEvent.toContract(): MeshCallEvent = MeshCallEvent(
    eventId = eventId,
    callId = callId,
    roomId = roomId,
    eventType = MeshCallEventType.valueOf(eventType.name),
    actorPeerId = actorPeerId,
    subjectPeerId = subjectPeerId,
    state = state?.let { MeshCallState.valueOf(it.name) },
    participantState = participantState?.let { MeshCallParticipantState.valueOf(it.name) },
    note = note,
    payload = payload,
    createdAt = createdAt,
)

internal fun CallSession.toContract(): MeshCallSession = MeshCallSession(
    callId = callId,
    roomId = roomId,
    conversationId = conversationId,
    initiatorPeerId = initiatorPeerId,
    recipientPeerId = recipientPeerId,
    callType = MeshCallType.valueOf(callType.name),
    callScope = MeshCallScope.valueOf(callScope.name),
    targetPeerIds = targetPeerIds,
    status = MeshCallState.valueOf(status.name),
    participants = participants.map(CallParticipant::toContract),
    invitation = invitation?.toContract(),
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastSignalAt = lastSignalAt,
    qualitySnapshot = qualitySnapshot?.toContract(),
    reconnectAttempts = reconnectAttempts,
    metadata = metadata,
)

internal fun CallSignal.toContract(): MeshCallSignal = MeshCallSignal(
    callId = callId,
    roomId = roomId,
    signalType = MeshCallSignalType.valueOf(signalType.name),
    senderPeerId = senderPeerId,
    recipientPeerId = recipientPeerId,
    callType = callType?.let { MeshCallType.valueOf(it.name) },
    callScope = callScope?.let { MeshCallScope.valueOf(it.name) },
    participantState = participantState?.let { MeshCallParticipantState.valueOf(it.name) },
    muted = muted,
    videoEnabled = videoEnabled,
    correlationId = correlationId,
    payload = payload,
    createdAt = createdAt,
)

private fun PeerMediaState.toContract(): MeshPeerMediaState = MeshPeerMediaState(
    peerId = peerId,
    audioEnabled = audioEnabled,
    videoEnabled = videoEnabled,
    hasAudioTrack = hasAudioTrack,
    hasVideoTrack = hasVideoTrack,
    connectionState = MeshMediaConnectionState.valueOf(connectionState.name),
)

internal fun CallMediaState.toContract(): MeshCallMediaState = MeshCallMediaState(
    callId = callId,
    localPeerId = localPeerId,
    localAudioEnabled = localAudioEnabled,
    localVideoEnabled = localVideoEnabled,
    cameraFacing = MeshCameraFacing.valueOf(cameraFacing.name),
    connectionState = MeshMediaConnectionState.valueOf(connectionState.name),
    peers = peers.map(PeerMediaState::toContract),
    updatedAt = updatedAt,
    errorMessage = errorMessage,
)

internal fun CallMediaStats.toContract(): MeshMediaStats = MeshMediaStats(
    callId = callId,
    rttMs = rttMs,
    packetLossPercent = packetLossPercent,
    jitterMs = jitterMs,
    outboundBitrateKbps = outboundBitrateKbps,
    inboundBitrateKbps = inboundBitrateKbps,
    capturedAt = capturedAt,
)

internal fun SessionDescription.toContract(): MeshSessionDescription = MeshSessionDescription(
    type = MeshSdpType.valueOf(type.name),
    sdp = sdp,
)

internal fun IceCandidate.toContract(): MeshIceCandidate = MeshIceCandidate(
    sdpMid = sdpMid,
    sdpMLineIndex = sdpMLineIndex,
    candidate = candidate,
)

internal fun WebRtcSignalEvent.toContract(): MeshWebRtcSignalEvent = MeshWebRtcSignalEvent(
    callId = callId,
    signalType = MeshCallSignalType.valueOf(signalType.name),
    description = description?.toContract(),
    iceCandidate = iceCandidate?.toContract(),
    createdAt = createdAt,
)

internal fun MeshSessionDescription.toDomain(): SessionDescription = SessionDescription(
    type = SdpType.valueOf(type.name),
    sdp = sdp,
)

internal fun MeshIceCandidate.toDomain(): IceCandidate = IceCandidate(
    sdpMid = sdpMid,
    sdpMLineIndex = sdpMLineIndex,
    candidate = candidate,
)

internal fun MeshWebRtcSignalEvent.toDomain(): WebRtcSignalEvent = WebRtcSignalEvent(
    callId = callId,
    signalType = signalType.toDomain(),
    description = description?.toDomain(),
    iceCandidate = iceCandidate?.toDomain(),
    createdAt = createdAt,
)

private fun MeshPeerMediaState.toDomain(): PeerMediaState = PeerMediaState(
    peerId = peerId,
    audioEnabled = audioEnabled,
    videoEnabled = videoEnabled,
    hasAudioTrack = hasAudioTrack,
    hasVideoTrack = hasVideoTrack,
    connectionState = connectionState.toDomain(),
)

internal fun MeshCallMediaState.toDomain(): CallMediaState = CallMediaState(
    callId = callId,
    localPeerId = localPeerId,
    localAudioEnabled = localAudioEnabled,
    localVideoEnabled = localVideoEnabled,
    cameraFacing = cameraFacing.toDomain(),
    connectionState = connectionState.toDomain(),
    peers = peers.map(MeshPeerMediaState::toDomain),
    updatedAt = updatedAt,
    errorMessage = errorMessage,
)

internal fun MeshMediaStats.toDomain(): CallMediaStats = CallMediaStats(
    callId = callId,
    rttMs = rttMs,
    packetLossPercent = packetLossPercent,
    jitterMs = jitterMs,
    outboundBitrateKbps = outboundBitrateKbps,
    inboundBitrateKbps = inboundBitrateKbps,
    capturedAt = capturedAt,
)

internal fun MeshCallSignalType.toDomain(): CallSignalType = CallSignalType.valueOf(name)
internal fun MeshCallType.toDomain(): CallType = CallType.valueOf(name)
internal fun MeshCallScope.toDomain(): CallScope = CallScope.valueOf(name)
internal fun MeshCallState.toDomain(): CallState = CallState.valueOf(name)
internal fun MeshCallParticipantState.toDomain(): CallParticipantState = CallParticipantState.valueOf(name)
internal fun MeshMediaConnectionState.toDomain(): MediaConnectionState = MediaConnectionState.valueOf(name)
internal fun MeshCameraFacing.toDomain(): CameraFacing = CameraFacing.valueOf(name)
internal fun MeshSdpType.toDomain(): SdpType = SdpType.valueOf(name)
internal fun CallType.toContract(): MeshCallType = MeshCallType.valueOf(name)
internal fun CallScope.toContract(): MeshCallScope = MeshCallScope.valueOf(name)

internal fun EventLogEntry.toContract(): MeshEventLogEntry = MeshEventLogEntry(
    eventId = eventId,
    category = MeshEventCategory.valueOf(category.name),
    level = MeshEventLevel.valueOf(level.name),
    message = message,
    peerId = peerId,
    packetId = packetId,
    attributes = attributes,
    createdAt = createdAt,
)

internal fun MetricSnapshot.toContract(): MeshMetricSnapshot = MeshMetricSnapshot(
    nodePeerId = nodePeerId,
    capturedAt = capturedAt,
    counters = counters,
    gauges = gauges,
)

private fun EndpointSource.toContract(): MeshEndpointSource = MeshEndpointSource.valueOf(name)

internal fun PeerEndpointCandidate.toContract(): MeshNearbyPeer = MeshNearbyPeer(
    peerId = peerId,
    endpoint = endpoint.toContract(),
    source = source.toContract(),
    discoveredAt = discoveredAt,
    qualityScore = qualityScore,
    capabilities = capabilities,
)

internal fun RouteMode.toContract(): MeshRouteMode = MeshRouteMode.valueOf(name)

internal fun RouteEntry.toContract(): MeshRouteInfo = MeshRouteInfo(
    targetPeerId = targetPeerId,
    nextHopPeerId = nextHopPeerId,
    endpoint = endpoint?.toContract(),
    routeMode = routeMode.toContract(),
    hopCount = hopCount,
    expiresAt = expiresAt,
    learnedAt = learnedAt,
    direct = direct,
)

internal fun RouteHop.toContract(): MeshRouteHop = MeshRouteHop(
    peerId = peerId,
    endpoint = endpoint.toContract(),
    routeMode = routeMode.toContract(),
)

internal fun RoutingPlan.toContract(targetPeerId: String): MeshRoutingPlan = MeshRoutingPlan(
    targetPeerId = targetPeerId,
    routeMode = routeMode.toContract(),
    hops = hops.map(RouteHop::toContract),
    useRelayGateway = useRelayGateway,
)

internal fun ConnectivityStrategy.toContract(): MeshConnectivityStrategy = MeshConnectivityStrategy(
    targetPeerId = targetPeerId,
    routeMode = routeMode.toContract(),
    connectivityMode = connectivityMode.toContract(),
    relayMode = relayMode.toContract(),
    useRelayGateway = useRelayGateway,
    reason = reason,
    decidedAt = decidedAt,
)

internal fun HostCandidate.toContract(): MeshHostCandidate = MeshHostCandidate(
    peerId = peerId,
    endpoint = endpoint?.toContract(),
    score = score,
    reachable = reachable,
    roleHint = roleHint.toContract(),
    lastSeenAt = lastSeenAt,
)

internal fun RouteHealth.toContract(): MeshRouteHealth = MeshRouteHealth(
    targetPeerId = targetPeerId,
    nextHopPeerId = nextHopPeerId,
    routeMode = routeMode.toContract(),
    state = state.toContract(),
    failureCount = failureCount,
    lastUpdatedAt = lastUpdatedAt,
    expiresAt = expiresAt,
    detail = detail,
)

internal fun TopologyEvent.toContract(): MeshTopologyEvent = MeshTopologyEvent(
    eventId = eventId,
    eventType = eventType.toContract(),
    message = message,
    peerId = peerId,
    targetPeerId = targetPeerId,
    detail = detail,
    occurredAt = occurredAt,
)

internal fun NetworkRoleState.toContract(): MeshNetworkRoleState = MeshNetworkRoleState(
    localPeerId = localPeerId,
    localRole = localRole.toContract(),
    currentHostPeerId = currentHostPeerId,
    hostReachable = hostReachable,
    failoverInProgress = failoverInProgress,
    updatedAt = updatedAt,
)

internal fun NetworkTopologyState.toContract(): MeshTopologyState = MeshTopologyState(
    localPeerId = localPeerId,
    connectivityMode = connectivityMode.toContract(),
    relayMode = relayMode.toContract(),
    networkRoleState = networkRoleState.toContract(),
    hostCandidates = hostCandidates.map(HostCandidate::toContract),
    routeHealth = routeHealth.map(RouteHealth::toContract),
    activeStrategies = activeStrategies.map(ConnectivityStrategy::toContract),
    pendingAckCount = pendingAckCount,
    queuedPacketCount = queuedPacketCount,
    activeFileTransfers = activeFileTransfers,
    activeCallSessions = activeCallSessions,
    continuityDegraded = continuityDegraded,
    recentEvents = recentEvents.map(TopologyEvent::toContract),
    refreshedAt = refreshedAt,
)

internal fun RelayMode.toContract(): MeshRelayMode = MeshRelayMode.valueOf(name)
internal fun ConnectivityMode.toContract(): MeshConnectivityMode = MeshConnectivityMode.valueOf(name)
internal fun HostRole.toContract(): MeshHostRole = MeshHostRole.valueOf(name)
internal fun RouteHealthState.toContract(): MeshRouteHealthState = MeshRouteHealthState.valueOf(name)
internal fun TopologyEventType.toContract(): MeshTopologyEventType = MeshTopologyEventType.valueOf(name)

internal fun Organization.toContract(): MeshOrganization = MeshOrganization(
    organizationId = organizationId,
    name = name,
    description = description,
    createdByPeerId = createdByPeerId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun OrganizationMember.toContract(): MeshOrganizationMember = MeshOrganizationMember(
    organizationId = organizationId,
    peerId = peerId,
    displayName = displayName,
    roleIds = roleIds,
    departmentId = departmentId,
    locationIds = locationIds,
    position = position,
    isCommissionMember = isCommissionMember,
    status = MeshOrganizationMemberStatus.valueOf(status.name),
    joinedAt = joinedAt,
    updatedAt = updatedAt,
)

internal fun Role.toContract(): MeshRole = MeshRole(
    roleId = roleId,
    organizationId = organizationId,
    name = name,
    description = description,
    permissions = permissions.map(InventoryPermission::toContract).toSet(),
    system = system,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun InventoryCategory.toContract(): MeshInventoryCategory = MeshInventoryCategory(
    categoryId = categoryId,
    organizationId = organizationId,
    name = name,
    description = description,
    parentCategoryId = parentCategoryId,
    templateId = templateId,
    createdByPeerId = createdByPeerId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun InventoryLocation.toContract(): MeshInventoryLocation = MeshInventoryLocation(
    locationId = locationId,
    organizationId = organizationId,
    name = name,
    description = description,
    parentLocationId = parentLocationId,
    locationType = locationType.toContract(),
    code = code,
    path = path,
    departmentId = departmentId,
    archived = archived,
    createdByPeerId = createdByPeerId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun InventoryItem.toContract(): MeshInventoryItem = MeshInventoryItem(
    inventoryItemId = inventoryItemId,
    organizationId = organizationId,
    inventoryNumber = inventoryNumber,
    localNumber = localNumber,
    qrCode = qrCode,
    barcode = barcode,
    categoryId = categoryId,
    subcategoryId = subcategoryId,
    itemType = itemType.toContract(),
    title = title,
    description = description,
    brand = brand,
    model = model,
    serialNumber = serialNumber,
    manufacturer = manufacturer,
    purchaseDate = purchaseDate,
    commissioningDate = commissioningDate,
    warrantyUntil = warrantyUntil,
    depreciationGroup = depreciationGroup,
    usefulLifeMonths = usefulLifeMonths,
    condition = condition.toContract(),
    locationId = locationId,
    responsiblePerson = responsiblePerson,
    responsibleDepartment = responsibleDepartment,
    responsibleUserId = responsibleUserId,
    responsibleOwnerIds = responsibleOwnerIds,
    ownerOrganizationId = ownerOrganizationId,
    ownerId = ownerId,
    departmentId = departmentId,
    costCenterId = costCenterId,
    legalHolderId = legalHolderId,
    supplierId = supplierId,
    fundingSourceId = fundingSourceId,
    createdByPeerId = createdByPeerId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastInventoryAt = lastInventoryAt,
    nextInventoryAt = nextInventoryAt,
    photoAttachmentIds = photoAttachmentIds,
    attachmentIds = attachmentIds,
    commentIds = commentIds,
    incidentIds = incidentIds,
    tagIds = tagIds,
    attributes = attributes.map { it.toContract() },
    currentStatus = currentStatus.toContract(),
    syncStatus = syncStatus.toContract(),
    revision = revision,
    sessionIds = sessionIds,
    chatId = chatId,
    threadRootMessageId = threadRootMessageId,
    metadata = metadata,
)

internal fun InventorySession.toContract(): MeshInventorySession = MeshInventorySession(
    sessionId = sessionId,
    organizationId = organizationId,
    title = title,
    description = description,
    periodStart = periodStart,
    periodEnd = periodEnd,
    status = status.toContract(),
    reviewStatus = reviewStatus.toContract(),
    workflowStatus = workflowStatus.toContract(),
    result = result?.toContract(),
    departmentIds = departmentIds,
    locationIds = locationIds,
    ownerIds = ownerIds,
    requiresPhotoForDiscrepancy = requiresPhotoForDiscrepancy,
    completionBlockedReason = completionBlockedReason,
    createdByPeerId = createdByPeerId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    itemIds = itemIds,
    memberPeerIds = memberPeerIds,
    exportTaskIds = exportTaskIds,
    chatId = chatId,
    threadRootMessageId = threadRootMessageId,
    revision = revision,
    metadata = metadata,
)

internal fun InventorySessionMember.toContract(): MeshInventorySessionMember = MeshInventorySessionMember(
    sessionId = sessionId,
    organizationId = organizationId,
    peerId = peerId,
    role = role.toContract(),
    assignedByPeerId = assignedByPeerId,
    assignedAt = assignedAt,
    updatedAt = updatedAt,
)

internal fun InventoryReview.toContract(): MeshInventoryReview = MeshInventoryReview(
    reviewId = reviewId,
    organizationId = organizationId,
    inventoryItemId = inventoryItemId,
    sessionId = sessionId,
    reviewerPeerId = reviewerPeerId,
    status = status.toContract(),
    presenceStatus = presenceStatus.toContract(),
    acceptanceStatus = acceptanceStatus.toContract(),
    confirmationStatus = confirmationStatus.toContract(),
    requiresPhoto = requiresPhoto,
    comment = comment,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun InventoryComment.toContract(): MeshInventoryComment = MeshInventoryComment(
    commentId = commentId,
    organizationId = organizationId,
    inventoryItemId = inventoryItemId,
    sessionId = sessionId,
    authorPeerId = authorPeerId,
    body = body,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun InventoryAttachment.toContract(): MeshInventoryAttachment = MeshInventoryAttachment(
    attachmentId = attachmentId,
    organizationId = organizationId,
    inventoryItemId = inventoryItemId,
    sessionId = sessionId,
    uploadedByPeerId = uploadedByPeerId,
    descriptor = descriptor.toContract(),
    transferId = transferId,
    attachmentType = attachmentType.toContract(),
    status = status.toContract(),
    checksum = checksum,
    preview = preview?.toContract(),
    createdAt = createdAt,
    updatedAt = updatedAt,
    note = note,
)

internal fun InventoryExportTask.toContract(): MeshInventoryExportTask = MeshInventoryExportTask(
    exportTaskId = exportTaskId,
    organizationId = organizationId,
    sessionId = sessionId,
    requestedByPeerId = requestedByPeerId,
    format = format.toContract(),
    status = status.toContract(),
    createdAt = createdAt,
    updatedAt = updatedAt,
    resultAttachmentId = resultAttachmentId,
    resultDescriptor = resultDescriptor?.toContract(),
    errorMessage = errorMessage,
)

internal fun InventoryQrCode.toContract(): MeshInventoryQrCode = MeshInventoryQrCode(
    codeId = codeId,
    organizationId = organizationId,
    inventoryItemId = inventoryItemId,
    qrCode = qrCode,
    barcode = barcode,
    createdByPeerId = createdByPeerId,
    createdAt = createdAt,
)

internal fun InventorySubcategory.toContract(): MeshInventorySubcategory = MeshInventorySubcategory(
    subcategoryId = subcategoryId,
    categoryId = categoryId,
    organizationId = organizationId,
    name = name,
    description = description,
    createdByPeerId = createdByPeerId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun InventoryTag.toContract(): MeshInventoryTag = MeshInventoryTag(
    tagId = tagId,
    organizationId = organizationId,
    name = name,
    color = color,
    createdByPeerId = createdByPeerId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun InventoryAttributeDefinition.toContract(): MeshInventoryAttributeDefinition = MeshInventoryAttributeDefinition(
    attributeId = attributeId,
    organizationId = organizationId,
    key = key,
    label = label,
    description = description,
    type = type.toContract(),
    required = required,
    unit = unit,
    options = options,
    validationRules = validationRules.map { it.toContract() },
    createdByPeerId = createdByPeerId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun InventoryAttributeValue.toContract(): MeshInventoryAttributeValue = MeshInventoryAttributeValue(
    attributeId = attributeId,
    value = value,
    updatedByPeerId = updatedByPeerId,
    updatedAt = updatedAt,
)

internal fun InventoryCategoryTemplate.toContract(): MeshInventoryCategoryTemplate = MeshInventoryCategoryTemplate(
    templateId = templateId,
    organizationId = organizationId,
    categoryId = categoryId,
    name = name,
    description = description,
    fields = fields.map { it.toContract() },
    requiredFields = requiredFields.map { it.toContract() },
    createdByPeerId = createdByPeerId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun InventoryFieldTemplate.toContract(): MeshInventoryFieldTemplate = MeshInventoryFieldTemplate(
    fieldId = fieldId,
    key = key,
    label = label,
    type = type.toContract(),
    required = required,
    validationRules = validationRules.map { it.toContract() },
    helpText = helpText,
    order = order,
)

internal fun InventoryValidationRule.toContract(): MeshInventoryValidationRule = MeshInventoryValidationRule(
    type = type.toContract(),
    params = params,
    message = message,
)

internal fun InventoryRequiredFieldRule.toContract(): MeshInventoryRequiredFieldRule = MeshInventoryRequiredFieldRule(
    fieldId = fieldId,
    message = message,
)

internal fun InventoryOwner.toContract(): MeshInventoryOwner = MeshInventoryOwner(
    ownerId = ownerId,
    organizationId = organizationId,
    name = name,
    type = type.toContract(),
    legalHolderId = legalHolderId,
    contactInfo = contactInfo,
    code = code,
    departmentId = departmentId,
    locationIds = locationIds,
    archived = archived,
    createdByPeerId = createdByPeerId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun InventoryDepartment.toContract(): MeshInventoryDepartment = MeshInventoryDepartment(
    departmentId = departmentId,
    organizationId = organizationId,
    name = name,
    parentDepartmentId = parentDepartmentId,
    code = code,
    locationIds = locationIds,
    ownerIds = ownerIds,
    archived = archived,
    createdByPeerId = createdByPeerId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun InventoryCostCenter.toContract(): MeshInventoryCostCenter = MeshInventoryCostCenter(
    costCenterId = costCenterId,
    organizationId = organizationId,
    code = code,
    name = name,
    description = description,
    createdByPeerId = createdByPeerId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun InventoryLegalHolder.toContract(): MeshInventoryLegalHolder = MeshInventoryLegalHolder(
    legalHolderId = legalHolderId,
    organizationId = organizationId,
    name = name,
    taxId = taxId,
    registrationNumber = registrationNumber,
    address = address,
    bankDetails = bankDetails,
    createdByPeerId = createdByPeerId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun InventorySupplier.toContract(): MeshInventorySupplier = MeshInventorySupplier(
    supplierId = supplierId,
    organizationId = organizationId,
    name = name,
    contactInfo = contactInfo,
    bankDetails = bankDetails,
    createdByPeerId = createdByPeerId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun InventoryFundingSource.toContract(): MeshInventoryFundingSource = MeshInventoryFundingSource(
    fundingSourceId = fundingSourceId,
    organizationId = organizationId,
    name = name,
    description = description,
    createdByPeerId = createdByPeerId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun InventoryIncident.toContract(): MeshInventoryIncident = MeshInventoryIncident(
    incidentId = incidentId,
    organizationId = organizationId,
    inventoryItemId = inventoryItemId,
    sessionId = sessionId,
    locationId = locationId,
    type = type.toContract(),
    severity = severity.toContract(),
    status = status.toContract(),
    title = title,
    description = description,
    assigneePeerIds = assigneePeerIds,
    reportedByPeerId = reportedByPeerId,
    reportedAt = reportedAt,
    updatedAt = updatedAt,
    reviewedByPeerId = reviewedByPeerId,
    reviewedAt = reviewedAt,
    reviewComment = reviewComment,
    resolvedByPeerId = resolvedByPeerId,
    resolvedAt = resolvedAt,
    attachmentIds = attachmentIds,
    commentIds = commentIds,
    metadata = metadata,
)

internal fun InventoryAlertEvent.toContract(): MeshInventoryAlertEvent = MeshInventoryAlertEvent(
    alertId = alertId,
    organizationId = organizationId,
    ruleId = ruleId,
    inventoryItemId = inventoryItemId,
    sessionId = sessionId,
    severity = severity.toContract(),
    message = message,
    createdAt = createdAt,
    acknowledgedByPeerId = acknowledgedByPeerId,
    acknowledgedAt = acknowledgedAt,
)

internal fun InventoryRuleThreshold.toContract(): MeshInventoryRuleThreshold = MeshInventoryRuleThreshold(
    ruleId = ruleId,
    organizationId = organizationId,
    ruleType = ruleType.toContract(),
    thresholdValue = thresholdValue,
    active = active,
    createdByPeerId = createdByPeerId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    metadata = metadata,
)

internal fun InventoryDeadlineRule.toContract(): MeshInventoryDeadlineRule = MeshInventoryDeadlineRule(
    ruleId = ruleId,
    organizationId = organizationId,
    target = target.toContract(),
    daysBefore = daysBefore,
    active = active,
    createdByPeerId = createdByPeerId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun InventoryReminder.toContract(): MeshInventoryReminder = MeshInventoryReminder(
    reminderId = reminderId,
    organizationId = organizationId,
    ruleId = ruleId,
    inventoryItemId = inventoryItemId,
    sessionId = sessionId,
    dueAt = dueAt,
    status = status.toContract(),
    message = message,
    recipientPeerIds = recipientPeerIds,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun InventoryChangeLog.toContract(): MeshInventoryChangeLog = MeshInventoryChangeLog(
    changeId = changeId,
    organizationId = organizationId,
    entityType = MeshInventoryEntityType.valueOf(entityType.name),
    entityId = entityId,
    changedByPeerId = changedByPeerId,
    changedAt = changedAt,
    changes = changes.map { it.toContract() },
    reason = reason,
    sessionId = sessionId,
    sourcePeerId = sourcePeerId,
    metadata = metadata,
)

internal fun InventoryFieldChange.toContract(): MeshInventoryFieldChange = MeshInventoryFieldChange(
    field = field,
    previousValue = previousValue,
    newValue = newValue,
)

internal fun InventoryDashboardSnapshot.toContract(): MeshInventoryDashboardSnapshot = MeshInventoryDashboardSnapshot(
    snapshotId = snapshotId,
    organizationId = organizationId,
    generatedAt = generatedAt,
    totalItems = totalItems,
    byStatus = byStatus.mapKeys { it.key.toContract() },
    byCondition = byCondition.mapKeys { it.key.toContract() },
    byCategoryId = byCategoryId,
    byLocationId = byLocationId,
    incidentsOpen = incidentsOpen,
    sessionsActive = sessionsActive,
    metadata = metadata,
)

internal fun InventorySearchResult.toContract(): MeshInventorySearchResult = MeshInventorySearchResult(
    organizationId = organizationId,
    total = total,
    items = items.map { it.toContract() },
)

internal fun InventorySessionResult.toContract(): MeshInventorySessionResult = MeshInventorySessionResult(
    summary = summary,
    confirmedCount = confirmedCount,
    rejectedCount = rejectedCount,
    requiresUpdateCount = requiresUpdateCount,
    incidentCount = incidentCount,
    completedAt = completedAt,
    approvedByPeerId = approvedByPeerId,
)

internal fun InventoryCodeBinding.toContract(): MeshInventoryCodeBinding = MeshInventoryCodeBinding(
    codeId = codeId,
    organizationId = organizationId,
    inventoryItemId = inventoryItemId,
    sessionId = sessionId,
    codeType = codeType.toContract(),
    codeValue = codeValue,
    createdByPeerId = createdByPeerId,
    createdAt = createdAt,
)

internal fun InventoryCode.toContract(): MeshInventoryCode = MeshInventoryCode(
    inventoryCodeId = inventoryCodeId,
    inventoryItemId = inventoryItemId,
    organizationId = organizationId,
    codeType = codeType.toContract(),
    barcodeFormat = barcodeFormat?.toContract(),
    rawValue = rawValue,
    displayValue = displayValue,
    isActive = isActive,
    createdAt = createdAt,
    createdByPeerId = createdByPeerId,
    version = version,
)

internal fun InventoryBarcodeFormat.toContract(): MeshInventoryBarcodeFormat = MeshInventoryBarcodeFormat.valueOf(name)

internal fun InventoryLabelTemplate.toContract(): MeshInventoryLabelTemplate = MeshInventoryLabelTemplate(
    templateId = templateId,
    organizationId = organizationId,
    name = name,
    description = description,
    templateType = templateType.toContract(),
    fields = fields.map { it.toContract() },
    includeBarcode = includeBarcode,
    includeQr = includeQr,
    isDefault = isDefault,
    createdByPeerId = createdByPeerId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun InventoryLabelTemplateType.toContract(): MeshInventoryLabelTemplateType =
    MeshInventoryLabelTemplateType.valueOf(name)

internal fun InventoryLabelFieldKey.toContract(): MeshInventoryLabelFieldKey =
    MeshInventoryLabelFieldKey.valueOf(name)

internal fun InventoryLabelField.toContract(): MeshInventoryLabelField =
    MeshInventoryLabelField(key = key.toContract(), label = label, value = value)

internal fun InventoryLabel.toContract(): MeshInventoryLabel = MeshInventoryLabel(
    labelId = labelId,
    organizationId = organizationId,
    inventoryItemId = inventoryItemId,
    templateId = templateId,
    templateName = templateName,
    fields = fields.map { it.toContract() },
    barcodeValue = barcodeValue,
    barcodeFormat = barcodeFormat?.toContract(),
    qrValue = qrValue,
    createdAt = createdAt,
    createdByPeerId = createdByPeerId,
    version = version,
)

internal fun InventoryPrintTask.toContract(): MeshInventoryPrintTask = MeshInventoryPrintTask(
    printTaskId = printTaskId,
    organizationId = organizationId,
    templateId = templateId,
    labelIds = labelIds,
    itemIds = itemIds,
    status = status.toContract(),
    createdByPeerId = createdByPeerId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    resultDescriptor = resultDescriptor?.toContract(),
    localPath = localPath,
    errorMessage = errorMessage,
)

internal fun InventoryPrintStatus.toContract(): MeshInventoryPrintStatus = MeshInventoryPrintStatus.valueOf(name)

internal fun InventoryScanEvent.toContract(): MeshInventoryScanEvent = MeshInventoryScanEvent(
    scanEventId = scanEventId,
    organizationId = organizationId,
    codeType = codeType.toContract(),
    codeValue = codeValue,
    rawValue = rawValue,
    inventoryItemId = inventoryItemId,
    sessionId = sessionId,
    locationId = locationId,
    locationHint = locationHint,
    scannedByPeerId = scannedByPeerId,
    scannedAt = scannedAt,
    resultStatus = resultStatus.toContract(),
    deviceId = deviceId,
    note = note,
)

internal fun InventoryRevision.toContract(): MeshInventoryRevision = MeshInventoryRevision(
    organizationId = organizationId,
    entityType = MeshInventoryEntityType.valueOf(entityType.name),
    entityId = entityId,
    revision = revision,
    updatedAt = updatedAt,
)

internal fun InventoryConflict.toContract(): MeshInventoryConflict = MeshInventoryConflict(
    conflictId = conflictId,
    organizationId = organizationId,
    entityType = MeshInventoryEntityType.valueOf(entityType.name),
    entityId = entityId,
    localRevision = localRevision,
    incomingRevision = incomingRevision,
    detectedAt = detectedAt,
    status = status.toContract(),
    note = note,
)

internal fun InventoryMergeResult.toContract(): MeshInventoryMergeResult = MeshInventoryMergeResult(
    organizationId = organizationId,
    entityType = MeshInventoryEntityType.valueOf(entityType.name),
    entityId = entityId,
    resolved = resolved,
    resolvedAt = resolvedAt,
    note = note,
)

internal fun InventorySearchQuery.toContract(): MeshInventorySearchQuery = MeshInventorySearchQuery(
    organizationId = organizationId,
    text = text,
    inventoryNumber = inventoryNumber,
    localNumber = localNumber,
    serialNumber = serialNumber,
    qrCode = qrCode,
    barcode = barcode,
    filter = filter.toContract(),
    sort = sort.toContract(),
    limit = limit,
    offset = offset,
)

internal fun InventoryFilterSet.toContract(): MeshInventoryFilterSet = MeshInventoryFilterSet(
    categoryIds = categoryIds,
    subcategoryIds = subcategoryIds,
    locationIds = locationIds,
    departmentIds = departmentIds,
    costCenterIds = costCenterIds,
    ownerIds = ownerIds,
    tagIds = tagIds,
    status = status.map { it.toContract() }.toSet(),
    condition = condition.map { it.toContract() }.toSet(),
    incidentOnly = incidentOnly,
    sessionId = sessionId,
    updatedSince = updatedSince,
    nextInventoryBefore = nextInventoryBefore,
)

internal fun InventorySortMode.toContract(): MeshInventorySortMode = MeshInventorySortMode.valueOf(name)

internal fun InventoryEvent.toContract(): MeshInventoryEvent = MeshInventoryEvent(
    eventId = eventId,
    organizationId = organizationId,
    entityType = MeshInventoryEntityType.valueOf(entityType.name),
    entityId = entityId,
    eventType = MeshInventoryEventType.valueOf(eventType.name),
    actorPeerId = actorPeerId,
    occurredAt = occurredAt,
    sequence = sequence,
    entityRevision = entityRevision,
    previousEntityRevision = previousEntityRevision,
    sessionId = sessionId,
    payload = payload.toContract(),
    conflict = conflict,
    notes = notes,
)

internal fun InventoryEventPayload.toContract(): MeshInventoryEventPayload = when (this) {
    is InventoryOrganizationSnapshot -> MeshInventoryOrganizationSnapshot(organization.toContract())
    is InventoryMemberSnapshot -> MeshInventoryMemberSnapshot(member.toContract())
    is InventoryRoleSnapshot -> MeshInventoryRoleSnapshot(role.toContract())
    is InventoryCategorySnapshot -> MeshInventoryCategorySnapshot(category.toContract())
    is InventorySubcategorySnapshot -> MeshInventorySubcategorySnapshot(subcategory.toContract())
    is InventoryTagSnapshot -> MeshInventoryTagSnapshot(tag.toContract())
    is InventoryAttributeDefinitionSnapshot -> MeshInventoryAttributeDefinitionSnapshot(definition.toContract())
    is InventoryCategoryTemplateSnapshot -> MeshInventoryCategoryTemplateSnapshot(template.toContract())
    is InventoryOwnerSnapshot -> MeshInventoryOwnerSnapshot(owner.toContract())
    is InventoryDepartmentSnapshot -> MeshInventoryDepartmentSnapshot(department.toContract())
    is InventoryCostCenterSnapshot -> MeshInventoryCostCenterSnapshot(costCenter.toContract())
    is InventoryLegalHolderSnapshot -> MeshInventoryLegalHolderSnapshot(legalHolder.toContract())
    is InventorySupplierSnapshot -> MeshInventorySupplierSnapshot(supplier.toContract())
    is InventoryFundingSourceSnapshot -> MeshInventoryFundingSourceSnapshot(fundingSource.toContract())
    is InventoryLocationSnapshot -> MeshInventoryLocationSnapshot(location.toContract())
    is InventoryItemSnapshot -> MeshInventoryItemSnapshot(item.toContract())
    is InventorySessionSnapshot -> MeshInventorySessionSnapshot(session.toContract())
    is InventoryReviewSnapshot -> MeshInventoryReviewSnapshot(review.toContract())
    is InventoryCommentSnapshot -> MeshInventoryCommentSnapshot(comment.toContract())
    is InventoryAttachmentSnapshot -> MeshInventoryAttachmentSnapshot(attachment.toContract())
    is InventoryIncidentSnapshot -> MeshInventoryIncidentSnapshot(incident.toContract())
    is InventoryAlertSnapshot -> MeshInventoryAlertSnapshot(alert.toContract())
    is InventoryReminderSnapshot -> MeshInventoryReminderSnapshot(reminder.toContract())
    is InventoryRuleThresholdSnapshot -> MeshInventoryRuleThresholdSnapshot(rule.toContract())
    is InventoryDeadlineRuleSnapshot -> MeshInventoryDeadlineRuleSnapshot(rule.toContract())
    is InventoryChangeLogSnapshot -> MeshInventoryChangeLogSnapshot(changeLog.toContract())
    is InventoryDashboardSnapshotPayload -> MeshInventoryDashboardSnapshotPayload(snapshot.toContract())
    is InventoryCodeSnapshot -> MeshInventoryCodeSnapshot(code.toContract())
    is InventoryCodeBindingSnapshot -> MeshInventoryCodeBindingSnapshot(binding.toContract())
    is InventoryLabelTemplateSnapshot -> MeshInventoryLabelTemplateSnapshot(template.toContract())
    is InventoryLabelSnapshot -> MeshInventoryLabelSnapshot(label.toContract())
    is InventoryPrintTaskSnapshot -> MeshInventoryPrintTaskSnapshot(task.toContract())
    is InventoryScanEventSnapshot -> MeshInventoryScanEventSnapshot(event.toContract())
    is InventoryConflictSnapshot -> MeshInventoryConflictSnapshot(conflict.toContract())
    is InventoryRevisionSnapshot -> MeshInventoryRevisionSnapshot(revision.toContract())
    is InventoryExportSnapshot -> MeshInventoryExportSnapshot(exportTask.toContract())
    is InventoryQrSnapshot -> MeshInventoryQrSnapshot(qrCode.toContract())
}

internal fun InventoryCondition.toContract(): MeshInventoryCondition = MeshInventoryCondition.valueOf(name)
internal fun InventoryStatus.toContract(): MeshInventoryStatus = MeshInventoryStatus.valueOf(name)
internal fun InventorySessionStatus.toContract(): MeshInventorySessionStatus = MeshInventorySessionStatus.valueOf(name)
internal fun InventorySessionReviewStatus.toContract(): MeshInventorySessionReviewStatus = MeshInventorySessionReviewStatus.valueOf(name)
internal fun InventoryWorkflowStatus.toContract(): MeshInventoryWorkflowStatus = MeshInventoryWorkflowStatus.valueOf(name)
internal fun InventorySessionRole.toContract(): MeshInventorySessionRole = MeshInventorySessionRole.valueOf(name)
internal fun InventoryReviewStatus.toContract(): MeshInventoryReviewStatus = MeshInventoryReviewStatus.valueOf(name)
internal fun InventoryPresenceStatus.toContract(): MeshInventoryPresenceStatus = MeshInventoryPresenceStatus.valueOf(name)
internal fun InventoryAcceptanceStatus.toContract(): MeshInventoryAcceptanceStatus = MeshInventoryAcceptanceStatus.valueOf(name)
internal fun InventoryConfirmationStatus.toContract(): MeshInventoryConfirmationStatus = MeshInventoryConfirmationStatus.valueOf(name)
internal fun InventoryAttachmentType.toContract(): MeshInventoryAttachmentType = MeshInventoryAttachmentType.valueOf(name)
internal fun InventoryExportStatus.toContract(): MeshInventoryExportStatus = MeshInventoryExportStatus.valueOf(name)
internal fun InventoryExportFormat.toContract(): MeshInventoryExportFormat = MeshInventoryExportFormat.valueOf(name)
internal fun InventoryPermission.toContract(): MeshInventoryPermission = MeshInventoryPermission.valueOf(name)
internal fun InventoryItemType.toContract(): MeshInventoryItemType = MeshInventoryItemType.valueOf(name)
internal fun InventoryLocationType.toContract(): MeshInventoryLocationType = MeshInventoryLocationType.valueOf(name)
internal fun InventoryAttachmentStatus.toContract(): MeshInventoryAttachmentStatus = MeshInventoryAttachmentStatus.valueOf(name)
internal fun InventoryAttributeType.toContract(): MeshInventoryAttributeType = MeshInventoryAttributeType.valueOf(name)
internal fun InventoryValidationRuleType.toContract(): MeshInventoryValidationRuleType = MeshInventoryValidationRuleType.valueOf(name)
internal fun InventoryOwnerType.toContract(): MeshInventoryOwnerType = MeshInventoryOwnerType.valueOf(name)
internal fun InventoryIncidentType.toContract(): MeshInventoryIncidentType = MeshInventoryIncidentType.valueOf(name)
internal fun InventoryIncidentSeverity.toContract(): MeshInventoryIncidentSeverity = MeshInventoryIncidentSeverity.valueOf(name)
internal fun InventoryIncidentStatus.toContract(): MeshInventoryIncidentStatus = MeshInventoryIncidentStatus.valueOf(name)
internal fun InventoryRuleType.toContract(): MeshInventoryRuleType = MeshInventoryRuleType.valueOf(name)
internal fun InventoryReminderStatus.toContract(): MeshInventoryReminderStatus = MeshInventoryReminderStatus.valueOf(name)
internal fun InventoryDeadlineTarget.toContract(): MeshInventoryDeadlineTarget = MeshInventoryDeadlineTarget.valueOf(name)
internal fun InventoryCodeType.toContract(): MeshInventoryCodeType = MeshInventoryCodeType.valueOf(name)
internal fun InventoryScanResultStatus.toContract(): MeshInventoryScanResultStatus = MeshInventoryScanResultStatus.valueOf(name)
internal fun InventorySyncStatus.toContract(): MeshInventorySyncStatus = MeshInventorySyncStatus.valueOf(name)
internal fun MeshInventoryPermission.toDomain(): InventoryPermission = InventoryPermission.valueOf(name)
internal fun MeshInventoryCondition.toDomain(): InventoryCondition = InventoryCondition.valueOf(name)
internal fun MeshInventoryStatus.toDomain(): InventoryStatus = InventoryStatus.valueOf(name)
internal fun MeshInventorySessionStatus.toDomain(): InventorySessionStatus = InventorySessionStatus.valueOf(name)
internal fun MeshInventorySessionReviewStatus.toDomain(): InventorySessionReviewStatus = InventorySessionReviewStatus.valueOf(name)
internal fun MeshInventoryWorkflowStatus.toDomain(): InventoryWorkflowStatus = InventoryWorkflowStatus.valueOf(name)
internal fun MeshInventorySessionRole.toDomain(): InventorySessionRole = InventorySessionRole.valueOf(name)
internal fun MeshInventoryReviewStatus.toDomain(): InventoryReviewStatus = InventoryReviewStatus.valueOf(name)
internal fun MeshInventoryPresenceStatus.toDomain(): InventoryPresenceStatus = InventoryPresenceStatus.valueOf(name)
internal fun MeshInventoryAcceptanceStatus.toDomain(): InventoryAcceptanceStatus = InventoryAcceptanceStatus.valueOf(name)
internal fun MeshInventoryConfirmationStatus.toDomain(): InventoryConfirmationStatus = InventoryConfirmationStatus.valueOf(name)
internal fun MeshInventoryAttachmentType.toDomain(): InventoryAttachmentType = InventoryAttachmentType.valueOf(name)
internal fun MeshInventoryExportStatus.toDomain(): InventoryExportStatus = InventoryExportStatus.valueOf(name)
internal fun MeshInventoryExportFormat.toDomain(): InventoryExportFormat = InventoryExportFormat.valueOf(name)
internal fun MeshInventoryItemType.toDomain(): InventoryItemType = InventoryItemType.valueOf(name)
internal fun MeshInventoryLocationType.toDomain(): InventoryLocationType = InventoryLocationType.valueOf(name)
internal fun MeshInventoryAttachmentStatus.toDomain(): InventoryAttachmentStatus = InventoryAttachmentStatus.valueOf(name)
internal fun MeshInventoryAttributeType.toDomain(): InventoryAttributeType = InventoryAttributeType.valueOf(name)
internal fun MeshInventoryValidationRuleType.toDomain(): InventoryValidationRuleType = InventoryValidationRuleType.valueOf(name)
internal fun MeshInventoryOwnerType.toDomain(): InventoryOwnerType = InventoryOwnerType.valueOf(name)
internal fun MeshInventoryIncidentType.toDomain(): InventoryIncidentType = InventoryIncidentType.valueOf(name)
internal fun MeshInventoryIncidentSeverity.toDomain(): InventoryIncidentSeverity = InventoryIncidentSeverity.valueOf(name)
internal fun MeshInventoryIncidentStatus.toDomain(): InventoryIncidentStatus = InventoryIncidentStatus.valueOf(name)
internal fun MeshInventoryRuleType.toDomain(): InventoryRuleType = InventoryRuleType.valueOf(name)
internal fun MeshInventoryReminderStatus.toDomain(): InventoryReminderStatus = InventoryReminderStatus.valueOf(name)
internal fun MeshInventoryDeadlineTarget.toDomain(): InventoryDeadlineTarget = InventoryDeadlineTarget.valueOf(name)
internal fun MeshInventoryCodeType.toDomain(): InventoryCodeType = InventoryCodeType.valueOf(name)
internal fun MeshInventoryBarcodeFormat.toDomain(): InventoryBarcodeFormat = InventoryBarcodeFormat.valueOf(name)
internal fun MeshInventoryLabelTemplateType.toDomain(): InventoryLabelTemplateType = InventoryLabelTemplateType.valueOf(name)
internal fun MeshInventoryLabelFieldKey.toDomain(): InventoryLabelFieldKey = InventoryLabelFieldKey.valueOf(name)
internal fun MeshInventoryPrintStatus.toDomain(): InventoryPrintStatus = InventoryPrintStatus.valueOf(name)
internal fun MeshInventoryScanResultStatus.toDomain(): InventoryScanResultStatus = InventoryScanResultStatus.valueOf(name)
internal fun MeshInventorySyncStatus.toDomain(): InventorySyncStatus = InventorySyncStatus.valueOf(name)

internal fun InventoryAttachmentPreviewMetadata.toContract(): MeshInventoryAttachmentPreviewMetadata =
    MeshInventoryAttachmentPreviewMetadata(width = width, height = height, mimeType = mimeType)

internal fun MeshInventoryAttachmentPreviewMetadata.toDomain(): InventoryAttachmentPreviewMetadata =
    InventoryAttachmentPreviewMetadata(width = width, height = height, mimeType = mimeType)

internal fun MeshInventoryFieldTemplate.toDomain(): InventoryFieldTemplate = InventoryFieldTemplate(
    fieldId = fieldId,
    key = key,
    label = label,
    type = type.toDomain(),
    required = required,
    validationRules = validationRules.map { it.toDomain() },
    helpText = helpText,
    order = order,
)

internal fun MeshInventoryValidationRule.toDomain(): InventoryValidationRule = InventoryValidationRule(
    type = type.toDomain(),
    params = params,
    message = message,
)

internal fun MeshInventoryRequiredFieldRule.toDomain(): InventoryRequiredFieldRule = InventoryRequiredFieldRule(
    fieldId = fieldId,
    message = message,
)

internal fun MeshInventoryAttributeValue.toDomain(): InventoryAttributeValue = InventoryAttributeValue(
    attributeId = attributeId,
    value = value,
    updatedByPeerId = updatedByPeerId,
    updatedAt = updatedAt,
)

internal fun MeshInventorySearchQuery.toDomain(): InventorySearchQuery = InventorySearchQuery(
    organizationId = organizationId,
    text = text,
    inventoryNumber = inventoryNumber,
    localNumber = localNumber,
    serialNumber = serialNumber,
    qrCode = qrCode,
    barcode = barcode,
    filter = filter.toDomain(),
    sort = sort.toDomain(),
    limit = limit,
    offset = offset,
)

internal fun MeshInventoryFilterSet.toDomain(): InventoryFilterSet = InventoryFilterSet(
    categoryIds = categoryIds,
    subcategoryIds = subcategoryIds,
    locationIds = locationIds,
    departmentIds = departmentIds,
    costCenterIds = costCenterIds,
    ownerIds = ownerIds,
    tagIds = tagIds,
    status = status.map { it.toDomain() }.toSet(),
    condition = condition.map { it.toDomain() }.toSet(),
    incidentOnly = incidentOnly,
    sessionId = sessionId,
    updatedSince = updatedSince,
    nextInventoryBefore = nextInventoryBefore,
)

internal fun MeshInventorySortMode.toDomain(): InventorySortMode = InventorySortMode.valueOf(name)

internal fun MeshInventorySessionResult.toDomain(): InventorySessionResult = InventorySessionResult(
    summary = summary,
    confirmedCount = confirmedCount,
    rejectedCount = rejectedCount,
    requiresUpdateCount = requiresUpdateCount,
    incidentCount = incidentCount,
    completedAt = completedAt,
    approvedByPeerId = approvedByPeerId,
)

internal fun InventorySyncResult.toContract(): MeshInventorySyncResult = MeshInventorySyncResult(
    requestId = requestId,
    organizationId = organizationId,
    peerId = peerId,
    eventsReceived = eventsReceived,
    lastSequence = lastSequence,
    timedOut = timedOut,
)
