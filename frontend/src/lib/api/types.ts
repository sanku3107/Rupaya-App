// Shared types used across multiple API modules

export interface PaginatedResponse<T> {
    items: T[];
    total: number;
    skip: number;
    limit: number;
    has_more: boolean;
}
