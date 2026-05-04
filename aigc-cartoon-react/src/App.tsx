import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import BasicLayout from './layouts/BasicLayout';
import HomePage from './pages/HomePage';
import ProjectListPage from './pages/ProjectListPage';
import CreationPage from './pages/CreationPage';
import GlobalConfigPage from './pages/GlobalConfigPage';
import WorkflowPage from './pages/WorkflowPage';
import WorkflowProjectPage from './pages/WorkflowProjectPage';
import CartoonWorkflowPage from './pages/CartoonWorkflowPage';
import SubjectPage from './pages/SubjectPage';
import V2LandingPage from './pages/v2/V2LandingPage';
import V2ProjectsPage from './pages/v2/V2ProjectsPage';
import V2ProjectWorkspacePage from './pages/v2/V2ProjectWorkspacePage';
import V2AssetsPage from './pages/v2/V2AssetsPage';
import V2ConfigPage from './pages/v2/V2ConfigPage';

function App() {
  return (
    <ConfigProvider locale={zhCN}>
      <BrowserRouter
        future={{
          v7_startTransition: true,
          v7_relativeSplatPath: true,
        }}
      >
        <Routes>
          <Route path="/" element={<BasicLayout />}>
            <Route index element={<HomePage />} />
            <Route path="projects" element={<ProjectListPage />} />
            <Route path="creation" element={<CreationPage />} />
            <Route path="creation/global" element={<GlobalConfigPage />} />
          </Route>
          <Route path="workflow" element={<WorkflowPage />} />
          <Route path="workflow/project" element={<WorkflowProjectPage />} />
          <Route path="subject" element={<SubjectPage />} />
          <Route path="workflow/cartoon" element={<CartoonWorkflowPage />} />
          <Route path="v2">
            <Route index element={<V2LandingPage />} />
            <Route path="projects" element={<V2ProjectsPage />} />
            <Route path="projects/:projectId" element={<V2ProjectWorkspacePage />} />
            <Route path="assets" element={<V2AssetsPage />} />
            <Route path="config" element={<V2ConfigPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </ConfigProvider>
  );
}

export default App;