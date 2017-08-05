package com.redhat.lightblue.rest.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * {@link Filter} implementation which captures request information and a
 * breakdown of the response codes being returned.
 */
public abstract class AbstractInstrumentedFilter implements Filter {
	private final Map<Integer, String> meterNamesByStatusCode;
	private final String registryAttribute;
	private final String api_prefix = "api";

	// initialized after each api call
	private ConcurrentMap<Integer, Meter> metersByStatusCode;
	private Meter timeoutsMeter;
	private Meter errorsMeter;
	private Counter activeRequests;
	private Timer requestTimer;

	private MetricRegistry metricsRegistry;

	/**
	 * Creates a new instance of the filter.
	 *
	 * @param registryAttribute
	 *            the attribute used to look up the metrics registry in the
	 *            servlet context
	 * @param meterNamesByStatusCode
	 *            A map, keyed by status code, of meter names that we are
	 *            interested in.
	 */
	protected AbstractInstrumentedFilter(String registryAttribute, Map<Integer, String> meterNamesByStatusCode) {
		this.registryAttribute = registryAttribute;
		this.meterNamesByStatusCode = meterNamesByStatusCode;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		this.metricsRegistry = getMetricsFactory(filterConfig);
	}

	/**
	 * Initializes metrics for each api call with the uri namespace
	 * 
	 * @param filterConfig
	 * @param merticNamespace
	 */
	private void initializeMetrics(String merticNamespace) {
		this.metersByStatusCode = new ConcurrentHashMap<Integer, Meter>(meterNamesByStatusCode.size());
		for (Entry<Integer, String> entry : meterNamesByStatusCode.entrySet()) {
			metersByStatusCode.put(entry.getKey(), metricsRegistry.meter(name(merticNamespace, entry.getValue())));
		}
		this.timeoutsMeter = metricsRegistry.meter(name(merticNamespace, "timeouts"));
		this.errorsMeter = metricsRegistry.meter(name(merticNamespace, "errors"));
		this.activeRequests = metricsRegistry.counter(name(merticNamespace, "activeRequests"));
		this.requestTimer = metricsRegistry.timer(name(merticNamespace, "requests"));
	}

	/**
	 * @param filterConfig
	 * @return MetricRegistry
	 */
	public MetricRegistry getMetricsFactory(FilterConfig filterConfig) {
		final MetricRegistry metricsRegistry;

		final Object o = filterConfig.getServletContext().getAttribute(this.registryAttribute);
		if (o instanceof MetricRegistry) {
			metricsRegistry = (MetricRegistry) o;
		} else {
			metricsRegistry = new MetricRegistry();
		}
		return metricsRegistry;
	}

	/**
	 * @param request
	 * @return String
	 */
	public String getCurrentUrlFromRequest(ServletRequest request) {
		if (!(request instanceof HttpServletRequest))
			return null;

		return ((HttpServletRequest) request).getRequestURI().toString();
	}

	/**
	 * @param request
	 * @return boolean
	 */
	public boolean checkIfStreamingResponse(ServletRequest request) {
		boolean isStreamingResponse = false;
		if (!(request instanceof HttpServletRequest))
			return false;
		else {
			String stream = ((HttpServletRequest) request).getParameter("stream");
			if (null != stream && !"".equalsIgnoreCase(stream)) {
				isStreamingResponse = Boolean.valueOf(stream);
			}
		}
		return isStreamingResponse;
	}

	/**
	 * 
	 * @param wrappedResponse
	 * @return boolean
	 */
	public boolean checkIfStreamEnding(WrappedServletResponse wrappedResponse) {
		boolean isStreamEnding = false;
		try {
			byte[] response = wrappedResponse.getResponseCopy();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode jsonObj = mapper.readValue(new String(response, wrappedResponse.getCharacterEncoding()),
					JsonNode.class);
			JsonNode lastNode = jsonObj.get("last");
			if (null != lastNode && lastNode.booleanValue()) {
				isStreamEnding = true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return isStreamEnding;
	}

	@Override
	public void destroy() {

	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		final WrappedServletResponse wrappedResponse = new WrappedServletResponse((HttpServletResponse) response);

		String requrestUrl = getCurrentUrlFromRequest(request);
		String metricNamespace = api_prefix + requrestUrl.replaceAll("/", ".");
		initializeMetrics(metricNamespace);

		activeRequests.inc();
		final Timer.Context context = requestTimer.time();
		boolean error = false;
		try {
			chain.doFilter(request, wrappedResponse);
			wrappedResponse.flushBuffer();
		} catch (IOException | ServletException | RuntimeException e) {
			error = true;
			throw e;
		} finally {
			if (!error && request.isAsyncStarted()) {
				request.getAsyncContext().addListener(new AsyncResultListener(context));
			} else {
				if (checkIfStreamingResponse(request) && !checkIfStreamEnding(wrappedResponse)) {
					// do nothing if there are more response chunks to be
					// processed for a request resulting in streaming response
				} else {
					context.stop();
					activeRequests.dec();
					if (error) {
						errorsMeter.mark();
					} else {
						markMeterForStatusCode(wrappedResponse.getStatus());
					}
				}
			}
		}
	}

	private void markMeterForStatusCode(int status) {
		final Meter metric = metersByStatusCode.get(status);
		if (metric != null) {
			metric.mark();
		}
	}

	private static class CopyServletOutputStream extends ServletOutputStream {
		private OutputStream outputStream;
		private ByteArrayOutputStream baos;

		public CopyServletOutputStream(OutputStream outputStream) {
			this.outputStream = outputStream;
			this.baos = new ByteArrayOutputStream(1024);
		}

		@Override
		public void write(int b) throws IOException {
			outputStream.write(b);
			baos.write(b);
		}

		public byte[] getResponseCopy() {
			return baos.toByteArray();
		}

		@Override
		public boolean isReady() {
			return false;
		}

		@Override
		public void setWriteListener(WriteListener writeListener) {
		}

	}

	private static class WrappedServletResponse extends HttpServletResponseWrapper {
		// The Servlet spec says: calling setStatus is optional, if no status is
		// set, the default is 200.
		private int httpStatus = 200;
		private ServletOutputStream servletOutputStream;
		private PrintWriter printWriter;
		private CopyServletOutputStream copyServletOuptputStream;

		public WrappedServletResponse(HttpServletResponse response) {
			super(response);
		}

		@Override
		public void sendError(int sc) throws IOException {
			httpStatus = sc;
			super.sendError(sc);
		}

		@Override
		public void sendError(int sc, String msg) throws IOException {
			httpStatus = sc;
			super.sendError(sc, msg);
		}

		@Override
		public void setStatus(int sc) {
			httpStatus = sc;
			super.setStatus(sc);
		}

		@SuppressWarnings("deprecation")
		@Override
		public void setStatus(int sc, String sm) {
			httpStatus = sc;
			super.setStatus(sc, sm);
		}

		public int getStatus() {
			return httpStatus;
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			if (servletOutputStream == null) {
				servletOutputStream = getResponse().getOutputStream();
				copyServletOuptputStream = new CopyServletOutputStream(servletOutputStream);
			}
			return copyServletOuptputStream;
		}

		@Override
		public PrintWriter getWriter() throws IOException {
			if (printWriter == null) {
				copyServletOuptputStream = new CopyServletOutputStream(getResponse().getOutputStream());
				printWriter = new PrintWriter(
						new OutputStreamWriter(copyServletOuptputStream, getResponse().getCharacterEncoding()), true);
			}
			return printWriter;
		}

		@Override
		public void flushBuffer() throws IOException {
			if (printWriter != null) {
				printWriter.flush();
			} else if (servletOutputStream != null) {
				copyServletOuptputStream.flush();
			}
		}

		public byte[] getResponseCopy() {
			if (copyServletOuptputStream != null) {
				return copyServletOuptputStream.getResponseCopy();
			} else {
				return new byte[0];
			}
		}
	}

	private class AsyncResultListener implements AsyncListener {
		private Timer.Context context;
		private boolean done = false;

		public AsyncResultListener(Timer.Context context) {
			this.context = context;
		}

		@Override
		public void onComplete(AsyncEvent event) throws IOException {
			if (!done) {
				HttpServletResponse suppliedResponse = (HttpServletResponse) event.getSuppliedResponse();
				context.stop();
				activeRequests.dec();
				markMeterForStatusCode(suppliedResponse.getStatus());
			}
		}

		@Override
		public void onTimeout(AsyncEvent event) throws IOException {
			context.stop();
			activeRequests.dec();
			timeoutsMeter.mark();
			done = true;
		}

		@Override
		public void onError(AsyncEvent event) throws IOException {
			context.stop();
			activeRequests.dec();
			errorsMeter.mark();
			done = true;
		}

		@Override
		public void onStartAsync(AsyncEvent event) throws IOException {

		}
	}
}
