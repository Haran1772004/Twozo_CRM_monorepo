package com.tlc.attachment.generator.internal;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.tlc.attachment.storage.status.StorageErrorCodes;
import com.tlc.commons.code.ErrorCode;

/**
 * Singleton service responsible for managing the Playwright client and Chromium browser instance.
 *
 * <p>This service creates and maintains a shared headless Chromium browser instance that can be
 * reused for multiple HTML-to-PDF rendering operations. Keeping a single browser instance avoids
 * the overhead of starting a new Chromium process for every PDF generation request.</p>
 *
 * <p>The browser instance is thread-safe; however, individual {@link com.microsoft.playwright.Page}
 * instances should be created per request and must not be shared across threads.</p>
 *
 * <p>The lifecycle of the browser is managed by this service. Call {@link #stop()} during
 * application shutdown to release browser and Playwright resources.</p>
 *
 * @author Sathish
 * @version 1.0
 */
class PlaywrightService {

    private Playwright playwright;
    private Browser browser;

    private volatile boolean stopped;

    private PlaywrightService() {
    }

    private static final class InstanceHolder {
        private static final PlaywrightService PLAYWRIGHT_SERVICE = new PlaywrightService();
    }

    /**
     * Returns the singleton instance of {@link PlaywrightService}.
     *
     * @return shared Playwright service instance.
     */
    public static PlaywrightService getInstance() {
        return InstanceHolder.PLAYWRIGHT_SERVICE;
    }

    /**
     * Initializes the Playwright runtime and launches a headless Chromium browser instance.
     *
     * <p>The browser executable path points to the Chromium binary installed in the runtime
     * environment (for example, Docker container). The created browser instance is reused for
     * all PDF generation requests handled by this service.</p>
     *
     * @throws ErrorCode if Playwright client creation or browser initialization fails.
     */
    public void init() {
        try {
            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        } catch (final Exception exception) {
            throw ErrorCode.get(StorageErrorCodes.PLAYWRIGHT_CLIENT_CREATION_FAILED, "Failed to initialize Playwright service", exception);
        }
    }

    /**
     * Returns the shared Chromium browser instance.
     *
     * <p>Consumers should create a new {@code Page} from this browser for each rendering
     * operation and close the page after completion.</p>
     *
     * @return initialized Chromium browser instance.
     */
    public Browser browser() {
        return browser;
    }

    /**
     * Stops the Playwright runtime and releases all browser resources.
     *
     * <p>This method closes the Chromium browser process and shuts down the Playwright client.
     * It should be invoked during application shutdown to prevent resource leaks.</p>
     */
    public synchronized void stop() {
        if (stopped) {
            return;
        }
        stopped = true;

        browser.close();
        playwright.close();
    }
}
