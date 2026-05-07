import { createBrowserRouter, Navigate } from "react-router-dom";
import { LoginPage } from "../pages/LoginPage";
import { ProjectListPage } from "../pages/ProjectListPage";
import { WorkspacePage } from "../pages/WorkspacePage";

function requireAuth(element: JSX.Element) {
  const token = localStorage.getItem("miioo_token") ?? sessionStorage.getItem("miioo_token");
  return token ? element : <Navigate to="/login" replace />;
}

export const router = createBrowserRouter([
  { path: "/login", element: <LoginPage /> },
  { path: "/projects", element: requireAuth(<ProjectListPage />) },
  { path: "/workspace/:projectId", element: requireAuth(<WorkspacePage />) },
  { path: "*", element: <Navigate to="/projects" replace /> },
]);
