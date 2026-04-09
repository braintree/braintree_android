(function () {
  "use strict";

  /**
   * Parses the current URL to determine:
   *  - which version is active ("current" or a semver string like "1.0.0")
   *  - the absolute path to the docs root (e.g. "/braintree_android")
   *  - the page path relative to the docs root (e.g. "Card/index.html")
   */
  function getVersionInfo() {
    // Normalize: treat trailing slash as pointing to index.html
    var pathname = window.location.pathname.replace(/\/$/, "/index.html");

    var prevMatch = pathname.match(/\/prev\/([\w.-]+)\//);
    if (prevMatch) {
      var version = prevMatch[1];
      var prefix = "/prev/" + version + "/";
      var docsRootPath = pathname.substring(0, pathname.indexOf(prefix));
      var pagePath = pathname.substring(pathname.indexOf(prefix) + prefix.length);
      return { activeVersion: version, docsRootPath: docsRootPath, pagePath: pagePath };
    }

    var pathToRootVal = (typeof pathToRoot !== "undefined") ? pathToRoot : "";
    var depth = pathToRootVal ? pathToRootVal.split("/").filter(Boolean).length : 0;
    var segments = pathname.split("/").filter(Boolean);
    var docsRootSegments = segments.slice(0, segments.length - depth - 1);
    var docsRootPath = docsRootSegments.length ? "/" + docsRootSegments.join("/") : "";
    var pagePath = segments.slice(-(depth + 1)).join("/");
    return { activeVersion: "current", docsRootPath: docsRootPath, pagePath: pagePath };
  }

  /** Injects styles for the dropdown (runs once). */
  function injectStyles() {
    if (document.getElementById("version-switcher-styles")) return;
    var style = document.createElement("style");
    style.id = "version-switcher-styles";
    style.textContent = [
      "#version-switcher { margin-top: 6px; }",
      ".version-switcher-select {",
      "  width: 100%;",
      "  padding: 4px 6px;",
      "  border-radius: 4px;",
      "  border: 1px solid var(--border-color, #ccc);",
      "  background: var(--color-scrollbar, #f5f5f5);",
      "  color: var(--color-text-primary, #333);",
      "  font-size: 0.85rem;",
      "  cursor: pointer;",
      "  box-sizing: border-box;",
      "}",
      ".theme-dark .version-switcher-select {",
      "  background: var(--color-scrollbar, #2b2b2b);",
      "  color: var(--color-text-primary, #eee);",
      "  border-color: var(--border-color, #555);",
      "}"
    ].join("\n");
    document.head.appendChild(style);
  }

  /**
   * Fetches targetUrl and swaps #content and #sideMenu in-place.
   * Falls back to fallbackUrl if the target returns a non-OK response.
   * Updates the browser address bar via history.pushState.
   */
  function swapContent(targetUrl, fallbackUrl, selectedVersion, docsRootPath) {
    fetch(targetUrl)
      .then(function (r) {
        if (!r.ok) {
          if (fallbackUrl && targetUrl !== fallbackUrl) {
            return swapContent(fallbackUrl, null, selectedVersion, docsRootPath);
          }
          throw new Error("Page not found: " + targetUrl);
        }
        return r.text();
      })
      .then(function (html) {
        if (!html) return;
        var parser = new DOMParser();
        var doc = parser.parseFromString(html, "text/html");

        // Swap main content
        var newContent = doc.getElementById("content");
        var oldContent = document.getElementById("content");
        if (newContent && oldContent) {
          oldContent.replaceWith(newContent);
        }

        // Swap sidebar
        var newSideMenu = doc.getElementById("sideMenu");
        var oldSideMenu = document.getElementById("sideMenu");
        if (newSideMenu && oldSideMenu) {
          oldSideMenu.replaceWith(newSideMenu);
        }

        // Update the address bar without reloading
        history.pushState({ version: selectedVersion }, "", targetUrl);

        // Re-run Dokka's navigation highlighter if available
        if (typeof initNavigation === "function") {
          initNavigation();
        }

        // Keep the dropdown in sync — re-inject if it was inside the swapped sidebar
        ensureDropdown(selectedVersion, docsRootPath);
      })
      .catch(function (err) {
        console.warn("[version-switcher] Failed to load page:", err);
      });
  }

  /** Ensures the version-switcher container still exists and has the right value selected. */
  function ensureDropdown(activeVersion, docsRootPath) {
    var container = document.getElementById("version-switcher");
    if (!container) return;
    var select = container.querySelector(".version-switcher-select");
    if (!select) return;
    var options = select.options;
    for (var i = 0; i < options.length; i++) {
      options[i].selected = options[i].value === activeVersion;
    }
  }

  function init() {
    var info = getVersionInfo();
    var versionsJsonUrl = info.docsRootPath + "/versions.json";

    fetch(versionsJsonUrl)
      .then(function (r) {
        if (!r.ok) throw new Error("versions.json not found");
        return r.json();
      })
      .then(function (data) {
        var current = data.current;
        var prevVersions = (data.versions || []).slice().reverse(); // newest first

        injectStyles();

        var select = document.createElement("select");
        select.className = "version-switcher-select";
        select.setAttribute("aria-label", "Select documentation version");

        select.appendChild(new Option(current + " (latest)", "current"));
        prevVersions.forEach(function (v) {
          select.appendChild(new Option(v, v));
        });

        // Set initial selection based on current URL
        ensureDropdown(info.activeVersion, info.docsRootPath);
        // Also set on the freshly created select before it's inserted
        var options = select.options;
        for (var i = 0; i < options.length; i++) {
          options[i].selected = options[i].value === info.activeVersion;
        }

        select.addEventListener("change", function () {
          var selected = select.value;
          var currentInfo = getVersionInfo(); // re-read in case URL changed via popstate
          if (selected === currentInfo.activeVersion) return;

          var targetUrl, fallbackUrl;
          if (selected === "current") {
            targetUrl   = currentInfo.docsRootPath + "/" + currentInfo.pagePath;
            fallbackUrl = currentInfo.docsRootPath + "/index.html";
          } else {
            targetUrl   = currentInfo.docsRootPath + "/prev/" + selected + "/" + currentInfo.pagePath;
            fallbackUrl = currentInfo.docsRootPath + "/prev/" + selected + "/index.html";
          }

          swapContent(targetUrl, fallbackUrl, selected, currentInfo.docsRootPath);
        });

        var container = document.getElementById("version-switcher");
        if (container) {
          container.appendChild(select);
        }
      })
      .catch(function (err) {
        console.warn("[version-switcher] Could not load versions.json:", err);
      });

    // Keep dropdown in sync when user hits back/forward
    window.addEventListener("popstate", function (e) {
      var newInfo = getVersionInfo();
      ensureDropdown(newInfo.activeVersion, newInfo.docsRootPath);
    });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
