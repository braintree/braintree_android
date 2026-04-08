(function () {
  "use strict";

  /**
   * Parses the current URL to determine:
   *  - which version is active ("current" or a semver string like "1.0.0")
   *  - the absolute path to the docs root (e.g. "/braintree_android")
   *  - the page path relative to the docs root (e.g. "Card/index.html")
   */
  function getVersionInfo() {
    var pathname = window.location.pathname;

    // Are we inside a prev/VERSION/ directory?
    var prevMatch = pathname.match(/\/prev\/([\w.-]+)\//);
    if (prevMatch) {
      var version = prevMatch[1];
      var prefixEnd = pathname.indexOf("/prev/" + version + "/") + ("/prev/" + version + "/").length;
      var docsRootPath = pathname.substring(0, pathname.indexOf("/prev/" + version + "/"));
      var pagePath = pathname.substring(prefixEnd); // e.g. "Card/index.html"
      return { activeVersion: version, docsRootPath: docsRootPath, pagePath: pagePath };
    }

    // We're in the current (latest) version.
    // Use Dokka's pathToRoot variable to determine depth inside the docs root.
    var pathToRootVal = (typeof pathToRoot !== "undefined") ? pathToRoot : "";
    // Count how many directory levels deep we are (each "../" = 1 level)
    var depth = pathToRootVal ? pathToRootVal.split("/").filter(Boolean).length : 0;
    var segments = pathname.split("/").filter(Boolean);
    // segments = ["repo", "Card", "index.html"]  (depth=1 means 1 folder + filename below root)
    var docsRootSegments = segments.slice(0, segments.length - depth - 1);
    var docsRootPath = docsRootSegments.length ? "/" + docsRootSegments.join("/") : "";
    var pagePath = segments.slice(-(depth + 1)).join("/"); // e.g. "index.html" or "Card/index.html"
    return { activeVersion: "current", docsRootPath: docsRootPath, pagePath: pagePath };
  }

  /**
   * Navigates to targetUrl, falling back to fallbackUrl if targetUrl returns a non-OK response.
   * Uses HEAD to avoid downloading the full page.
   */
  function navigateTo(targetUrl, fallbackUrl) {
    fetch(targetUrl, { method: "HEAD" })
      .then(function (r) {
        window.location.href = r.ok ? targetUrl : fallbackUrl;
      })
      .catch(function () {
        window.location.href = fallbackUrl;
      });
  }

  /** Injects a small <style> block for the dropdown (runs once). */
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

        // Current (latest) option
        var currentOpt = new Option(current + " (latest)", "current");
        currentOpt.selected = info.activeVersion === "current";
        select.appendChild(currentOpt);

        // Previous version options
        prevVersions.forEach(function (v) {
          var opt = new Option(v, v);
          opt.selected = info.activeVersion === v;
          select.appendChild(opt);
        });

        select.addEventListener("change", function () {
          var selected = select.value;
          if (selected === info.activeVersion) return;

          var targetUrl, fallbackUrl;
          if (selected === "current") {
            targetUrl  = info.docsRootPath + "/" + info.pagePath;
            fallbackUrl = info.docsRootPath + "/index.html";
          } else {
            targetUrl  = info.docsRootPath + "/prev/" + selected + "/" + info.pagePath;
            fallbackUrl = info.docsRootPath + "/prev/" + selected + "/index.html";
          }
          navigateTo(targetUrl, fallbackUrl);
        });

        var container = document.getElementById("version-switcher");
        if (container) {
          container.appendChild(select);
        }
      })
      .catch(function (err) {
        console.warn("[version-switcher] Could not load versions.json:", err);
      });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
