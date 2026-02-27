"use client";

import React, { useEffect } from "react";
import Link from "next/link";
import { motion } from "framer-motion";
import { AlertTriangle, RefreshCcw, Home } from "lucide-react";
import { Button } from "@/components/ui/button";

export default function Error({
    error,
    reset,
}: {
    error: Error & { digest?: string };
    reset: () => void;
}) {
    useEffect(() => {
        // Log the error to an error reporting service
        console.error("Global Catch:", error);
    }, [error]);

    return (
        <div className="min-h-screen bg-background flex items-center justify-center p-6">
            <motion.div
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                className="max-w-md w-full bg-card border border-border rounded-[2.5rem] p-10 shadow-2xl space-y-8 text-center"
            >
                <div className="w-20 h-20 bg-rose-500/10 rounded-3xl flex items-center justify-center mx-auto">
                    <AlertTriangle className="w-10 h-10 text-rose-500" />
                </div>

                <div className="space-y-2">
                    <h2 className="text-2xl font-black tracking-tight">Something went <span className="text-rose-500">wrong</span></h2>
                    <p className="text-muted-foreground text-sm font-medium leading-relaxed italic">
                        &quot;{error.message || "An unexpected system error occurred while processing your request."}&quot;
                    </p>
                </div>

                <div className="flex flex-col gap-3 pt-4">
                    <Button
                        onClick={() => reset()}
                        className="w-full rounded-2xl h-12 text-sm font-bold shadow-xl shadow-primary/20"
                    >
                        <RefreshCcw className="mr-2 w-4 h-4" />
                        Try Again
                    </Button>
                    <Link href="/dashboard">
                        <Button
                            variant="ghost"
                            className="w-full rounded-2xl h-12 text-sm font-bold text-muted-foreground hover:text-foreground"
                        >
                            <Home className="mr-2 w-4 h-4" />
                            Return Home
                        </Button>
                    </Link>
                </div>

                <div className="pt-6">
                    <p className="text-[10px] text-muted-foreground/40 font-black uppercase tracking-[0.4em]">
                        Internal Error ID: {error.digest || "SYSTEM_FAULT"}
                    </p>
                </div>
            </motion.div>
        </div>
    );
}
