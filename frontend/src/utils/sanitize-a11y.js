import DOMPurify from "dompurify";

/**
 * audit-v8 a11y: lesson content and exercise questions are rendered from stored
 * HTML (marked + DOMPurify) with v-html. Scraped images in that content carry no
 * alt attribute, so screen readers announce a bare "graphic" with no context
 * (WCAG 1.1.1). DOMPurify is a singleton, so this one hook covers every sanitize
 * call in the app instead of patching 20+ call sites.
 *
 * Only fills in a missing/empty alt; an author-provided alt is left untouched.
 */
DOMPurify.addHook("afterSanitizeElements", (node) => {
  if (node.nodeName === "IMG" && !node.getAttribute("alt")) {
    node.setAttribute("alt", "Hình minh họa trong bài học");
  }
});
