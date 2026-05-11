package org.expert.link.mesh.application.service

import org.expert.link.mesh.application.support.newId
import org.expert.link.mesh.application.support.now
import org.expert.link.mesh.domain.model.inventory.InventoryAttributeDefinition
import org.expert.link.mesh.domain.model.inventory.InventoryAttributeType
import org.expert.link.mesh.domain.model.inventory.InventoryCategory
import org.expert.link.mesh.domain.model.inventory.InventoryCategoryTemplate
import org.expert.link.mesh.domain.model.inventory.InventoryEntityType
import org.expert.link.mesh.domain.model.inventory.InventoryEventType
import org.expert.link.mesh.domain.model.inventory.InventoryFieldTemplate
import org.expert.link.mesh.domain.model.inventory.InventoryLocation
import org.expert.link.mesh.domain.model.inventory.InventoryLocationType
import org.expert.link.mesh.domain.model.inventory.InventoryRequiredFieldRule
import org.expert.link.mesh.domain.model.inventory.InventorySubcategory
import org.expert.link.mesh.domain.model.inventory.InventoryTag
import org.expert.link.mesh.domain.model.inventory.InventoryValidationRule
import org.expert.link.mesh.domain.model.inventory.InventoryCategorySnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryCategoryTemplateSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryLocationSnapshot
import org.expert.link.mesh.domain.model.inventory.InventorySubcategorySnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryTagSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryAttributeDefinitionSnapshot
import org.expert.link.mesh.domain.model.inventory.InventoryPermission
import org.expert.link.mesh.domain.port.repository.InventoryAttributeDefinitionRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryCategoryTemplateRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryCategoryRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryLocationRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventorySubcategoryRepositoryPort
import org.expert.link.mesh.domain.port.repository.InventoryTagRepositoryPort

class InventoryCatalogService(
    private val localProfileService: LocalProfileService,
    private val categoryRepositoryPort: InventoryCategoryRepositoryPort,
    private val subcategoryRepositoryPort: InventorySubcategoryRepositoryPort,
    private val tagRepositoryPort: InventoryTagRepositoryPort,
    private val attributeDefinitionRepositoryPort: InventoryAttributeDefinitionRepositoryPort,
    private val categoryTemplateRepositoryPort: InventoryCategoryTemplateRepositoryPort,
    private val locationRepositoryPort: InventoryLocationRepositoryPort,
    private val rbacService: InventoryRbacService,
    private val inventoryEventService: InventoryEventService,
    private val inventorySyncService: InventorySyncService,
) {
    suspend fun createCategory(
        organizationId: String,
        name: String,
        description: String?,
        parentCategoryId: String?,
        templateId: String?,
    ): InventoryCategory {
        require(name.isNotBlank()) { "Category name is blank" }
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, InventoryPermission.CATEGORY_MANAGE)
        val category = InventoryCategory(
            categoryId = newId("category"),
            organizationId = organizationId,
            name = name,
            description = description,
            parentCategoryId = parentCategoryId,
            templateId = templateId,
            createdByPeerId = localProfile.peerId,
            createdAt = now(),
            updatedAt = now(),
        )
        categoryRepositoryPort.save(category)
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.CATEGORY,
                entityId = category.categoryId,
                eventType = InventoryEventType.CREATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryCategorySnapshot(category),
            ),
        )
        return category
    }

    suspend fun updateCategory(
        categoryId: String,
        name: String,
        description: String?,
        parentCategoryId: String?,
        templateId: String?,
    ): InventoryCategory? {
        val category = categoryRepositoryPort.findByCategoryId(categoryId) ?: return null
        val localProfile = localProfileService.require()
        rbacService.requirePermission(category.organizationId, localProfile.peerId, InventoryPermission.CATEGORY_MANAGE)
        val updated = category.copy(
            name = name,
            description = description,
            parentCategoryId = parentCategoryId ?: category.parentCategoryId,
            templateId = templateId ?: category.templateId,
            updatedAt = now(),
        )
        categoryRepositoryPort.save(updated)
        publish(
            inventoryEventService.recordEvent(
                organizationId = category.organizationId,
                entityType = InventoryEntityType.CATEGORY,
                entityId = categoryId,
                eventType = InventoryEventType.UPDATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryCategorySnapshot(updated),
            ),
        )
        return updated
    }

    suspend fun listCategories(organizationId: String): List<InventoryCategory> =
        categoryRepositoryPort.listByOrganization(organizationId)

    suspend fun listSubcategories(organizationId: String, categoryId: String?): List<InventorySubcategory> {
        return if (categoryId == null) {
            subcategoryRepositoryPort.listByOrganization(organizationId)
        } else {
            subcategoryRepositoryPort.listByCategory(categoryId)
        }
    }

    suspend fun createSubcategory(
        organizationId: String,
        categoryId: String,
        name: String,
        description: String?,
    ): InventorySubcategory {
        require(name.isNotBlank()) { "Subcategory name is blank" }
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, InventoryPermission.CATEGORY_MANAGE)
        val subcategory = InventorySubcategory(
            subcategoryId = newId("subcategory"),
            categoryId = categoryId,
            organizationId = organizationId,
            name = name,
            description = description,
            createdByPeerId = localProfile.peerId,
            createdAt = now(),
            updatedAt = now(),
        )
        subcategoryRepositoryPort.save(subcategory)
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.SUBCATEGORY,
                entityId = subcategory.subcategoryId,
                eventType = InventoryEventType.CREATED,
                actorPeerId = localProfile.peerId,
                payload = InventorySubcategorySnapshot(subcategory),
            ),
        )
        return subcategory
    }

    suspend fun updateSubcategory(
        subcategoryId: String,
        name: String,
        description: String?,
    ): InventorySubcategory? {
        val current = subcategoryRepositoryPort.findBySubcategoryId(subcategoryId) ?: return null
        val localProfile = localProfileService.require()
        rbacService.requirePermission(current.organizationId, localProfile.peerId, InventoryPermission.CATEGORY_MANAGE)
        val updated = current.copy(
            name = name,
            description = description,
            updatedAt = now(),
        )
        subcategoryRepositoryPort.save(updated)
        publish(
            inventoryEventService.recordEvent(
                organizationId = updated.organizationId,
                entityType = InventoryEntityType.SUBCATEGORY,
                entityId = updated.subcategoryId,
                eventType = InventoryEventType.UPDATED,
                actorPeerId = localProfile.peerId,
                payload = InventorySubcategorySnapshot(updated),
            ),
        )
        return updated
    }

    suspend fun listTags(organizationId: String): List<InventoryTag> =
        tagRepositoryPort.listByOrganization(organizationId)

    suspend fun createTag(organizationId: String, name: String, color: String?): InventoryTag {
        require(name.isNotBlank()) { "Tag name is blank" }
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, InventoryPermission.TAG_MANAGE)
        val tag = InventoryTag(
            tagId = newId("tag"),
            organizationId = organizationId,
            name = name,
            color = color,
            createdByPeerId = localProfile.peerId,
            createdAt = now(),
            updatedAt = now(),
        )
        tagRepositoryPort.save(tag)
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.TAG,
                entityId = tag.tagId,
                eventType = InventoryEventType.CREATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryTagSnapshot(tag),
            ),
        )
        return tag
    }

    suspend fun updateTag(tagId: String, name: String, color: String?): InventoryTag? {
        val current = tagRepositoryPort.findByTagId(tagId) ?: return null
        val localProfile = localProfileService.require()
        rbacService.requirePermission(current.organizationId, localProfile.peerId, InventoryPermission.TAG_MANAGE)
        val updated = current.copy(
            name = name,
            color = color,
            updatedAt = now(),
        )
        tagRepositoryPort.save(updated)
        publish(
            inventoryEventService.recordEvent(
                organizationId = updated.organizationId,
                entityType = InventoryEntityType.TAG,
                entityId = updated.tagId,
                eventType = InventoryEventType.UPDATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryTagSnapshot(updated),
            ),
        )
        return updated
    }

    suspend fun listAttributeDefinitions(organizationId: String): List<InventoryAttributeDefinition> =
        attributeDefinitionRepositoryPort.listByOrganization(organizationId)

    suspend fun createAttributeDefinition(
        organizationId: String,
        key: String,
        label: String,
        description: String?,
        type: InventoryAttributeType,
        required: Boolean,
        unit: String?,
        options: List<String>,
        validationRules: List<InventoryValidationRule>,
    ): InventoryAttributeDefinition {
        require(key.isNotBlank()) { "Attribute key is blank" }
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, InventoryPermission.ATTRIBUTE_MANAGE)
        val definition = InventoryAttributeDefinition(
            attributeId = newId("attr"),
            organizationId = organizationId,
            key = key,
            label = label,
            description = description,
            type = type,
            required = required,
            unit = unit,
            options = options,
            validationRules = validationRules,
            createdByPeerId = localProfile.peerId,
            createdAt = now(),
            updatedAt = now(),
        )
        attributeDefinitionRepositoryPort.save(definition)
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.ATTRIBUTE_DEFINITION,
                entityId = definition.attributeId,
                eventType = InventoryEventType.CREATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryAttributeDefinitionSnapshot(definition),
            ),
        )
        return definition
    }

    suspend fun updateAttributeDefinition(
        attributeId: String,
        key: String?,
        label: String?,
        description: String?,
        type: InventoryAttributeType?,
        required: Boolean?,
        unit: String?,
        options: List<String>?,
        validationRules: List<InventoryValidationRule>?,
    ): InventoryAttributeDefinition? {
        val current = attributeDefinitionRepositoryPort.findByAttributeId(attributeId) ?: return null
        val localProfile = localProfileService.require()
        rbacService.requirePermission(current.organizationId, localProfile.peerId, InventoryPermission.ATTRIBUTE_MANAGE)
        val updated = current.copy(
            key = key ?: current.key,
            label = label ?: current.label,
            description = description ?: current.description,
            type = type ?: current.type,
            required = required ?: current.required,
            unit = unit ?: current.unit,
            options = options ?: current.options,
            validationRules = validationRules ?: current.validationRules,
            updatedAt = now(),
        )
        attributeDefinitionRepositoryPort.save(updated)
        publish(
            inventoryEventService.recordEvent(
                organizationId = updated.organizationId,
                entityType = InventoryEntityType.ATTRIBUTE_DEFINITION,
                entityId = updated.attributeId,
                eventType = InventoryEventType.UPDATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryAttributeDefinitionSnapshot(updated),
            ),
        )
        return updated
    }

    suspend fun listCategoryTemplates(organizationId: String, categoryId: String?): List<InventoryCategoryTemplate> {
        return if (categoryId == null) {
            categoryTemplateRepositoryPort.listByOrganization(organizationId)
        } else {
            categoryTemplateRepositoryPort.listByCategory(categoryId)
        }
    }

    suspend fun createCategoryTemplate(
        organizationId: String,
        categoryId: String,
        name: String,
        description: String?,
        fields: List<InventoryFieldTemplate>,
        requiredFields: List<InventoryRequiredFieldRule>,
    ): InventoryCategoryTemplate {
        require(name.isNotBlank()) { "Template name is blank" }
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, InventoryPermission.TEMPLATE_MANAGE)
        val template = InventoryCategoryTemplate(
            templateId = newId("template"),
            organizationId = organizationId,
            categoryId = categoryId,
            name = name,
            description = description,
            fields = fields,
            requiredFields = requiredFields,
            createdByPeerId = localProfile.peerId,
            createdAt = now(),
            updatedAt = now(),
        )
        categoryTemplateRepositoryPort.save(template)
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.CATEGORY_TEMPLATE,
                entityId = template.templateId,
                eventType = InventoryEventType.CREATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryCategoryTemplateSnapshot(template),
            ),
        )
        return template
    }

    suspend fun updateCategoryTemplate(
        templateId: String,
        name: String?,
        description: String?,
        fields: List<InventoryFieldTemplate>?,
        requiredFields: List<InventoryRequiredFieldRule>?,
    ): InventoryCategoryTemplate? {
        val current = categoryTemplateRepositoryPort.findByTemplateId(templateId) ?: return null
        val localProfile = localProfileService.require()
        rbacService.requirePermission(current.organizationId, localProfile.peerId, InventoryPermission.TEMPLATE_MANAGE)
        val updated = current.copy(
            name = name ?: current.name,
            description = description ?: current.description,
            fields = fields ?: current.fields,
            requiredFields = requiredFields ?: current.requiredFields,
            updatedAt = now(),
        )
        categoryTemplateRepositoryPort.save(updated)
        publish(
            inventoryEventService.recordEvent(
                organizationId = updated.organizationId,
                entityType = InventoryEntityType.CATEGORY_TEMPLATE,
                entityId = updated.templateId,
                eventType = InventoryEventType.UPDATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryCategoryTemplateSnapshot(updated),
            ),
        )
        return updated
    }
    suspend fun createLocation(
        organizationId: String,
        name: String,
        description: String?,
        parentLocationId: String?,
        locationType: InventoryLocationType,
        code: String?,
        path: String?,
        departmentId: String? = null,
        archived: Boolean = false,
    ): InventoryLocation {
        require(name.isNotBlank()) { "Location name is blank" }
        val localProfile = localProfileService.require()
        rbacService.requirePermission(organizationId, localProfile.peerId, InventoryPermission.LOCATION_MANAGE)
        val location = InventoryLocation(
            locationId = newId("location"),
            organizationId = organizationId,
            name = name,
            description = description,
            parentLocationId = parentLocationId,
            locationType = locationType,
            code = code,
            path = path,
            departmentId = departmentId,
            archived = archived,
            createdByPeerId = localProfile.peerId,
            createdAt = now(),
            updatedAt = now(),
        )
        locationRepositoryPort.save(location)
        publish(
            inventoryEventService.recordEvent(
                organizationId = organizationId,
                entityType = InventoryEntityType.LOCATION,
                entityId = location.locationId,
                eventType = InventoryEventType.CREATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryLocationSnapshot(location),
            ),
        )
        return location
    }

    suspend fun updateLocation(
        locationId: String,
        name: String,
        description: String?,
        parentLocationId: String?,
        locationType: InventoryLocationType?,
        code: String?,
        path: String?,
        departmentId: String? = null,
        archived: Boolean? = null,
    ): InventoryLocation? {
        val location = locationRepositoryPort.findByLocationId(locationId) ?: return null
        val localProfile = localProfileService.require()
        rbacService.requirePermission(location.organizationId, localProfile.peerId, InventoryPermission.LOCATION_MANAGE)
        val updated = location.copy(
            name = name,
            description = description,
            parentLocationId = parentLocationId ?: location.parentLocationId,
            locationType = locationType ?: location.locationType,
            code = code ?: location.code,
            path = path,
            departmentId = departmentId ?: location.departmentId,
            archived = archived ?: location.archived,
            updatedAt = now(),
        )
        locationRepositoryPort.save(updated)
        publish(
            inventoryEventService.recordEvent(
                organizationId = location.organizationId,
                entityType = InventoryEntityType.LOCATION,
                entityId = locationId,
                eventType = InventoryEventType.UPDATED,
                actorPeerId = localProfile.peerId,
                payload = InventoryLocationSnapshot(updated),
            ),
        )
        return updated
    }

    suspend fun listLocations(organizationId: String): List<InventoryLocation> =
        locationRepositoryPort.listByOrganization(organizationId)

    private suspend fun publish(event: org.expert.link.mesh.domain.model.inventory.InventoryEvent) {
        inventorySyncService.broadcastEvent(event)
    }
}
