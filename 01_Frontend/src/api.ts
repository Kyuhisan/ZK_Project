import axios from "axios";

export const backendUrl = (window as { env?: { API_URL?: string } }).env?.API_URL || "localhost:8080";

export const api = axios.create({
	baseURL: `http://${backendUrl}/api`,
});
