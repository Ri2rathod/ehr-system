import axios from "axios";

const configuredUrl = (process.env.API_URL || "http://localhost:8080").replace(
  /\/+$/,
  ""
);

export const backendApiBaseUrl = configuredUrl.endsWith("/api/v1")
  ? configuredUrl
  : `${configuredUrl}/api/v1`;

/**
 * Server-side client for Next.js route handlers that communicate with Spring Boot.
 * Browser code must use services/api-client instead, so authentication cookies stay
 * on the Next.js origin.
 */
export const backendApi = axios.create({
  baseURL: backendApiBaseUrl,
});
