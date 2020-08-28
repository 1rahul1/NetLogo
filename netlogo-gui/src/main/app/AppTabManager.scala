// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.app
import java.awt.Component

import org.nlogo.core.I18N
import org.nlogo.app.codetab.{ CodeTab, MainCodeTab }

// The class AppTabManager handles relationships between tabs (JPanels) and the two
// classes Tabs and MainCodeTabPanel that are the JTabbedPanes that contain them.

class AppTabManager( val appTabs:          Tabs,
                     var mainCodeTabPanel: Option[MainCodeTabPanel]) {

  def getAppTabs = appTabs

  def setMainCodeTabPanel(_mainCodeTabPanel: Option[MainCodeTabPanel]): Unit = {
    mainCodeTabPanel = _mainCodeTabPanel
  }

  def getMainCodeTabOwner =
    mainCodeTabPanel match {
      case None           => appTabs
      case Some(theValue) => theValue
    }
    // aab this might not be needed

  def getAppsTab = appTabs
  def getCodeTab = appTabs.getCodeTab
  private var currentTab: Component = appTabs.interfaceTab

  def getCodeTabOwner(tab: CodeTab): AbstractTabs = {
    if (tab.isInstanceOf[CodeTab]) getMainCodeTabOwner else appTabs
  }

  def getTabOwner(tab: Component): AbstractTabs = {
    if (tab.isInstanceOf[MainCodeTab]) getMainCodeTabOwner else appTabs
  }

  def setSelectedCodeTab(tab: CodeTab): Unit = {
    getCodeTabOwner(tab).setSelectedComponent(tab)
  }

  def setCurrentTab(tab: Component): Unit = {
    currentTab = tab
  }

  def getCurrentTab(): Component = {
    currentTab
  }
  def switchToTabsCodeTab(): Unit = {
    // nothing to do if code tab is already part of Tabs
    val codeTabOwner = getCodeTabOwner _
    if (codeTabOwner.isInstanceOf[Tabs]) {
      println("nothing doing")
      return
    } else {
      println("switchToTabsCodeTab")
    }
  }

  def switchToSeparateCodeWindow(): Unit = {
    // nothing to do if code tab is already separate
    val codeTabOwner = getCodeTabOwner _
    // can invert condition and not use return
    if (codeTabOwner.isInstanceOf[MainCodeTabPanel]) {
      println("nothing doing")
      return
    } else {
      val actualMainCodeTabPanel = new MainCodeTabPanel(getAppsTab.workspace,
        getAppsTab.interfaceTab,
        getAppsTab.externalFileManager,
        getAppsTab.codeTab,
        getAppsTab.externalFileTabs)
        mainCodeTabPanel = Some(actualMainCodeTabPanel)
        actualMainCodeTabPanel.setTabManager(this)
        actualMainCodeTabPanel.add(I18N.gui.get("tabs.code"), getAppsTab.codeTab)
        actualMainCodeTabPanel.initManagerMonitor(getAppsTab.fileManager, getAppsTab.dirtyMonitor)
        actualMainCodeTabPanel.codeTabContainer.requestFocus()
        getAppsTab.codeTab.requestFocus()
        // add mouse listener, which should be not set when
        // there is no code tab
      }
  }
}