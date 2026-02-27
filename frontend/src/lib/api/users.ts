import { api } from "../http";

export interface User {
    id: string;
    name: string;
    email: string;
}

export interface UserCreate {
    name: string;
    email: string;
    password: string;
}

export interface PasswordChangeRequest {
    old_password: string;
    new_password: string;
}

export const UsersAPI = {
    register(data: UserCreate) {
        return api.post<User>("/users/register", data);
    },

    getCurrentUser() {
        return api.get<User>("/users/me");
    },

    changePassword(data: PasswordChangeRequest) {
        return api.post("/users/change-password", data);
    },

    search(query: string) {
        return api.get<User[]>(`/users/search?q=${encodeURIComponent(query)}`);
    },
};
