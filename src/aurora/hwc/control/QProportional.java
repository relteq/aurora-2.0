/**
 * @(#)QProportional.java
 */

package aurora.hwc.control;

import java.io.*;
import java.text.NumberFormat;
import org.w3c.dom.*;

import aurora.*;
import aurora.hwc.*;


/**
 * Implementation of proportional queue controller.
 * @author Alex Kurzhanskiy
 * @version $Id: QProportional.java 44 2010-02-18 20:05:29Z akurzhan $
 */
public final class QProportional extends AbstractQueueController implements Serializable, Cloneable {
	private static final long serialVersionUID = 1907427195234602994L;

	public double kp = 1.0;
	
	
	public QProportional() { }
	public QProportional(double kp) { this.kp = kp; }
	
	
	/**
	 * Initializes the proportional queue controller from given DOM structure.
	 * @param p DOM node.
	 * @return <code>true</code> if operation succeeded, <code>false</code> - otherwise.
	 * @throws ExceptionConfiguration
	 */
	public boolean initFromDOM(Node p) throws ExceptionConfiguration {
		boolean res = super.initFromDOM(p);
		if (p == null)
			return !res;
		try  {
			if (p.hasChildNodes()) {
				NodeList pp = p.getChildNodes();
				for (int i = 0; i < pp.getLength(); i++) {
					if (pp.item(i).getNodeName().equals("parameter"))
						if (pp.item(i).getAttributes().getNamedItem("name").getNodeValue().equals("kp")) 
							kp = Double.parseDouble(pp.item(i).getAttributes().getNamedItem("value").getNodeValue());
					if (pp.item(i).getNodeName().equals("parameters"))
						if (pp.item(i).hasChildNodes()) {
							NodeList pp2 = pp.item(i).getChildNodes();
							for (int j = 0; j < pp2.getLength(); j++) {
								if (pp2.item(j).getNodeName().equals("parameter"))
									if (pp2.item(j).getAttributes().getNamedItem("name").getNodeValue().equals("kp")) 
										kp = Double.parseDouble(pp2.item(j).getAttributes().getNamedItem("value").getNodeValue());
							}
						}
				}
			}
			else
				res = false;
		}
		catch(Exception e) {
			res = false;
			throw new ExceptionConfiguration(e.getMessage());
		}
		return res;
	}
	
	/**
	 * Generates XML description of the Proportional Queue controller.<br>
	 * If the print stream is specified, then XML buffer is written to the stream.
	 * @param out print stream.
	 * @throws IOException
	 */
	public void xmlDump(PrintStream out) throws IOException {
		super.xmlDump(out);
		out.print("<parameters><parameter name=\"kp\" value=\"" + Double.toString(kp) + "\"/></parameters>");
		out.print("</qcontroller>");
		return;
	}
	
	/**
	 * Computes desired input Link flow.<br>
	 * The flow value depends on the queue size and queue limit.
	 * @param nd Node to which the controller belongs.
	 * @param lk input Link.
	 * @return desired flow.  
	 */
	public Object computeInput(AbstractNodeHWC nd, AbstractLinkHWC lk) {
		double flw = 0.0;
		if (usesensors) {
			inOverride = queuesensor.Density() > lk.getCriticalDensity();
		}
		else {
			if (lk.getPredecessors().size() == 0)
				inOverride = ((AuroraIntervalVector)lk.getQueue()).sum().getCenter() > lk.getQueueMax();
			else
				inOverride = ((((AuroraIntervalVector)lk.getDensity()).sum().getCenter()) * lk.getLength()) > lk.getQueueMax();
		}
		double qmax = lk.getQueueMax();
		if (inOverride) {
			if (lk.getPredecessors().size() == 0) {
				double q = ((AuroraIntervalVector)lk.getQueue()).sum().getCenter();
				flw = ((AuroraIntervalVector)lk.getDemandValue2()).sum().getCenter();
				flw -= (q / lk.getMyNetwork().getTP());
				flw += ((kp / lk.getMyNetwork().getTP()) * (q - qmax));
			}
			else {
				double dq = (((AuroraIntervalVector)lk.getDensity()).sum().getCenter()) * lk.getLength() - qmax;
				double dm = ((AuroraIntervalVector)lk.getBeginNode().getOutputs().get(lk.getBeginNode().getSuccessors().indexOf(lk))).sum().getCenter();
				flw = dm + ((kp / nd.getMyNetwork().getTP()) * dq);
			}
		}
		return (Double)Math.max(flw, 0.0);
	}
	
	/**
	 * Returns controller description.
	 */
	public String getDescription() {
		NumberFormat form = NumberFormat.getInstance();
		form.setMinimumFractionDigits(2);
		form.setMaximumFractionDigits(2);
		return "Proportional (Kp = " + form.format(kp) + ")";
	}
	
	/**
	 * Implementation of a "deep copy" of the object.
	 */
	public AbstractQueueController deepCopy() {
		QProportional qcCopy = null;
		try {
			qcCopy = (QProportional)clone();
		}
		catch(Exception e) { }
		return qcCopy;
	}
	
	/**
	 * Returns letter code of the queue controller type.
	 */
	public final String getTypeLetterCode() {
		return "PROPORTIONAL";
	}
	
	/**
	 * Overrides <code>java.lang.Object.toString()</code>.
	 * @return string that describes the Controller.
	 */
	public String toString() {
		return "Proportional";
	}

}
