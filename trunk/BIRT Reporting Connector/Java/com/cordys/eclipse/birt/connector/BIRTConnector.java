package com.cordys.eclipse.birt.connector;

/**
 * @author senthil.kumar
 * 
 */

import java.net.InetAddress;
import java.util.logging.Level;

import org.eclipse.birt.core.framework.Platform;
import org.eclipse.birt.report.engine.api.EngineConfig;
import org.eclipse.birt.report.engine.api.HTMLRenderOption;
import org.eclipse.birt.report.engine.api.HTMLServerImageHandler;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportEngineFactory;
import org.eclipse.core.internal.registry.RegistryProviderFactory;

import com.cordys.coe.birtconnector.Messages;
import com.eibus.soap.ApplicationConnector;
import com.eibus.soap.ApplicationTransaction;
import com.eibus.soap.Processor;
import com.eibus.soap.SOAPTransaction;
import com.eibus.util.logger.CordysLogger;
import com.eibus.util.logger.Severity;
import com.eibus.util.system.EIBProperties;
import com.eibus.xml.nom.Node;
import com.eibus.xml.xpath.XPath;

public class BIRTConnector extends ApplicationConnector {
	public static CordysLogger birtLogger = CordysLogger
			.getCordysLogger(BIRTConnector.class);
		
	IReportEngine engine = null;
	String engine_home = "";
	String birt_repository = "";
	String engine_log_folder = "";
	String report_web_folder = "";
	static String cordys_server = "";

	@SuppressWarnings("unchecked")
	public void open(Processor processor) {
		if (birtLogger.isDebugEnabled()) {
			birtLogger.debug("Processor start");
		}
		try {
			ReadConfiguration(processor);
			SetupReportFolders();
			EngineConfig config = new EngineConfig();

			config.setLogConfig(this.engine_log_folder, Level.FINE);
			
			HTMLRenderOption renderOption = new HTMLRenderOption();
		    HTMLServerImageHandler imageHandler = new HTMLServerImageHandler();
		    renderOption.setImageHandler(imageHandler);
		    renderOption.setEmbeddable(true);
		    config.getEmitterConfigs().put("html", renderOption);

			Platform.startup(config);
			
			IReportEngineFactory factory = (IReportEngineFactory) Platform
					.createFactoryObject(IReportEngineFactory.EXTENSION_REPORT_ENGINE_FACTORY);
			
			this.engine = factory.createReportEngine(config);
			this.engine.changeLogLevel(Level.FINE);
		} catch (Exception e) {
			birtLogger.log(Severity.ERROR, e, Messages.startError);
			close(processor);
			e.printStackTrace();
		}
	}

	public void reset(Processor processor) {
		if (birtLogger.isDebugEnabled()) {
			birtLogger.debug("Processor reset");
		}
		super.reset(processor);
	}

	public void close(Processor processor) {
		if (birtLogger.isDebugEnabled()) {
			birtLogger.debug("Processor stop");
		}
		this.engine.destroy();
		Platform.shutdown();
		RegistryProviderFactory.releaseDefault();
		super.close(processor);
	}

	public ApplicationTransaction createTransaction(
			SOAPTransaction soapTransaction) {
		return new BIRTTransaction(this);
	}

	private void ReadConfiguration(Processor processor) throws Exception {
		if (birtLogger.isDebugEnabled()) {
			birtLogger.debug("Reading Configuration");
		}

		int configurationode = processor.getProcessorConfigurationNode();
		if (birtLogger.isDebugEnabled()) {
			birtLogger.debug(Node.writeToString(configurationode, true));
		}
		this.engine_home = Node.getDataWithDefault(XPath.getFirstMatch(
				".//engine_home", null, configurationode), "");
		this.birt_repository = Node.getDataWithDefault(XPath.getFirstMatch(
				".//birt_repository", null, configurationode), "");
		this.engine_log_folder = Node.getDataWithDefault(XPath.getFirstMatch(
				".//engine_log_folder", null, configurationode),
				EIBProperties.getInstallDir() + "/logs/birt");
		cordys_server = InetAddress.getLocalHost().getHostName() + ":"
				+ EIBProperties.getProperty("web.server.portnumber");
	}

	private void SetupReportFolders() {
		this.report_web_folder = (EIBProperties.getInstallDir() + "/web/birt/reports/");
	}

	public IReportEngine getEngine() {
		return this.engine;
	}

	public void setEngine(IReportEngine engine) {
		this.engine = engine;
	}

	public String getReport_web_folder() {
		return this.report_web_folder;
	}

	public void setReport_web_folder(String report_web_folder) {
		this.report_web_folder = report_web_folder;
	}
	
	
}