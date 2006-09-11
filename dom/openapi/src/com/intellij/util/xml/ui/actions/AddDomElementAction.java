/*
 * Copyright (c) 2000-2006 JetBrains s.r.o. All Rights Reserved.
 */

package com.intellij.util.xml.ui.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.util.xml.*;
import com.intellij.util.xml.reflect.DomCollectionChildDescription;
import com.intellij.util.xml.ui.DomCollectionControl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * User: Sergey.Vasiliev
 */
public abstract class AddDomElementAction extends AnAction {

  private final static ShortcutSet shortcutSet = new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0));

  public AddDomElementAction() {
    super(ApplicationBundle.message("action.add"), null, DomCollectionControl.ADD_ICON);
  }

  public void update(AnActionEvent e) {
    if (!isEnabled(e)) return;

    final AnAction[] actions = getChildren(e);
    for (final AnAction action : actions) {
      e.getPresentation().setEnabled(true);
      action.update(e);
      if (e.getPresentation().isEnabled()) {
        break;
      }
    }
    if (actions.length == 1) {
      e.getPresentation().setText(actions[0].getTemplatePresentation().getText());
    } else {
      final String actionText = getActionText(e);
      if (!actionText.endsWith("...")) {
        e.getPresentation().setText(actionText + (actions.length > 1 ? "..." : ""));
      }
    }
    e.getPresentation().setIcon(DomCollectionControl.ADD_ICON);

    super.update(e);
  }

  public void actionPerformed(AnActionEvent e) {
    final AnAction[] actions = getChildren(e);
    if (actions.length > 1) {
      final DefaultActionGroup group = new DefaultActionGroup();
      for (final AnAction action : actions) {
        group.add(action);
      }

      final DataContext dataContext = e.getDataContext();
      final ListPopup groupPopup =
        JBPopupFactory.getInstance().createActionGroupPopup(null,//J2EEBundle.message("label.menu.title.add.activation.config.property"),
                                                            group, dataContext, JBPopupFactory.ActionSelectionAid.NUMBERING, true);

      showPopup(groupPopup, e);
    }
    else {
      actions[0].actionPerformed(e);
    }
  }

  protected String getActionText(final AnActionEvent e) {
    return e.getPresentation().getText();
  }

  protected boolean isEnabled(final AnActionEvent e) {
    return true;
  }

  protected void showPopup(final ListPopup groupPopup, final AnActionEvent e) {
    final Component component = e.getInputEvent().getComponent();
    if (component instanceof JMenuItem) {
      groupPopup.showInBestPositionFor(e.getDataContext());
    } else {
      groupPopup.showUnderneathOf(component);
    }
  }

  @NotNull
  public AnAction[] getChildren(final AnActionEvent e) {
    Project project = (Project)e.getDataContext().getData(DataConstants.PROJECT);
    if (project == null) return AnAction.EMPTY_ARRAY;

    DomCollectionChildDescription[] descriptions = getDomCollectionChildDescriptions(e);
    final List<AnAction> actions = new ArrayList<AnAction>();
    for (DomCollectionChildDescription description : descriptions) {
      final TypeChooser chooser = DomManager.getDomManager(project).getTypeChooserManager().getTypeChooser(description.getType());
      for (Type type : chooser.getChooserTypes()) {

        final Class<?> rawType = DomReflectionUtil.getRawType(type);
        String name = ElementPresentationManager.getTypeName(rawType);
        Icon icon = null;
        if (!showAsPopup() || descriptions.length == 1) {
//          if (descriptions.length > 1) {
            icon = ElementPresentationManager.getIconForClass(rawType);
//          }
        }
        actions.add(createAddingAction(e, ApplicationBundle.message("action.add") + " " + name, icon, type, description));
      }
    }
    if (actions.size() > 1 && showAsPopup()) {
      ActionGroup group = new ActionGroup() {
        public AnAction[] getChildren(@Nullable AnActionEvent e) {
          return actions.toArray(AnAction.EMPTY_ARRAY);
        }
      };
      return new AnAction[]{new ShowPopupAction(group)};
    }
    else {
      if (actions.size() > 1) {
        actions.add(Separator.getInstance());
      } else if (actions.size() == 1) {

      }
    }
    return actions.toArray(AnAction.EMPTY_ARRAY);
  }

  protected abstract AnAction createAddingAction(final AnActionEvent e,
                                                 final String name,
                                                 final Icon icon,
                                                 final Type type,
                                                 final DomCollectionChildDescription description);

  @NotNull
  protected abstract DomCollectionChildDescription[] getDomCollectionChildDescriptions(final AnActionEvent e);

  @Nullable
  protected abstract DomElement getParentDomElement(final AnActionEvent e);

  protected abstract JComponent getComponent(AnActionEvent e);

  protected boolean showAsPopup() {
    return true;
  }

  protected class ShowPopupAction extends AnAction {

    protected final ActionGroup myGroup;

    protected ShowPopupAction(ActionGroup group) {
      super(ApplicationBundle.message("action.add"), null, DomCollectionControl.ADD_ICON);
      myGroup = group;
      setShortcutSet(shortcutSet);
    }

    public void actionPerformed(AnActionEvent e) {
      final ListPopup groupPopup =
        JBPopupFactory.getInstance().createActionGroupPopup(null,//J2EEBundle.message("label.menu.title.add.activation.config.property"),
                                                            myGroup, e.getDataContext(), JBPopupFactory.ActionSelectionAid.NUMBERING, true);

      showPopup(groupPopup, e);
    }
  }
}
