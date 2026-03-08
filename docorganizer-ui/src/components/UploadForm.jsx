import { useState } from "react";
import API from "../api/api";

function UploadForm({ onUpload }) {

  const [file, setFile] = useState(null);

  const uploadFile = async () => {

    const formData = new FormData();
    formData.append("file", file);

    const token = localStorage.getItem("token");

    await API.post("/documents/upload", formData, {
      headers: {
        Authorization: "Bearer " + token,
        "Content-Type": "multipart/form-data"
      }
    });

    alert("File uploaded");

    onUpload();
  };

  return (
    <div>

      <h3>Upload Document</h3>

      <input
        type="file"
        onChange={(e) => setFile(e.target.files[0])}
      />

      <button onClick={uploadFile}>
        Upload
      </button>

    </div>
  );
}

export default UploadForm;