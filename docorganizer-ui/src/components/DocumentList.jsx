import { useEffect, useState } from "react";
import API, { errorMessage } from "../api/api";
import { formatBytes, formatDate, shortType } from "../utils/format";

const SORT_OPTIONS = [
  { value: "uploadedAt:desc", label: "Newest first" },
  { value: "uploadedAt:asc", label: "Oldest first" },
  { value: "name:asc", label: "Name A–Z" },
  { value: "name:desc", label: "Name Z–A" },
  { value: "sizeBytes:desc", label: "Largest first" },
  { value: "sizeBytes:asc", label: "Smallest first" },
];

function DocumentList({
  documents,
  tags,
  filters,
  onFiltersChange,
  sort,
  direction,
  onSortChange,
  pageInfo,
  onPageChange,
  loading,
  error,
  onRetry,
  onDeleted,
  onError,
}) {
  const [draftName, setDraftName] = useState(filters.filename);
  const [busyId, setBusyId] = useState(null);
  const [confirmId, setConfirmId] = useState(null);
  const [showAdvanced, setShowAdvanced] = useState(false);

  // Debounce the filename box so typing does not fire a request per keystroke.
  useEffect(() => {
    if (draftName === filters.filename) return undefined;

    const timer = setTimeout(() => {
      onFiltersChange({ ...filters, filename: draftName });
    }, 350);

    return () => clearTimeout(timer);
  }, [draftName, filters, onFiltersChange]);

  const setFilter = (key, value) => {
    onFiltersChange({ ...filters, [key]: value });
  };

  const clearAll = () => {
    setDraftName("");
    onFiltersChange({
      filename: "",
      tag: "",
      contentType: "",
      uploadedAfter: "",
      uploadedBefore: "",
    });
  };

  const activeFilters = Object.values(filters).filter(Boolean).length;

  const download = async (doc) => {
    setBusyId(doc.id);

    try {
      // The endpoint needs the bearer token, so the file is fetched as a blob and
      // handed to the browser rather than opened in a new tab.
      const res = await API.get(`/api/documents/${doc.id}/download`, { responseType: "blob" });

      const url = URL.createObjectURL(new Blob([res.data], { type: doc.contentType }));
      const anchor = document.createElement("a");

      anchor.href = url;
      anchor.download = doc.name;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();

      URL.revokeObjectURL(url);
    } catch (err) {
      onError(errorMessage(err, "Download failed"));
    } finally {
      setBusyId(null);
    }
  };

  const remove = async (doc) => {
    setBusyId(doc.id);

    try {
      await API.delete(`/api/documents/${doc.id}`);
      setConfirmId(null);
      onDeleted(doc.name);
    } catch (err) {
      onError(errorMessage(err, "Delete failed"));
    } finally {
      setBusyId(null);
    }
  };

  return (
    <>
      <div className="toolbar">
        <div className="search">
          <span className="search-icon" aria-hidden="true">
            ⌕
          </span>
          <input
            value={draftName}
            onChange={(event) => setDraftName(event.target.value)}
            placeholder="Search by filename…"
            aria-label="Search by filename"
          />
        </div>

        <select
          value={filters.tag}
          onChange={(event) => setFilter("tag", event.target.value)}
          aria-label="Filter by tag"
        >
          <option value="">All tags</option>
          {tags.map((tag) => (
            <option key={tag} value={tag}>
              {tag}
            </option>
          ))}
        </select>

        <select
          value={`${sort}:${direction}`}
          onChange={(event) => {
            const [nextSort, nextDirection] = event.target.value.split(":");
            onSortChange(nextSort, nextDirection);
          }}
          aria-label="Sort order"
        >
          {SORT_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>

        <button
          type="button"
          className={showAdvanced ? "btn ghost active" : "btn ghost"}
          onClick={() => setShowAdvanced(!showAdvanced)}
          aria-expanded={showAdvanced}
        >
          Filters{activeFilters > 0 ? ` (${activeFilters})` : ""}
        </button>
      </div>

      {showAdvanced && (
        <div className="advanced">
          <label className="field inline">
            <span>Type</span>
            <select
              value={filters.contentType}
              onChange={(event) => setFilter("contentType", event.target.value)}
            >
              <option value="">Any</option>
              <option value="application/pdf">PDF</option>
              <option value="image/*">Images</option>
              <option value="text/plain">Plain text</option>
              <option value="text/csv">CSV</option>
            </select>
          </label>

          <label className="field inline">
            <span>From</span>
            <input
              type="date"
              value={filters.uploadedAfter}
              onChange={(event) => setFilter("uploadedAfter", event.target.value)}
            />
          </label>

          <label className="field inline">
            <span>To</span>
            <input
              type="date"
              value={filters.uploadedBefore}
              onChange={(event) => setFilter("uploadedBefore", event.target.value)}
            />
          </label>

          <button type="button" className="btn ghost" onClick={clearAll} disabled={!activeFilters}>
            Clear all
          </button>
        </div>
      )}

      {error && (
        <div className="alert error spread" role="alert">
          <span>{error}</span>
          <button type="button" className="btn ghost" onClick={onRetry}>
            Retry
          </button>
        </div>
      )}

      {loading && (
        <ul className="doc-list">
          {[0, 1, 2].map((key) => (
            <li key={key} className="doc-row">
              <div className="skeleton square" />
              <div className="grow">
                <div className="skeleton line short" />
                <div className="skeleton line tiny" />
              </div>
            </li>
          ))}
        </ul>
      )}

      {!loading && !error && documents.length === 0 && (
        <div className="empty">
          <p className="empty-icon" aria-hidden="true">
            ◇
          </p>
          <h3>{activeFilters ? "No documents match those filters" : "No documents yet"}</h3>
          <p className="muted">
            {activeFilters
              ? "Try widening the search or clearing the filters."
              : "Upload your first file using the panel on the left."}
          </p>
          {activeFilters > 0 && (
            <button type="button" className="btn ghost" onClick={clearAll}>
              Clear filters
            </button>
          )}
        </div>
      )}

      {!loading && !error && documents.length > 0 && (
        <ul className="doc-list">
          {documents.map((doc) => (
            <li key={doc.id} className="doc-row">
              <span className="filetype" data-type={shortType(doc.contentType)}>
                {shortType(doc.contentType)}
              </span>

              <div className="grow">
                <p className="doc-name" title={doc.name}>
                  {doc.name}
                </p>
                <p className="doc-meta">
                  {formatBytes(doc.sizeBytes)} · {formatDate(doc.uploadedAt)}
                </p>
                {doc.tags.length > 0 && (
                  <p className="tag-row">
                    {doc.tags.map((tag) => (
                      <button
                        key={tag}
                        type="button"
                        className="chip clickable"
                        onClick={() => setFilter("tag", tag)}
                        title={`Filter by ${tag}`}
                      >
                        {tag}
                      </button>
                    ))}
                  </p>
                )}
              </div>

              {confirmId === doc.id ? (
                <div className="confirm">
                  <span className="small">Delete permanently?</span>
                  <button
                    type="button"
                    className="btn danger"
                    onClick={() => remove(doc)}
                    disabled={busyId === doc.id}
                  >
                    {busyId === doc.id ? "Deleting…" : "Delete"}
                  </button>
                  <button type="button" className="btn ghost" onClick={() => setConfirmId(null)}>
                    Cancel
                  </button>
                </div>
              ) : (
                <div className="actions">
                  <button
                    type="button"
                    className="btn ghost"
                    onClick={() => download(doc)}
                    disabled={busyId === doc.id}
                  >
                    {busyId === doc.id ? "…" : "Download"}
                  </button>
                  <button
                    type="button"
                    className="btn ghost danger-text"
                    onClick={() => setConfirmId(doc.id)}
                  >
                    Delete
                  </button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}

      {!loading && !error && pageInfo.totalPages > 1 && (
        <nav className="pager" aria-label="Pagination">
          <button
            type="button"
            className="btn ghost"
            onClick={() => onPageChange(pageInfo.page - 1)}
            disabled={pageInfo.page === 0}
          >
            Previous
          </button>

          <span className="small muted">
            Page {pageInfo.page + 1} of {pageInfo.totalPages} · {pageInfo.totalElements} document
            {pageInfo.totalElements === 1 ? "" : "s"}
          </span>

          <button
            type="button"
            className="btn ghost"
            onClick={() => onPageChange(pageInfo.page + 1)}
            disabled={pageInfo.last}
          >
            Next
          </button>
        </nav>
      )}
    </>
  );
}

export default DocumentList;
