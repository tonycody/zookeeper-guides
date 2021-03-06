package zx.soft.zk.manager.utils;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public enum ServletUtil {

	INSTANCE;
	private final static Logger logger = LoggerFactory.getLogger(ServletUtil.class);

	public void renderHtml(HttpServletRequest request, HttpServletResponse response, Map<String, Object> templateParam,
			String view) throws IOException, TemplateException {

		if (request != null && response != null && templateParam != null) {
			//There is no way to access session info in freemarker template. 
			//Hence all view rendering happens via this function which adds session info to attribute for each request.
			HttpSession session = request.getSession();
			if (session != null) {
				//Flash messages are always set at session level and need to be propgrated to attributes.
				//They are reset after being displayed once.
				if (session.getAttribute("flashMsg") != null) {
					templateParam.put("flashMsg", session.getAttribute("flashMsg"));
					session.setAttribute("flashMsg", null);
				}
				templateParam.put("authName", session.getAttribute("authName"));
				templateParam.put("authRole", session.getAttribute("authRole"));

				response.setContentType("text/html");
				Template template = null;

				Configuration config = new Configuration();
				config.setClassForTemplateLoading(request.getServletContext().getClass(), "/");
				template = config.getTemplate("/webapp/template/" + view);

				try (Writer out = new OutputStreamWriter(response.getOutputStream())) {
					template.process(templateParam, out);
					out.flush();
				}

			}
		}

	}

	public void renderError(HttpServletRequest request, HttpServletResponse response, String error) {
		try {
			logger.error("Error :" + error);
			Map<String, Object> templateParam = new HashMap<>();
			response.setContentType("text/html");
			Template template = null;
			Configuration config = new Configuration();
			config.setClassForTemplateLoading(request.getServletContext().getClass(), "/");
			template = config.getTemplate("/webapp/template/error.ftl.html");
			templateParam.put("error", error);
			try (Writer out = new OutputStreamWriter(response.getOutputStream())) {
				template.process(templateParam, out);
				out.flush();
			}
		} catch (TemplateException | IOException ex) {
			logger.error(ex.getMessage());
		}

	}

	public ZooKeeper getZookeeper(HttpServletRequest request, HttpServletResponse response, String zkServer) {
		try {

			HttpSession session = request.getSession();
			ZooKeeper zk = (ZooKeeper) session.getAttribute("zk");
			if (zk == null || zk.getState() != ZooKeeper.States.CONNECTED) {
				zk = ZooKeeperUtil.INSTANCE.createZKConnection(zkServer);
				if (zk.getState() != ZooKeeper.States.CONNECTED) {
					session.setAttribute("zk", null);
				} else {
					session.setAttribute("zk", zk);
				}

			}
			return zk;
		} catch (IOException | InterruptedException ex) {
			logger.error(ex.getMessage());
		}
		return null;
	}

	public String externalizeNodeValue(String value) {
		return value == null ? null : value.replaceAll("\\n", "\\\\n").replaceAll("\\r", "");
		// We might want to BASE64 encode it
	}

	//Using X-Forwarded-For to capture IP addresses coming via load balancer.
	public String getRemoteAddr(HttpServletRequest request) {
		String remoteAddr = request.getHeader("X-Forwarded-For");
		if (remoteAddr == null) {
			remoteAddr = request.getRemoteAddr();
		}
		return remoteAddr;
	}

}
