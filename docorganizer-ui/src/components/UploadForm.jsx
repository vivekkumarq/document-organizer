import { useRef, useState } from "react";
import API, { errorMessage } from "../api/api";
import { formatBytes } from "../utils/format";

function UploadForm({ knownTags, onUploaded, onError }) {
  const [file, setFile] = useState(null);
  const [tags, setTags] = useState("");
  const [busy, setBusy] = useState(false);
  const [dragging, setDragging] = useState(false);
  const inputRef = useRef(null);

  const reset = () => {
    setFile(null);
    setTags("");
    if (inputRef.current) inputRef.current.value = "";
  };

  const submit = async (event) => {
    event.preventDefault();

    if (!file || busy) return;

    setBusy(true);

    const formData = new FormData();
    formData.append("file", file);

    if (tags.trim()) {
      formData.append("tags", tags.trim());
    }

    try {
      await API.post("/api/documents/upload", formData);
      const name = file.name;
      reset();
      onUploaded(name);
    } catch (err) {
      onError(errorMessage(err, "Upload failed"));
    } finally {
      setBusy(false);
    }
  };

  const onDrop = (event) => {
    event.preventDefault();
    setDragging(false);

    const dropped = event.dataTransfer.files?.[0];
    if (dropped) setFile(dropped);
  };

  return (
    <form className="card" onSubmit={submit}>
      <h2 className="card-title">Upload</h2>

      <div
        className={dragging ? "dropzone dragging" : "dropzone"}
        onDragOver={(event) => {
          event.preventDefault();
          setDragging(true);
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={onDrop}
        onClick={() => inputRef.current?.click()}
        role="button"
        tabIndex={0}
        onKeyDown={(event) => {
          if (event.key === "Enter" || event.key === " ") {
            event.preventDefault();
            inputRef.current?.click();
          }
        }}
      >
        <input
          ref={inputRef}
          type="file"
          className="visually-hidden"
          onChange={(event) => setFile(event.target.files?.[0] ?? null)}
        />

        {file ? (
          <>
            <strong className="dropzone-name">{file.name}</strong>
            <span className="muted small">{formatBytes(file.size)}</span>
          </>
        ) : (
          <>
            <span className="dropzone-icon" aria-hidden="true">
              ↑
            </span>
            <span>
              Drop a file here or <span className="link-look">browse</span>
            </span>
          </>
        )}
      </div>

      <label className="field">
        <span>Tags</span>
        <input
          value={tags}
          onChange={(event) => setTags(event.target.value)}
          placeholder="invoice, 2026, tax"
          list="known-tags"
        />
        <datalist id="known-tags">
          {knownTags.map((tag) => (
            <option key={tag} value={tag} />
          ))}
        </datalist>
        <span className="hint">Comma separated, up to 10 tags.</span>
      </label>

      <div className="row gap">
        <button type="submit" className="btn primary block" disabled={!file || busy}>
          {busy ? "Uploading…" : "Upload"}
        </button>
        {file && !busy && (
          <button type="button" className="btn ghost" onClick={reset}>
            Clear
          </button>
        )}
      </div>
    </form>
  );
}

export default UploadForm;
