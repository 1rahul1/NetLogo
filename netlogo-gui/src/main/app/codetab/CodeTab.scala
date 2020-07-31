// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.app.codetab

import java.awt.event.{ActionEvent, TextEvent, TextListener}
import java.awt.print.PageFormat
import java.awt.{BorderLayout, Component, Dimension, Graphics, Insets}
import java.io.IOException
import java.net.MalformedURLException
import javax.swing.{AbstractAction, Action, JComponent, JPanel}

import org.nlogo.agent.Observer
import org.nlogo.app.common.{CodeToHtml, EditorFactory, FindDialog, MenuTab, TabsInterface, Events => AppEvents}
import org.nlogo.core.{AgentKind, I18N}
import org.nlogo.editor.DumbIndenter
import org.nlogo.ide.FocusedOnlyAction
import org.nlogo.swing.Utils.icon
import org.nlogo.swing.{PrinterManager, ToolBar, ToolBarActionButton, UserAction, WrappedAction, Printable => NlogoPrintable}
import org.nlogo.window.{CommentableError, ProceduresInterface, Zoomable, Events => WindowEvents}
import org.nlogo.workspace.AbstractWorkspace

abstract class CodeTab(val workspace: AbstractWorkspace, tabs: TabsInterface) extends JPanel
with ProceduresInterface
with ProceduresMenuTarget
with AppEvents.SwitchedTabsEvent.Handler
with WindowEvents.CompiledEvent.Handler
with Zoomable
with NlogoPrintable
with MenuTab {

  private var _dirty = false
  def dirty = _dirty

  protected def dirty_=(b: Boolean) = {
    CompileAction.setDirty(b)
    _dirty = b
  }


  private lazy val listener = new TextListener {
    override def textValueChanged(e: TextEvent) = {
      if (e != null) {
        println("CodeTab TextListener, text event: e.paramString")
      } else {
        if (!dirty) {
          println("CodeTab TextListener, null, becoming dirty")
        }
      }
      dirty = true
    }
  }

  lazy val editorFactory = new EditorFactory(workspace, workspace.getExtensionManager)

  def editorConfiguration =
    editorFactory.defaultConfiguration(100, 80)
      .withCurrentLineHighlighted(true)
      .withListener(listener)

  val text = {
    val editor = editorFactory.newEditor(editorConfiguration, true)
    editor.setMargin(new Insets(4, 7, 4, 7))
    editor
  }

  lazy val undoAction: Action = {
    new WrappedAction(text.undoAction,
      UserAction.EditCategory,
      UserAction.EditUndoGroup,
      UserAction.KeyBindings.keystroke('Z', withMenu = true))
  }

  lazy val redoAction: Action = {
    new WrappedAction(text.redoAction,
      UserAction.EditCategory,
      UserAction.EditUndoGroup,
      UserAction.KeyBindings.keystroke('Y', withMenu = true))
  }

  override def zoomTarget = text

  val errorLabel = new CommentableError(text)
  val toolBar = getToolBar
  val scrollableEditor = editorFactory.scrollPane(text)
  def compiler = workspace
  def program = workspace.world.program

  locally {
    setIndenter(false)
    setLayout(new BorderLayout)
    add(toolBar, BorderLayout.NORTH)
    val codePanel = new JPanel(new BorderLayout) {
      add(scrollableEditor, BorderLayout.CENTER)
      add(errorLabel.component, BorderLayout.NORTH)
    }
    add(codePanel, BorderLayout.CENTER)
  }

  def getToolBar = new ToolBar {
    override def addControls() {
      val proceduresMenu = new ProceduresMenu(CodeTab.this)
      this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(UserAction.KeyBindings.keystroke('G', withMenu = true), "procmenu")
      this.getActionMap.put("procmenu", proceduresMenu.getAction)

      add(new ToolBarActionButton(FindDialog.FIND_ACTION))
      add(new ToolBarActionButton(CompileAction))
      add(new ToolBar.Separator)
      add(proceduresMenu)
      add(new IncludedFilesMenu(getIncludesTable, tabs))
      val additionalComps = getAdditionalToolBarComponents
      if (additionalComps.nonEmpty) {
        add(new ToolBar.Separator)
        additionalComps foreach add
      }
    }
  }

  protected def getAdditionalToolBarComponents: Seq[Component] = Seq.empty[Component]

  override val permanentMenuActions =
    Seq(new CodeToHtml.Action(workspace, this, () => getText)) ++ editorConfiguration.permanentActions

  override val activeMenuActions =
    editorConfiguration.contextActions.filter(_.isInstanceOf[FocusedOnlyAction]) ++ Seq(undoAction, redoAction)

  // don't let the editor influence the preferred size,
  // since the editor tends to want to be huge - ST
  override def getPreferredSize: Dimension = toolBar.getPreferredSize

  def getIncludesTable: Option[Map[String, String]] = {
    val path = Option(workspace.getModelPath).getOrElse{
      // we create an arbitrary model name for checking include paths when we don't have an actual
      // modelPath or directory
      try workspace.attachModelDir("foo.nlogo")
      catch {
        case ex: MalformedURLException =>
          // if we can't even figure out where we are, we certainly can't have includes
          return None
      }
    }
    workspace.compiler.findIncludes(path, getText, workspace.getCompilationEnvironment)
  }

  def agentClass = classOf[Observer]

  def kind = AgentKind.Observer

  def handle(e: AppEvents.SwitchedTabsEvent) = {
    // println("CodeTab handle SwitchedTabsEvent, dirty: " + dirty)
    // printSwingObject(e.oldTab, "  oldTab: ")
    // printSwingObject(this, "  this tab: ")
    if (dirty && e.oldTab == this) {
      println("** compile due to SwitchedTabsEvent")
      compile()
    }
  }

  private var originalFontSize = -1
  override def handle(e: WindowEvents.ZoomedEvent) {
    super.handle(e)
    if (originalFontSize == -1)
      originalFontSize = text.getFont.getSize
    text.setFont(text.getFont.deriveFont(StrictMath.ceil(originalFontSize * zoomFactor).toFloat))
    scrollableEditor.setFont(text.getFont)
    errorLabel.zoom(zoomFactor)
  }

  def printSwingObject(obj: Object, description: String): Unit = {
    val some = Option(obj)
    some match {
      case None           => println(description + "<null>")
      case Some(theValue) =>  {
        val pattern = """(^.*)\[(.*$)""".r
        val pattern(name, _) = obj.toString
        val shortName = name.split("\\.").last
        println(description + System.identityHashCode(obj) +
        ", " + shortName)
      }
    }
  }

 def printHandleCompiledEvent(e: WindowEvents.CompiledEvent, inClass: String): Unit = {
   println(">" + inClass + " handle CompiledEvent")
   // println("  error: " + java.util.Objects.toString(e.error, "<null>"))
   printSwingObject(e.sourceOwner, "  sourceOwner: ")
 }

  def handle(e: WindowEvents.CompiledEvent) = {
    // printHandleCompiledEvent(e, "CodeTab")
    dirty = false
    if (e.sourceOwner == this) {
      //println("  errorLabel.setError")
      errorLabel.setError(e.error, headerSource.length)
    }
    // this was needed to get extension colorization showing up reliably in the editor area - RG 23/3/16
    text.revalidate()
    //println("<CodeTab CompiledEvent")
  }

  protected def compile(): Unit = new WindowEvents.CompileAllEvent().raise(this)

  override def requestFocus(): Unit = text.requestFocus()

  def innerSource = text.getText
  def getText = text.getText  // for ProceduresMenuTarget
  def headerSource = ""
  def source = headerSource + innerSource

  override def innerSource_=(s: String) = {
    text.setText(s)
    text.setCaretPosition(0)
    text.resetUndoHistory()
  }

  def select(start: Int, end: Int) = text.select(start, end)

  def classDisplayName = "Code"

  @throws(classOf[IOException])
  def print(g: Graphics, pageFormat: PageFormat,pageIndex: Int, printer: PrinterManager) =
    printer.printText(g, pageFormat, pageIndex, text.getText)

  def setIndenter(isSmart: Boolean): Unit = {
    if(isSmart) text.setIndenter(new SmartIndenter(new EditorAreaWrapper(text), workspace))
    else text.setIndenter(new DumbIndenter(text))
  }

  def lineNumbersVisible = scrollableEditor.lineNumbersEnabled
  def lineNumbersVisible_=(visible: Boolean) = scrollableEditor.setLineNumbersEnabled(visible)

  def isTextSelected: Boolean = text.getSelectedText != null && !text.getSelectedText.isEmpty

  private object CompileAction extends AbstractAction(I18N.gui.get("tabs.code.checkButton")) {
    putValue(Action.SMALL_ICON, icon("/images/check-gray.gif"))
    def actionPerformed(e: ActionEvent) = {
      println("*** CompileAction actionPerformed in CodeTab")
      compile()
    }
    def setDirty(isDirty: Boolean) = {
      val iconPath =
        if (isDirty) "/images/check.gif"
        else         "/images/check-gray.gif"
      putValue(Action.SMALL_ICON, icon(iconPath))
    }
  }
}
