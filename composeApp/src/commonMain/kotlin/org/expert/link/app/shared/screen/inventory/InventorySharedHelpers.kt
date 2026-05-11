package org.expert.link.app.shared.screen.inventory

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import org.expert.link.mesh.contract.model.MeshInventoryDepartment
import org.expert.link.mesh.contract.model.MeshInventoryItem
import org.expert.link.mesh.contract.model.MeshInventoryLocation
import org.expert.link.mesh.contract.model.MeshInventoryOwner
import org.expert.link.mesh.contract.model.MeshInventoryOwnerType

fun MeshInventoryItem.allOwnerIds(): Set<String> = responsibleOwnerIds + setOfNotNull(ownerId)

fun molOwners(owners: List<MeshInventoryOwner>): List<MeshInventoryOwner> = owners
    .filter { it.type == MeshInventoryOwnerType.PERSON && !it.archived }
    .sortedBy { it.name.lowercase() }

fun ownerNames(
    item: MeshInventoryItem,
    owners: List<MeshInventoryOwner>,
): List<String> {
    val ownerMap = owners.associateBy { it.ownerId }
    return item.allOwnerIds()
        .mapNotNull { ownerMap[it]?.name }
        .ifEmpty { listOfNotNull(item.responsiblePerson) }
}

fun departmentName(
    departmentId: String?,
    departments: List<MeshInventoryDepartment>,
): String? = departments.firstOrNull { it.departmentId == departmentId }?.name

fun locationName(
    locationId: String?,
    locations: List<MeshInventoryLocation>,
): String? = locations.firstOrNull { it.locationId == locationId }?.name

fun locationPath(
    locationId: String?,
    locations: List<MeshInventoryLocation>,
): String? {
    if (locationId == null) return null
    val map = locations.associateBy { it.locationId }
    val names = mutableListOf<String>()
    var current = map[locationId]
    val visited = mutableSetOf<String>()
    while (current != null && visited.add(current.locationId)) {
        names += current.name
        current = current.parentLocationId?.let(map::get)
    }
    return names.reversed().joinToString(" / ").ifBlank { null }
}

fun locationAndDescendants(
    rootId: String,
    locations: List<MeshInventoryLocation>,
): Set<String> {
    val children = locations.groupBy { it.parentLocationId }
    val result = linkedSetOf<String>()

    fun visit(locationId: String) {
        if (!result.add(locationId)) return
        children[locationId].orEmpty().forEach { visit(it.locationId) }
    }

    visit(rootId)
    return result
}

fun matchesLocationFilter(
    itemLocationId: String?,
    selectedLocationIds: Set<String>,
    locations: List<MeshInventoryLocation>,
): Boolean {
    if (selectedLocationIds.isEmpty()) return true
    if (itemLocationId == null) return false
    return selectedLocationIds.any { selected ->
        itemLocationId in locationAndDescendants(selected, locations)
    }
}

fun availableOwnersForSelection(
    owners: List<MeshInventoryOwner>,
    selectedDepartmentIds: Set<String>,
    selectedLocationIds: Set<String>,
    locations: List<MeshInventoryLocation>,
): List<MeshInventoryOwner> = owners.filter { owner ->
    val departmentMatches = selectedDepartmentIds.isEmpty() || owner.departmentId in selectedDepartmentIds
    val locationMatches = if (selectedLocationIds.isEmpty()) {
        true
    } else {
        owner.locationIds.isEmpty() || selectedLocationIds.any { selected ->
            owner.locationIds.any { ownerLocationId ->
                ownerLocationId in locationAndDescendants(selected, locations) ||
                    selected in locationAndDescendants(ownerLocationId, locations)
            }
        }
    }
    departmentMatches && locationMatches
}.sortedBy { it.name }

fun filterItemsByStructure(
    items: List<MeshInventoryItem>,
    selectedDepartmentIds: Set<String>,
    selectedLocationIds: Set<String>,
    selectedOwnerIds: Set<String>,
    locations: List<MeshInventoryLocation>,
): List<MeshInventoryItem> = items.filter { item ->
    val departmentMatches = selectedDepartmentIds.isEmpty() || item.departmentId in selectedDepartmentIds
    val locationMatches = matchesLocationFilter(item.locationId, selectedLocationIds, locations)
    val ownerMatches = selectedOwnerIds.isEmpty() || item.allOwnerIds().any { it in selectedOwnerIds }
    departmentMatches && locationMatches && ownerMatches
}

fun parseInventoryDate(input: String): LocalDate? = try {
    if (input.isBlank()) null else LocalDate.parse(input.trim())
} catch (_: IllegalArgumentException) {
    null
}

fun inventoryDateStart(date: LocalDate, timeZone: TimeZone): Instant = date.atStartOfDayIn(timeZone)

fun inventoryDateEnd(date: LocalDate, timeZone: TimeZone): Instant =
    date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone)
