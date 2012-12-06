package com.cordys.eclipse.birt.connector;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.eclipse.birt.report.engine.api.EngineException;
import org.eclipse.birt.report.engine.api.HTMLRenderOption;
import org.eclipse.birt.report.engine.api.HTMLServerImageHandler;
import org.eclipse.birt.report.engine.api.IGetParameterDefinitionTask;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.IRunAndRenderTask;

import sun.misc.BASE64Encoder;

import com.cordys.coe.birtconnector.Messages;
import com.cordys.util.InstallationEnvironment;
import com.eibus.soap.ApplicationTransaction;
import com.eibus.soap.BodyBlock;
import com.eibus.soap.fault.Fault;
import com.eibus.util.logger.CordysLogger;
import com.eibus.xml.nom.Node;
import com.eibus.xml.xpath.XPath;

public class BIRTTransaction implements ApplicationTransaction {
	CordysLogger birtLogger = null;

	BIRTConnector connector = null;

	public BIRTTransaction(BIRTConnector connector) {
		this.connector = connector;
		this.birtLogger = BIRTConnector.birtLogger;
	}

	public void abort() {
	}

	public boolean canProcess(String implemantationLanguage) {
		if (this.birtLogger.isDebugEnabled()) {
			this.birtLogger.debug("canProcess(String) - start");
		}
		boolean returnboolean = "BIRT".equals(implemantationLanguage);
		return returnboolean;
	}

	public void commit() {
	}

	@SuppressWarnings("deprecation")
	public boolean process(BodyBlock requestBlock, BodyBlock responseBlock) {
		if (this.birtLogger.isDebugEnabled()) {
			this.birtLogger.debug("process(BodyBlock, BodyBlock) - start");
		}
		boolean returnValue = true;
		String methodName = requestBlock.getMethodDefinition().getMethodName();
		String reqOrg = requestBlock.getSOAPTransaction().getIdentity().getUserOrganization();
		File reportWebDir = InstallationEnvironment.getAbsoluteOrganizationWebRoot(reqOrg);
		if (!reportWebDir.exists()) {
			if (!reportWebDir.mkdirs()) {
				responseBlock.createSOAPFault(Fault.Codes.SERVER, Messages.unexpected);
				return true;
			}
		}
		if (this.birtLogger.isInfoEnabled()) {
			this.birtLogger.info(Messages.requestedMethod, methodName);
		}

		int requestNode = requestBlock.getXMLNode();
		int responseNode = responseBlock.getXMLNode();
		this.birtLogger.debug(Node.writeToString(requestNode, true));
		try {
			if ("GetReport".equals(methodName)) {
				this.birtLogger.debug("Inside get reports");

				String rptdesign = Node
						.getDataWithDefault(XPath.getFirstMatch(
								".//ReportName", null, requestNode), "");
				if (rptdesign.indexOf(".rptdesign") == -1)
					rptdesign = rptdesign + ".rptdesign";

				String reportName = this.connector.birt_repository + "/"
						+ rptdesign;
				String outputFormat = Node.getDataWithDefault(XPath
						.getFirstMatch(".//OutputFormat", null, requestNode),
						"html");
				String embeddable = Node
						.getDataWithDefault(XPath.getFirstMatch(
								".//Embeddable", null, requestNode), "true");
				String outputToFile = Node.getDataWithDefault(XPath
						.getFirstMatch(".//OutputToFile", null, requestNode),
						"false");
				String encodeFile = Node
						.getDataWithDefault(XPath.getFirstMatch(
								".//EncodeFile", null, requestNode), "false");
				String SAMLart = Node.getAttribute(requestNode, "SAMLart");

				if (this.birtLogger.isInfoEnabled())
					this.birtLogger.info(Messages.requestInfo, reportName,
							outputFormat, embeddable, outputToFile, encodeFile,
							SAMLart);

				File reportFile = new File(reportName);
				if (!reportFile.exists()) {
					responseBlock.createSOAPFault(
							Fault.Codes.SERVER, Messages.requestedFileNotFound,
							rptdesign);
					responseBlock.abortTransaction();
					return true;
				}

				IReportEngine engine = this.connector.getEngine();
				IReportRunnable design = engine.openReportDesign(reportName);
				IRunAndRenderTask task = engine.createRunAndRenderTask(design);

				IGetParameterDefinitionTask param = engine
						.createGetParameterDefinitionTask(design);
				param.evaluateDefaults();

				int[] paramNodes = XPath.getMatchingNodes(".//PARAM", null,
						requestNode);

				if (paramNodes != null) {
					for (int i = 0; i < paramNodes.length; i++) {
						String paramName = Node.getDataWithDefault(XPath
								.getFirstMatch(".//Name", null, paramNodes[i]),
								"");
						this.birtLogger.debug("paramName - " + paramName);
						String paramType = Node.getDataWithDefault(XPath
								.getFirstMatch(".//Type", null, paramNodes[i]),
								"");
						Object paramValue = null;
						if (paramType.equalsIgnoreCase("date")) {
							SimpleDateFormat sdf = new SimpleDateFormat(
									"mm/dd/yyyy");
							try {
								paramValue = sdf.parse(Node.getDataWithDefault(
										XPath.getFirstMatch(".//Value", null,
												paramNodes[i]), ""));
							} catch (ParseException e) {
								e.printStackTrace();
							}
						} else {
							paramValue = Node.getDataWithDefault(XPath
									.getFirstMatch(".//Value", null,
											paramNodes[i]), "");
						}
						this.birtLogger.debug("paramValue - " + paramValue);
						task.setParameterValue(paramName, paramValue);
					}
				}

				String soapEndPointValue = "http://"
						+ BIRTConnector.cordys_server
						+ "/cordys/com.eibus.web.soap.Gateway.wcp?organization="
						+ reqOrg;

				if ((SAMLart != null) && (!SAMLart.equals(""))) {
					soapEndPointValue = soapEndPointValue + "&SAMLart="
							+ SAMLart;
				}
				task.setParameterValue("SOAP_END_POINT", soapEndPointValue);

				HTMLRenderOption options = new HTMLRenderOption();
				options.setEmbeddable(Boolean.parseBoolean(embeddable));
				options.setBaseImageURL("../images");
				options.setImageDirectory(reportWebDir.getAbsolutePath() + "/birt/reports/images");
				options.setSupportedImageFormats("PNG;JPG;BMP");
				options.setImageHandler(new HTMLServerImageHandler());

				ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();

				options.setActionHandler(new HyperlinkHandler(outputFormat,
						reportFile, embeddable, outputToFile, encodeFile,
						SAMLart, reqOrg));
				String fileName = Long.toString(System.nanoTime());
				fileName = fileName + "." + outputFormat;
				final String reportOutputFile = reportWebDir.getAbsolutePath()
						+ "/birt/reports/reportFiles/" + fileName;
				if (Boolean.parseBoolean(outputToFile)) {
					options.setOutputFileName(reportOutputFile);
				} else {
					options.setOutputStream(byteOutput);
				}
				options.setOutputFormat(outputFormat);
				task.setRenderOption(options);
				task.run();

				if (Boolean.parseBoolean(outputToFile)) {
					Node.createCDataElement("HyperLink",
							"/home/" + reqOrg.split(",")[0].split("=")[1] + "/birt/reports/reportFiles/" + fileName,
							responseNode);
					Node.createCDataElement("PhysicalLink",
							reportOutputFile, responseNode);
				} else {
					Node.setDataElement(
							responseNode,
							"REPORT_CONTENT",
							Boolean.parseBoolean(encodeFile) ? new BASE64Encoder()
									.encode(byteOutput.toByteArray())
									: byteOutput.toString());
				}
			}
		} catch (EngineException e) {
			Fault fault = responseBlock.createSOAPFault(Fault.Codes.SERVER, Messages.unexpected);
			fault.getDetail().addDetailEntry(e);
			responseBlock.abortTransaction();
		}

		return returnValue;
	}
}