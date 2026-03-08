import { useState } from "react";
import UploadForm from "../components/UploadForm";
import DocumentList from "../components/DocumentList";

function Dashboard() {

  const [refresh, setRefresh] = useState(false);

  const reload = () => {
    setRefresh(!refresh);
  };

  const logout = () => {

    localStorage.removeItem("token");

    window.location.reload();
  };

  return (
    <div style={{padding:"30px"}}>

      <h2>Document Dashboard</h2>

      <button onClick={logout}>
        Logout
      </button>

      <hr/>

      <UploadForm onUpload={reload} />

      <hr/>

      <DocumentList refresh={refresh} />

    </div>
  );
}

export default Dashboard;