import { apiClient } from "@/services/api-client";
import { LoginResponse, User } from "../types";
import axios from "axios";

export const authApi = {
  login: (credentials: any) =>
    axios
      .post<{ success: boolean; user: LoginResponse }>("/api/auth/login", credentials, {
        withCredentials: true,
      })
      .then((res) => res.data.user),

  register: (payload: any) => 
    apiClient.post("/auth/register", payload).then(res => res.data),

  logout: () => 
    apiClient.post("/auth/logout").then(res => res.data),

  me: () =>
    axios.get<User>("/api/auth/me", { withCredentials: true }).then((res) => {
      const data = res.data;
      // Explicit mapping for enterprise safety
      return {
        userId: data.userId,
        userUuid: data.userUuid,
        email: data.email,
        username: data.username,
        firstName: data.firstName,
        lastName: data.lastName,
        displayName: data.displayName,
        roles: data.roles,
        lastLoginAt: data.lastLoginAt,
      } as User;
    }),
};
