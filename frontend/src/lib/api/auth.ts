import { API_BASE_URL } from "../http";

export interface LoginResponse {
  access_token: string;
  token_type: string;
}

export interface RefreshTokenRequest {
  refresh_token: string;
}

export const AuthAPI = {
  async login(email: string, password: string): Promise<LoginResponse> {
    const formData = new URLSearchParams();
    formData.append("username", email);
    formData.append("password", password);

    const response = await fetch(`${API_BASE_URL}/auth/login`, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
      },
      body: formData,
    });

    if (!response.ok) {
      let message = "Login failed";
      try {
        const data = await response.json();
        message = data.detail ?? message;
      } catch { }
      throw new Error(message);
    }

    return response.json();
  },

  async logout(token: string): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/auth/logout`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (!response.ok) {
      throw new Error("Logout failed");
    }

    localStorage.removeItem("token");
  },

  async logoutAll(token: string): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/auth/logout-all`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (!response.ok) {
      throw new Error("Logout all sessions failed");
    }

    localStorage.removeItem("token");
  },

  async refresh(refreshToken: string): Promise<LoginResponse> {
    const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ refresh_token: refreshToken }),
    });

    if (!response.ok) {
      throw new Error("Token refresh failed");
    }

    return response.json();
  },
};
