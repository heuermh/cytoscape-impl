package org.cytoscape.app.internal.net.server;

import org.apache.http.HttpStatus;

public class ScreenOriginsBeforeResponse implements CyHttpBeforeResponse
{
    static final CyHttpResponseFactory responseFactory = new CyHttpResponseFactoryImpl();
    final String[] allowedOrigins;

    /**
     * @param allowedOrigins 
     */
    public ScreenOriginsBeforeResponse(final String... allowedOrigins)
    {
        this.allowedOrigins = allowedOrigins;
    }

    public CyHttpResponse intercept(CyHttpRequest request)
    {
        final String origin = request.getHeaders().get("Origin");
        if (origin == null)
            return responseFactory.createHttpResponse(HttpStatus.SC_FORBIDDEN, "<html><body><h1>403 Forbidden</h1>No <tt>Origin</tt> header specified.</body></html>", "text/html");

        for (final String allowedOrigin : allowedOrigins)
        {
            if (allowedOrigin.equals(origin))
                return null;
        }

        return responseFactory.createHttpResponse(HttpStatus.SC_FORBIDDEN, String.format("<html><body><h1>403 Forbidden</h1><tt>Origin: %s</tt> not allowed.</body></html>", origin), "text/html");
    }
}
