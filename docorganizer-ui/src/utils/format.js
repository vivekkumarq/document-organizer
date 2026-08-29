/** Human-readable byte size, e.g. 1536 -> "1.5 KB". */
export function formatBytes(bytes) {
  if (bytes === null || bytes === undefined) return "-";
  if (bytes < 1024) return `${bytes} B`;

  const units = ["KB", "MB", "GB", "TB"];
  let value = bytes / 1024;
  let unit = 0;

  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024;
    unit += 1;
  }

  return `${value.toFixed(value < 10 ? 1 : 0)} ${units[unit]}`;
}

/** Renders the backend's LocalDateTime string in the viewer's locale. */
export function formatDate(value) {
  if (!value) return "-";

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) return value;

  return date.toLocaleString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

const TYPE_LABELS = {
  "application/pdf": "PDF",
  "image/png": "PNG",
  "image/jpeg": "JPG",
  "image/gif": "GIF",
  "image/webp": "WEBP",
  "image/svg+xml": "SVG",
  "text/plain": "TXT",
  "text/csv": "CSV",
  "text/html": "HTML",
  "application/json": "JSON",
  "application/zip": "ZIP",
};

/** Short label for a MIME type, used on the file icon chip. Max four characters. */
export function shortType(contentType) {
  if (!contentType) return "FILE";

  const known = TYPE_LABELS[contentType];
  if (known) return known;

  if (contentType.includes("wordprocessingml") || contentType.includes("msword")) return "DOC";
  if (contentType.includes("spreadsheetml") || contentType.includes("ms-excel")) return "XLS";
  if (contentType.includes("presentationml") || contentType.includes("ms-powerpoint")) return "PPT";

  // Fall back to the subtype, e.g. "audio/ogg" -> "OGG".
  const subtype = contentType.split("/")[1];
  if (subtype) return subtype.replace(/[^a-z0-9]/gi, "").slice(0, 4).toUpperCase();

  return "FILE";
}
