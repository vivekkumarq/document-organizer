import axios from "axios";

const TOKEN_KEY = "docorganizer.token";

const API = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080",
});

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }
}

// Attach the bearer token to every outgoing request rather than repeating it per call.
API.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let onUnauthorized = () => {};

export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler;
}

API.interceptors.response.use(
  (response) => response,
  (error) => {
    // An expired or revoked token should drop the session rather than leaving a
    // dashboard on screen where every request fails.
    if (error.response?.status === 401 && getToken()) {
      setToken(null);
      onUnauthorized();
    }
    return Promise.reject(error);
  },
);

/** Pulls a human-readable message out of whatever the backend or the network returned. */
export function errorMessage(error, fallback = "Something went wrong") {
  if (error.response?.data?.message) return error.response.data.message;
  if (error.response?.status === 401) return "Your session has expired. Please sign in again.";
  if (error.code === "ERR_NETWORK") return "Cannot reach the API. Is the backend running?";
  return error.message || fallback;
}

export default API;
