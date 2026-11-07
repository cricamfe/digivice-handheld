package dev.cricamfe.digivice.core.events.diffing.extensions

import dev.cricamfe.digivice.core.events.dto.shared.Optional
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Port of `Backend/Events/Diffing/Extensions/DifferExtensions.cs`: there is nothing to report when the new object is
 * missing or equal (by value) to the previous one. Returning `false` means the new object exists.
 */
@OptIn(ExperimentalContracts::class)
fun <T : Any> T?.hasNoChanges(previous: T?): Boolean {
    contract { returns(false) implies (this@hasNoChanges != null) }
    return this == null || this == previous
}

/** The C# `if (new.X != previous.X) dto = dto with { X = new.X }`: the new value when it changed, absent when not. */
internal fun <T> changed(previous: T, new: T): Optional<T> = if (previous != new) Optional.Present(new) else Optional.Empty

/** The delta of a list of children: absent when no child changed, the changed children otherwise. */
internal fun <T> changedItems(deltas: List<T>): Optional<List<T>> = if (deltas.isNotEmpty()) Optional.Present(deltas) else Optional.Empty

/** The delta of a nested object: absent when it did not change (the differ returned `null`). */
internal fun <T : Any> nested(delta: T?): Optional<T> = if (delta != null) Optional.Present(delta) else Optional.Empty
