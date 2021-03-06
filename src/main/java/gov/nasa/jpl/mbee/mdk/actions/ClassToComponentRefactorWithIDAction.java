package gov.nasa.jpl.mbee.mdk.actions;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.openapi.uml.ReadOnlyElementException;
import com.nomagic.magicdraw.openapi.uml.SessionManager;
import com.nomagic.magicdraw.ui.browser.actions.DefaultBrowserAction;
import com.nomagic.magicdraw.uml.ConvertElementInfo;
import com.nomagic.magicdraw.uml.Refactoring;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.components.mdbasiccomponents.Component;
import gov.nasa.jpl.mbee.mdk.api.incubating.convert.Converters;
import gov.nasa.jpl.mbee.mdk.mms.sync.local.LocalDeltaProjectEventListenerAdapter;
import gov.nasa.jpl.mbee.mdk.mms.sync.local.LocalDeltaTransactionCommitListener;
import gov.nasa.jpl.mbee.mdk.util.Utils;

import java.awt.event.ActionEvent;
import java.util.Collection;

public class ClassToComponentRefactorWithIDAction extends DefaultBrowserAction {

    Collection<Element> elements;
    public static final String DEFAULT_ID = "ConvertClass";
    private static final long serialVersionUID = 1L;

    public ClassToComponentRefactorWithIDAction(Collection<Element> e) {
        super(DEFAULT_ID, "Convert Class To Component", null, null);
        this.elements = e;
    }

    public void actionPerformed(ActionEvent e) {
        Boolean con = Utils.getUserYesNoAnswer("Warning! Refactor with ID action is best used with an immediate commit/save"
                + "\nand no remote users on this project. Otherwise, data loss may \n"
                + "happen on remote update. Do you want to continue?");
        if (con == null || !con) {
            return;
        }
        LocalDeltaTransactionCommitListener listener = LocalDeltaProjectEventListenerAdapter.getProjectMapping(Application.getInstance().getProject()).getLocalDeltaTransactionCommitListener();
        if (listener != null) {
            listener.setDisabled(true);
        }

        SessionManager sessionManager = SessionManager.getInstance();
        sessionManager.createSession("Convert Class To Component");
        for (Element element : elements) {
            if (!(element instanceof Class)) {
                continue;
            }
            String elementID = Converters.getElementToIdConverter().apply(element);


            // Converts the element to an interface.
            ConvertElementInfo info = new ConvertElementInfo(Component.class);
            // Preserves the old element ID for the new element. this doesn't work, will throw md error if set to true
            info.setPreserveElementID(false);
            try {
                Element conversionTarget = Refactoring.Converting.convert(element, info);
                Application.getInstance().getProject().getCounter().setCanResetIDForObject(true);
                // element.setID(elementID);
                conversionTarget.setID(elementID);
                // String newElementID = conversionTarget.getID();
            } catch (ReadOnlyElementException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

        }
        sessionManager.closeSession();
        if (listener != null) {
            listener.setDisabled(false);
        }
    }

}
