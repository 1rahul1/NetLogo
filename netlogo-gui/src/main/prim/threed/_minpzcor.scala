// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.prim.threed

import org.nlogo.agent.World3D
import org.nlogo.core.Syntax
import org.nlogo.nvm.{ Context, Reporter }

class _minpzcor extends Reporter {

  override def report(context: Context) =
    Double.box(world.asInstanceOf[World3D].minPzcor)
}
