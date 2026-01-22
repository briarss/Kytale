package aster.amo.kytale.dsl

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.ExtraInfo
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.codec.builder.BuilderField
import com.hypixel.hytale.codec.schema.metadata.Metadata
import com.hypixel.hytale.codec.validation.LateValidator
import com.hypixel.hytale.codec.validation.Validator
import java.util.function.Supplier

/**
 * Usage:
 *
 * ```kotlin
 * @JvmStatic <- Required for access from java.
 * val CODEC = buildCodec(::ArcioBaseComponent) {
 *     versioned()
 *     documentation {
 *         """
 *             Very long and through explanation on the behaviour of this component*
 *             You think this is amazing*
 *         """.trimIndent()
 *     }
 *
 *     addField("NodeIndex", Codec.INTEGER) {
 *         setter { this.index = it }
 *         getter { this.index }
 *         documentation { "Index of the current node in the manathread network." }
 *     }
 *
 *
 *     addField("IO", ArrayCodec(ManathreadConnector.CODEC) { i -> arrayOfNulls<ManathreadConnector>(i) }) {
 *         setter { this.connectors = it.toMutableList() }
 *         getter { this.connectors.toTypedArray()}
 *         documentation { "List of connections to and from the current node." }
 *     }
 * }
 * ```
 */

inline fun <reified T> buildCodec(supplier: Supplier<T>, scopeReceiver: CodecBuilderScope<T>.() -> Unit): BuilderCodec<T?> {
    return BuilderCodec.builder(T::class.java, supplier)
        .also { CodecBuilderScope(it).apply(scopeReceiver) }
        .build()
}

@Suppress("unused")
class CodecBuilderScope <Base>(val builder: BuilderCodec.Builder<Base>) {

    inner class FieldBuilderScope <FieldType> () {
        internal var _setter: (Base.(FieldType, ExtraInfo) -> Unit)? = null
        internal var _getter: (Base.(ExtraInfo) -> FieldType)? = null
        internal var _inherit: (Base.(Base, ExtraInfo) -> Unit)? = null

        internal var _documentation = ""
        internal var validators: MutableList<Validator<FieldType>> = mutableListOf()
        internal var lateValidators: MutableList<() -> (LateValidator<in FieldType>?)> = mutableListOf()
        internal var metadata: MutableList<Metadata> = mutableListOf()

        internal val isInherited: Boolean
            get() = _inherit == null

        /**
         *  Define getter for codec field value. This version is provided with `ExtraInfo`. Overwrites `setter()`.
         */
        fun setterExt(closure: Base.(FieldType, ExtraInfo) -> Unit) { _setter = closure }

        /**
         *  Define getter for codec field value. This version is provided with `ExtraInfo`. Overwrites `getter()`.
         */
        fun getterExt(closure: Base.(ExtraInfo) -> FieldType) { _getter = closure }

        /**
         *  handle inheritance for codec field value. This version is provided with `ExtraInfo`. Overwrites `inherit()`.
         */
        fun inheritExt(closure: Base.(Base, ExtraInfo) -> Unit) { _inherit = closure }

        /**
         *  Define setter for codec field value.
         */
        fun setter(closure: Base.(FieldType) -> Unit) { _setter = { it, _ -> closure(it) } }

        /**
         *  Define getter for codec field value.
         */
        fun getter(closure: Base.() -> FieldType) { _getter = { _ -> closure()} }

        /**
         *  handle inheritance for codec field value.
         */
        fun inherit(closure: Base.(Base) -> Unit) { _inherit = { parent, _ -> closure(parent)} }

        /**
         *  Define documentation for codec field value.
         */
        fun documentation(doc: () -> String) {
            builder.documentation(doc())
        }

        /**
         *  Add a validator for the codec field.
         */
        fun addValidator(vararg validator: Validator<FieldType>) {
            this.validators.addAll(validator)
        }

        /**
         *  Add a *late* validator for the codec field.
         */
        fun addValidatorLate(validator: () -> LateValidator<FieldType>) {
            lateValidators.add(validator)
        }

        /**
         *  Add a piece of metadata for the codec field.
         */
        fun addMetadata(vararg metadata: Metadata) {
            this.metadata.addAll(metadata)
        }
    }

    /** Add a field to the codec. */
    fun <FieldType> addField(
        fieldName: String,
        codec: Codec<FieldType>,
        fieldBuilderReceiver: FieldBuilderScope<FieldType>.() -> Unit
    ) {
        val fieldBuilderScope = this.FieldBuilderScope<FieldType>().apply(fieldBuilderReceiver)
        val keyedCodec = KeyedCodec(fieldName, codec)

        val fieldBuilder = BuilderField.FieldBuilder(
            builder,
            keyedCodec,
            fieldBuilderScope._setter,
            fieldBuilderScope._getter,
            fieldBuilderScope._inherit
        )

        fieldBuilderScope.validators.forEach {
            fieldBuilder.addValidator(it)
        }

        fieldBuilderScope.lateValidators.forEach {
            fieldBuilder.addValidatorLate { it() }
        }

        fieldBuilderScope.metadata.forEach {
            fieldBuilder.metadata(it)
        }

        fieldBuilder.documentation(fieldBuilderScope._documentation)

        fieldBuilder.add()
    }

    /** Defines a `afterDecode` callback on the codec. */
    fun afterDecode(receiver: Base.() -> Unit) {
        builder.afterDecode(receiver)
    }

    /** Defines a `afterDecode` callback on the codec. This variant receives extra information. */
    fun afterDecodeExt(receiver: Base.(ExtraInfo) -> Unit) {
        builder.afterDecode(receiver)
    }

    /** Adds a piece of metadata to the codec */
    fun metadata(meta: Metadata) {
        builder.metadata(meta)
    }

    /** Sets the current codec version and minimal required codec version. */
    fun codecVersion(minVersion: Int, version: Int) {
        builder.codecVersion(minVersion, version)
    }

    /** Sets the current codec version */
    fun codecVersion(version: Int) {
        builder.codecVersion(version)
    }

    /** Enabled the `versioned` flag on the codec */
    fun versioned() {
        builder.versioned()
    }

    /** Defines documentation to the current codec */
    fun documentation(doc: () -> String) {
        builder.documentation(doc())
    }
}