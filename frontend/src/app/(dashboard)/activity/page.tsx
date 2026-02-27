"use client";

import React, { useState, useEffect, useCallback } from "react";
import {
    History,
    Receipt,
    ArrowUpRight,
    ArrowDownLeft
} from "lucide-react";
import { cn } from "@/lib/utils";
import { BillsAPI, type Bill } from "@/lib/api";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import { UsersAPI } from "@/lib/api/users";
import { motion } from "framer-motion";
import { BillCard } from "@/components/bills/BillCard";

export default function ActivityPage() {
    const [currentUser, setCurrentUser] = useState<{ id: string } | null>(null);

    useEffect(() => {
        const fetchUser = async () => {
            try {
                const user = await UsersAPI.getCurrentUser();
                setCurrentUser(user);
            } catch (error) {
                console.error("Failed to fetch current user:", error);
            }
        };
        fetchUser();
    }, []);

    const fetchData = useCallback(
        async (skip: number, limit: number) => {
            return await BillsAPI.getUserBills(skip, limit);
        },
        []
    );

    const {
        items: bills,
        loading,
        hasMore,
        loaderRef,
    } = useInfiniteScroll<Bill>({
        fetchData,
        limit: 15,
    });

    return (
        <motion.div
            initial={{ opacity: 0, rotateX: -20, scale: 0.95, y: 30 }}
            animate={{ opacity: 1, rotateX: 0, scale: 1, y: 0 }}
            transition={{ duration: 0.8, ease: [0.4, 0, 0.2, 1] }}
            className="space-y-8 pb-20 max-w-4xl mx-auto [perspective:1200px]"
        >
            {/* Header */}
            <div className="bg-card border border-border rounded-[2.5rem] p-10 relative overflow-hidden group">
                <div className="absolute top-0 right-0 w-64 h-64 bg-primary/5 rounded-full -translate-y-1/2 translate-x-1/2 blur-3xl group-hover:bg-primary/10 transition-colors duration-500" />

                <div className="relative z-10">
                    <div className="flex items-center gap-3 mb-4">
                        <div className="bg-primary/10 p-2.5 rounded-2xl text-primary">
                            <History className="w-5 h-5" />
                        </div>
                        <span className="text-sm font-bold uppercase tracking-widest text-primary/60">Transaction History</span>
                    </div>
                    <h1 className="text-5xl font-black tracking-tight mb-2">Recent <span className="text-primary">Activity</span></h1>
                    <p className="text-muted-foreground text-lg max-w-md">Track every rupee you spend, lend, or owe across all your groups.</p>
                </div>
            </div>

            {/* Activity Feed */}
            <div className="space-y-4">
                {bills.map((item) => (
                    <BillCard
                        key={item.id}
                        bill={item}
                        currentUserId={currentUser?.id}
                        showGroup
                    />
                ))}

                {/* Scroll Sentinel */}
                <div ref={loaderRef} className="h-4" />

                {/* Loading / Status UI */}
                <div className="py-10">
                    {loading ? (
                        <div className="space-y-4">
                            {[1, 2, 3].map((i) => (
                                <div key={i} className="h-24 bg-card border border-border/20 rounded-3xl animate-pulse" />
                            ))}
                        </div>
                    ) : hasMore ? (
                        <div className="flex flex-col items-center gap-4 py-10 opacity-50">
                            <div className="w-8 h-8 border-2 border-primary/20 border-t-primary rounded-full animate-spin" />
                            <p className="text-[10px] font-bold text-muted-foreground uppercase tracking-[0.2em]">Checking for more</p>
                        </div>
                    ) : bills.length === 0 ? (
                        <div className="py-20 text-center border-2 border-dashed border-border/50 rounded-[3rem] bg-secondary/5">
                            <div className="w-20 h-20 bg-secondary/50 rounded-full flex items-center justify-center mx-auto mb-6">
                                <Receipt className="w-10 h-10 text-muted-foreground/30" />
                            </div>
                            <h3 className="text-2xl font-bold mb-2">No activity yet</h3>
                            <p className="text-muted-foreground max-w-xs mx-auto">
                                Start adding bills in your groups to see your financial journey unfold here.
                            </p>
                        </div>
                    ) : (
                        <div className="flex flex-col items-center opacity-40 py-10">
                            <div className="w-px h-12 bg-gradient-to-b from-transparent to-border mb-4" />
                            <p className="text-xs font-black text-muted-foreground uppercase tracking-[0.3em]">End of activity</p>
                        </div>
                    )}
                </div>
            </div>
        </motion.div>
    );
}
