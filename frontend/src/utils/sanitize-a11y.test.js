import { describe, it, expect } from "vitest";
import DOMPurify from "dompurify";
import "./sanitize-a11y.js";

describe("sanitize-a11y DOMPurify hook", () => {
  it("fills in alt for an image that has none", () => {
    const html = DOMPurify.sanitize('<img src="https://x/y.png">');
    expect(html).toMatch(/alt="Hình minh họa trong bài học"/);
  });

  it("keeps an author-provided alt untouched", () => {
    const html = DOMPurify.sanitize('<img src="https://x/y.png" alt="Bảng thì quá khứ">');
    expect(html).toMatch(/alt="Bảng thì quá khứ"/);
    expect(html).not.toMatch(/Hình minh họa/);
  });

  it("does not touch non-image markup", () => {
    const html = DOMPurify.sanitize("<p>xoá</p>");
    expect(html).toBe("<p>xoá</p>");
  });
});
