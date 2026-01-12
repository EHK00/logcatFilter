package domain.model

data class Device(
    val id: String,
    val name: String,
    val state: DeviceState,
    val type: DeviceType
) {
    val displayName: String
        get() = if (name.isNotBlank()) "$name ($id)" else id
}

enum class DeviceState {
    ONLINE,
    OFFLINE,
    UNAUTHORIZED,
    UNKNOWN;

    companion object {
        fun fromString(state: String): DeviceState {
            return when (state.lowercase()) {
                "device" -> ONLINE
                "offline" -> OFFLINE
                "unauthorized" -> UNAUTHORIZED
                else -> UNKNOWN
            }
        }
    }
}

enum class DeviceType {
    DEVICE,
    EMULATOR;

    companion object {
        fun fromDeviceId(id: String): DeviceType {
            return if (id.startsWith("emulator-")) EMULATOR else DEVICE
        }
    }
}
