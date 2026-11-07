package dev.cricamfe.digivice.core.events.models

import kotlinx.serialization.Serializable

/** Port of `Backend/Events/Models/HealthStatus.cs`; it goes out as its name (`Loading`, `Healthy`, `Error`). */
@Serializable
enum class HealthStatus { Loading, Healthy, Error }
