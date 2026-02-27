"use client";

import { useState, useCallback, useRef, useEffect } from "react";

interface UseInfiniteScrollOptions<T> {
    fetchData: (skip: number, limit: number) => Promise<{ items: T[]; total: number; has_more: boolean }>;
    limit?: number;
    initialItems?: T[];
}

export function useInfiniteScroll<T>({
    fetchData,
    limit = 20,
    initialItems = [],
}: UseInfiniteScrollOptions<T>) {
    const [items, setItems] = useState<T[]>(initialItems);
    const [loading, setLoading] = useState(false);
    const [hasMore, setHasMore] = useState(true);
    const [error, setError] = useState<Error | null>(null);

    // Use refs to avoid closure staleness and unnecessary effect re-runs
    const isFetchingRef = useRef(false);
    const skipRef = useRef(0);
    const hasMoreRef = useRef(true);
    const loaderRef = useRef<HTMLDivElement>(null);

    const loadMore = useCallback(async (isInitial = false) => {
        if (isFetchingRef.current || (!hasMoreRef.current && !isInitial)) return;

        isFetchingRef.current = true;
        setLoading(true);
        const currentSkip = isInitial ? 0 : skipRef.current;

        try {
            const result = await fetchData(currentSkip, limit);
            setItems((prev) => (isInitial ? result.items : [...prev, ...result.items]));

            const newSkip = currentSkip + result.items.length;
            skipRef.current = newSkip;

            hasMoreRef.current = result.has_more;
            setHasMore(result.has_more);
        } catch (err) {
            setError(err instanceof Error ? err : new Error("Failed to fetch data"));
        } finally {
            isFetchingRef.current = false;
            setLoading(false);
        }
    }, [fetchData, limit]);

    const reset = useCallback(() => {
        setItems([]);
        skipRef.current = 0;
        hasMoreRef.current = true;
        setHasMore(true);
        setError(null);
        isFetchingRef.current = false;
        loadMore(true);
    }, [loadMore]);

    useEffect(() => {
        const currentLoader = loaderRef.current;
        if (!currentLoader) return;

        const observer = new IntersectionObserver(
            (entries) => {
                // Use the ref values for the condition to avoid re-binding the observer too often
                if (entries[0].isIntersecting && hasMoreRef.current && !isFetchingRef.current) {
                    loadMore();
                }
            },
            {
                threshold: 0.1, // Trigger slightly earlier
                rootMargin: "100px" // Start loading before it hits the viewport
            }
        );

        observer.observe(currentLoader);
        return () => observer.disconnect();
    }, [loadMore]); // Only re-run if loadMore identity changes (which it shouldn't often)

    return {
        items,
        loading,
        hasMore,
        error,
        loaderRef,
        reset,
        setItems,
    };
}
