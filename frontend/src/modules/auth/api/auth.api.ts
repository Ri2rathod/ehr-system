import { apiClient } from "@/services/api-client";
import { LoginResponse, User } from "../types";

export const authApi = {
  login: (credentials: any) => 
    apiClient.post<LoginResponse>("/auth/login", credentials).then(res => res.data),

  register: (payload: any) => 
    apiClient.post("/auth/register", payload).then(res => res.data),

  logout: () => 
    apiClient.post("/auth/logout").then(res => res.data),

  me: () => 
    apiClient.get<User>("/auth/me").then(res => {
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
