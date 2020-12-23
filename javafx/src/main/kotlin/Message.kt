interface IMessage {
    override fun toString(): String
}

open class PalettiError(override val message: String) : Exception(message), IMessage {
    override fun toString() = message
}

object Uninitialized : PalettiError("First load an image into Paletti.")
object LeptonicaReadError : PalettiError("Could not load this file.")
object LeptonicaError : PalettiError("Could not run quantization on this image.")
