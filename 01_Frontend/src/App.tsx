import "./components/style.css";
import Sidebar from "./components/Sidebar";
import MainContent from "./components/MainContent";
import { useEffect, useState } from "react";
import { api } from "./api";
import ManagementPanel from "./components/ManagmentPanel";
import { Route, Routes, BrowserRouter as Router, useLocation } from "react-router-dom";
import { VersionBanner, ExpiredOverlay } from "./components/VersionControl";
import { isExpired } from "./components/utils";

export interface Listing {
  id: string;
  status: string;
  source: string;
  url: string;
  title: string;
  summary: string | null;
  description: string | null;
  deadlineDate: string;
  budget: string;
  industries: string[] | null;
  technologies: string[] | null;
}

function AppContent() {
  const location = useLocation();
  const [filters, setFilters] = useState({
    status: [] as string[],
    source: [] as string[],
    industries: [] as string[],
    budget: { min: 0, max: 100000000 },
    deadLine: { from: "", to: "" },
  });
  const [listings, setListings] = useState<Listing[]>([]);
  
  const allIndustries = listings
    .map((l) => l.industries)
    .filter((ind): ind is string[] => Array.isArray(ind))
    .flat();
  const uniqueIndustries = Array.from(new Set(allIndustries));
  
  useEffect(() => {
    if (isExpired()) return;
    api.get("/listings/show/all")
      .then((response) => setListings(response.data))
      .catch((error) => console.error("Error fetching listings:", error));
  }, []);
  
  const isManagement = location.pathname === "/management";
  
  return (
    <div className="layout">
      <ExpiredOverlay />
      
      {!isExpired() && (
        <>
          {!isManagement && (
            <Sidebar
              filters={filters}
              onFilterChange={setFilters}
              industries={uniqueIndustries}
            />
          )}
          
          <Routes>
            <Route path="/" element={<MainContent filters={filters} listings={listings} />} />
            <Route path="/listings/:id" element={<MainContent filters={filters} listings={listings} />} />
            <Route path="/management" element={<ManagementPanel />} />
          </Routes>
        </>
      )}
    </div>
  );
}

function App() {
  return (
    <Router>
      <VersionBanner />
      <AppContent />
    </Router>
  );
}

export default App;