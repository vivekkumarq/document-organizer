import { formatBytes, shortType } from "../utils/format";

function StorageMeter({ stats }) {
  if (!stats) {
    return (
      <div className="card">
        <h2 className="card-title">Storage</h2>
        <div className="skeleton bar" />
        <div className="skeleton line" />
      </div>
    );
  }

  const percent = Math.min(100, stats.percentUsed);
  const level = percent >= 90 ? "danger" : percent >= 70 ? "warn" : "ok";

  return (
    <div className="card">
      <h2 className="card-title">Storage</h2>

      <div
        className="meter"
        role="progressbar"
        aria-valuenow={Math.round(percent)}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-label="Storage used"
      >
        <div className={`meter-fill ${level}`} style={{ width: `${percent}%` }} />
      </div>

      <p className="meter-caption">
        <strong>{formatBytes(stats.bytesUsed)}</strong> of {formatBytes(stats.quotaBytes)} used
        <span className="muted"> · {formatBytes(stats.bytesRemaining)} free</span>
      </p>

      <dl className="stat-row">
        <div>
          <dt>Files</dt>
          <dd>{stats.totalFiles}</dd>
        </div>
        <div>
          <dt>Used</dt>
          <dd>{percent.toFixed(1)}%</dd>
        </div>
      </dl>

      {stats.byContentType.length > 0 && (
        <>
          <h3 className="card-subtitle">By type</h3>
          <ul className="breakdown">
            {stats.byContentType.map((row) => (
              <li key={row.contentType}>
                <span className="chip small">{shortType(row.contentType)}</span>
                <span className="breakdown-count">
                  {row.fileCount} file{row.fileCount === 1 ? "" : "s"}
                </span>
                <span className="breakdown-size">{formatBytes(row.bytesUsed)}</span>
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  );
}

export default StorageMeter;
