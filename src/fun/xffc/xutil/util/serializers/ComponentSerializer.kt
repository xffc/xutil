package `fun`.xffc.xutil.util.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonObject
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer

@Serializable
private data class ComponentSurrogate(
    val component: Component
)

object ComponentSerializer: KSerializer<Component> {
    override val descriptor = buildClassSerialDescriptor(Component::class.qualifiedName!!)

    override fun deserialize(decoder: Decoder): Component =
        JSONComponentSerializer.json().deserialize(
            (decoder as JsonDecoder).decodeJsonElement().jsonObject.toString()
        )

    override fun serialize(encoder: Encoder, value: Component) =
        encoder.encodeString(JSONComponentSerializer.json().serialize(value))
}