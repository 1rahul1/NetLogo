// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.prim.threed

import org.nlogo.core.Syntax
import org.nlogo.nvm.{ Context, Reporter }

class _opitch extends Reporter {

  override def report(context: Context) =
    Double.box(world.observer.orientation.get.pitch)
}
