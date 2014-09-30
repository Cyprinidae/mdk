package gov.nasa.jpl.mbee.ems.sync;

import gov.nasa.jpl.mbee.ems.ExportUtility;
import gov.nasa.jpl.mbee.ems.ImportUtility;

import java.util.Map;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.openapi.uml.SessionManager;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;

public class JMSMessageListener implements MessageListener {

	private Project project;

	public JMSMessageListener(Project project) {
		this.project = project;
	}

	@Override
	public void onMessage(Message msg) {
		try {
			// Take the incoming message and parse it into a
			// JSONObject.
			//
			TextMessage message = (TextMessage) msg;
			JSONObject ob = (JSONObject) JSONValue.parse(message.getText());

			// Changed element are encapsulated in the "workspace2"
			// JSONObject.
			//
			JSONObject ws2 = (JSONObject) ob.get("workspace2");

			// Retrieve the changed elements: each type of change (updated,
			// added, moved, deleted)
			// will be returned as an JSONArray.
			//
			final JSONArray updated = (JSONArray) ws2.get("updatedElements");
			final JSONArray added = (JSONArray) ws2.get("addedElements");
			final JSONArray deleted = (JSONArray) ws2.get("deletedElements");
			final JSONArray moved = (JSONArray) ws2.get("movedElements");

			Runnable runnable = new Runnable() {
				public void run() {
					Map<String, ?> projectInstances = ProjectListenerMapping.getInstance().get(project);
					AutoSyncCommitListener listener = (AutoSyncCommitListener) projectInstances
							.get("AutoSyncCommitListener");

					// Disable the listener so we do not react to the
					// changes we are importing from MMS.
					//
					if (listener != null)
						listener.disable();

					SessionManager sm = SessionManager.getInstance();
					sm.createSession("mms sync change");
					try {
						// Loop through each specified element.
						//
						for (Object element : updated) {
							makeChange((JSONObject) element);
						}
						for (Object element : added) {
							addElement((JSONObject) element);
						}
						for (Object element : deleted) {
							deleteElement((JSONObject) element);
						}
						for (Object element : moved) {
							moveElement((JSONObject) element);
						}
						sm.closeSession();
					}
					catch (Exception e) {
						sm.cancelSession();
					}

					// Once we've completed make all the
					// changes, enable the listener.
					//
					if (listener != null)
						listener.enable();
				}

				private void makeChange(JSONObject ob) {
					Element changedElement = ExportUtility.getElementFromID((String) (ob).get("sysmlid"));
					if (changedElement == null) {
						Application.getInstance().getGUILog().log("element not found from mms sync change");
						return;
					}
					else if (!changedElement.isEditable()) {
						Application.getInstance().getGUILog()
								.log("[ERROR] " + changedElement.getID() + " is not editable!");
						return;
					}
					ImportUtility.updateElement(changedElement, ob);
				}

				private void addElement(JSONObject ob) {
					ImportUtility.createElement(ob);
				}

				private void deleteElement(JSONObject ob) {
					Element changedElement = ExportUtility.getElementFromID((String) (ob).get("sysmlid"));
					if (changedElement == null) {
						Application.getInstance().getGUILog().log("element not found from mms sync delete");
						return;
					}
					// modelelementsmanager util functions.
					//
					project.removeElementByID(changedElement);
				}

				private void moveElement(JSONObject ob) {
					Element changedElement = ExportUtility.getElementFromID((String) (ob).get("sysmlid"));
					if (changedElement == null) {
						Application.getInstance().getGUILog().log("element not found from mms sync move");
						return;
					}
					ImportUtility.setOwner(changedElement, ob);
				}
			};
			project.getRepository().invokeAfterTransaction(runnable);
			message.acknowledge();

		}
		catch (Exception e) {

		}
	}
}