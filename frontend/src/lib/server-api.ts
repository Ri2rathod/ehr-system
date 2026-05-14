import { cookies } from "next/headers";
import axios, { AxiosRequestConfig } from "axios";

const API_URL = process.env.API_URL || "http://localhost:8080";

export async function serverApi(endpoint: string, options: AxiosRequestConfig = {}) {
  const cookieStore = await cookies();
  const accessToken = cookieStore.get("accessToken")?.value;

  const headers = {
    "Content-Type": "application/json",
    ...options.headers,
  } as Record<string, string>;

  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`;
  }

  return axios({
    url: `${API_URL}${endpoint}`,
    ...options,
    headers,
  });
}
