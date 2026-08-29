import { useCallback, useEffect, useState } from "react";
import API, { errorMessage } from "../api/api";
import UploadForm from "../components/UploadForm";
import DocumentList from "../components/DocumentList";
import StorageMeter from "../components/StorageMeter";

const EMPTY_FILTERS = {
  filename: "",
  tag: "",
  contentType: "",
  uploadedAfter: "",
  uploadedBefore: "",
};

function Dashboard({ user, onSignOut }) {
  const [documents, setDocuments] = useState([]);
  const [pageInfo, setPageInfo] = useState({ page: 0, totalPages: 0, totalElements: 0, last: true });
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [page, setPage] = useState(0);
  const [sort, setSort] = useState("uploadedAt");
  const [direction, setDirection] = useState("desc");
  const [stats, setStats] = useState(null);
  const [tags, setTags] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [toast, setToast] = useState(null);

  const size = 8;

  const notify = useCallback((message, kind = "success") => {
    setToast({ message, kind });
  }, []);

  useEffect(() => {
    if (!toast) return undefined;
    const timer = setTimeout(() => setToast(null), 4000);
    return () => clearTimeout(timer);
  }, [toast]);

  const loadDocuments = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const params = { page, size, sort, direction };

      Object.entries(filters).forEach(([key, value]) => {
        if (value) params[key] = value;
      });

      const res = await API.get("/api/documents", { params });

      setDocuments(res.data.content);
      setPageInfo({
        page: res.data.page,
        totalPages: res.data.totalPages,
        totalElements: res.data.totalElements,
        last: res.data.last,
      });
    } catch (err) {
      setError(errorMessage(err, "Could not load your documents"));
      setDocuments([]);
    } finally {
      setLoading(false);
    }
  }, [page, sort, direction, filters]);

  const loadSidebar = useCallback(async () => {
    try {
      const [statsRes, tagsRes] = await Promise.all([
        API.get("/api/documents/stats"),
        API.get("/api/documents/tags"),
      ]);
      setStats(statsRes.data);
      setTags(tagsRes.data);
    } catch {
      // The document list already surfaces connectivity problems; do not double-report.
      setStats(null);
    }
  }, []);

  useEffect(() => {
    loadDocuments();
  }, [loadDocuments]);

  useEffect(() => {
    loadSidebar();
  }, [loadSidebar]);

  const refreshAll = useCallback(() => {
    loadDocuments();
    loadSidebar();
  }, [loadDocuments, loadSidebar]);

  const applyFilters = (next) => {
    setFilters(next);
    setPage(0);
  };

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">
          <span className="logo" aria-hidden="true">
            ◆
          </span>
          <span className="brand-name">Document Organizer</span>
        </div>

        <div className="topbar-right">
          {user && (
            <span className="who" title={user.email}>
              {user.name}
            </span>
          )}
          <button type="button" className="btn ghost" onClick={onSignOut}>
            Sign out
          </button>
        </div>
      </header>

      {toast && (
        <div className={`toast ${toast.kind}`} role="status">
          {toast.message}
        </div>
      )}

      <main className="layout">
        <aside className="sidebar">
          <StorageMeter stats={stats} />
          <UploadForm
            knownTags={tags}
            onUploaded={(name) => {
              notify(`Uploaded ${name}`);
              refreshAll();
            }}
            onError={(message) => notify(message, "error")}
          />
        </aside>

        <section className="content">
          <DocumentList
            documents={documents}
            tags={tags}
            filters={filters}
            onFiltersChange={applyFilters}
            sort={sort}
            direction={direction}
            onSortChange={(nextSort, nextDirection) => {
              setSort(nextSort);
              setDirection(nextDirection);
              setPage(0);
            }}
            pageInfo={pageInfo}
            onPageChange={setPage}
            loading={loading}
            error={error}
            onRetry={loadDocuments}
            onDeleted={(name) => {
              notify(`Deleted ${name}`);
              refreshAll();
            }}
            onError={(message) => notify(message, "error")}
          />
        </section>
      </main>
    </div>
  );
}

export default Dashboard;
