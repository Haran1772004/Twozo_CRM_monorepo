package com.twozo.crm.client.service;

import com.twozo.crm.client.models.Asset;
import com.twozo.crm.client.models.SecuredContent;
import jakarta.annotation.PostConstruct;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class CSPService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CSPService.class);
    private static final String CONNECT_SRC_DIRECTIVE = "connect-src";
    private static final String IMG_SRC_DIRECTIVE = "img-src";
    private static final String MEDIA_SRC_DIRECTIVE = "media-src";
    private static final String SCRIPT_SRC_ELEM_DIRECTIVE = "script-src-elem";

    private final AssetService assetService;
    private final String indexFileName;

    private final boolean isCspEnabled;
    private final boolean isNonceEnabled;

    private String htmlContent = "<h1>Internal Server Error<h1>";
    private String connectSrcDirValue;
    private String mediaSrcDirValue;
    private String imgSrcDirValue;
    private String scriptSrcElemDirValue;
    private String cspHeader;

    public CSPService(@Autowired AssetService assetService, @Value("${twozo.client.index-file}") String indexFileName,
                      @Value("${twozo.csp.enabled}") boolean isCspEnabled,  @Value("${twozo.csp.nonce-enabled}") boolean isNonceEnabled,
                      @Value("${twozo.csp.directive.connect-src}") String connectSrcDirValue,  @Value("${twozo.csp.directive.img-src}") String imgSrcDirValue,
                      @Value("${twozo.csp.directive.media-src}") String mediaSrcDirValue, @Value("${twozo.csp.directive.script-src-elem}") String scriptSrcElemDirValue) {
        this.assetService = assetService;
        this.indexFileName = indexFileName;
        this.isNonceEnabled = isNonceEnabled;
        this.isCspEnabled = isCspEnabled;
        this.connectSrcDirValue = connectSrcDirValue;
        this.mediaSrcDirValue = mediaSrcDirValue;
        this.scriptSrcElemDirValue = scriptSrcElemDirValue;
        this.imgSrcDirValue = imgSrcDirValue;
    }

    @PostConstruct
    public void init() {

        try {
            readHtmlContent();

            if (!isNonceEnabled) {

                this.cspHeader = String.join(" ",
                        "default-src 'self';",
                        "script-src 'self' " + String.join(" ", generateScriptHashes()) + ";",
                        "style-src 'self' 'unsafe-inline';",
                        getDirectiveValue(IMG_SRC_DIRECTIVE, imgSrcDirValue),
                        getDirectiveValue(CONNECT_SRC_DIRECTIVE, connectSrcDirValue),
                        getDirectiveValue(MEDIA_SRC_DIRECTIVE, mediaSrcDirValue),
                        getDirectiveValue(SCRIPT_SRC_ELEM_DIRECTIVE, scriptSrcElemDirValue),
                        "form-action 'self';",
                        "frame-ancestors 'none';"
                );
            }
        } catch (Exception e) {
            LOGGER.error("Failed to init CSP service", e);
            throw new RuntimeException(e);
        }

    }

    private String getDirectiveValue (final String directiveName, final String directiveValue) {
        return String.format("%s %s", directiveName, directiveValue);
    }

    private List<String> generateScriptHashes() throws Exception {
        final List<String> scriptHashes = new ArrayList<String>();

        for (Asset asset : assetService.loadAndGetAssets("static/js/*.js")) {
            final String hash = generateFileHash(asset);

            if (!hash.isEmpty()) {
                scriptHashes.add(hash);
            }
        }

        return scriptHashes;
    }

    private List<String> generateStyleHashes() throws Exception {
        final List<String> styleHashes = new ArrayList<String>();

        for (Asset asset : assetService.loadAndGetAssets("static/css/*.css")) {
            final String cssHash = generateFileHash(asset);

            if (!cssHash.isEmpty()) {
                styleHashes.add(cssHash);
            }
        }

        return styleHashes;
    }

    private void readHtmlContent() throws IOException {
        this.htmlContent = new String(assetService.getAsset(indexFileName).data());
    }

    private String generateFileHash(final Asset asset) throws Exception {
        final byte[] fileContent = asset.data();
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        final byte[] hashBytes = digest.digest(fileContent);

        return "'sha256-" + Base64.getEncoder().encodeToString(hashBytes) + "'";
    }

    public SecuredContent getSecuredIndexPage() {
        final Asset asset = assetService.getAsset(indexFileName);

        if (isCspEnabled) {
            if (isNonceEnabled) {
                final org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(this.htmlContent);
                final String nonce = UUID.randomUUID().toString();

                for (Element element : doc.select("meta")) {
                    if (element.hasAttr("property") && "csp-nonce".equals(element.attr("property"))) {
                        element.attr("content", nonce);
                       // element.attr("nonce", nonce);
                        break;
                    }
                }

                for (Element script : doc.select("script")) {
                    script.attr("nonce", nonce);
                }

                for (Element link : doc.select("link[rel=stylesheet]")) {
                    link.attr("nonce", nonce);
                }

                for (Element style : doc.select("style")) {
                    style.attr("nonce", nonce);
                }

                String cspHeader = String.join(" ",
                        "default-src 'self';",
                        "script-src 'self' 'nonce-" + nonce + "';",  // Add nonce for script-src
                        "style-src 'self' 'unsafe-inline';",   // Add nonce for style-src
                        getDirectiveValue(IMG_SRC_DIRECTIVE, imgSrcDirValue),
                        getDirectiveValue(CONNECT_SRC_DIRECTIVE, connectSrcDirValue),
                        getDirectiveValue(MEDIA_SRC_DIRECTIVE, mediaSrcDirValue),
                        getDirectiveValue(SCRIPT_SRC_ELEM_DIRECTIVE, scriptSrcElemDirValue),
                        "form-action 'self';",
                        "frame-ancestors 'none';"
                );

                final Asset securedAsset = new Asset(asset.path(), doc.html().getBytes(), asset.mediaType());
                return new SecuredContent(securedAsset, cspHeader);
            } else {
                return new SecuredContent(asset, this.cspHeader);
            }
        } else {
            return new SecuredContent(asset, "frame-ancestors 'none';");
        }

    }

}