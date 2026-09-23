export interface User {
  userId: number;
  userUuid: string;
  email: string;
  username: string;
  firstName: string;
  lastName: string;
  displayName: string;
  roles: string[];
  lastLoginAt: string;
}

export interface LoginResponse {
  userId: number;
  userUuid: string;
  email: string;
  username: string;
  firstName: string;
  lastName: string;
  displayName: string;
  roles: string[];
  lastLoginAt: string;
}

export interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  setAuth: (response: LoginResponse) => void;
  logout: () => void;
}
