package io.kaitai.struct.precompile

import io.kaitai.struct.format.{ClassSpec, ClassSpecs, ValueInstanceSpec, YAMLParseException}
import io.kaitai.struct.translators.TypeDetector
import io.kaitai.struct.{ClassTypeProvider, Log}

class ValueTypesDeriver(specs: ClassSpecs, topClass: ClassSpec) {
  val provider = new ClassTypeProvider(specs, topClass)
  val detector = new TypeDetector(provider)

  def run(): Boolean =
    deriveValueType(topClass)

  def deriveValueType(curClass: ClassSpec): Boolean = {
    Log.typeProcValue.info(() => s"deriveValueType(${curClass.nameAsStr})")
    var hasChanged = false
    var hasUndecided = false

    provider.nowClass = curClass
    curClass.instances.foreach {
      case (instName, inst) =>
        inst match {
          case vi: ValueInstanceSpec =>
            vi.dataType match {
              case None =>
                try {
                  val viType = detector.detectType(vi.value)
                  vi.dataType = Some(viType)
                  Log.typeProcValue.info(() => s"${instName.name} derived type: $viType")
                  hasChanged = true
                } catch {
                  case tue: TypeUndecidedError =>
                    Log.typeProcValue.info(() => s"${instName.name} type undecided: ${tue.getMessage}")
                    hasUndecided = true
                    // just ignore, we're not there yet, probably we'll get it on next iteration
                  case err: ExpressionError =>
                    throw new ErrorInInput(err, vi.path ++ List("value"))
                }
              case Some(_) =>
                // already derived, do nothing
            }
          case _ =>
            // do nothing
        }
    }

    // Continue with all nested types
    curClass.types.foreach {
      case (_, classSpec) =>
        hasChanged ||= deriveValueType(classSpec)
    }

    hasChanged
  }
}
