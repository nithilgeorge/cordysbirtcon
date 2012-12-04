package com.cordys.eclipse.birt.connector;

import com.eibus.util.logger.CordysLogger;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.eclipse.birt.report.engine.api.HTMLActionHandler;
import org.eclipse.birt.report.engine.api.IAction;

public class HyperlinkHandler extends HTMLActionHandler {
	String outputFormat = "";
	String embeddable = "false";
	String outputToFile = "true";
	String encodeFile = "false";
	String orgDN = "";
	String SAMLart = "";
	File reportFile;
	public static CordysLogger birtLogger = CordysLogger
			.getCordysLogger(HyperlinkHandler.class);

	public HyperlinkHandler(String p_OutputFormat, File p_ReportFile,
			String p_OrgDN) {
		this.outputFormat = p_OutputFormat;
		this.reportFile = p_ReportFile;
		this.orgDN = p_OrgDN;
	}

	public HyperlinkHandler(String p_OutputFormat, File p_ReportFile,
			String p_Embeddable, String p_OutputToFile, String p_EncodeFile,
			String p_SAMLart, String p_OrgDN) {
		this.outputFormat = p_OutputFormat;
		this.reportFile = p_ReportFile;
		this.orgDN = p_OrgDN;

		this.embeddable = p_Embeddable;
		this.outputToFile = p_OutputToFile;
		this.encodeFile = p_EncodeFile;
		this.SAMLart = p_SAMLart;
	}

	@SuppressWarnings("rawtypes")
	protected String buildDrillAction(IAction action, Object context) {
		Map map = action.getParameterBindings();
		String reportName = action.getReportName();

		String url = "http://" + BIRTConnector.cordys_server
				+ "/cordys/birt/reports/viewreport.htm?";
		url = url + "ReportName=" + reportName;
		url = url + "&OutputFormat=" + this.outputFormat;
		url = url + "&Embeddable=" + this.embeddable;
		url = url + "&OutputToFile=" + this.outputToFile;
		url = url + "&EncodeFile=" + this.encodeFile;
		String params = "";
		if (map != null) {
			Set keys = map.keySet();
			if (keys != null) {
				Iterator iter_Keys = keys.iterator();
				while (iter_Keys.hasNext()) {
					String key = (String) iter_Keys.next();
					ArrayList keyArrayList = (ArrayList) map.get(key);
					String value = (String) keyArrayList.get(0);
					params = params + "&p_" + key + "=" + value;
				}
			}
		}
		url = url + params;
		url = url + "&org=" + this.orgDN.substring(2, this.orgDN.indexOf(','));
		if ((this.SAMLart != null) && (!this.SAMLart.equals("")))
			url = url + "&SAMLart=" + this.SAMLart;
		return url;
	}
}