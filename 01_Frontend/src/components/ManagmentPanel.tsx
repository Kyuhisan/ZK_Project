import { useEffect, useState } from "react";
import { api } from "../api";
import "./style.css";
import "./management.css";
import Swal from "sweetalert2";
import openrouterImage from '../images/manageGet.png';
import openrouterImage1 from '../images/loginApi.png';
import openrouterImage2 from '../images/createKey.png';
import openrouterImage3 from '../images/createLocalKey.png';
import openrouterImage4 from '../images/key.png';
import openrouterImage5 from '../images/manageKey.png';
import { ChartBar, FileKey2, HeartPulse , BotMessageSquare, Mailbox , ScrollText,  MonitorCog, FolderClock,Wand,History, CalendarClock, Info } from 'lucide-react';

interface FetchLog {
  source: string;
  status: string;
  timeOfFetch: string;
}

interface ManagmentData {
  totalListings: number;
  healthStatus: string;
  fetchLogs: FetchLog[];
  nextAllowedScrapeTime?: string;
  openCount?: number;
  closedCount?: number;
  forthcomingCount?: number;
}
const SIX_HOURS_MS = 6 * 60 * 60 * 1000;

const formatDateTime = (dateString: string): string => {
  const date = new Date(dateString);
  const day = date.getDate().toString().padStart(2, '0');
  const month = (date.getMonth() + 1).toString().padStart(2, '0');
  const year = date.getFullYear();
  const hours = date.getHours().toString().padStart(2, '0');
  const minutes = date.getMinutes().toString().padStart(2, '0');
  const seconds = date.getSeconds().toString().padStart(2, '0');
  
  return `${day}.${month}.${year} ${hours}:${minutes}:${seconds}`;
};

const ManagmentPanel = () => {
  const [data, setData] = useState<ManagmentData | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [canScrape, setCanScrape] = useState<boolean>(true);
  const [timeLeft, setTimeLeft] = useState<string | null>(null);
  const [shortInterval, setShortInterval] = useState<number>(24);
  const [longInterval, setLongInterval] = useState<number>(72);
  const [scraperInterval, setScraperInterval] = useState<number>(48);
  const [scrapingHour, setScrapingHour] = useState<number>(3);
  const [apiKey, setApiKey] = useState<string>("");
  const [isOpen, setIsOpen] = useState(false);

  const fetchData = async () => {
    const res = await api.get("/management/status");
    setData(res.data);
    if (res.data.nextAllowedScrapeTime) {
      const now = new Date();
      const next = new Date(res.data.nextAllowedScrapeTime);
      const diff = next.getTime() - now.getTime();

      if (diff > 0) {
        const mins = Math.floor((diff / 1000 / 60) % 60);
        const hrs = Math.floor(diff / 1000 / 60 / 60);
        setCanScrape(false);
        setTimeLeft(`${hrs}h ${mins}min`);

        const timeout = setTimeout(() => {
          setCanScrape(true);
          setTimeLeft(null);
        }, diff);

        return () => clearTimeout(timeout);
      } else {
        setCanScrape(true);
        setTimeLeft(null);
      }
    } else {
      setCanScrape(true);
      setTimeLeft(null);
    }
  };

  const triggerScraping = async () => {
    setLoading(true);
    try {
      const res = await api.post("/management/geather-now");
      await Swal.fire({
        title: "✅ Success",
        text: res.data,
        icon: "success",
        confirmButtonText: "OK",
        confirmButtonColor: "#00B417",
      });
      await fetchData();
    } catch (err: unknown) {
      const axiosError = err as { response: { status: number; data?: string } };
      if (axiosError.response && axiosError.response.status === 429) {
        await Swal.fire({
          title: "⚠️ Warning",
          text: axiosError.response.data || "Scraping is currently limited.",
          icon: "warning",
          confirmButtonText: "OK",
          confirmButtonColor: "#00B417",
        });
        await fetchData();
      } else {
        await Swal.fire({
          title: "❌ Error",
          text: "Error triggering scraping.",
          icon: "error",
          confirmButtonText: "OK",
          confirmButtonColor: "#00B417",
        });
      }
    } finally {
      setLoading(false);
    }
  };
  const saveIntervals = async () => {
    try {
      await api.post("/management/set-intervals", {
        shortHours: shortInterval,
        longHours: longInterval,
        scrapingHours: scraperInterval,
        scrapingHourOfDay: scrapingHour,
      });
      await Swal.fire({
        title: "✅ Saved",
        text: "Intervals saved successfully.",
        icon: "success",
        confirmButtonText: "OK",
        confirmButtonColor: "#00B417",
      });
    } catch {
      await Swal.fire({
        title: "❌ Error",
        text: "Failed to save intervals.",
        icon: "error",
        confirmButtonText: "OK",
        confirmButtonColor: "#00B417",
      });
    }
  };
  const saveApiKey = async () => {
    try {
      await api.post("/management/set-api-key", { apiKey });
      await Swal.fire({
        title: "✅ API key saved",
        text: "Your new API key is now active.",
        icon: "success",
        confirmButtonText: "OK",
        confirmButtonColor: "#00B417",
      });
    } catch {
      await Swal.fire({
        title: "❌ Error",
        text: "Failed to save API key.",
        icon: "error",
        confirmButtonText: "OK",
        confirmButtonColor: "#00B417",
      });
    }
  };
  const confirmScraping = () => {
  Swal.fire({
    title: "Are you sure?",
    text: "This will fetch the latest listings.",
    icon: "warning",
    showCancelButton: true,
    confirmButtonText: "Yes, fetch them!",
    cancelButtonText: "Cancel",
    confirmButtonColor: "#00B417",
    cancelButtonColor: "#d33",
  }).then((result) => {
    if (result.isConfirmed) {
      triggerScraping();
    }
  });
};

  useEffect(() => {
    fetchData();
  }, []);

  useEffect(() => {
    if (data && data.fetchLogs.length > 0) {
      const lastFetchTime = new Date(data.fetchLogs[0].timeOfFetch).getTime();
      const now = Date.now();
      const diff = now - lastFetchTime;

      if (diff < SIX_HOURS_MS) {
        setCanScrape(false);
        const timeout = setTimeout(() => {
          setCanScrape(true);
        }, SIX_HOURS_MS - diff);
        return () => clearTimeout(timeout);
      } else {
        setCanScrape(true);
      }
    }
  }, [data]);

  return (
    <div className="management-container">
      <h1 className="title"> <ChartBar size={32} /> Management Panel</h1>

      <div className="status-block">
        <div className="status-item">
          <strong><HeartPulse size={20} /> Health Status:</strong>
          <span>{data ? data.healthStatus : "Loading..."}</span>
        </div>
        <div className="status-item">
          <strong className="opnerouter-label"><BotMessageSquare size={20} /> OpenRouter API Chatbot Status:</strong>
          <button
            className="opnerouter-button"
            onClick={() =>
              window.open("https://status.openrouter.ai", "_blank")
            }
          >
            Check Status
          </button>
        </div>
         <div className="status-item api-config-block">
  <button onClick={() => setIsOpen(true)} className="info-button" aria-label="Help">
    <Info size={16} />
  </button>

  <label htmlFor="apiKeyInput" className="api-label">
    <FileKey2 size={20} /> OpenRouter API Key:
  </label>

  <button className="opnerouter-button" onClick={() => window.open("https://openrouter.ai/settings/keys", "_blank")}>
    Get API Key
  </button>

  <br />

  <input
    id="apiKeyInput"
    type="text"
    className="api-input"
    placeholder="Enter API key..."
    value={apiKey}
    onChange={(e) => setApiKey(e.target.value)}
  />
  <button className="api-save-button" onClick={saveApiKey}>
    Save
  </button>
</div>

      {isOpen && (
        <div className="modal-overlay" onClick={() => setIsOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <button className="close-button" onClick={() => setIsOpen(false)}>×</button>
            <h2>Get your API key</h2>

            <div className="step-row">
              <img src={openrouterImage} alt="OpenRouter stran" />
              <div>
                <h3>1. Press on button Get API key on our website</h3>
                <p> and log in on openRouter website </p>
              </div>
            </div>

            <div className="step-row reverse">
              <div>
                <h3>2.  Log in</h3>
                
              </div>
              <img src={openrouterImage1} alt="Primer API ključa" />
            </div>

             <div className="step-row">
              <img src={openrouterImage2} alt="OpenRouter stran" />
              <div>
                <h3>3. Create your key</h3>
                 <p>When you are loged in pres button <strong>Create API key</strong> </p>
              </div>
            </div>
            
            <div className="step-row reverse">
              <div>
                <h3>4.  Give your key a name</h3>
                 <p> press button <strong>Create</strong></p>
              </div>
              <img src={openrouterImage3} alt="Primer API ključa" />
            </div>
            
            <div className="step-row">
              <img src={openrouterImage4} alt="OpenRouter stran" />
              <div>
                <h3>5.Copy your API key</h3>
                 <p>Press copy icon and go back to management panel </p>
              </div>
            </div>

            
            <div className="step-row reverse">
              <div>
                <h3>6.  Paste your key in input field</h3>
                 <p>And press button <strong>Save</strong></p>
              </div>
              <img src={openrouterImage5} alt="Primer API ključa" />
            </div>

          </div>
        </div>
      )}
    
  

      </div>

      <h2 className="listing-data-subtitle">Listing Data</h2>
      <div className="listing-data-block">
        <div className="listing-item">
          <strong> < Mailbox size={20} /> Total Listings:</strong>
          <span>{data ? data.totalListings : "Loading..."}</span>
        </div>
        <hr />
        <div className="listing-item">
          <strong><ScrollText size={20} /> Forthcoming:</strong>
          <span>{data?.forthcomingCount ?? "..."}</span>
        </div>
        <div className="listing-item">
          <strong><ScrollText size={20} />  Open:</strong>
          <span>{data?.openCount ?? "..."}</span>
        </div>
        <div className="listing-item">
          <strong><ScrollText size={20} />  Closed:</strong>
          <span>{data?.closedCount ?? "..."}</span>
        </div>
      </div>
      <h2 className="listing-data-subtitle"> Data Fetching Settings</h2>
      <div className="scrape-block">
        <div className="scrape-left">
          <p className="scrape-text-log"><Wand size={20} /> API retrieve</p>
          <button
            onClick={confirmScraping}
            disabled={loading || !canScrape}
            className="scrape-button"
          >
            {loading ? "Scraping..." : "Get Latest Listings"}
          </button>
          <button
            onClick={() => window.location.reload()}
            className="refresh-button"
          >
            <History size={20} /> Get time for manual scraping
          </button>

          {loading && <p className="loading-text">Scraping in progress...</p>}
          {!loading && !canScrape && timeLeft && (
            <p className="cooldown-text">
              Manual scraping available in: <br/>
              <strong>{timeLeft}</strong>.
            </p>
          )}
        </div>

        <div className="scrape-right">
          <p className="scrape-text-log"> <MonitorCog size={20} /> Interval Settings</p>
          <label className="interval-label">
            <p className="scrape-text">Select time</p>
    Listings will be geathered (between 1–5 AM):
    <select
      className="interval-input"
      value={scrapingHour}
      onChange={(e) => setScrapingHour(Number(e.target.value))}
    >
      {[1, 2, 3, 4, 5].map((h) => (
        <option key={h} value={h}>
          {h.toString().padStart(2, "0")}:00
        </option>
      ))}
    </select>
  </label>
          <label className="interval-label">
            <p className="scrape-text">EcEuropa</p>
            Fetching open and forthcoming Listing <br />
            Short Interval (h):
            <select
              className="interval-input"
              value={shortInterval}
              onChange={(e) => setShortInterval(Number(e.target.value))}
            >
              {Array.from({ length: 10 }, (_, i) => 24 * (i + 1)).map((h) => (
                <option key={h} value={h}>
                  {h}
                </option>
              ))}
            </select>
          </label>

          <label className="interval-label">
            Fetching closed Listing <br />
            Long Interval (h):
            <select
              className="interval-input"
              value={longInterval}
              onChange={(e) => setLongInterval(Number(e.target.value))}
            >
              {Array.from({ length: 9 }, (_, i) => 24 * (i + 1)).map((h) => (
                <option key={h} value={h}>
                  {h}
                </option>
              ))}
            </select>
          </label>
          <label className="interval-label">
            <p className="scrape-text">OnePass and CasscadeFunding</p>
            Scraping all Listings <br />
            Interval (h):
            <select
              className="interval-input"
              value={scraperInterval}
              onChange={(e) => setScraperInterval(Number(e.target.value))}
            >
              {Array.from({ length: 10 }, (_, i) => 24  * (i + 1)).map((h) => (
                <option key={h} value={h}>
                  {h}
                </option>
              ))}
            </select>
          </label>

          <button className="save-interval-button" onClick={saveIntervals}>
            Save Intervals
          </button>
        </div>

        <div className="scrape-log">
          <p className="scrape-text-log"> <FolderClock size={20} /> Fetch Log History</p>
          <table className="log-table">
            <thead>
              <tr>
                <th>Source</th>
                <th>Status</th>
                <th><CalendarClock size={14} /> Time </th>
              </tr>
            </thead>
            <tbody>
              {data ? (
                data.fetchLogs.map((log, idx) => (
                  <tr key={idx}>
                    <td><strong>{log.source}</strong></td>
                    <td>{log.status}</td>
                    <td> {formatDateTime(log.timeOfFetch)}</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={10}>Loading log history...</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default ManagmentPanel;
