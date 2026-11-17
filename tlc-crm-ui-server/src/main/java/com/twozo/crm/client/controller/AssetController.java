package com.twozo.crm.client.controller;

import com.twozo.crm.client.models.Asset;
import com.twozo.crm.client.models.SecuredContent;
import com.twozo.crm.client.service.AssetService;
import com.twozo.crm.client.service.CSPService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Controller
@RequestMapping
public class AssetController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssetController.class);
    private static final String ASSET_PATTERN = "(?:.*\\/)?([^\\/]+\\.(txt|html|jpg|png|json|ico|js|woff2|css|svg|mp3))$";
    private static final String DOT_PREFIXED_PATH = "/.";
    private static final Collection<String> RESTRICTED_PATHS;
    private static final Collection<String> RESTRICTED_REQUEST_METHODS;
    // Define regex patterns for blocking paths
    private static final Pattern SENSITIVE_FILES = Pattern.compile(
            ".*/\\.(git|svn|hg|bzr|darcs|BitKeeper|env|htaccess|htpasswd|lock|DS_Store|terraform|circleci|gitlab-ci).*"
    );

    private static final Pattern BACKUP_LOG_TEMP_FILES = Pattern.compile(
            ".*\\.(bak|tmp|swp|old|save|log)$"
    );

    private static final Pattern KEY_CERT_FILES = Pattern.compile(
            ".*\\.(crt|key|pem|pfx|p12)$"
    );

    private static final Pattern DATABASE_DUMP_FILES = Pattern.compile(
            ".*\\.(sql|sqlite|db)$"
    );

    private static final Pattern CICD_FILES = Pattern.compile(
            ".*(BitKeeper|Dockerfile|docker-compose\\.yml|\\.circleci|\\.gitlab-ci\\.yml).*"
    );

    private static final Pattern CONFIG_FILES = Pattern.compile(
            ".*\\.(yml|yaml|properties|config|xml)$"
    );

    static {
        RESTRICTED_PATHS = Set.of("/sitemap.xml");
        RESTRICTED_REQUEST_METHODS = Set.of(RequestMethod.TRACE.name(), RequestMethod.OPTIONS.name(), "TRACK");
    }

    @Autowired
    private AssetService assetService;
    @Autowired
    private CSPService cspService;
    @Value("${twozo.client.index-file}")
    private String indexFileName;

//    @GetMapping(path = "/**", produces = "text/html")
//    public void serveIndex(final HttpServletResponse response) {
//        try {
//
//            response.setHeader("Content-Security-Policy", cspService.getCspHeader());
//            response.setContentType("text/html");
//            response.setCharacterEncoding("UTF-8");
//            response.getWriter().write(cspService.getIndexPage());
//        } catch (Exception e) {
//            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//            e.printStackTrace();
//        }
//    }

//    @GetMapping(path = "/static/css/**", produces = "text/css")
//    public ResponseEntity<byte[]> serveStyleSheets(final HttpServletRequest request) {
//        try {
//            String requestPath = request.getRequestURI();
//
//            return new ResponseEntity<>(assetService.getAsset(requestPath).data(), HttpStatus.OK);
//        } catch (Exception e) {
//            return new ResponseEntity<>(HttpStatusCode.valueOf(500));
//        }
//    }
//
//    @GetMapping(path = "/static/js/**", produces = "application/javascript")
//    public ResponseEntity<byte[]> serveScriptFiles(final HttpServletRequest request) {
//        try {
//            return new ResponseEntity<>(assetService.getAsset(request.getRequestURI()).data(), HttpStatus.OK);
//        } catch (Exception e) {
//            return new ResponseEntity<>(HttpStatusCode.valueOf(500));
//        }
//    }

    @GetMapping(path = "/manifest.json")
    public ResponseEntity<byte[]> getManifestJson(final HttpServletRequest request) {
        return serveSecurelyCachableAsset(request);
    }

    @GetMapping(path = "/robots.txt")
    public ResponseEntity<byte[]> getRobotsText(final HttpServletRequest request) {
        return serveSecurelyCachableAsset(request);
    }

    @GetMapping(path = "/latest/meta-data/**")
    public ResponseEntity<byte[]> validateLatestPath(final HttpServletRequest request) {
        final HttpHeaders headers = new HttpHeaders();

        loadDefaultHeaders(headers, getDomain(request));
        loadNoStoreCacheControlHeader(headers);

        return new ResponseEntity<>(headers, HttpStatusCode.valueOf(403));
    }

    @GetMapping(path = "/static/**")
    public ResponseEntity<byte[]> validateStaticPath(final HttpServletRequest request) {
        final String path = request.getRequestURI(); // Get the actual request path

        // Check if the path ends with a resource identifier (e.g., .js, .css, etc.)
        if (path.matches(ASSET_PATTERN)) {
            return serveAsset(request);
        } else {
            final HttpHeaders headers = new HttpHeaders();

            loadDefaultHeaders(headers, getDomain(request));
            loadNoStoreCacheControlHeader(headers);
            return new ResponseEntity<>(headers, HttpStatusCode.valueOf(403));
        }
    }

    @GetMapping(path = "/**")
    public ResponseEntity<byte[]> serveAsset(final HttpServletRequest request) {

        try {
            final String requestMethod = request.getMethod();
            final HttpHeaders headers = new HttpHeaders();

            loadDefaultHeaders(headers, getDomain(request));

            if (RESTRICTED_REQUEST_METHODS.contains(requestMethod.toUpperCase())) {
                loadNoStoreCacheControlHeader(headers);
                return new ResponseEntity<>(headers, HttpStatusCode.valueOf(405));
            }
            final String requestPath = request.getRequestURI();
            logClientIP(request, requestPath);
            final Asset asset;

            if (isRestricted(requestPath)) {
                loadNoStoreCacheControlHeader(headers);
                return new ResponseEntity<>(headers, HttpStatusCode.valueOf(403));
            }

            if (requestPath.startsWith("/api")) {
                return new ResponseEntity<>(headers, HttpStatusCode.valueOf(404));
            } else if (requestPath.matches(ASSET_PATTERN)) {
                asset = assetService.getAsset(request.getRequestURI());
                loadCacheControlHeader(headers, "public, max-age=31536000, immutable");
            } else {
                final SecuredContent securedContent = cspService.getSecuredIndexPage();
                loadCacheControlHeader(headers, "no-store, no-cache, must-revalidate, max-age=0");
                headers.add("Content-Security-Policy", securedContent.header());
                asset = securedContent.asset();
            }
            headers.setContentType(asset.mediaType());
            final ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(asset.data(), headers, HttpStatus.OK);
            LOGGER.info("Response headers of {} : {}", requestPath, responseEntity.getHeaders());
            return responseEntity;
        } catch (Exception e) {
            LOGGER.error("Failed to serve request", e);
            return new ResponseEntity<>(HttpStatusCode.valueOf(500));
        }
    }

    public void logClientIP(HttpServletRequest request, final String requestPath) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        LOGGER.info("Request received from client: {}, Path: {}", ip, requestPath);
    }

    private static String getDomain(final HttpServletRequest request) {
        String scheme = request.getScheme(); // e.g., "http" or "https"
        String serverName = request.getServerName(); // e.g., "example.com"
        int serverPort = request.getServerPort(); // e.g., 80, 443, or custom port

        // If using a non-standard port, include it in the domain.
        if ((scheme.equals("http") && serverPort != 80) ||
                (scheme.equals("https") && serverPort != 443)) {
            return scheme + "://" + serverName + ":" + serverPort;
        } else {
            return "https://" + serverName;
        }
    }

    private void loadDefaultHeaders(final HttpHeaders headers, final String domain) {

        // CORS headers for cross domain requests
        headers.add("Access-Control-Allow-Origin", domain);
        headers.add("Access-Control-Allow-Credentials", "true");
        headers.add("Access-Control-Allow-Methods", "GET, POST");

        //Permission policy headers
        headers.add("Permission-Policy", String.format("camera=(), geolocation=(), microphone=(), payment=(), fullscreen=\"self\", \"%s\"", domain));
    }

    private void loadNoStoreCacheControlHeader(final HttpHeaders headers) {
        headers.add("Cache-Control", "no-store");
    }

    private void loadCacheControlHeader(final HttpHeaders headers, final String value) {
        headers.add("Cache-Control", Objects.requireNonNull(value));
    }

    private boolean isRestricted(String requestPath) {
        return SENSITIVE_FILES.matcher(requestPath).matches()
                || BACKUP_LOG_TEMP_FILES.matcher(requestPath).matches()
                || KEY_CERT_FILES.matcher(requestPath).matches()
                || DATABASE_DUMP_FILES.matcher(requestPath).matches()
                || CICD_FILES.matcher(requestPath).matches()
                || CONFIG_FILES.matcher(requestPath).matches()
                || requestPath.startsWith(DOT_PREFIXED_PATH)
                || RESTRICTED_PATHS.contains(requestPath);
    }

    private ResponseEntity<byte[]> serveSecurelyCachableAsset(final HttpServletRequest request) {
        final Asset asset = assetService.getAsset(request.getRequestURI());
        final HttpHeaders headers = new HttpHeaders();

        headers.setContentType(asset.mediaType());
        loadCacheControlHeader(headers, "no-cache, no-store, must-revalidate");

        return new ResponseEntity<>(asset.data(), headers, HttpStatus.OK);
    }
}
