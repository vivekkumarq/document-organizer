import { useEffect, useState } from "react";
import API from "../api/api";

function DocumentList({ refresh }) {

  const [documents, setDocuments] = useState([]);

  const loadDocuments = async () => {

    const token = localStorage.getItem("token");

    const res = await API.get("/documents/my", {
      headers: {
        Authorization: "Bearer " + token
      }
    });

    setDocuments(res.data);
  };

  useEffect(() => {
    loadDocuments();
  }, [refresh]);

  const download = (id) => {

    const token = localStorage.getItem("token");

    window.open(
      "http://localhost:8080/documents/download/" + id,
      "_blank"
    );
  };

  return (
    <div>

      <h3>Your Documents</h3>

      {documents.map(doc => (

        <div key={doc.id} style={{marginBottom:"10px"}}>

          {doc.name}

          <button onClick={() => download(doc.id)}>
            Download
          </button>

        </div>

      ))}

    </div>
  );
}

export default DocumentList;